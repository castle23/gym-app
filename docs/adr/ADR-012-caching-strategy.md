# ADR-012: Caching Strategy (Redis/In-Memory)

## Status
Accepted

## Date
2026-03-21

## Context

The Gym Platform serves requests that require database lookups:
- Get user profile
- Get workout history
- Get available exercises
- Get tracking statistics

Performance challenges:
1. **Database Queries**: Every request hits PostgreSQL (slow, ~50-100ms)
2. **Repeated Requests**: Same user asks for profile multiple times
3. **Compute Cost**: Complex aggregations (stats, analytics) expensive
4. **Scale**: As users grow, database load increases linearly
5. **Latency**: Users expect fast responses (<200ms)

Example problem:
```
100,000 concurrent users
Each makes 10 requests/minute
Each request hits database (50ms)
Database throughput: 1,000,000 requests/minute
That's 16,666 requests/second!
Database might max out at 5,000 requests/second
→ Response time: 3+ seconds (unacceptable)
```

## Decision

We implemented **Redis caching** with strategic cache layers:

1. **Query Caching**: Cache database query results
2. **Session Caching**: Cache user session tokens (JWT blacklist)
3. **Computed Results**: Cache expensive calculations (stats)
4. **Rate Limit Tracking**: Cache rate limit counters
5. **Cache Invalidation**: TTL + manual invalidation on updates

## Rationale

### 1. Dramatic Performance Improvement
Redis in-memory: ~1ms vs PostgreSQL ~50ms = 50x faster!

```
OLD (No Cache):
  User request → database query (50ms) → response
  
NEW (With Cache):
  User request → Redis lookup (1ms) → response
  (50x faster!)
```

### 2. Reduce Database Load
Cache hits reduce database queries:

```
Before: 100% of requests hit database
After cache: Maybe 80% hit cache, 20% hit database
Result: 80% reduction in database load!
```

### 3. Handle Scale
Enables serving more users without database upgrade:

```
Database capacity: 5,000 requests/second
With cache (80% hit rate): Can serve ~25,000 requests/second!
```

### 4. Enables Complex Features
Some features only feasible with cache:
- Real-time stats dashboards
- Personalized recommendations
- Leaderboards
- Rate limiting (needs fast counters)

### 5. In-Memory Speed
Redis is optimized for speed:
- In-memory (no disk I/O)
- Single-threaded (no locking overhead)
- Optimized data structures (strings, lists, sets, sorted sets)
- Sub-millisecond latency

## Consequences

### Positive
- ✅ 50x performance improvement (1ms vs 50ms)
- ✅ Dramatically reduced database load
- ✅ Enables serving more users
- ✅ Supports advanced features (leaderboards, etc.)
- ✅ Horizontal scaling (multiple Redis instances)
- ✅ Simple to implement

### Negative
- ❌ Additional infrastructure (Redis server)
- ❌ Cache invalidation complexity ("two hard problems")
- ❌ Stale data (eventual consistency)
- ❌ Memory limitations (can't cache everything)
- ❌ Redis single point of failure (needs HA/cluster)
- ❌ Requires careful TTL tuning

## Alternatives Considered

### 1. No Caching
- **Pros**: Simple, always fresh data
- **Cons**: Can't scale, poor performance
- **Why not**: Doesn't work at scale

### 2. Database Query Optimization
- **Pros**: Solves some problems
- **Cons**: Still can't handle peak load, might hit hardware limits
- **Why not**: Complementary but insufficient

### 3. CDN Caching (for static data)
- **Pros**: Works well for static content
- **Cons**: Doesn't help with dynamic user data
- **Why not**: Already use CDN for static assets

### 4. Application-Level In-Memory Cache
- **Pros**: Simple, no external dependency
- **Cons**: Each instance has own cache, cache incoherence, wastes memory
- **Why not**: Doesn't work in Kubernetes (pods restart)

## Related ADRs

- **Depends on**: ADR-001 (Microservices need cache coordination)
- **Related to**: ADR-002 (Cache reduces database load)
- **Related to**: ADR-004 (Redis runs in Kubernetes)
- **Related to**: ADR-011 (Cache sensitive data encrypted)

## Implementation Details

### Redis Deployment

```yaml
# Redis StatefulSet in Kubernetes
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
spec:
  serviceName: redis
  replicas: 1  # Can scale to 3 for high availability
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7
        command:
          - redis-server
          - "--appendonly"
          - "yes"          # Enable persistence
          - "--maxmemory"
          - "4gb"         # Max memory
          - "--maxmemory-policy"
          - "allkeys-lru"  # Evict LRU when max reached
        ports:
        - containerPort: 6379
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "1000m"
        volumeMounts:
        - name: redis-data
          mountPath: /data
  volumeClaimTemplates:
  - metadata:
      name: redis-data
    spec:
      storageClassName: fast-ssd
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 100Gi
```

### Caching Patterns

**1. Cache-Aside Pattern (Lazy Loading)**
```java
public UserProfile getUserProfile(String userId) {
    // Try cache first
    UserProfile cached = cache.get("user:" + userId);
    if (cached != null) {
        return cached;
    }
    
    // Cache miss, fetch from database
    UserProfile profile = db.getUserProfile(userId);
    
    // Store in cache (1 hour TTL)
    cache.set("user:" + userId, profile, 3600);
    
    return profile;
}
```

**2. Write-Through Pattern**
```java
public void updateUserProfile(String userId, UserProfile profile) {
    // Update database first
    db.updateUserProfile(userId, profile);
    
    // Update cache immediately
    cache.set("user:" + userId, profile, 3600);
}
```

**3. Computed Results Caching**
```java
public WorkoutStats getStats(String userId, String period) {
    // Check cache
    String key = "stats:" + userId + ":" + period;
    WorkoutStats cached = cache.get(key);
    if (cached != null) {
        return cached;
    }
    
    // Compute from database
    WorkoutStats stats = db.computeStats(userId, period);
    
    // Cache (depends on period)
    int ttl = period.equals("daily") ? 300 : 3600;
    cache.set(key, stats, ttl);
    
    return stats;
}
```

### Cache Invalidation

```java
// After user updates their profile
public void updateProfile(String userId, UserProfile profile) {
    db.updateUserProfile(userId, profile);
    
    // Invalidate relevant caches
    cache.delete("user:" + userId);
    cache.delete("stats:" + userId + ":*");  // All stats for user
    cache.delete("profile:public:" + userId); // Public profile
}

// When new workout added
public void createWorkout(String userId, Workout workout) {
    db.createWorkout(userId, workout);
    
    // Invalidate caches affected by new workout
    cache.delete("stats:" + userId + ":*");
    cache.delete("workouts:" + userId);
    cache.delete("leaderboard:*");  // Leaderboard might be affected
}
```

### Cache Key Strategy

```
User Profile:       user:{userId}
User Sessions:      session:{sessionId}
Workout History:    workouts:{userId}:{page}
Exercise List:      exercises:{disciplineId}
Leaderboard:        leaderboard:{period}:{limit}
User Stats:         stats:{userId}:{period}
Rate Limit Count:   ratelimit:{userId}:{endpoint}
```

### TTL (Time To Live) Strategy

```
Real-time data (1-5 minutes):
  - Current stats, leaderboard positions
  - User's active sessions
  
Semi-fresh data (15-60 minutes):
  - User profile
  - Exercise lists
  - Historical stats
  
Long-lived data (1-7 days):
  - Static reference data
  - Public content
  - Configuration
```

### Cache Size Planning

```
Expected data:
- 1,000,000 users
- ~2KB per user profile
- Total: ~2GB for all user profiles

Strategy:
- Allocate 4GB Redis (50% safety margin)
- LRU eviction policy (most accessed stay)
- Monitor hit rate (should be > 80%)
- If < 80%, increase cache size or optimize keys
```

### Monitoring Cache Health

```
Metrics to track:
- Cache hit rate (target: > 85%)
- Cache miss rate (target: < 15%)
- Eviction rate (should be low)
- Memory usage (shouldn't exceed 80%)
- Response time (should be < 5ms with cache)

Alerts:
- Hit rate < 70% (cache not effective)
- Memory > 90% (increase or add eviction)
- Eviction rate > 100/second (cache thrashing)
```

### Redis Cluster HA

```yaml
# For production, use Redis Cluster
# Provides:
# - Automatic partitioning
# - High availability
# - Fault tolerance

master-1 ← replicates to → replica-1
master-2 ← replicates to → replica-2
master-3 ← replicates to → replica-3

# Automatic failover if master fails
```

## Cache Warming

```java
// On startup, warm cache with hot data
@PostConstruct
public void warmCache() {
    // Get popular exercises
    List<Exercise> popular = db.getPopularExercises(100);
    for (Exercise e : popular) {
        cache.set("exercise:" + e.getId(), e, 86400); // 24 hours
    }
    
    // Load reference data
    List<Discipline> disciplines = db.getAllDisciplines();
    cache.set("disciplines:all", disciplines, 86400);
}
```

## Future Considerations

- Add Redis Cluster for high availability
- Add Redis Sentinel for automatic failover
- Implement cache warming strategies
- Add cache analysis (which keys, how often accessed)
- Consider separate cache layers (L1 local, L2 distributed)
- Add cache preheating before peak hours
- Monitor cache efficiency metrics closely
