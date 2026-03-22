# Gym Platform API - Troubleshooting Guide

## Table of Contents

1. [Getting Started](#getting-started)
2. [Common Issues & Solutions](#common-issues--solutions)
3. [Service-Specific Troubleshooting](#service-specific-troubleshooting)
4. [Database Troubleshooting](#database-troubleshooting)
5. [Network & Connectivity Issues](#network--connectivity-issues)
6. [Performance Issues](#performance-issues)
7. [Security Issues](#security-issues)
8. [Diagnostic Commands](#diagnostic-commands)
9. [Advanced Debugging](#advanced-debugging)

---

## Getting Started

When troubleshooting, follow this systematic approach:

1. **Identify the symptom**: What is not working?
2. **Check logs**: Review relevant service and database logs
3. **Isolate the problem**: Is it a single service or system-wide?
4. **Verify prerequisites**: Are all dependencies running?
5. **Apply solution**: Use the appropriate fix from this guide
6. **Document**: Record the issue and resolution for future reference

---

## Common Issues & Solutions

### Issue: Container Won't Start

**Symptom**: Docker container exits immediately or shows status "Exited"

**Diagnosis**:

```bash
# Check container status
docker ps -a | grep gym

# View container logs
docker logs auth-service
docker logs training-service

# Check for error messages
docker logs auth-service 2>&1 | grep -i error
```

**Solutions**:

1. **Check Docker resources**

```bash
docker system df
docker system prune -a  # Clean up unused resources
```

2. **Verify environment variables**

```bash
# Check if .env file exists
ls -la .env

# Validate environment variables
docker run --env-file .env alpine env | grep -i gym
```

3. **Rebuild image**

```bash
docker-compose build --no-cache auth-service
docker-compose up -d auth-service
```

4. **Check port conflicts**

```bash
# Check if ports are in use
lsof -i :8081  # On Linux/Mac
netstat -ano | findstr :8081  # On Windows

# Kill process using port
kill -9 <PID>  # On Linux/Mac
taskkill /PID <PID> /F  # On Windows
```

---

### Issue: 502 Bad Gateway / Connection Refused

**Symptom**: Client receives 502 error or connection refused

**Diagnosis**:

```bash
# Test service endpoint
curl -v http://localhost:8081/auth/actuator/health
curl -v http://127.0.0.1:8081/auth/register

# Check if ports are listening
netstat -tuln | grep 8081

# Check service logs
docker logs auth-service | tail -50
```

**Solutions**:

1. **Verify service is running**

```bash
docker-compose ps
docker container ls | grep auth-service
```

2. **Restart the service**

```bash
docker-compose restart auth-service
sleep 10

# Verify restart
curl http://localhost:8081/auth/actuator/health
```

3. **Check firewall rules**

```bash
# Linux: Check iptables
sudo iptables -L | grep 8081

# Check firewall status
sudo firewall-cmd --list-ports
sudo firewall-cmd --add-port=8081/tcp --permanent
```

---

### Issue: High Memory Usage

**Symptom**: Service consuming excessive memory or OOMKilled

**Diagnosis**:

```bash
# Check memory usage
docker stats --no-stream

# Check container memory limit
docker inspect auth-service | grep -A 5 Memory

# Check JVM memory settings
docker logs auth-service | grep Xmx
```

**Solutions**:

1. **Increase JVM heap size**

Update `docker-compose.yml` environment:
```yaml
environment:
  JAVA_OPTS: "-Xmx2048m -Xms1024m"
```

2. **Restart with new settings**

```bash
docker-compose restart
```

3. **Add memory limit to docker-compose.yml**

```yaml
auth-service:
  mem_limit: 1g
  memswap_limit: 2g
```

4. **Check for memory leaks**

```bash
# View heap dump
jmap -heap <PID>

# Monitor GC activity
jstat -gc -h 20 <PID> 1000
```

---

### Issue: Slow API Response

**Symptom**: API requests taking unusually long time (>5 seconds)

**Diagnosis**:

```bash
# Time the request
time curl http://localhost:8081/auth/login

# Check database query performance
psql -h localhost -U gym_admin -d gym_db
SELECT query, calls, total_time, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;
```

**Solutions**:

1. **Check database performance**

```bash
# Run ANALYZE to update statistics
psql -h localhost -U gym_admin -d gym_db -c "ANALYZE;"

# Check for slow queries
docker exec postgres psql -U gym_admin -d gym_db -c "
  SELECT query, calls, total_time, mean_time
  FROM pg_stat_statements
  WHERE mean_time > 1000
  ORDER BY mean_time DESC;"
```

2. **Add indexes**

```bash
psql -h localhost -U gym_admin -d gym_db
CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_plans_user_id ON tracking_schema.plans(user_id);
```

3. **Optimize query**

Review slow queries and consider:
- Adding appropriate indexes
- Rewriting complex queries

---

### Issue: Database Connection Pool Exhausted

**Symptom**: Error "Cannot get connection, pool exhausted"

**Diagnosis**:

```bash
# Check active connections
psql -h localhost -U gym_admin -d gym_db -c "SELECT datname, usename, state, count(*) FROM pg_stat_activity GROUP BY datname, usename, state;"

# Check connection pool status in service logs
docker logs auth-service | grep "HikariPool"
```

**Solutions**:

1. **Increase connection pool size**

Update application.yml:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

2. **Terminate idle connections**

```bash
psql -h localhost -U gym_admin -d gym_db -c "
  SELECT pg_terminate_backend(pid)
  FROM pg_stat_activity
  WHERE datname = 'gym_db'
  AND pid != pg_backend_pid()
  AND state = 'idle'
  AND query_start < NOW() - INTERVAL '10 minutes';"
```

3. **Restart services**

```bash
docker-compose restart
```

---

## Service-Specific Troubleshooting

### Auth Service Issues

**Login failing with 401 Unauthorized**

```bash
# Check JWT configuration
docker logs auth-service | grep -i jwt

# Test login endpoint
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Check user exists in database
psql -h localhost -U gym_admin -d gym_db -c "SELECT * FROM auth_schema.users WHERE email='test@example.com';"
```

**Password validation failing**

```bash
# Check if user account is locked
psql -h localhost -U gym_admin -d gym_db -c "SELECT id, email, failed_login_attempts, locked_until FROM auth_schema.users WHERE email='test@example.com';"

# Unlock user if needed
psql -h localhost -U gym_admin -d gym_db -c "UPDATE auth_schema.users SET failed_login_attempts=0, locked_until=NULL WHERE email='test@example.com';"
```

### Training Service Issues

**Exercise creation failing**

```bash
# Check service logs
docker logs training-service | grep -i error

# Verify database connection
psql -h localhost -U gym_admin -d gym_db -c "SELECT * FROM training_schema.exercises LIMIT 5;"

# Check table structure
psql -h localhost -U gym_admin -d gym_db -c "\d training_schema.exercises"
```

### Tracking Service Issues

**Plan updates not reflected**

```bash
# Check tracking service logs
docker logs tracking-service | grep -i error

# Verify data consistency
psql -h localhost -U gym_admin -d gym_db -c "
  SELECT COUNT(*) FROM tracking_schema.plans;
  SELECT COUNT(*) FROM tracking_schema.objectives;"

# Check for transaction rollbacks
docker logs tracking-service | grep -i "rollback"
```

### Notification Service Issues

**Notifications not being sent**

```bash
# Check notification service logs
docker logs notification-service | grep -i error

# Check notification records
psql -h localhost -U gym_admin -d gym_db -c "SELECT * FROM notification_schema.notifications WHERE status='PENDING';"
```

---

## Database Troubleshooting

### Cannot Connect to Database

```bash
# Check PostgreSQL service
docker ps | grep postgres

# Test connection with correct parameters
psql -h localhost -U gym_admin -d gym_db -c "SELECT 1;"

# If connection fails, restart database
docker-compose restart postgres

# Check PostgreSQL logs
docker logs postgres | tail -50
```

### Database Locked / Deadlock

```bash
# Check for locks
psql -h localhost -U gym_admin -d gym_db -c "SELECT * FROM pg_locks WHERE NOT granted;"

# Cancel long-running queries
psql -h localhost -U gym_admin -d gym_db -c "
  SELECT pg_cancel_backend(pid)
  FROM pg_stat_activity
  WHERE query LIKE '%UPDATE%'
  AND state = 'active';"
```

### Out of Disk Space

```bash
# Check disk usage
df -h /var/lib/postgresql

# Identify large tables
psql -h localhost -U gym_admin -d gym_db -c "
  SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
  FROM pg_tables
  WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
  ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
  LIMIT 10;"

# Archive old data
psql -h localhost -U gym_admin -d gym_db -c "
  DELETE FROM tracking_schema.progress_logs
  WHERE created_at < NOW() - INTERVAL '90 days';"
```

---

## Network & Connectivity Issues

### DNS Resolution Issues

```bash
# Test DNS resolution
nslookup gym-api.example.com
dig gym-api.example.com

# Check /etc/hosts file
cat /etc/hosts | grep gym

# Flush DNS cache
sudo systemctl restart systemd-resolved
```

### SSL/TLS Certificate Issues

```bash
# Check certificate validity
openssl s_client -connect gym-api.example.com:443 -showcerts

# Check certificate expiration
certbot certificates

# Renew certificate
certbot renew --force-renewal -d gym-api.example.com

# Restart Nginx
sudo systemctl restart nginx
```

---

## Performance Issues

### Slow Database Queries

```bash
# Enable slow query log
ALTER SYSTEM SET log_min_duration_statement = 1000;
SELECT pg_reload_conf();

# View slow queries
docker logs --since 1h gym-db-prod | grep "duration:"

# Analyze query plan
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
```

### High CPU Usage

```bash
# Identify process using CPU
top -b -n 1 | sort -k 9 -rn | head -10

# Check Docker stats
docker stats --no-stream | sort -k3 -rn

# Restart service if needed
docker-compose -f docker-compose.prod.yml restart auth-service
```

---

## Security Issues

### Unauthorized Access Attempts

```bash
# Check logs for failed auth attempts
docker logs auth-service | grep "UNAUTHORIZED\|401"

# Check for brute force patterns
docker logs auth-service | grep "Invalid credentials" | wc -l

# Block suspicious IP addresses
# Update firewall rules to drop traffic from attacking IP
sudo iptables -I INPUT -s <ATTACKER_IP> -j DROP
```

### SQL Injection Attempts

```bash
# Check database logs for suspicious queries
docker exec postgres psql -U gym_admin -d gym_db -c "
  SELECT query FROM pg_stat_statements
  WHERE query LIKE '%OR%1=1%' OR query LIKE '%DROP%' OR query LIKE '%UNION%';"

# Check application logs
docker logs auth-service | grep -i "sql\|injection"
```

---

## Diagnostic Commands

### Comprehensive Health Check

```bash
#!/bin/bash
echo "=== Comprehensive System Health Check ==="

# Docker status
echo "Docker Status:"
docker ps

# Service health
echo "Service Health:"
curl -s http://localhost:8080/actuator/health | jq '.status'
curl -s http://localhost:8081/auth/actuator/health | jq '.status'
curl -s http://localhost:8082/training/actuator/health | jq '.status'
curl -s http://localhost:8083/tracking/actuator/health | jq '.status'
curl -s http://localhost:8084/notifications/actuator/health | jq '.status'

# Database health
echo "Database Health:"
psql -h localhost -U gym_admin -d gym_db -c "SELECT version();"

# Disk usage
echo "Disk Usage:"
df -h /

# Memory usage
echo "Memory Usage:"
free -h
```

### Log Collection

```bash
# Collect all service logs for analysis
mkdir -p /tmp/gym-logs
docker logs api-gateway > /tmp/gym-logs/api-gateway.log 2>&1
docker logs auth-service > /tmp/gym-logs/auth.log 2>&1
docker logs training-service > /tmp/gym-logs/training.log 2>&1
docker logs tracking-service > /tmp/gym-logs/tracking.log 2>&1
docker logs notification-service > /tmp/gym-logs/notification.log 2>&1
docker logs postgres > /tmp/gym-logs/database.log 2>&1

tar -czf /tmp/gym-logs-$(date +%Y%m%d_%H%M%S).tar.gz /tmp/gym-logs/
```

---

## Advanced Debugging

### Enable Debug Logging

Update application.yml:

```yaml
logging:
  level:
    root: INFO
    com.gym: DEBUG
    org.springframework.web: DEBUG
    org.hibernate: DEBUG
```

Restart services:

```bash
docker-compose restart
```

### Attach to Running Container

```bash
# Start interactive shell in container
docker exec -it auth-service /bin/bash

# View running processes
ps aux

# Check Java process details
jps -l

# Monitor threads
jstack <PID>
```

### Network Traffic Analysis

```bash
# Capture network traffic
tcpdump -i docker0 -w /tmp/capture.pcap

# Analyze with Wireshark
wireshark /tmp/capture.pcap

# Check active connections
netstat -an | grep ESTABLISHED
```

---

## When to Escalate

Escalate to Senior Engineer if:

- Multiple services are affected
- Data corruption is suspected
- Root cause cannot be identified within 30 minutes
- Database requires point-in-time recovery
- Security breach is suspected

---

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)
- [Nginx Documentation](https://nginx.org/en/docs/)

