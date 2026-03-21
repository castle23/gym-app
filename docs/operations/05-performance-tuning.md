# Performance Tuning

## Overview

JVM tuning, database optimization, query performance analysis, and bottleneck identification for Gym Platform microservices.

## JVM Tuning

### JVM Arguments

**Development:**
```bash
export JAVA_OPTS="-Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:-UseAdaptiveSizePolicy \
  -XX:InitialHeapSize=512m \
  -XX:MaxHeapSize=1024m"
```

**Production:**
```bash
export JAVA_OPTS="-Xms2G -Xmx4G \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:G1SummarizeRSetStatsPeriod=1 \
  -XX:ConcGCThreads=2 \
  -XX:ParallelGCThreads=8 \
  -XX:StringTableSize=1000003 \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Xloggc:/var/log/gc-%t.log"
```

### GC Configuration

**G1GC (Recommended for microservices):**
```
Pros: Low latency, predictable pause times, good for heaps > 1GB
Cons: Slightly higher overhead than Parallel GC

Configuration:
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200  # Target pause time
-XX:ParallelGCThreads=8    # Match CPU cores
-XX:ConcGCThreads=2        # Concurrent threads
```

**ParallelGC (High throughput):**
```
Pros: Maximum throughput, good for batch processing
Cons: Longer pause times

Configuration:
-XX:+UseParallelGC
-XX:ParallelGCThreads=8
-XX:MaxGCPauseMillis=200
```

### Monitoring GC Performance

```bash
# Get GC statistics
jstat -gc -h10 <pid> 1000  # every 1 second, 10 line header

# Monitor in real-time
jconsole <pid>

# Get GC logs
grep "GC pause" gc-logs/gc-*.log | awk '{print $3}' | sort | tail -100
```

## Database Query Optimization

### Slow Query Analysis

```sql
-- Enable slow query log
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- 1 second
SELECT pg_reload_conf();

-- View slow queries
SELECT query, calls, total_time, mean_time, max_time
FROM pg_stat_statements
WHERE mean_time > 1000  -- > 1 second average
ORDER BY mean_time DESC
LIMIT 20;

-- Reset statistics
SELECT pg_stat_statements_reset();
```

### Query Optimization Tips

```sql
-- ❌ Bad: Missing index
SELECT u.id, u.username FROM users u WHERE u.created_at > NOW() - INTERVAL '7 days';

-- ✓ Good: With index
CREATE INDEX idx_users_created_at ON users(created_at DESC);
SELECT u.id, u.username FROM users u WHERE u.created_at > NOW() - INTERVAL '7 days';

-- ❌ Bad: SELECT *
SELECT * FROM users WHERE role = 'COACH';

-- ✓ Good: Select needed columns
SELECT id, username, email FROM users WHERE role = 'COACH';

-- ❌ Bad: N+1 query problem
users.forEach { u -> getOrders(u.id) }

-- ✓ Good: Single JOIN
SELECT u.id, u.username, o.id FROM users u LEFT JOIN orders o ON u.id = o.user_id;

-- ❌ Bad: Subquery in SELECT
SELECT u.id, (SELECT COUNT(*) FROM orders WHERE user_id = u.id) FROM users;

-- ✓ Good: Aggregate with JOIN
SELECT u.id, COUNT(o.id) FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.id;
```

### Index Strategy

```sql
-- Single column index (common filters)
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_programs_active ON programs(is_active);
CREATE INDEX idx_tracking_user_id ON tracking_data(user_id);

-- Composite index (WHERE + ORDER BY)
CREATE INDEX idx_orders_user_status 
    ON orders(user_id, status) 
    INCLUDE (created_at);

-- Partial index (specific conditions)
CREATE INDEX idx_active_users 
    ON users(id) 
    WHERE is_active = true;

-- BRIN index (large tables, sequential data)
CREATE INDEX idx_metrics_timestamp 
    ON metrics 
    USING BRIN (timestamp);

-- Check missing indexes
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY tablename, indexname;
```

## Connection Pool Tuning

```yaml
spring:
  datasource:
    hikari:
      # Connection pool size
      maximum-pool-size: 20  # Production: 8-32
      minimum-idle: 5
      
      # Timeouts
      connection-timeout: 30000      # 30 seconds
      idle-timeout: 600000           # 10 minutes
      max-lifetime: 1800000          # 30 minutes
      
      # Performance
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
      
      # Advanced
      data-source-properties:
        socketTimeout: 30
        logServerParameterDetails: true
        cacheServerConfiguration: true
        cacheCallableStatements: true
        preparedStatementCacheSizeInKb: 255
```

## Application Performance

### Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}

@Service
public class UserService {

    @Cacheable(value = "users", key = "#id")
    public User getUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

### Batch Processing

```java
@Service
public class BulkOperationService {

    @Autowired
    private UserRepository userRepository;

    // Process 1000 users at a time
    public void processUsersInBatches(Consumer<User> processor) {
        int BATCH_SIZE = 1000;
        int offset = 0;

        while (true) {
            Pageable pageable = PageRequest.of(offset / BATCH_SIZE, BATCH_SIZE);
            Page<User> page = userRepository.findAll(pageable);

            page.getContent().forEach(processor);

            if (!page.hasNext()) break;
            offset += BATCH_SIZE;
        }
    }

    // Bulk insert
    public void bulkInsertUsers(List<User> users) {
        final int BATCH_SIZE = 1000;

        for (int i = 0; i < users.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, users.size());
            userRepository.saveAll(users.subList(i, end));
            userRepository.flush();
        }
    }
}
```

## Network Optimization

### Connection Pooling

```yaml
spring:
  mvc:
    async:
      request-timeout: 60000
  http:
    encoding:
      charset: UTF-8
      force: true
```

### Response Compression

```yaml
server:
  compression:
    enabled: true
    min-response-size: 1024
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
```

## Bottleneck Analysis

### Method-Level Profiling

```java
@Component
@Aspect
@Slf4j
public class PerformanceMonitoringAspect {

    @Around("execution(* com.gym..*Service.*(..))")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        String method = pjp.getSignature().getName();

        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (duration > 1000) {
                log.warn("Slow method detected: {} took {}ms", method, duration);
            } else {
                log.debug("Method {} took {}ms", method, duration);
            }
        }
    }
}
```

### Thread Analysis

```bash
# List threads with CPU usage
jps  # Get process IDs
jstack <pid> | grep "tid" | head -20

# Monitor thread count
jcmd <pid> Thread.print | grep "tid" | wc -l

# Detect deadlocks
jstack <pid> | grep -A5 "Found one Java-level deadlock"
```

## Performance Testing

### Load Testing Script

```bash
#!/bin/bash
# scripts/performance/load-test.sh

SERVICE_URL="http://localhost:8081"
USERS=100
RAMP_TIME="60"
DURATION="300"

echo "Starting load test..."
echo "URL: ${SERVICE_URL}"
echo "Users: ${USERS}"
echo "Ramp time: ${RAMP_TIME}s"
echo "Duration: ${DURATION}s"

# Using Apache JMeter
jmeter -n -t test-plan.jmx \
  -Jservice_url="${SERVICE_URL}" \
  -Jusers="${USERS}" \
  -Jramp_time="${RAMP_TIME}" \
  -Jduration="${DURATION}" \
  -l results.jtl \
  -j jmeter.log

# Analyze results
echo "Response time summary:"
jmeter -g results.jtl -o report/
```

## Performance Tuning Checklist

- [ ] JVM heap size appropriate for workload
- [ ] GC pauses < 100ms in production
- [ ] Database indexes on frequently filtered columns
- [ ] Connection pool size tuned
- [ ] Caching strategy implemented
- [ ] Slow queries identified and optimized
- [ ] N+1 query problems eliminated
- [ ] Response compression enabled
- [ ] Load testing performed
- [ ] Monitoring and alerting configured

## Key References

- [JVM Tuning Guide](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/)
- [PostgreSQL Query Performance](https://www.postgresql.org/docs/current/sql-explain.html)
- [Spring Boot Performance](https://spring.io/blog/2020/05/27/spring-boot-performance)
- See also: [docs/operations/02-monitoring.md](02-monitoring.md)
