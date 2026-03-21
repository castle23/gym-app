# Gym Platform API - Operational Runbook

## Table of Contents

1. [Introduction](#introduction)
2. [Daily Operations](#daily-operations)
3. [Service Management](#service-management)
4. [Database Operations](#database-operations)
5. [Monitoring & Alerts](#monitoring--alerts)
6. [Performance Management](#performance-management)
7. [Capacity Planning](#capacity-planning)
8. [Maintenance Windows](#maintenance-windows)
9. [Security Operations](#security-operations)
10. [Incident Response](#incident-response)

---

## Introduction

This runbook provides operational procedures for managing the Gym Platform API in production. It covers daily tasks, troubleshooting, and incident response.

### On-Call Responsibilities

- Monitor service health every 30 minutes
- Respond to critical alerts within 5 minutes
- Maintain uptime SLA of 99.5% monthly
- Document all incidents and resolution steps
- Perform daily backup verification

### Key Contacts

- **Platform Lead**: [Contact Info]
- **DevOps Team**: [Contact Info]
- **Database Administrator**: [Contact Info]
- **Security Team**: [Contact Info]

---

## Daily Operations

### Morning Health Check (8:00 AM)

```bash
#!/bin/bash
echo "=== Gym Platform Daily Health Check ==="

# Check Docker containers status
echo "Checking container status..."
docker-compose -f /opt/gym-platform/docker-compose.prod.yml ps

# Check service health endpoints
echo "Checking service health..."
curl -s http://localhost:8081/actuator/health | jq .
curl -s http://localhost:8082/actuator/health | jq .
curl -s http://localhost:8083/actuator/health | jq .
curl -s http://localhost:8084/actuator/health | jq .

# Check database connection
echo "Checking database..."
psql -h localhost -U gym_admin -d gym_db -c "SELECT version();"

# Check disk space
echo "Checking disk usage..."
df -h /

# Check memory usage
echo "Checking memory..."
free -h

# Check logs for errors
echo "Checking error logs from last hour..."
docker logs --since 1h gym-auth-prod | grep ERROR
docker logs --since 1h gym-training-prod | grep ERROR
docker logs --since 1h gym-tracking-prod | grep ERROR
docker logs --since 1h gym-notification-prod | grep ERROR

echo "=== Health Check Complete ==="
```

### Backup Verification (10:00 AM)

```bash
#!/bin/bash
echo "=== Backup Verification ==="

# Check if daily backup exists
YESTERDAY=$(date -d yesterday +%Y%m%d)
BACKUP_FILE="/backups/gym_db/gym_db_${YESTERDAY}_*.sql.gz"

if ls $BACKUP_FILE 1> /dev/null 2>&1; then
  echo "✅ Backup from yesterday exists"
  ls -lh $BACKUP_FILE
else
  echo "❌ ALERT: Backup from yesterday NOT found!"
  # Send alert
fi

# Check backup size
SIZE=$(du -sh /backups/gym_db | cut -f1)
echo "Total backup storage: $SIZE"
```

### End-of-Day Summary (5:00 PM)

```bash
#!/bin/bash
echo "=== End of Day Summary ==="

# Service uptime
echo "Service Uptime (last 24 hours):"
for service in auth training tracking notification; do
  UPTIME=$(docker stats --no-stream gym-${service}-prod | awk 'NR==2 {print $3}')
  echo "  $service: $UPTIME"
done

# API Request Counts
echo "API Request Summary:"
docker logs --since 24h gym-auth-prod | grep "GET\|POST\|PUT\|DELETE" | wc -l

# Errors in last 24 hours
echo "Error Summary:"
for service in auth training tracking notification; do
  COUNT=$(docker logs --since 24h gym-${service}-prod | grep -i ERROR | wc -l)
  echo "  $service: $COUNT errors"
done

echo "=== End of Day Report Complete ==="
```

---

## Service Management

### Starting Services

```bash
# Start all services
cd /opt/gym-platform
docker-compose -f docker-compose.prod.yml up -d

# Wait for services to be ready
sleep 30

# Verify all services are running
docker-compose -f docker-compose.prod.yml ps
```

### Stopping Services

For maintenance:

```bash
# Graceful shutdown
docker-compose -f docker-compose.prod.yml stop

# Force shutdown if needed
docker-compose -f docker-compose.prod.yml kill
```

### Restarting Individual Services

```bash
# Restart Auth Service
docker-compose -f docker-compose.prod.yml restart auth-service

# Restart Training Service
docker-compose -f docker-compose.prod.yml restart training-service

# Restart with rebuild
docker-compose -f docker-compose.prod.yml up -d --build auth-service
```

### Viewing Service Logs

```bash
# Real-time logs for Auth Service
docker logs -f gym-auth-prod

# Last 100 lines
docker logs --tail 100 gym-auth-prod

# Logs from last hour with timestamps
docker logs --since 1h --timestamps gym-auth-prod

# Filter logs for specific keywords
docker logs gym-auth-prod | grep "ERROR"
docker logs gym-auth-prod | grep "SQLException"

# Export logs to file
docker logs gym-auth-prod > /tmp/auth-service.log 2>&1
```

### Service Resource Usage

```bash
# Monitor real-time resource usage
docker stats

# Specific service stats
docker stats gym-auth-prod gym-training-prod gym-tracking-prod gym-notification-prod

# View container memory limit
docker inspect -f '{{.HostConfig.Memory}}' gym-auth-prod

# View CPU limits
docker inspect -f '{{.HostConfig.CpuShares}}' gym-auth-prod
```

---

## Database Operations

### Database Connection

```bash
# Connect as admin
psql -h localhost -U gym_admin -d gym_db

# Common queries
SELECT version();
SELECT * FROM pg_stat_activity;
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) FROM pg_tables WHERE schemaname != 'pg_catalog' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Checking Database Size

```bash
# Total database size
psql -h localhost -U gym_admin -d gym_db -c "SELECT pg_size_pretty(pg_database_size('gym_db'));"

# Size by schema
psql -h localhost -U gym_admin -d gym_db -c "
SELECT
  schema_name,
  pg_size_pretty(SUM(table_size)::BIGINT) AS total_size
FROM (
  SELECT
    table_schema as schema_name,
    pg_total_relation_size(schemaname||'.'||tablename) as table_size
  FROM pg_tables
) AS t
GROUP BY schema_name
ORDER BY total_size DESC;"
```

### Database Maintenance

```bash
# Analyze query performance
psql -h localhost -U gym_admin -d gym_db -c "ANALYZE;"

# Vacuum database
psql -h localhost -U gym_admin -d gym_db -c "VACUUM ANALYZE;"

# Reindex tables
psql -h localhost -U gym_admin -d gym_db -c "REINDEX DATABASE gym_db;"
```

### Creating Manual Backups

```bash
# Full backup
pg_dump -h localhost -U gym_admin gym_db > /backups/gym_db_manual_$(date +%Y%m%d_%H%M%S).sql

# Compressed backup
pg_dump -h localhost -U gym_admin gym_db | gzip > /backups/gym_db_manual_$(date +%Y%m%d_%H%M%S).sql.gz

# Custom format for faster restoration
pg_dump -h localhost -U gym_admin -Fc gym_db > /backups/gym_db_manual_$(date +%Y%m%d_%H%M%S).dump
```

---

## Monitoring & Alerts

### Prometheus Metrics Collection

Metrics are exposed at service endpoints:

```bash
# Auth Service metrics
curl http://localhost:8081/actuator/prometheus

# Training Service metrics
curl http://localhost:8082/actuator/prometheus

# Filter for specific metric
curl http://localhost:8081/actuator/prometheus | grep http_server_requests
```

### Critical Metrics to Monitor

1. **Application Health**
   - Service availability
   - Response time (p99, p95, p50)
   - Error rate
   - Database connection pool usage

2. **Database Health**
   - Connection count
   - Query execution time
   - Table size growth
   - Cache hit ratio

3. **Infrastructure Health**
   - CPU usage
   - Memory usage
   - Disk I/O
   - Network throughput

### Setting Up Alerts

Example alert thresholds:

```yaml
alerts:
  - name: HighErrorRate
    condition: error_rate > 5%
    threshold: CRITICAL
    action: Page on-call engineer
    
  - name: DatabaseConnectionPoolNearCapacity
    condition: db_connections > 18/20
    threshold: WARNING
    action: Send email
    
  - name: DiskSpaceRunningLow
    condition: disk_available < 10GB
    threshold: CRITICAL
    action: Page on-call engineer
    
  - name: MemoryUsageHigh
    condition: memory_used > 90%
    threshold: WARNING
    action: Send email
```

---

## Performance Management

### Identifying Performance Bottlenecks

```bash
# Slow query log
docker exec gym-db-prod psql -U gym_admin -d gym_db -c "
  SELECT * FROM pg_stat_statements
  ORDER BY mean_exec_time DESC
  LIMIT 10;"

# Check index usage
docker exec gym-db-prod psql -U gym_admin -d gym_db -c "
  SELECT schemaname, tablename, indexname
  FROM pg_indexes
  WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
  ORDER BY tablename;"
```

### Optimizing Database Queries

1. Add indexes on frequently filtered columns
2. Update statistics regularly
3. Monitor and optimize slow queries
4. Consider query result caching

### Application Performance Tuning

1. Configure JVM heap size appropriately
2. Enable connection pooling
3. Implement query caching
4. Use async operations where possible

---

## Capacity Planning

### Current Resource Allocation

```
Service              Memory    CPU Cores    Disk
Auth Service         512MB     0.5         1GB
Training Service     512MB     0.5         1GB
Tracking Service     512MB     0.5         1GB
Notification Service 512MB     0.5         1GB
Database             1GB       1.0         50GB
```

### Growth Projections

Monitor these metrics quarterly:

- Database growth rate: GB/month
- Request volume: Requests/second
- User growth: % monthly increase
- Storage requirements: Total GB

### Scaling Recommendations

When metrics reach thresholds:

- CPU > 70%: Add more cores or scale services horizontally
- Memory > 80%: Increase instance memory or optimize application
- Disk > 80%: Archive old data or increase storage
- DB connections > 80%: Increase connection pool or add read replicas

---

## Maintenance Windows

### Planned Maintenance Schedule

```
Recurring Maintenance:
- Database VACUUM: Weekly (Sunday 2:00 AM)
- Database ANALYZE: Weekly (Sunday 2:30 AM)
- Log rotation: Daily (3:00 AM)
- Backup verification: Daily (10:00 AM)

Ad-hoc Maintenance:
- Security patches: Within 48 hours
- Critical bugs: Within 24 hours
- Performance optimization: Quarterly
```

### Maintenance Procedure

1. Create backup before starting
2. Post maintenance notice
3. Drain connections gracefully
4. Execute maintenance
5. Run health checks
6. Notify users of completion

---

## Security Operations

### Regular Security Tasks

1. **Weekly**
   - Review access logs
   - Check for unauthorized login attempts
   - Verify SSL certificate validity

2. **Monthly**
   - Rotate database credentials
   - Review firewall rules
   - Check for security patches

3. **Quarterly**
   - Security audit
   - Penetration testing
   - Backup restoration test

### Secret Management

```bash
# Rotate JWT secret
# 1. Generate new secret
openssl rand -base64 32

# 2. Update .env file
nano .env

# 3. Restart services
docker-compose -f docker-compose.prod.yml restart
```

---

## Incident Response

### Critical Service Down

1. **Assess** (immediately)
   - Check container status
   - Review recent logs
   - Check database connectivity

2. **Communicate** (within 5 minutes)
   - Notify stakeholders
   - Post status page
   - Activate incident channel

3. **Investigate** (within 15 minutes)
   - Collect logs and metrics
   - Check recent deployments
   - Review recent changes

4. **Remediate** (ongoing)
   - Restart service if needed
   - Rollback if necessary
   - Apply fix

5. **Restore** (ongoing)
   - Verify service health
   - Run full test suite
   - Monitor closely

### Database Connection Errors

```bash
# Check database status
psql -h localhost -U gym_admin -d gym_db -c "SELECT 1;"

# Check connection pool
docker logs gym-auth-prod | grep "HikariPool"

# Restart services if needed
docker-compose -f docker-compose.prod.yml restart
```

### High CPU or Memory Usage

```bash
# Identify resource-heavy process
docker stats --no-stream | sort -k3 -rn

# Check container limits
docker inspect gym-auth-prod | grep -A 10 HostConfig

# Restart service if needed
docker-compose -f docker-compose.prod.yml restart auth-service
```

### Disk Space Issues

```bash
# Check disk usage
df -h

# Identify large files
find / -type f -size +100M 2>/dev/null

# Clean up old logs
find /var/lib/docker -name "*.log" -mtime +30 -delete
```

---

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-03-21 | Initial runbook creation | DevOps Team |
