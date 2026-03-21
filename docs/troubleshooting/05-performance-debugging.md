# Performance Debugging

## Overview

This guide covers identifying and resolving performance issues in the Gym Platform microservices. Performance problems can manifest as slow response times, high CPU/memory usage, or database bottlenecks. This guide provides tools, techniques, and systematic approaches to isolate and fix performance issues.

**Gym Platform Performance Targets:**
- API response time: <500ms (p95)
- Database queries: <1000ms
- Memory usage: <80% of allocated
- CPU usage: <70% average
- Request throughput: >1000 requests/second per service

---

## Table of Contents

1. [Performance Analysis Tools](#performance-analysis-tools)
2. [CPU Profiling](#cpu-profiling)
3. [Memory Profiling](#memory-profiling)
4. [Database Performance Analysis](#database-performance-analysis)
5. [Network Performance](#network-performance)
6. [Bottleneck Identification](#bottleneck-identification)
7. [Optimization Techniques](#optimization-techniques)

---

## Performance Analysis Tools

### Spring Boot Actuator Metrics

**Purpose:** Real-time application metrics without external tools

**Access:**
```bash
# Get all available metrics
curl http://localhost:8080/actuator/metrics | jq '.names'

# Get specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.'
```

**Key Metrics:**

1. **Request Latency:**
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements'
# Shows: count, total, max latencies
```

2. **Database Connection Pool:**
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections | jq '.measurements'
# Active, idle, pending connections
```

3. **Memory Usage:**
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.measurements'
# Heap, non-heap memory
```

4. **Garbage Collection:**
```bash
curl http://localhost:8080/actuator/metrics/jvm.gc.max.data.size | jq '.measurements'
```

---

### Prometheus Scraping

**Purpose:** Time-series collection of application metrics

**Configuration:**
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'gym-platform'
    static_configs:
      - targets: ['localhost:8080', 'localhost:8081', 'localhost:8082', 'localhost:8083']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

**Query Examples:**

```bash
# Average response time
curl 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_sum[5m])/rate(http_server_requests_seconds_count[5m])'

# Request rate
curl 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count[5m])'

# Error rate
curl 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count{status=~"5.."}[5m])'
```

---

### Java Flight Recorder (JFR)

**Purpose:** Low-overhead, continuous profiling

**Start recording:**
```bash
docker exec gym-auth jcmd <pid> JFR.start duration=60s filename=/tmp/recording.jfr
```

**View recording:**
- Download recording.jfr
- Open in JDK Mission Control or jmc
- Analyze: CPU usage, memory allocations, lock contention, GC pauses

---

## CPU Profiling

### Identify CPU Hotspots

**Step 1: Start CPU profiling**
```bash
# Get Java process ID
docker exec gym-auth pgrep -f SpringApplication

# Start async-profiler (if available)
docker exec gym-auth \
  java -cp /opt/async-profiler/profiler.jar \
  jvm.Profiler -d 30 -f /tmp/cpu.html <pid>
```

**Step 2: Generate flamegraph**
```bash
# Using JFR
docker exec gym-auth jcmd <pid> JFR.start duration=60s filename=/tmp/profile.jfr

# Convert to flamegraph
# Download from container and process with online flamegraph tools
docker cp gym-auth:/tmp/profile.jfr ./profile.jfr
```

**Step 3: Analyze results**
```
Flamegraph shows:
- Width: Time spent (wider = more CPU time)
- Height: Call stack depth
- Look for widest stacks at top
```

### Example: Optimize Hot Method

**Before profiling detected hot spot:**
```java
// CPU profile showed 15% time in this method
public List<Session> getAllUserSessions(Long userId) {
    List<Session> allSessions = sessionRepository.findAll();
    
    // Inefficient filtering - O(n)
    return allSessions.stream()
        .filter(s -> s.getUser().getId().equals(userId))
        .collect(Collectors.toList());
}
```

**After optimization:**
```java
// Direct query - O(1)
public List<Session> getAllUserSessions(Long userId) {
    return sessionRepository.findByUserId(userId);
}

// Add index to support query
CREATE INDEX idx_sessions_user_id ON training_sessions(user_id);
```

---

## Memory Profiling

### Generate Heap Dump

**Method 1: JVM Command**
```bash
# Force garbage collection first
docker exec gym-auth jcmd <pid> GC.run

# Generate heap dump
docker exec gym-auth jcmd <pid> GC.heap_dump /tmp/heap.hprof

# Copy to host
docker cp gym-auth:/tmp/heap.hprof ./heap.hprof
```

**Method 2: Out-of-memory trigger**
```bash
# Configure automatic heap dump on OOM
docker-compose.yml:
environment:
  - JAVA_OPTS=-Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp
```

### Analyze Heap Dump

**Using Eclipse MAT:**
1. Download and install Eclipse Memory Analyzer
2. Open heap.hprof
3. Reports tab → Leak Suspects report
4. Identify:
   - Largest objects
   - Retained objects
   - Memory leaks

**Using command line:**
```bash
# Find large objects in heap dump
jhat -J-Xmx2g heap.hprof
# Open http://localhost:7000
# Click "Histogram" to see object counts
```

### Example: Memory Leak Fix

**Detected: SessionCache not releasing old entries**

```java
// Before: Memory leak
@Component
public class SessionCache {
    private Map<Long, Session> cache = new HashMap<>();
    
    public void put(Long sessionId, Session session) {
        cache.put(sessionId, session);  // Never removed!
    }
    
    public Session get(Long sessionId) {
        return cache.get(sessionId);
    }
}

// After: Bounded cache with automatic eviction
@Component
public class SessionCache {
    private final LoadingCache<Long, Session> cache;
    
    public SessionCache(SessionRepository repository) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10000)  // Maximum 10k sessions
            .expireAfterWrite(1, TimeUnit.HOURS)  // Expire after 1 hour
            .recordStats()  // Track hits/misses
            .build(sessionId -> 
                repository.findById(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException(sessionId))
            );
    }
    
    public Session get(Long sessionId) {
        return cache.get(sessionId);
    }
}
```

### Monitor Memory Pressure

**Real-time monitoring:**
```bash
# Watch JVM memory
while true; do
    docker exec gym-auth jcmd <pid> VM.memory_usage
    sleep 5
done
```

**Metrics to track:**
```bash
# Used heap percentage
curl http://localhost:8080/actuator/metrics/jvm.memory.usage \
  -H "Accept: application/vnd.spring-boot.actuator.v3+json" | jq '.measurements'

# Expected: Keep <80% of max heap
```

---

## Database Performance Analysis

### Slow Query Identification

**Enable query logging:**
```sql
-- Log queries >1000ms
ALTER DATABASE gym_db SET log_min_duration_statement = 1000;
```

**Find slow queries:**
```bash
# Check PostgreSQL logs
docker logs postgres | grep "duration:" | head -20

# Example output:
# duration: 2547.582 ms execute <unnamed>: SELECT * FROM sessions WHERE ...
```

**Analyze query execution plan:**
```bash
# Get EXPLAIN ANALYZE output
docker exec postgres psql -U gym_user -d gym_db << 'EOF'
EXPLAIN ANALYZE
SELECT s.id, s.name, u.username, COUNT(e.id) as exercise_count
FROM training_sessions s
JOIN users u ON s.user_id = u.id
LEFT JOIN exercises e ON s.id = e.session_id
WHERE s.created_at > NOW() - INTERVAL '7 days'
GROUP BY s.id, u.id
ORDER BY s.created_at DESC
LIMIT 20;
EOF
```

**Interpret EXPLAIN output:**
- **Seq Scan**: Full table scan (slow on large tables)
- **Index Scan**: Using index (fast)
- **Hash Join**: Building in-memory hash table
- **Rows**: Estimated vs actual (if wildly different, update statistics)

### Create Missing Indexes

**Identify missing indexes:**
```sql
-- Find queries doing full table scans
SELECT schemaname, tablename, seq_scan, idx_scan
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan
ORDER BY seq_tup_read DESC
LIMIT 10;
```

**Create indexes:**
```sql
-- Single column index
CREATE INDEX idx_sessions_user_id ON training_sessions(user_id);

-- Composite index (for WHERE + ORDER BY)
CREATE INDEX idx_sessions_user_created 
ON training_sessions(user_id, created_at DESC);

-- Partial index (for filtered queries)
CREATE INDEX idx_active_sessions 
ON training_sessions(user_id) 
WHERE status = 'active';

-- Verify index was used
ANALYZE training_sessions;
```

### Fix N+1 Query Problem

**Detect N+1 queries:**
```properties
# Enable query logging in application.properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

Look for repeated queries like:
```
SELECT * FROM sessions WHERE id = ?  -- Repeated for each session
SELECT * FROM users WHERE id = ?      -- Repeated for each user
```

**Fix with JOIN FETCH:**
```java
// Before: N+1 query
List<Session> sessions = sessionRepository.findAll();
for (Session session : sessions) {
    User user = session.getUser();  // Triggers query for each session!
}

// After: JOIN FETCH
@Query("SELECT DISTINCT s FROM Session s LEFT JOIN FETCH s.user WHERE s.createdAt > :date")
List<Session> findRecentSessions(@Param("date") LocalDateTime date);

// Or use Specification
public List<Session> findRecentSessions(LocalDateTime date) {
    return sessionRepository.findAll((root, query, criteriaBuilder) -> {
        root.fetch("user", JoinType.LEFT);
        return criteriaBuilder.greaterThan(root.get("createdAt"), date);
    });
}
```

### Connection Pool Optimization

**Monitor pool usage:**
```bash
# Real-time monitoring
while true; do
    curl -s http://localhost:8080/actuator/metrics/hikaricp.connections | jq '.measurements'
    sleep 5
done
```

**Optimize pool size:**
```properties
# application.properties
# Pool size = core_count * 2-4 (for I/O bound)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000  # 10 minutes
spring.datasource.hikari.connection-timeout=30000  # 30 seconds
spring.datasource.hikari.max-lifetime=1800000  # 30 minutes
```

**Connection validation:**
```properties
# Test connections before using from pool
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.leak-detection-threshold=60000
```

---

## Network Performance

### Measure Request Latency

**Client-side measurement:**
```bash
# Single request timing
time curl http://localhost:8080/api/training/sessions

# Batch requests with stats
ab -n 1000 -c 10 http://localhost:8080/api/training/sessions
# Shows: requests/sec, min/mean/max/stddev latency
```

**Server-side metrics:**
```bash
# From Prometheus
curl 'http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))'

# p50, p95, p99 latencies
```

### Optimize Response Times

**Reduce payload size:**
```java
// Before: Large DTO returned
@GetMapping("/sessions")
public List<SessionDTO> getSessions() {
    return sessionRepository.findAll()
        .stream()
        .map(SessionDTO::fromEntity)
        .collect(Collectors.toList());
}

// After: Projection with only needed fields
@Query("SELECT new com.gym.dto.SessionCompactDTO(s.id, s.name, s.createdAt) FROM Session s")
List<SessionCompactDTO> getSessions();
```

**Add compression:**
```yaml
# docker-compose.yml
services:
  gym-auth:
    environment:
      - SERVER_COMPRESSION_ENABLED=true
      - SERVER_COMPRESSION_MIME_TYPES=application/json,application/xml,text/html,text/xml,text/plain
      - SERVER_COMPRESSION_MIN_RESPONSE_SIZE=1024
```

**Implement caching:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("sessions", "users", "trainingTypes");
    }
}

@Service
public class TrainingService {
    
    @Cacheable("sessions")
    public Session getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }
}
```

---

## Bottleneck Identification

### Systematic Approach

**1. Identify Slow Operations (Tracing)**
```bash
# Enable Spring Cloud Sleuth for request tracing
docker-compose.yml:
environment:
  - MANAGEMENT_TRACING_BAGGAGE_REMOTE_FIELDS=x-trace-id
```

**2. Measure Each Component**
- Request handling time
- Database query time
- Cache hit ratio
- External service call time

**3. Find Top 3 Bottlenecks**
```bash
# Example: Database is slowest
curl http://localhost:8080/actuator/metrics | grep db

# Example: Specific endpoint is slow
curl 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_sum{uri=~"/api/training.*"}[5m])'
```

**4. Optimize One at a Time**
- Measure before optimization
- Apply fix
- Measure after
- Document improvement

### Common Bottlenecks and Fixes

**Bottleneck: Database queries**
```
Fix: Add indexes, use JOIN FETCH, cache results
Expected improvement: 50-80% latency reduction
```

**Bottleneck: External API calls**
```
Fix: Implement timeout, retry logic, circuit breaker
Expected improvement: 30-50% availability improvement
```

**Bottleneck: Large response payloads**
```
Fix: Use pagination, field selection, compression
Expected improvement: 40-60% bandwidth reduction
```

**Bottleneck: Garbage collection pauses**
```
Fix: Tune JVM GC, reduce object allocation
Expected improvement: 20-40% latency variance reduction
```

---

## Optimization Techniques

### Batch Processing

**Before: One-by-one processing**
```java
public void processAllUsers() {
    List<User> users = userRepository.findAll();
    for (User user : users) {
        // Individual update for each user
        updateUserStatistics(user);
        userRepository.save(user);  // N database writes!
    }
}
```

**After: Batch processing**
```java
@Transactional
public void processAllUsers() {
    List<User> users = userRepository.findAll();
    List<User> usersToUpdate = new ArrayList<>();
    
    for (int i = 0; i < users.size(); i++) {
        User user = users.get(i);
        updateUserStatistics(user);
        usersToUpdate.add(user);
        
        // Batch save every 100 users
        if (i % 100 == 0) {
            userRepository.saveAll(usersToUpdate);
            usersToUpdate.clear();
        }
    }
    
    userRepository.saveAll(usersToUpdate);  // Save remainder
}

// Or use batch updates
@Query("UPDATE User u SET u.sessionCount = u.sessionCount + 1 WHERE u.status = 'active'")
@Modifying
void incrementActiveUserSessions();
```

### Asynchronous Processing

**Before: Synchronous blocking call**
```java
@PostMapping("/training/sessions")
public ResponseEntity<Session> createSession(@RequestBody CreateSessionRequest request) {
    Session session = sessionService.createSession(request);
    
    // Blocks response while sending email
    emailService.sendSessionCreatedEmail(session);
    
    return ResponseEntity.ok(session);
}
```

**After: Asynchronous processing**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class EmailService {
    
    @Async("taskExecutor")
    public CompletableFuture<Void> sendSessionCreatedEmail(Session session) {
        // Runs in thread pool, doesn't block
        emailClient.send(session);
        return CompletableFuture.completedFuture(null);
    }
}

@PostMapping("/training/sessions")
public ResponseEntity<Session> createSession(@RequestBody CreateSessionRequest request) {
    Session session = sessionService.createSession(request);
    
    // Fire and forget
    emailService.sendSessionCreatedEmail(session);
    
    // Responds immediately
    return ResponseEntity.ok(session);
}
```

### Pagination and Streaming

**Before: Load entire dataset**
```java
@GetMapping("/sessions")
public ResponseEntity<List<Session>> getAllSessions() {
    // Loads all sessions into memory - OOM risk!
    return ResponseEntity.ok(sessionRepository.findAll());
}
```

**After: Pagination**
```java
@GetMapping("/sessions")
public ResponseEntity<Page<Session>> getAllSessions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ResponseEntity.ok(sessionRepository.findAll(pageable));
}

// Usage: GET /api/sessions?page=0&size=20
```

### Caching Strategy

**Three-level caching:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    // L1: Local in-memory cache
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("sessions", "users");
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES));
        return manager;
    }
    
    // L2: Redis cache (if configured)
    // L3: Database
}

@Service
public class SessionService {
    
    @Cacheable(value = "sessions", key = "#sessionId")
    public Session getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow();
    }
    
    @CacheEvict(value = "sessions", key = "#sessionId")
    public void updateSession(Long sessionId, UpdateRequest request) {
        Session session = getSession(sessionId);
        session.update(request);
        sessionRepository.save(session);
    }
}
```

---

## Performance Testing

### Load Testing with Apache Bench

```bash
# Simple load test
ab -n 1000 -c 10 http://localhost:8080/api/sessions

# Results show:
# - Requests per second
# - Time per request
# - Transfer rate
# - Percentile latencies
```

### Load Testing with Gatling

```bash
# Create Gatling test
docker run -it gatling/gatling:latest \
  -bm com.example.GymPlatformSimulation \
  -m "/opt/results" \
  -rf "/opt/results"
```

### Monitor During Load Test

```bash
# In another terminal
watch -n 1 'docker stats'

# Also check metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## Performance Checklist

- [ ] API response time < 500ms (p95)
- [ ] Database queries < 1000ms (p95)
- [ ] Memory usage < 80% of allocated
- [ ] CPU usage < 70% average
- [ ] Error rate < 0.1%
- [ ] Connection pool healthy (active < max)
- [ ] No memory leaks (heap grows over time)
- [ ] Cache hit ratio > 70% (if caching used)
- [ ] GC pause time < 100ms
- [ ] Request throughput scales linearly with resources

---

## Related Documentation

- [02-debugging-techniques.md](02-debugging-techniques.md) - Debugging tools overview
- [03-common-issues.md](03-common-issues.md) - Common performance issues
- [04-diagnostic-procedures.md](04-diagnostic-procedures.md) - Diagnostic steps
- [06-security-troubleshooting.md](06-security-troubleshooting.md) - Security diagnostics
- [07-database-troubleshooting.md](07-database-troubleshooting.md) - Database optimization
- docs/operations/05-performance-tuning.md - System-level tuning
- docs/database/query-optimization.md - SQL optimization techniques
