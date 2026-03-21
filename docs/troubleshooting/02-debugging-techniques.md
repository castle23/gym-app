# Debugging Techniques

## Overview

Comprehensive debugging tools, techniques, remote debugging capabilities, profiling strategies, and debugging workflows for Gym Platform microservices.

## Debugging Hierarchy

```
Level 1: Log Analysis
├─ Application logs
├─ Docker logs
└─ System logs

Level 2: Health Checks & Metrics
├─ Actuator endpoints
├─ Prometheus queries
└─ Docker stats

Level 3: Interactive Debugging
├─ JVM debugger
├─ Database queries
└─ Network tools

Level 4: Advanced Profiling
├─ Flame graphs
├─ Memory dumps
└─ Thread dumps
```

## JVM Debugging

### Remote Debugging Setup

**Start service with debug port:**
```bash
export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
java $JAVA_OPTS -jar auth-service.jar

# Or in docker-compose.yml
services:
  auth-service:
    environment:
      JAVA_OPTS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    ports:
      - "5005:5005"
```

**Connect with IDE (IntelliJ IDEA):**
1. Run → Edit Configurations
2. Add new "Remote"
3. Host: localhost, Port: 5005
4. Debug the application

### Thread Dump Analysis

```bash
# Get thread dump
jstack <pid> > thread_dump.txt

# Or from Docker container
docker exec gym-auth-service jstack 1 > thread_dump.txt

# Analyze for deadlocks
grep -A 5 "Found one Java-level deadlock" thread_dump.txt

# Find blocked threads
grep -B 5 "waiting to lock" thread_dump.txt

# Count thread states
grep "java.lang.Thread.State:" thread_dump.txt | sort | uniq -c
```

### Heap Dump Analysis

```bash
# Generate heap dump
jmap -dump:live,format=b,file=heap.bin <pid>

# From Docker
docker exec gym-auth-service jmap -dump:live,format=b,file=heap.bin 1

# Copy from container
docker cp gym-auth-service:/heap.bin ./heap.bin

# Analyze with Eclipse Memory Analyzer
# Download: https://www.eclipse.org/mat/
# Open heap.bin → Analyze for memory leaks
```

## Logging Investigation

### Enable Debug Logging Temporarily

```yaml
# In application.yml or via Spring environment variables
logging:
  level:
    com.gym: DEBUG
    org.springframework: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Via system property (runtime):**
```bash
java -Dlogging.level.com.gym=DEBUG -jar auth-service.jar
```

**Via environment variable:**
```bash
export SPRING_LOG_LEVEL_COM_GYM=DEBUG
```

### Log Filtering & Analysis

```bash
# Show only errors
docker logs gym-auth-service | grep ERROR

# Show with timestamps
docker logs --timestamps gym-auth-service | tail -100

# Search for specific request
docker logs gym-auth-service | grep "requestId:abc123"

# Count errors by type
docker logs gym-auth-service 2>&1 | grep ERROR | awk -F: '{print $NF}' | sort | uniq -c

# Follow logs in real-time with filter
docker logs -f gym-auth-service | grep --color=auto "ERROR\|WARN\|Exception"
```

## Database Debugging

### Query Analysis

```sql
-- Enable query logging (1 second threshold)
ALTER SYSTEM SET log_min_duration_statement = 1000;
ALTER SYSTEM SET log_statement = 'all';
SELECT pg_reload_conf();

-- View slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
WHERE mean_time > 1000
ORDER BY mean_time DESC;

-- Analyze specific query
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

-- Find missing indexes
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');
```

### Connection Debugging

```sql
-- List all active connections
SELECT pid, usename, datname, state, query
FROM pg_stat_activity
WHERE datname = 'gym_db';

-- Find idle connections
SELECT pid, usename, query, query_start
FROM pg_stat_activity
WHERE state = 'idle'
AND query_start < NOW() - INTERVAL '10 minutes';

-- Kill idle connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
AND query_start < NOW() - INTERVAL '30 minutes';

-- Check for locks
SELECT blocked_locks.pid,
  blocked_statement,
  blocking_locks.pid,
  blocking_statement
FROM pg_stat_statements AS blocked_statement
JOIN pg_stat_activity AS blocked_locks ON (blocked_locks.queryid = blocked_statement.queryid)
JOIN pg_stat_statements AS blocking_statement ON (blocking_locks.queryid = blocking_statement.queryid)
WHERE blocked_locks.pid != blocking_locks.pid;
```

## Profiling

### JProfiler / YourKit Setup

```bash
# Start with profiler agent
export JAVA_OPTS="-agentpath:/path/to/yjpagent.so=port=10001"
java $JAVA_OPTS -jar auth-service.jar
```

### Async Profiler (Lightweight)

```bash
# Record CPU flame graph (60 seconds)
./profiler.sh -d 60 -f cpu.html <pid>

# Record memory allocations
./profiler.sh -d 60 -e alloc -f memory.html <pid>

# Record lock contention
./profiler.sh -d 60 -e lock -f locks.html <pid>

# Analyze results in browser
open cpu.html
```

## Performance Debugging

### Monitor JVM GC

```bash
# Continuous GC monitoring
jstat -gc -h5 <pid> 1000  # every 1 second, header every 5 lines

# Parse GC logs
grep "GC pause" gc.log | awk '{print $3}' | sort -n | tail -20

# Analyze with GCViewer
# Download: https://github.com/chewiebug/GCViewer
```

### CPU Profiling

```bash
# Using perf-map-agent (Linux)
perf record -F 99 -p <pid> -g -- sleep 30
perf script | stackcollapse-perf.pl | flamegraph.pl > flamegraph.svg

# Using Java Flight Recorder (Java 11+)
jcmd <pid> JFR.start filename=recording.jfr duration=60s
jcmd <pid> JFR.dump filename=recording.jfr
```

## API Debugging

### Using curl for Testing

```bash
# Simple GET request
curl -X GET http://localhost:8081/api/v1/users

# POST with data
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"pass"}'

# With authentication
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/v1/users/me

# Verbose output (headers + body)
curl -v http://localhost:8081/api/v1/health

# Save response to file
curl http://localhost:8081/api/v1/users > response.json

# Follow redirects
curl -L http://localhost:8081/api/v1/redirect

# Measure timing
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8081/api/v1/health
```

### Using Postman for Complex Tests

1. Create collection with requests
2. Use environment variables
3. Add tests/assertions
4. Run collection with monitors
5. Export results

### Network Tracing

```bash
# Capture network traffic
tcpdump -i eth0 -w capture.pcap port 5432 or port 8081

# Analyze in Wireshark
wireshark capture.pcap

# Trace HTTP requests
tcpdump -i eth0 -A 'tcp port 8081'
```

## Service Integration Debugging

### Test Service-to-Service Communication

```bash
# From inside container
docker exec gym-auth-service curl -v http://training-service:8082/actuator/health

# Check DNS resolution
docker exec gym-auth-service nslookup training-service

# Test connection timing
docker exec gym-auth-service bash -c 'time curl http://training-service:8082/actuator/health'

# Check open ports
docker exec gym-auth-service netstat -tuln | grep LISTEN
```

## Debugging Tools Reference

| Tool | Purpose | Command |
|------|---------|---------|
| **jps** | List JVM processes | `jps -lmv` |
| **jstat** | JVM statistics | `jstat -gc <pid>` |
| **jstack** | Thread dump | `jstack <pid>` |
| **jmap** | Heap analysis | `jmap -heap <pid>` |
| **jcmd** | JVM commands | `jcmd <pid> help` |
| **docker logs** | Container logs | `docker logs -f <container>` |
| **curl** | HTTP testing | `curl -v http://...` |
| **tcpdump** | Network capture | `tcpdump -A port 8081` |

## Key References

- [Java Platform Debugger Architecture](https://docs.oracle.com/en/java/javase/17/docs/specs/jpda/conninv.html)
- [JVM Internals Guide](https://chriswhocodes.com/hsdis/)
- [PostgreSQL Query Planning](https://www.postgresql.org/docs/current/using-explain.html)
- See also: [docs/operations/02-monitoring.md](../operations/02-monitoring.md)
- See also: [docs/troubleshooting/05-performance-debugging.md](05-performance-debugging.md)
