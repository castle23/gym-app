# Common Issues

## Overview

This guide documents the most frequently encountered problems in the Gym Platform microservices environment and their proven solutions. Issues are organized by category: application, database, infrastructure, and deployment. Each entry includes root cause analysis, diagnostic steps, and resolution procedures specific to our Spring Boot + PostgreSQL + Docker architecture.

**Gym Platform Stack Context:**
- Services: Auth, Training, Tracking, Notification (Java 17 Spring Boot 3.x)
- Database: PostgreSQL 15+
- Container Orchestration: Docker Compose
- API Gateway: Spring Boot (port 8080)

---

## Table of Contents

1. [Application Issues](#application-issues)
2. [Database Issues](#database-issues)
3. [Deployment Issues](#deployment-issues)
4. [Network Issues](#network-issues)
5. [Performance Issues](#performance-issues)
6. [Security Issues](#security-issues)

---

## Application Issues

### Issue: Service Not Starting - Port Already in Use

**Symptoms:**
```
ERROR: Cannot assign requested address
java.net.BindException: Address already in use: bind
```

**Root Cause:**
- Another process is using the service's configured port (8080, 8081, etc.)
- Service crashed but port remains bound in TIME_WAIT state
- Multiple instances started simultaneously

**Diagnostic Steps:**

1. Identify process using the port:
```bash
# Linux/macOS
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

2. Check if it's our service:
```bash
docker ps -a | grep auth-service  # or other service
```

3. Verify process details:
```bash
ps aux | grep java
```

**Resolution:**

**Option A - Kill conflicting process (if safe):**
```bash
# Linux/macOS
kill -9 <PID>

# Windows
taskkill /PID <PID> /F
```

**Option B - Change service port (temporary):**
```yaml
# docker-compose.yml
services:
  auth-service:
    ports:
      - "8091:8081"  # Changed host port
    environment:
      - SERVER_PORT=8081  # Internal port remains
```

**Option C - Wait for TIME_WAIT timeout (production workaround):**
```bash
# Increase ephemeral port range temporarily
sysctl -w net.ipv4.ip_local_port_range="1024 65535"
```

**Prevention:**
- Use unique ports per service in docker-compose.yml
- Enable graceful shutdown in Spring Boot (see server.shutdown config)
- Implement health checks to detect port binding issues

**Related Documentation:**
- See [05-performance-debugging.md](05-performance-debugging.md#port-conflicts)
- See docs/stack/05-deployment-docker.md for port configuration

---

### Issue: Out of Memory (OOM) Errors

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
or
Exception in thread "main" java.lang.OutOfMemoryError: Direct buffer memory
```

**Root Cause:**
- JVM heap size too small for workload
- Memory leak in application code
- Unbounded caches or collections
- Large objects not being garbage collected
- Direct ByteBuffer allocation exceeding configured max

**Diagnostic Steps:**

1. Check current JVM memory configuration:
```bash
docker exec gym-auth jps -l
docker exec gym-auth cat /proc/<pid>/status | grep Vm
```

2. Get heap dump (triggers GC first):
```bash
docker exec gym-auth jcmd <pid> GC.heap_dump /tmp/heap.hprof
docker cp gym-auth:/tmp/heap.hprof ./heap.hprof
```

3. Analyze heap dump:
```bash
# Using Eclipse MAT or jhat
jhat heap.hprof
# Visit http://localhost:7000
```

4. Check garbage collection logs:
```bash
docker logs gym-auth | grep GC
```

**Resolution:**

**Increase heap size (short-term fix):**
```yaml
# docker-compose.yml
services:
  gym-auth:
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
```

**Optimize JVM settings:**
```bash
# For 2GB container limit, allocate 60% to heap
-Xmx1200m                    # Max heap
-Xms800m                     # Initial heap
-XX:+UseG1GC                 # Better for >4GB heaps
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=8m
```

**Find and fix memory leaks:**
```bash
# Check for unbounded collections in code
grep -r "new HashMap\|new ArrayList" src/ --include="*.java" | grep -v "Capacity\|initialCapacity"

# Use Spring Boot metrics to monitor memory
curl http://localhost:8080/actuator/metrics/jvm.memory.usage
```

**Implement caching limits:**
```java
// Before: Unbounded cache
private Map<String, User> cache = new HashMap<>();

// After: Bounded cache using Caffeine
private LoadingCache<String, User> cache = 
    Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(userId -> fetchUserFromDB(userId));
```

**Prevention:**
- Set JVM memory limits matching container limits
- Monitor memory usage with Prometheus metrics
- Implement circuit breakers for external calls
- Use bounded collections with eviction policies
- Review heap dump weekly if services restart frequently

**Related Documentation:**
- See docs/operations/05-performance-tuning.md for JVM optimization
- See docs/stack/01-java-spring-boot.md for Spring Boot memory settings

---

### Issue: Application Startup Hangs or Slow Startup

**Symptoms:**
```
Service container runs but doesn't accept connections
Startup takes >60 seconds
Logs show spinning on initialization
```

**Root Cause:**
- Database connectivity issues during initialization
- Large data initialization
- Slow DNS resolution
- Resource contention (CPU, disk I/O)

**Diagnostic Steps:**

1. Check startup logs with timestamps:
```bash
docker logs -f auth-service | tail -100
```

2. Measure startup time:
```bash
time docker-compose up auth-service
# Time from container start to "tomcat started" message
```

3. Check if service accepts connections:
```bash
for i in {1..10}; do
  curl -s http://localhost:8081/auth/actuator/health || echo "Failed $i"
  sleep 5
done
```

4. Check database connectivity:
```bash
docker exec auth-service nc -zv postgres 5432
```

5. Monitor container resources during startup:
```bash
docker stats auth-service --no-stream
```

**Resolution:**

**Add startup health check with retry:**
```yaml
# docker-compose.yml
services:
  auth-service:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/auth/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 30  # 5 minutes total
      start_period: 60s  # Grace period
```

**Fix database connection delay:**
```yaml
# docker-compose.yml
services:
  auth-service:
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      - SPRING_JPA_HIBERNATE_DDL_AUTO=update
      - SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
      - SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
```

**Async initialization for slow operations:**
```java
@Component
public class ApplicationStartup {
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Don't block startup with long operations
        CompletableFuture.runAsync(() -> {
            loadCacheAsync();
            initializeExternalConnections();
        });
    }
    
    private void loadCacheAsync() {
        // This runs after service is ready
        log.info("Loading cache in background...");
    }
}
```

**Lazy loading for expensive resources:**
```java
@Configuration
public class LazyLoadingConfig {
    
    @Bean
    public Supplier<ExpensiveResource> expensiveResourceSupplier() {
        return () -> new ExpensiveResource();  // Loaded on first access
    }
}
```

**Prevention:**
- Use @EventListener(ApplicationReadyEvent.class) for background tasks
- Implement lazy loading for expensive beans
- Monitor startup time with Spring Boot metrics
- Add database connection pooling configuration
- Use database connection verification on startup

**Related Documentation:**
- See docs/deployment/03-health-checks.md for health check configuration
- See docs/troubleshooting/02-debugging-techniques.md for Spring Boot debugging

---

### Issue: NullPointerException in Service Layer

**Symptoms:**
```
java.lang.NullPointerException
  at com.gym.service.TrainingService.createSession(TrainingService.java:45)
```

**Root Cause:**
- Uninitialized dependency injection
- Missing @Autowired or @Inject annotation
- Null value from repository query
- Missing validation on input parameters
- Null return from conditional logic

**Diagnostic Steps:**

1. Identify the exact line:
```bash
docker logs training-service | grep -A 5 "TrainingService.java:45"
```

2. Check if beans are properly initialized:
```bash
# Print all registered beans
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans | keys'
```

3. Enable debug logging:
```properties
# application.properties
logging.level.org.springframework.beans=DEBUG
logging.level.com.gym=DEBUG
```

**Resolution:**

**Ensure proper dependency injection:**
```java
// Before: Incorrect - field never initialized
public class TrainingService {
    private UserRepository userRepository;  // NullPointerException!
}

// After: Constructor injection (preferred)
@Service
public class TrainingService {
    private final UserRepository userRepository;
    
    public TrainingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// Also acceptable: Field injection with @Autowired
@Service
public class TrainingService {
    @Autowired
    private UserRepository userRepository;
}
```

**Validate incoming parameters:**
```java
@Service
public class TrainingService {
    public Session createSession(CreateSessionRequest request) {
        // Before: NullPointerException if request is null
        // After: Fail fast with meaningful error
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        return createSessionInternal(request);
    }
}
```

**Check for null returns:**
```java
public Session getSessionOrThrow(Long sessionId) {
    // Before: Returns null, causing NullPointerException elsewhere
    // After: Throw immediately with context
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new SessionNotFoundException(
            "Session not found: " + sessionId));
    return session;
}
```

**Add null safety with Optional:**
```java
// Before: Returns null
public User findUser(Long userId) {
    return userRepository.findById(userId).orElse(null);
}

// After: Returns Optional for explicit null handling
public Optional<User> findUser(Long userId) {
    return userRepository.findById(userId);
}

// Usage:
Optional<User> user = findUser(123L);
if (user.isPresent()) {
    processUser(user.get());
} else {
    log.warn("User not found");
}
```

**Prevention:**
- Use constructor injection (compile-time safety)
- Enable IDE inspections for @Nullable and @NonNull
- Write unit tests covering null parameter cases
- Use Optional instead of null returns
- Add input validation at controller level

**Related Documentation:**
- See docs/stack/03-api-design-patterns.md for dependency injection patterns
- See docs/troubleshooting/02-debugging-techniques.md for exception analysis

---

### Issue: HTTP 401 Unauthorized on Valid Tokens

**Symptoms:**
```
Request: GET /training/sessions
Response: 401 Unauthorized
Message: "Token has expired" or "Invalid token signature"
```

**Root Cause:**
- Token signature verification failing in API Gateway
- JWT secret misconfigured in gateway
- Token expiration time too short
- Clock skew
- Token tampered with

**Diagnostic Steps:**

1. Decode the JWT token (without validation):
```bash
# Copy token and use jwt.io or:
curl https://jwt.io/ -d "token=eyJhbGciOiJIUzI1NiIs..."

# Or via command line:
echo "eyJhbGciOiJIUzI1NiIs..." | base64 -d
```

2. Decode token claims:
```bash
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.'
```

3. Verify JWT secret in gateway:
```bash
docker exec api-gateway env | grep JWT_SECRET
```

4. Check clock synchronization:
```bash
date
docker exec api-gateway date
docker exec postgres date
```

5. Enable JWT debug logging:
```properties
# application.properties
logging.level.com.gym.security.jwt=DEBUG
```

**Resolution:**

**Ensure JWT secret is set in gateway:**
```yaml
# docker-compose.yml
services:
  api-gateway:
    environment:
      - JWT_SECRET=${JWT_SECRET}
```

> **Note**: JWT validation only happens in the API Gateway. Downstream services trust the `X-User-Id` and `X-User-Roles` headers injected by the gateway.

**Verify JWT configuration:**
```java
@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Bean
    public JwtProvider jwtProvider() {
        return new JwtProvider(jwtSecret, 86400000);  // 24h expiration
    }
}
```

**Check token expiration time:**
```java
public void validateToken(String token) throws JwtException {
    try {
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseClaimsJws(token);
        log.info("Token valid");
    } catch (ExpiredJwtException e) {
        log.error("Token expired at: {}", e.getExpiredDate());
        throw new TokenExpiredException("Token expired");
    } catch (JwtException e) {
        log.error("Invalid token: {}", e.getMessage());
        throw new InvalidTokenException("Invalid token");
    }
}
```

**Fix clock skew (if necessary):**
```java
// Allow 60 second clock skew
@Bean
public JwtProvider jwtProvider() {
    return new JwtProvider(jwtSecret, 86400000) {
        @Override
        protected void validateToken(String token) {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token);
        }
    };
}
```

**Refresh token before expiration:**
```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        Claims claims = jwtProvider.extractClaims(token);
        String newToken = jwtProvider.generateToken(claims);
        return ResponseEntity.ok(new TokenResponse(newToken));
    }
}
```

**Prevention:**
- Keep JWT_SECRET in gateway only
- Set reasonable token expiration (24-48 hours for web, shorter for mobile)
- Implement token refresh endpoint
- Synchronize system clocks via NTP

**Related Documentation:**
- See docs/stack/04-security-framework.md for JWT implementation details
- See docs/security/authentication-security.md for auth best practices

---

## Database Issues

### Issue: Connection Pool Exhausted (HikariPool - Connection is not available)

**Symptoms:**
```
HikariPool - Connection is not available, request timed out after 30000ms
java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available
```

**Root Cause:**
- All database connections in pool are in use and none are released
- Long-running queries holding connections
- Connection leak (not closing statements/results)
- Insufficient pool size for workload
- Stale connections being reclaimed

**Diagnostic Steps:**

1. Check current connection pool status:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections \
  | jq '.measurements'
```

2. Monitor active connections in real-time:
```bash
docker exec postgres psql -U gym_admin -d gym_db -c \
  "SELECT count(*) as active_connections FROM pg_stat_activity;"
```

3. Find long-running queries:
```sql
-- Query running for >5 minutes
SELECT 
    pid,
    usename,
    application_name,
    state,
    query_start,
    NOW() - query_start as duration,
    query
FROM pg_stat_activity
WHERE query_start < NOW() - INTERVAL '5 minutes'
ORDER BY query_start;
```

4. Check pool configuration:
```bash
docker exec auth-service env | grep HIKARI
```

**Resolution:**

**Increase connection pool size:**
```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000  # 10 minutes
spring.datasource.hikari.max-lifetime=1800000  # 30 minutes
```

**Add connection validation:**
```properties
# Ensure connections are alive
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.leak-detection-threshold=60000  # Warn if conn held >1min
```

**Find and fix connection leaks in code:**
```java
// Before: Resource leak
public List<User> getAllUsers() {
    Connection conn = dataSource.getConnection();
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM users");
    return mapResultSet(rs);  // Connection never closed!
}

// After: Try-with-resources (auto close)
public List<User> getAllUsers() {
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
        return mapResultSet(rs);
    } catch (SQLException e) {
        throw new DataAccessException("Failed to fetch users", e);
    }
}

// Best: Use Spring Data JPA (handles pooling)
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAll();
}
```

**Optimize long-running queries:**
```sql
-- Identify problematic query
EXPLAIN ANALYZE
SELECT u.id, u.username, COUNT(s.id) as session_count
FROM users u
LEFT JOIN sessions s ON u.id = s.user_id
GROUP BY u.id;

-- Add index if needed
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
```

**Use query timeouts:**
```yaml
# docker-compose.yml
services:
  auth-service:
    environment:
      - SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_FETCH_SIZE=50
      - SPRING_JPA_PROPERTIES_HIBERNATE_QUERY_TIMEOUT=30
```

**Prevention:**
- Monitor connection pool metrics via Actuator
- Set up alerts for pool exhaustion
- Use Spring Data JPA to prevent manual resource leaks
- Implement request timeouts
- Limit concurrent requests with circuit breakers

**Related Documentation:**
- See docs/operations/05-performance-tuning.md for connection pool optimization
- See docs/database/query-optimization.md for SQL optimization

---

### Issue: Database Deadlock (ERROR: deadlock detected)

**Symptoms:**
```
ERROR: deadlock detected
DETAIL: Process 12345 waits for ShareLock on transaction 67890; blocked by process 98765.
CONTEXT: while updating tuple (0,1) in relation "training_sessions"
```

**Root Cause:**
- Two transactions trying to update same rows in different order
- Circular dependencies between tables
- Long-running transactions
- Missing indexes causing full table locks
- Insufficient transaction isolation level

**Diagnostic Steps:**

1. Check PostgreSQL logs for deadlock:
```bash
docker logs postgres | grep "deadlock"
```

2. View recent deadlocks:
```sql
-- PostgreSQL deadlock log (requires log_statement='all')
SELECT * FROM pg_stat_statements WHERE query LIKE '%UPDATE%';
```

3. Identify blocking transactions:
```sql
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
  AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
  AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
  AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
  AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
  AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
  AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
  AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
  AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
  AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
  AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

**Resolution:**

**Fix transaction ordering (primary solution):**
```java
// Before: Deadlock-prone - inconsistent update order
@Transactional
public void swapUsersSessions(Long userId1, Long userId2) {
    User user1 = userRepository.findById(userId1);
    User user2 = userRepository.findById(userId2);
    
    user1.setSessionCount(user2.getSessionCount());
    user2.setSessionCount(user1.getSessionCount());
    
    userRepository.save(user1);
    userRepository.save(user2);
}

// After: Always update in consistent order (by ID)
@Transactional
public void swapUsersSessions(Long userId1, Long userId2) {
    // Always use same order: lower ID first
    Long minId = Math.min(userId1, userId2);
    Long maxId = Math.max(userId1, userId2);
    
    User userLower = userRepository.findById(minId);
    User userUpper = userRepository.findById(maxId);
    
    // Swap logic
    int temp = userLower.getSessionCount();
    userLower.setSessionCount(userUpper.getSessionCount());
    userUpper.setSessionCount(temp);
    
    userRepository.save(userLower);
    userRepository.save(userUpper);
}
```

**Shorter transaction boundaries:**
```java
// Before: Long transaction increases deadlock risk
@Transactional
public void processLargeDataset() {
    List<User> users = userRepository.findAll();  // Long query
    for (User user : users) {
        performExpensiveOperation(user);  // External service call
        userRepository.save(user);  // Update
    }
}

// After: Split into smaller transactions
public void processLargeDataset() {
    List<Long> userIds = getUserIds();
    for (Long userId : userIds) {
        processSingleUser(userId);  // Each in separate transaction
    }
}

@Transactional
public void processSingleUser(Long userId) {
    User user = userRepository.findById(userId);
    performExpensiveOperation(user);
    userRepository.save(user);
}
```

**Add indexes to prevent full table locks:**
```sql
-- Create indexes on frequently updated columns
CREATE INDEX idx_sessions_user_status 
ON training_sessions(user_id, status);

CREATE INDEX idx_users_last_activity 
ON users(last_activity_timestamp);
```

**Increase isolation level carefully (if needed):**
```properties
# Rarely needed - use only if necessary
spring.jpa.properties.hibernate.connection.isolation=2
# 1 = READ_UNCOMMITTED
# 2 = READ_COMMITTED (default)
# 4 = REPEATABLE_READ
# 8 = SERIALIZABLE (highest, most deadlocks)
```

**Prevention:**
- Always update tables in consistent order (by ID)
- Keep transactions short
- Avoid long-running batch operations in transactions
- Create indexes on frequently updated columns
- Monitor deadlock metrics in application logs
- Use pessimistic locking only when necessary

**Related Documentation:**
- See docs/database/transaction-management.md for transaction best practices
- See docs/operations/04-backup-recovery.md for database recovery procedures

---

### Issue: Slow Queries (Query taking >1 second)

**Symptoms:**
```
Log: Query took 2500ms: SELECT * FROM sessions...
Response time: 5+ seconds
User complaint: Page loads slowly
```

**Root Cause:**
- Missing indexes on frequently queried columns
- N+1 query problem (loading related entities)
- Full table scans on large tables
- Suboptimal join conditions
- Memory pressure causing disk I/O

**Diagnostic Steps:**

1. Enable query logging:
```properties
# application.properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

2. Find slow queries in logs:
```bash
docker logs training-service | grep "Query took" | awk -F'took ' '{print $2}' | sort -rn | head -10
```

3. Use EXPLAIN ANALYZE on suspect queries:
```sql
EXPLAIN ANALYZE
SELECT s.id, s.user_id, s.created_at, u.username
FROM training_sessions s
JOIN users u ON s.user_id = u.id
WHERE s.created_at > NOW() - INTERVAL '7 days'
ORDER BY s.created_at DESC
LIMIT 20;
```

4. Check table statistics:
```sql
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Resolution:**

**Add missing indexes:**
```sql
-- Identify unused indexes
SELECT schemaname, tablename, indexname 
FROM pg_indexes 
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY tablename;

-- Create index on frequently queried column
CREATE INDEX idx_sessions_created_at ON training_sessions(created_at DESC);

-- Composite index for common WHERE + ORDER BY
CREATE INDEX idx_sessions_user_created 
ON training_sessions(user_id, created_at DESC);
```

**Fix N+1 query problem:**
```java
// Before: N+1 - loads all sessions then queries user for each
List<Session> sessions = sessionRepository.findAll();
for (Session session : sessions) {
    User user = session.getUser();  // N+1 queries!
}

// After: Eager loading with JOIN FETCH
@Query("SELECT DISTINCT s FROM Session s LEFT JOIN FETCH s.user WHERE s.createdAt > :date")
List<Session> findRecentSessions(@Param("date") LocalDateTime date);

// Or: Use Spring Data projection
@Query("SELECT new com.gym.dto.SessionDTO(s.id, s.user.username) FROM Session s")
List<SessionDTO> findSessionDTOs();
```

**Optimize JOIN queries:**
```sql
-- Before: Multiple joins without optimization
SELECT s.*, u.*, t.*, e.*
FROM training_sessions s
JOIN users u ON s.user_id = u.id
JOIN training_types t ON s.training_type_id = t.id
LEFT JOIN exercises e ON t.id = e.training_type_id;

-- After: Add indexes and limit results
CREATE INDEX idx_sessions_user_type ON training_sessions(user_id, training_type_id);
CREATE INDEX idx_exercises_type ON exercises(training_type_id);

SELECT s.id, s.name, u.username, t.name as type
FROM training_sessions s
INNER JOIN users u ON s.user_id = u.id
INNER JOIN training_types t ON s.training_type_id = t.id
LIMIT 100;
```

**Tune query execution:**
```properties
# Adjust connection pool for better concurrency
spring.datasource.hikari.maximum-pool-size=20

# Tune Hibernate batch size
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.jdbc.fetch_size=50
```

**Cache frequently accessed data:**
```java
@Service
public class TrainingTypeService {
    
    private final Cache<Long, TrainingType> cache;
    
    public TrainingTypeService() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();
    }
    
    public TrainingType getType(Long typeId) {
        return cache.get(typeId, id -> 
            trainingTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Type not found"))
        );
    }
}
```

**Prevention:**
- Monitor slow query logs regularly
- Use EXPLAIN ANALYZE before deploying new queries
- Index columns used in WHERE, JOIN, and ORDER BY clauses
- Use database query monitoring tools
- Implement query result caching
- Archive old data to reduce table size

**Related Documentation:**
- See docs/database/query-optimization.md for advanced optimization
- See docs/operations/05-performance-tuning.md for JVM and database tuning

---

## Deployment Issues

### Issue: Container Fails to Start - Dependency Not Ready

**Symptoms:**
```
Error: postgres: host and port 5432 failed to respond
Service exits with code 1
Logs: Connection refused
```

**Root Cause:**
- Database not fully started before service attempts connection
- Network not initialized properly in Docker Compose
- health check hasn't passed yet

**Diagnostic Steps:**

1. Check service startup order:
```bash
docker-compose up -d
sleep 5
docker-compose logs
```

2. Test database connectivity:
```bash
docker exec auth-service ping postgres
docker exec auth-service nc -zv postgres 5432
```

3. Verify docker-compose dependencies:
```yaml
docker-compose config | grep -A 5 "depends_on"
```

**Resolution:**

**Configure depends_on with health checks:**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gym_admin -d gym_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    environment:
      - POSTGRES_DB=gym_db
      - POSTGRES_USER=gym_admin
      - POSTGRES_PASSWORD=${DB_PASSWORD}

  auth-service:
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gym_db
      - SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
```

**Connection retry in DataSource configuration:**
```properties
# application.properties
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.initialization-fail-timeout=60000
spring.jpa.hibernate.ddl-auto=update
```

**Prevention:**
- Always use healthchecks in docker-compose
- Use depends_on with condition: service_healthy
- Implement retry logic in services
- Set reasonable startup timeouts
- Test full startup sequence before deployment

**Related Documentation:**
- See docs/deployment/03-health-checks.md for health check configuration
- See docker-compose.yml in project root for full example

---

### Issue: Port Mapping Conflicts Between Services

**Symptoms:**
```
Error: Bind for 0.0.0.0:5432 failed
docker: Error response from daemon: driver failed programming external connectivity
```

**Root Cause:**
- Multiple services configured for same external port
- Port already bound by another application
- Service restart with same port binding attempted

**Diagnostic Steps:**

1. Check docker-compose port configuration:
```bash
docker-compose config | grep -A 2 "ports:"
```

2. List all listening ports:
```bash
netstat -tlnp | grep -E ':(5432|8080|8081|8082|3306|27017)'
```

3. Check which containers use ports:
```bash
docker-compose ps
docker port <container-name>
```

**Resolution:**

**Ensure unique port mappings:**
```yaml
# docker-compose.yml
services:
  postgres:
    ports:
      - "5432:5432"

  api-gateway:
    ports:
      - "8080:8080"

  auth-service:
    ports:
      - "8081:8081"

  training-service:
    ports:
      - "8082:8082"

  tracking-service:
    ports:
      - "8083:8083"

  notification-service:
    ports:
      - "8084:8084"
```

**Prevention:**
- Document port assignments in README
- Use consistent port numbering (8080, 8081, 8082...)
- Use docker-compose override for local development
- Never hardcode ports in services themselves

**Related Documentation:**
- See docker-compose.yml for port configuration examples
- See docs/deployment/01-production-deployment-guide.md for production setup

---

## Network Issues

### Issue: Service-to-Service Communication Fails (Connection Refused)

> **Note**: Services in this platform do not communicate directly with each other. All requests flow through the API Gateway (port 8080). If you see connection refused errors between services, this indicates a misconfiguration.

**Symptoms:**
```
ERROR: Connection refused connecting to http://training-service:8082/...
java.net.ConnectException: Connection refused
```

**Root Cause:**
- Service incorrectly attempting direct service-to-service call
- Should route through API Gateway instead

**Resolution:**

All client requests must go through the API Gateway at port 8080. Services receive `X-User-Id` and `X-User-Roles` headers from the gateway and process requests independently.

**Related Documentation:**
- See docs/troubleshooting/08-network-troubleshooting.md for network diagnostics

---

## Performance Issues

### Issue: High CPU Usage

**Symptoms:**
```
CPU usage: >80% consistently
Service responds slowly
docker stats shows HIGH % of available CPU
```

**Root Cause:**
- Inefficient algorithm causing busy waiting
- Excessive garbage collection (GC pauses)
- Too many threads competing for CPU
- Infinite loops or polling
- External service calls without timeout

**Diagnostic Steps:**

1. Monitor CPU usage:
```bash
docker stats auth-service --no-stream
# Or over time
docker stats auth-service
```

2. Check thread count and GC:
```bash
docker exec auth-service jps -l
docker exec auth-service jcmd <pid> Thread.print | head -100
```

3. Profile CPU usage:
```bash
# Generate CPU flamegraph
docker exec auth-service jcmd <pid> JFR.start duration=60s filename=/tmp/recording.jfr
docker cp auth-service:/tmp/recording.jfr ./recording.jfr
# Analyze with JFR viewer or Async Profiler
```

4. Check for garbage collection issues:
```bash
docker logs auth-service | grep "GC"
```

**Resolution:**

**Profile and optimize hot paths:**
```java
// Before: Inefficient O(n²) algorithm
public List<User> findDuplicateEmails() {
    List<User> users = userRepository.findAll();
    List<User> duplicates = new ArrayList<>();
    
    for (int i = 0; i < users.size(); i++) {
        for (int j = i + 1; j < users.size(); j++) {
            if (users.get(i).getEmail().equals(users.get(j).getEmail())) {
                duplicates.add(users.get(i));
            }
        }
    }
    return duplicates;
}

// After: Optimized O(n) algorithm
public List<User> findDuplicateEmails() {
    Map<String, Integer> emailCount = new HashMap<>();
    List<User> users = userRepository.findAll();
    
    for (User user : users) {
        emailCount.merge(user.getEmail(), 1, Integer::sum);
    }
    
    return users.stream()
        .filter(u -> emailCount.get(u.getEmail()) > 1)
        .collect(Collectors.toList());
}
```

**Optimize garbage collection:**
```yaml
# docker-compose.yml
services:
  auth-service:
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**Reduce thread count:**
```properties
# application.properties
server.tomcat.threads.max=100  # Limit request threads
server.tomcat.threads.min-spare=10

# Limit async executor threads
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
```

**Add timeouts to external calls:**
```java
@Bean
public RestTemplate restTemplate() {
    HttpClientHttpRequestFactory factory = new HttpClientHttpRequestFactory();
    factory.setConnectTimeout(2000);   // 2 second connect timeout
    factory.setReadTimeout(5000);      // 5 second read timeout
    return new RestTemplate(factory);
}

@Service
public class ExternalService {
    
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String callExternal() {
        try {
            return restTemplate.getForObject(
                "http://external-api.com/endpoint",
                String.class);
        } catch (RestClientException e) {
            log.error("External service call failed", e);
            throw new ServiceUnavailableException("External service unavailable");
        }
    }
}
```

**Prevention:**
- Profile regularly with JFR
- Monitor GC metrics via Actuator
- Implement request timeouts
- Use efficient data structures (HashMap vs List)
- Limit thread pools

**Related Documentation:**
- See docs/operations/05-performance-tuning.md for JVM tuning details
- See docs/troubleshooting/05-performance-debugging.md for profiling tools

---

## Security Issues

### Issue: SQL Injection Vulnerability

**Symptoms:**
```
Security scan reports SQL injection risk
Query fails with: "syntax error at or near..."
Log shows: Unexpected SQL keywords in logs
```

**Root Cause:**
- User input concatenated directly into SQL
- No parameterized queries
- Unsafe string formatting in queries

**Diagnostic Steps:**

1. Search for vulnerable patterns:
```bash
grep -r "executeQuery.*\".*+.*\"" src/ --include="*.java"
grep -r "sql = .*+.*request" src/ --include="*.java"
```

2. Check query builder patterns:
```bash
grep -r "createNativeQuery" src/ --include="*.java" | head -10
```

**Resolution:**

**Always use parameterized queries:**
```java
// Before: VULNERABLE - SQL Injection
public User findUserByUsername(String username) {
    String query = "SELECT * FROM users WHERE username = '" + username + "'";
    // Input: admin' OR '1'='1  -> Returns all users!
    return entityManager.createNativeQuery(query, User.class).getSingleResult();
}

// After: SAFE - Parameterized query
public User findUserByUsername(String username) {
    String query = "SELECT * FROM users WHERE username = ?1";
    return entityManager.createNativeQuery(query, User.class)
        .setParameter(1, username)
        .getSingleResult();
}

// Best: Use Spring Data JPA
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
```

**Use query builders safely:**
```java
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    // Safe: Query method
    List<Session> findByUserIdAndStatus(Long userId, String status);
    
    // Safe: @Query with parameters
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.status = :status")
    List<Session> findActiveUserSessions(@Param("userId") Long userId, 
                                         @Param("status") String status);
    
    // Safe: Criteria API
    // specification approach
}
```

**Prevention:**
- Use JPA/Hibernate ORM (prevents injection automatically)
- Never concatenate user input into SQL
- Use PreparedStatements for native queries
- Sanitize and validate input
- Use static code analysis tools to detect vulnerabilities
- Regular security audits

**Related Documentation:**
- See docs/stack/04-security-framework.md for security best practices
- See docs/security/injection-prevention.md for detailed prevention

---

## Related Documentation

This guide complements:
- [02-debugging-techniques.md](02-debugging-techniques.md) - Advanced debugging tools
- [04-diagnostic-procedures.md](04-diagnostic-procedures.md) - Step-by-step diagnosis
- [05-performance-debugging.md](05-performance-debugging.md) - Performance analysis
- [06-security-troubleshooting.md](06-security-troubleshooting.md) - Security issues
- [07-database-troubleshooting.md](07-database-troubleshooting.md) - Database problems
- [08-network-troubleshooting.md](08-network-troubleshooting.md) - Network issues
- [09-deployment-troubleshooting.md](09-deployment-troubleshooting.md) - Deployment problems

See also:
- docs/stack/01-java-spring-boot.md - Java/Spring Boot configuration
- docs/operations/05-performance-tuning.md - System optimization
- docs/security/ - Security documentation
