# Diagnostic Procedures

## Overview

This guide provides systematic, step-by-step procedures for diagnosing issues in the Gym Platform microservices environment. Each procedure is designed to isolate problems quickly and gather the necessary information for resolution. Procedures are organized by system component and include specific commands for the Gym Platform architecture.

**Gym Platform Stack:**
- Services: Auth, Training, Tracking, Notification (Java 17, Spring Boot 3.x)
- Persistence: PostgreSQL 15+
- Messaging: RabbitMQ
- Monitoring: Prometheus, Grafana, ELK Stack
- Orchestration: Docker Compose

---

## Table of Contents

1. [General System Health](#general-system-health)
2. [Application Diagnostics](#application-diagnostics)
3. [Database Diagnostics](#database-diagnostics)
4. [Network Diagnostics](#network-diagnostics)
5. [Container Diagnostics](#container-diagnostics)
6. [Performance Diagnostics](#performance-diagnostics)
7. [Security Diagnostics](#security-diagnostics)

---

## General System Health

### Quick System Status Check

**Objective:** Verify overall system health and identify critical issues

**Time Required:** 2-3 minutes

**Steps:**

1. **Check all containers are running:**
```bash
docker-compose ps
# Expected: All services in "Up" state
```

2. **Verify database connectivity:**
```bash
docker exec gym-auth curl -s http://localhost:8080/actuator/health | jq .
# Expected: status = "UP"
```

3. **Check logs for recent errors:**
```bash
docker-compose logs --tail=50 | grep -i "error\|exception\|fail"
```

4. **Verify network connectivity:**
```bash
docker exec gym-auth ping gym-training
docker exec gym-auth ping postgres
docker exec gym-auth ping rabbitmq
```

5. **Monitor resource usage:**
```bash
docker stats --no-stream
```

**Success Criteria:**
- All containers: Up
- Health check: UP
- No critical errors in logs
- All containers responding to health checks
- CPU/Memory usage: <80%

**Next Steps if Failed:**
- Review error logs in detail
- Check specific service logs
- Verify Docker network status
- See [Troubleshooting Service Startup](#troubleshooting-service-startup)

---

### Health Endpoint Verification

**Objective:** Verify service health endpoints are responding correctly

**Time Required:** 2 minutes

**Steps:**

1. **Check auth service health:**
```bash
curl -v http://localhost:8080/actuator/health
# Expected: 200 OK, status: "UP"
```

2. **Check liveness probe:**
```bash
curl http://localhost:8080/actuator/health/liveness
# Expected: 200 OK
```

3. **Check readiness probe:**
```bash
curl http://localhost:8080/actuator/health/readiness
# Expected: 200 OK when service is ready
```

4. **Get detailed health info:**
```bash
curl http://localhost:8080/actuator/health/live -s | jq '.components'
```

5. **Check all registered health indicators:**
```bash
curl http://localhost:8080/actuator/health -s | jq '.'
```

**Success Criteria:**
- All endpoints return 200 OK
- status: "UP" on all checks
- All components healthy

**Troubleshooting if Failed:**
- Service down, check container logs
- Database health failing, verify DB connectivity
- See [Database Diagnostics](#database-diagnostics)

---

## Application Diagnostics

### Troubleshooting Service Startup

**Objective:** Diagnose why a service fails to start or is unresponsive

**Time Required:** 5-10 minutes

**Steps:**

1. **Stop the problematic service:**
```bash
docker-compose stop gym-auth  # or other service
```

2. **View application logs:**
```bash
docker-compose logs -f gym-auth 2>&1 | head -100
# Look for: "Tomcat started", errors, exceptions
```

3. **Check for port binding errors:**
```bash
docker-compose logs gym-auth | grep -i "bind\|address\|port"
```

4. **Verify service dependencies:**
```bash
# Check if postgres is ready
docker exec postgres pg_isready -U gym_user
# Expected: accepting connections

# Check if RabbitMQ is ready
docker exec rabbitmq rabbitmq-diagnostics ping
```

5. **Restart service with verbose logging:**
```bash
SPRING_PROFILES_ACTIVE=debug docker-compose up gym-auth
```

6. **Check environment variables:**
```bash
docker exec gym-auth env | grep -E "SPRING_|DB_|JAVA_"
```

7. **Inspect container logs since last restart:**
```bash
docker inspect gym-auth | jq '.[0].State'
```

**Success Criteria:**
- Service reaches "Started" message
- Service responds to health check
- No exceptions in logs

**If Still Failing:**
- Check [Diagnostic Procedures: Database](#database-diagnostics)
- Review specific error messages
- Check docker-compose.yml for configuration errors

---

### Analyzing Application Logs

**Objective:** Extract and analyze key information from application logs

**Time Required:** 5 minutes per issue

**Steps:**

1. **Get recent logs (last 100 lines):**
```bash
docker-compose logs --tail=100 gym-auth | grep -i "error"
```

2. **Find logs for specific time period:**
```bash
# Last 5 minutes
docker-compose logs --since 5m gym-auth

# Between timestamps
docker logs gym-auth --since 2024-03-21T10:00:00 --until 2024-03-21T10:30:00
```

3. **Filter logs by severity:**
```bash
# Find all ERROR level
docker-compose logs gym-auth | grep "\[ERROR\]"

# Find all WARN level
docker-compose logs gym-auth | grep "\[WARN\]"

# Find exceptions
docker-compose logs gym-auth | grep -A 10 "Exception"
```

4. **Trace request flow:**
```bash
# Find all logs for request correlation ID
REQUEST_ID="a1b2c3d4"
docker-compose logs gym-auth | grep "$REQUEST_ID"
```

5. **Extract stack traces:**
```bash
docker-compose logs gym-auth > full_logs.txt
grep -A 20 "Exception" full_logs.txt | head -50
```

6. **Find performance issues:**
```bash
# Queries taking >1000ms
docker-compose logs gym-auth | grep "took [0-9]*ms" | awk '{print $NF}' | sort -nr
```

**Success Criteria:**
- Identified root cause of error
- Found relevant stack trace
- Understood request flow

**Next Steps:**
- Cross-reference error code with [Common Issues](03-common-issues.md)
- Search error message in logs
- Collect more information if needed

---

### Checking Dependency Injection Status

**Objective:** Verify Spring beans are properly initialized

**Time Required:** 2 minutes

**Steps:**

1. **List all registered beans:**
```bash
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans | length'
# Expected: Significant number (100+)
```

2. **Find specific bean:**
```bash
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans | keys[]' | grep -i "repository"
```

3. **Check bean dependencies:**
```bash
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans."com.gym.service.UserService".dependencies'
```

4. **Verify autowiring:**
```bash
# Enable debug logging temporarily
docker exec gym-auth curl -X POST http://localhost:8080/actuator/loggers/org.springframework.beans \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

**Success Criteria:**
- All expected beans are registered
- Dependencies are satisfied
- No wiring errors in logs

---

## Database Diagnostics

### Database Connectivity Check

**Objective:** Verify database is accessible and healthy

**Time Required:** 3 minutes

**Steps:**

1. **Test TCP connection:**
```bash
docker exec gym-auth nc -zv postgres 5432
# Expected: succeeded!
```

2. **Test SQL connection:**
```bash
docker exec postgres psql -U gym_user -d gym_db -c "SELECT 1"
# Expected: 1 result
```

3. **Check PostgreSQL is running:**
```bash
docker exec postgres pg_isready
# Expected: accepting connections
```

4. **Verify HikariCP pool status:**
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections | jq '.'
```

5. **Check active connections:**
```bash
docker exec postgres psql -U gym_user -d gym_db -c \
  "SELECT count(*) as connections FROM pg_stat_activity;"
```

**Success Criteria:**
- TCP connection successful
- SQL queries return results
- HikariCP shows available connections

**If Failed:**
- Check Docker network: `docker network ls`
- Check container running: `docker ps | grep postgres`
- Check logs: `docker logs postgres`

---

### Query Performance Analysis

**Objective:** Identify slow queries and performance issues

**Time Required:** 5-10 minutes

**Steps:**

1. **Enable slow query logging:**
```sql
ALTER DATABASE gym_db SET log_min_duration_statement = 1000;  -- Log queries >1s
```

2. **Find slow queries in logs:**
```bash
docker logs postgres | grep "duration:" | awk -F'duration: ' '{print $2}' | sort -n | tail -10
```

3. **Explain query execution plan:**
```bash
docker exec postgres psql -U gym_user -d gym_db << EOF
EXPLAIN ANALYZE
SELECT s.id, u.username, COUNT(e.id) as exercise_count
FROM training_sessions s
JOIN users u ON s.user_id = u.id
LEFT JOIN exercises e ON s.id = e.session_id
GROUP BY s.id, u.id
ORDER BY s.created_at DESC
LIMIT 20;
EOF
```

4. **Check for missing indexes:**
```sql
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');
```

5. **Find full table scans:**
```sql
SELECT schemaname, tablename, seq_scan, seq_tup_read, idx_scan
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan  -- More sequential than index scans
ORDER BY seq_tup_read DESC
LIMIT 10;
```

**Success Criteria:**
- Identified slow queries
- Query plans show reasonable execution strategy
- Indexes are being used

**Next Steps:**
- See [Common Issues: Slow Queries](03-common-issues.md#issue-slow-queries-query-taking-1-second)
- Add missing indexes
- Optimize query structure

---

### Transaction Deadlock Investigation

**Objective:** Identify and analyze database deadlocks

**Time Required:** 5 minutes

**Steps:**

1. **Check for deadlock errors:**
```bash
docker logs postgres | grep "deadlock"
```

2. **Identify blocking transactions:**
```sql
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename,
       blocked_activity.query AS blocked_query,
       blocking_activity.query AS blocking_query
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

3. **Check lock types held:**
```sql
SELECT pid, usename, application_name, state, query, lock_timeout
FROM pg_stat_activity
WHERE state IN ('active', 'idle in transaction');
```

4. **Kill blocking transaction (if safe):**
```sql
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid = <blocking_pid>;
```

**Success Criteria:**
- Identified blocking transactions
- Resolved without data corruption
- No new deadlocks after fix

---

### Backup and Recovery Status

**Objective:** Verify backup procedures are working

**Time Required:** 10 minutes

**Steps:**

1. **Check last backup:**
```bash
# If using pg_dump
ls -lah /path/to/backups/ | head -5

# Or in Docker:
docker exec postgres ls -lah /var/lib/postgresql/backups/ | head -5
```

2. **Verify WAL archiving:**
```sql
SELECT * FROM pg_stat_archiver;
```

3. **Test backup restoration (on test DB):**
```bash
# Create test database
docker exec postgres createdb -U gym_user gym_test

# Restore backup
docker exec postgres psql -U gym_user -d gym_test -f /path/to/backup.sql

# Verify data
docker exec postgres psql -U gym_user -d gym_test -c "SELECT COUNT(*) FROM users;"
```

4. **Check replication lag (if applicable):**
```sql
SELECT slot_name, restart_lsn, confirmed_flush_lsn FROM pg_replication_slots;
```

**Success Criteria:**
- Recent backup exists
- WAL archiving working
- Restoration test passes

---

## Network Diagnostics

### Service-to-Service Communication

**Objective:** Verify microservices can communicate

**Time Required:** 3 minutes

**Steps:**

1. **Test service hostname resolution:**
```bash
docker exec gym-auth nslookup gym-training
docker exec gym-auth nslookup postgres
docker exec gym-auth nslookup rabbitmq
```

2. **Test port connectivity:**
```bash
docker exec gym-auth nc -zv gym-training 8080
docker exec gym-auth nc -zv postgres 5432
docker exec gym-auth nc -zv rabbitmq 5672
```

3. **Test HTTP connectivity:**
```bash
docker exec gym-auth curl -v http://gym-training:8080/actuator/health
```

4. **Check Docker network:**
```bash
docker network ls
docker network inspect gym_default
```

5. **Verify container IPs:**
```bash
docker inspect gym-auth | jq '.[0].NetworkSettings.Networks'
```

**Success Criteria:**
- DNS resolves service names
- Port connectivity successful
- HTTP requests return responses

**If Failed:**
- Check docker-compose.yml for service names
- Verify containers on same network
- Check firewall rules

---

### External Service Connectivity

**Objective:** Test connectivity to external APIs and services

**Time Required:** 3 minutes

**Steps:**

1. **Test DNS resolution:**
```bash
docker exec gym-auth nslookup api.external-service.com
```

2. **Test TCP connectivity:**
```bash
docker exec gym-auth nc -zv api.external-service.com 443
```

3. **Test HTTPS connectivity:**
```bash
docker exec gym-auth curl -v https://api.external-service.com/health
```

4. **Check certificate validity:**
```bash
docker exec gym-auth openssl s_client -connect api.external-service.com:443
```

5. **Verify proxy settings (if applicable):**
```bash
docker exec gym-auth env | grep -i "proxy"
```

**Success Criteria:**
- DNS resolves correctly
- TCP connection successful
- HTTPS request succeeds
- Certificate valid

---

## Container Diagnostics

### Container Resource Usage

**Objective:** Monitor container CPU, memory, and I/O usage

**Time Required:** 5 minutes

**Steps:**

1. **Real-time stats:**
```bash
docker stats --no-stream
# View: CPU %, Memory usage, Network I/O
```

2. **Monitor over time:**
```bash
watch -n 5 'docker stats --no-stream'
# Watch for sustained >80% usage
```

3. **Check container limits:**
```bash
docker inspect gym-auth | jq '.[0].HostConfig | {Memory, MemorySwap, CpuShares}'
```

4. **View process inside container:**
```bash
docker exec gym-auth ps aux
docker exec gym-auth top -b -n 1 | head -20
```

5. **Check disk usage:**
```bash
docker exec gym-auth df -h
docker exec postgres du -sh /var/lib/postgresql/data
```

**Success Criteria:**
- CPU usage <80%
- Memory usage <80%
- Disk space available

**If Issues Found:**
- Increase container limits in docker-compose.yml
- Find resource-hogging processes
- Check for memory leaks

---

### Container File System

**Objective:** Inspect container contents and verify file structure

**Time Required:** 3 minutes

**Steps:**

1. **List container root directory:**
```bash
docker exec gym-auth ls -la /
```

2. **Check log directory:**
```bash
docker exec gym-auth ls -la /var/log/
docker exec gym-auth tail -100 /var/log/application.log
```

3. **Find configuration files:**
```bash
docker exec gym-auth find / -name "application*.properties" 2>/dev/null
docker exec gym-auth cat /app/config/application.properties
```

4. **Check Java home:**
```bash
docker exec gym-auth which java
docker exec gym-auth java -version
```

5. **Copy file from container:**
```bash
docker cp gym-auth:/app/logs/spring.log ./local_logs/
```

**Success Criteria:**
- All expected files present
- Correct permissions
- Configuration accessible

---

## Performance Diagnostics

### JVM Memory Analysis

**Objective:** Diagnose memory usage and heap issues

**Time Required:** 10 minutes

**Steps:**

1. **Check JVM memory settings:**
```bash
docker exec gym-auth jps -l -m | grep SpringApplication
```

2. **Get memory usage summary:**
```bash
docker exec gym-auth jcmd <pid> VM.memory_usage
```

3. **Generate heap dump:**
```bash
docker exec gym-auth jcmd <pid> GC.heap_dump /tmp/heap.hprof
docker cp gym-auth:/tmp/heap.hprof ./heap.hprof
```

4. **Analyze heap dump with Eclipse MAT:**
```bash
# Use MAT to open heap.hprof
# Analyze for: memory leaks, large objects, retained objects
```

5. **Check garbage collection statistics:**
```bash
docker exec gym-auth jcmd <pid> GC.stat
```

6. **Monitor GC in real-time:**
```bash
docker exec gym-auth jcmd <pid> JFR.start duration=60s filename=/tmp/recording.jfr
# Wait 60 seconds
docker cp gym-auth:/tmp/recording.jfr ./recording.jfr
# Analyze with JFR viewer
```

**Success Criteria:**
- Memory usage <80% of heap
- GC pauses <200ms
- No memory leaks detected

**If Issues Found:**
- See [Common Issues: Out of Memory](03-common-issues.md#issue-out-of-memory-oom-errors)
- Increase heap size or find memory leak

---

### Thread Analysis

**Objective:** Identify thread issues and deadlocks

**Time Required:** 5 minutes

**Steps:**

1. **Get thread dump:**
```bash
docker exec gym-auth jcmd <pid> Thread.print > thread_dump.txt
```

2. **Analyze thread states:**
```bash
grep "java.lang.Thread.State" thread_dump.txt | sort | uniq -c
```

3. **Find waiting threads:**
```bash
grep -A 3 "waiting on lock" thread_dump.txt
```

4. **Check thread pool status:**
```bash
curl http://localhost:8080/actuator/metrics/executor | jq '.measurements'
```

5. **List active threads:**
```bash
docker exec gym-auth jcmd <pid> Thread.print | grep "tid" | wc -l
```

**Success Criteria:**
- No thread deadlocks
- Thread count reasonable for workload
- No runaway thread creation

---

### Application Metrics

**Objective:** Collect performance metrics from Spring Boot Actuator

**Time Required:** 5 minutes

**Steps:**

1. **Get all available metrics:**
```bash
curl http://localhost:8080/actuator/metrics | jq '.names[]' | head -20
```

2. **Check HTTP request metrics:**
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements'
```

3. **Monitor database connection pool:**
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.'
```

4. **Check cache metrics:**
```bash
curl http://localhost:8080/actuator/metrics/cache.gets | jq '.'
```

5. **Get JVM metrics:**
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.measurements'
```

**Success Criteria:**
- Metrics available and updating
- No negative trends
- Request latency acceptable

---

## Security Diagnostics

### Authentication and Authorization Check

**Objective:** Verify security mechanisms are functioning

**Time Required:** 5 minutes

**Steps:**

1. **Test unauthenticated request:**
```bash
curl -v http://localhost:8080/api/training/protected
# Expected: 401 Unauthorized
```

2. **Obtain valid token:**
```bash
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.token')
echo $TOKEN
```

3. **Test authenticated request:**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/training/sessions
# Expected: 200 OK or 403 Forbidden (based on permissions)
```

4. **Verify JWT token contents:**
```bash
# Decode JWT (first two parts)
echo $TOKEN | cut -d. -f1,2 | sed 's/\./ /g' | while read a b; do
  echo "$a" | base64 -d; echo
  echo "$b" | base64 -d; echo
done
```

5. **Check authentication provider status:**
```bash
curl http://localhost:8080/actuator/beans | jq '.contexts.application.beans' | grep -i "auth"
```

**Success Criteria:**
- Unauthenticated requests blocked
- Token obtained successfully
- Authenticated requests succeeds
- Token claims are correct

---

### SSL/TLS Certificate Check

**Objective:** Verify HTTPS certificates and TLS configuration

**Time Required:** 5 minutes

**Steps:**

1. **Check certificate details:**
```bash
# From outside container
openssl s_client -connect localhost:8443 < /dev/null

# Or from inside container
docker exec gym-auth openssl s_client -connect localhost:8443
```

2. **Verify certificate validity:**
```bash
# Check expiration
openssl s_client -connect localhost:8443 2>/dev/null | \
  openssl x509 -noout -dates
```

3. **Check certificate chain:**
```bash
openssl s_client -connect localhost:8443 -showcerts < /dev/null
```

4. **Verify keystore:**
```bash
docker exec gym-auth keytool -list -v -keystore /app/keystore.p12 -storepass <password>
```

5. **Test TLS version:**
```bash
openssl s_client -connect localhost:8443 -tls1_2
openssl s_client -connect localhost:8443 -tls1_3
```

**Success Criteria:**
- Certificate valid and not expired
- Correct CN/SAN
- Strong TLS version (1.2+)
- No warnings

---

### Access Control Verification

**Objective:** Verify authorization and role-based access

**Time Required:** 5 minutes

**Steps:**

1. **Test role-based access:**
```bash
# Get admin token
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin_pass"}' | jq -r '.token')

# Test admin endpoint
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/admin/users

# Get user token
USER_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user_pass"}' | jq -r '.token')

# Should fail for user
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8080/api/admin/users
# Expected: 403 Forbidden
```

2. **Verify authorization annotations:**
```bash
grep -r "@PreAuthorize\|@Secured\|@RolesAllowed" src/ --include="*.java"
```

3. **Check method-level security enabled:**
```bash
docker exec gym-auth env | grep SPRING_SECURITY
```

**Success Criteria:**
- Admin can access admin endpoints
- User cannot access admin endpoints
- Authorization rules enforced

---

## Quick Reference Checklists

### 5-Minute Health Check
```bash
# 1. Container status
docker-compose ps

# 2. Service health
curl http://localhost:8080/actuator/health

# 3. Database connectivity
docker exec gym-auth curl -s http://localhost:8080/actuator/health/db

# 4. Recent errors
docker-compose logs --tail=20 | grep -i error

# 5. Resource usage
docker stats --no-stream
```

### 15-Minute Deep Diagnostic
```bash
# 1. Application logs
docker-compose logs gym-auth > /tmp/app_logs.txt

# 2. Database status
docker exec postgres psql -U gym_user -d gym_db -c \
  "SELECT version(); SELECT * FROM pg_stat_activity;"

# 3. Network connectivity
docker exec gym-auth ping gym-training
docker exec gym-auth nc -zv postgres 5432

# 4. Memory analysis
docker exec gym-auth jcmd $(docker exec gym-auth pgrep java) GC.heap_usage

# 5. Performance metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## Related Documentation

- [02-debugging-techniques.md](02-debugging-techniques.md) - Advanced debugging tools
- [03-common-issues.md](03-common-issues.md) - Common problems and solutions
- [05-performance-debugging.md](05-performance-debugging.md) - Performance analysis
- [06-security-troubleshooting.md](06-security-troubleshooting.md) - Security diagnostics
- [07-database-troubleshooting.md](07-database-troubleshooting.md) - Database procedures
- [08-network-troubleshooting.md](08-network-troubleshooting.md) - Network diagnostics
- [09-deployment-troubleshooting.md](09-deployment-troubleshooting.md) - Deployment procedures

See also:
- docs/operations/02-monitoring.md - Monitoring procedures
- docs/operations/03-logging.md - Log analysis techniques
- docs/deployment/03-health-checks.md - Health check configuration
