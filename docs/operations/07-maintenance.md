# Maintenance Schedule

## Overview

Routine maintenance tasks, update schedules, system optimization, and maintenance windows for Gym Platform operations.

## Maintenance Windows

### Standard Maintenance Window

**When:** Saturdays 2:00 AM - 4:00 AM (UTC)
- Low traffic period
- Advance notice: 1 week
- Communication: Status page + email + Slack

**Activities:**
- Database optimization (VACUUM, ANALYZE)
- Index rebuilds
- Dependency updates
- Configuration changes
- Testing scripts

### Emergency Maintenance

**When:** As needed for critical issues
- Status page notification immediately
- Expected impact < 5 minutes
- Auto-rollback if failed

## Daily Maintenance Tasks

### Morning Health Check (8:00 AM)

```bash
#!/bin/bash
# scripts/maintenance/daily-health-check.sh

echo "=== Daily Health Check Report ==="
DATE=$(date "+%Y-%m-%d %H:%M:%S")

# 1. Service status
echo "[$DATE] Service Status:"
for port in 8081 8082 8083 8084; do
    STATUS=$(curl -s -f http://localhost:$port/actuator/health | jq -r '.status' || echo "DOWN")
    echo "  Port $port: $STATUS"
done

# 2. Database status
echo "[$DATE] Database:"
CONNECTIONS=$(docker exec gym-postgres psql -U postgres -t -c "SELECT count(*) FROM pg_stat_activity;")
echo "  Active connections: $CONNECTIONS"

SIZE=$(docker exec gym-postgres psql -U postgres -t -c "SELECT pg_size_pretty(pg_database_size('gym_db'));")
echo "  Database size: $SIZE"

# 3. Disk usage
echo "[$DATE] Disk Usage:"
docker exec gym-postgres df -h | grep -E "Filesystem|/var/lib/postgresql"

# 4. Backup status
echo "[$DATE] Backup Status:"
LAST_BACKUP=$(ls -t /backups/postgres/full/full_backup_*.dump 2>/dev/null | head -1)
if [ -n "$LAST_BACKUP" ]; then
    MOD_TIME=$(stat -c %y "$LAST_BACKUP" | cut -d' ' -f1-2)
    SIZE=$(du -h "$LAST_BACKUP" | cut -f1)
    echo "  Last backup: $MOD_TIME ($SIZE)"
else
    echo "  No recent backup found!"
fi

# 5. Error trends
echo "[$DATE] Error Rate (last hour):"
ERROR_RATE=$(curl -s 'http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[1h])' \
    | jq '.data.result[0].value[1]' 2>/dev/null)
echo "  $ERROR_RATE errors/sec"

# Send report via email/Slack
curl -X POST https://hooks.slack.com/services/YOUR/WEBHOOK \
    -d '{"text":"Daily health check completed"}'
```

## Weekly Maintenance Tasks

### Monday - Database Optimization

```bash
#!/bin/bash
# scripts/maintenance/weekly-db-maintenance.sh

echo "=== Weekly Database Maintenance ==="

# 1. Update table statistics
echo "Analyzing tables..."
docker exec gym-postgres psql -U postgres -d gym_db -c "ANALYZE;"

# 2. Reclaim space
echo "Vacuuming tables..."
docker exec gym-postgres psql -U postgres -d gym_db -c "VACUUM;"

# 3. Reindex
echo "Reindexing tables..."
docker exec gym-postgres psql -U postgres -d gym_db -c "REINDEX DATABASE gym_db;"

# 4. Check table bloat
echo "Checking for bloat..."
docker exec gym-postgres psql -U postgres -d gym_db -c \
    "SELECT schemaname, tablename, round(100*DEAD*avg_width/CASE WHEN otta>0 THEN otta ELSE 1 END) AS ratio
     FROM pg_class; LIMIT 10;"

echo "Weekly maintenance completed"
```

### Wednesday - Log Rotation & Cleanup

```bash
#!/bin/bash
# scripts/maintenance/weekly-log-cleanup.sh

echo "=== Weekly Log Cleanup ==="

# Compress logs older than 7 days
find /var/log/gym -name "*.log" -mtime +7 -exec gzip {} \;

# Delete logs older than 30 days
find /var/log/gym -name "*.log.gz" -mtime +30 -delete

# Rotate Docker logs
for container in gym-auth-service gym-training-service gym-tracking-service gym-notification-service; do
    docker logs --tail 1000 "$container" > "/var/log/gym/${container}.log.old"
done

# Clean up temp files
rm -rf /tmp/gym-*

echo "Log cleanup completed"
```

### Friday - Security Scanning

```bash
#!/bin/bash
# scripts/maintenance/weekly-security-scan.sh

echo "=== Weekly Security Scan ==="

# Scan Docker images for vulnerabilities
echo "Scanning Docker images..."
docker scan gym-auth-service:latest
docker scan gym-training-service:latest
docker scan gym-tracking-service:latest
docker scan gym-notification-service:latest

# Check for exposed secrets
echo "Checking for exposed secrets..."
git log --all --oneline -S "password\|secret\|token" | head -20

# Review recent changes to security-critical files
echo "Security-critical file changes:"
git log --oneline -- "config/**/*" "docker-compose*.yml" | head -10

# Check for outdated dependencies
echo "Checking for vulnerable dependencies..."
./mvnw dependency-check:check 2>/dev/null || npm audit 2>/dev/null

echo "Security scan completed"
```

## Monthly Maintenance Tasks

### First Monday - Dependency Updates

```bash
#!/bin/bash
# scripts/maintenance/monthly-dependency-update.sh

echo "=== Monthly Dependency Update ==="

# Create update branch
git checkout -b chore/dependency-updates-$(date +%Y%m)

# Update Maven dependencies
./mvnw versions:display-dependency-updates
./mvnw versions:use-latest-releases -DallowMajorUpdates=false

# Update Docker base images
sed -i 's/openjdk:17-jdk.*/openjdk:17-jdk-slim/' Dockerfile
sed -i 's/postgres:15-.*/postgres:15-alpine/' docker-compose.yml

# Run tests
./mvnw clean test

# Commit and create PR
git add -A
git commit -m "chore: update dependencies ($(date +%Y-%m))"
git push origin chore/dependency-updates-$(date +%Y%m)

echo "Dependency update PR created"
echo "Review & merge after CI passes"
```

### Third Tuesday - Capacity Planning Review

```bash
#!/bin/bash
# scripts/maintenance/monthly-capacity-review.sh

echo "=== Monthly Capacity Review ==="

# Analyze growth trends
echo "Storage growth (last 30 days):"
du -sh /var/lib/postgresql/data | awk '{print $1}'

# Analyze traffic patterns
echo "Request rate trend:"
curl -s 'http://localhost:9090/api/v1/query_range?query=rate(http_requests_total[1h])&start=30d&step=1d' \
    | jq '.data.result[0].values[-1]'

# Predict scaling needs
echo "Projected capacity needs (6 months):"
# Calculate growth rate and project

# Check for unused resources
echo "Unused resources:"
docker ps -a | grep Exited
docker images | grep '<none>'

echo "Capacity review completed"
```

## Maintenance Communication

### Announcement Template

```
Subject: Scheduled Maintenance - Saturday 2:00 AM - 4:00 AM UTC

We will perform scheduled maintenance on Saturday from 2:00 AM to 4:00 AM UTC.

During this window, the Gym Platform API may be unavailable or experiencing 
intermittent connectivity issues.

Maintenance Activities:
- Database optimization and indexing
- System updates and patches
- Performance monitoring improvements

We apologize for any inconvenience and appreciate your patience.

Expected duration: 2 hours
Expected downtime: < 10 minutes

For questions, contact support@gym.local
```

## Maintenance Tracking

### Maintenance Log

```yaml
# /var/log/gym/maintenance.log

2024-01-20 02:00:00 - Started weekly database maintenance
2024-01-20 02:05:00 - VACUUM completed (recovered 50MB)
2024-01-20 02:10:00 - ANALYZE completed
2024-01-20 02:15:00 - REINDEX completed
2024-01-20 02:20:00 - Database maintenance finished
2024-01-20 02:25:00 - Services: OK
2024-01-20 02:30:00 - All systems normal

2024-01-22 01:00:00 - Started security scan
2024-01-22 01:30:00 - 2 vulnerabilities found (low severity)
2024-01-22 01:45:00 - Security scan completed
```

## Emergency Maintenance

### Emergency Restart Procedure

```bash
#!/bin/bash
# scripts/maintenance/emergency-restart.sh

echo "EMERGENCY RESTART - All services will be restarted"
echo "This should only be used for critical issues"
read -p "Continue? (y/N)" -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

# Stop all services
docker-compose down

# Clear potentially corrupted data
# WARNING: Only if instructed
# rm -rf /backups/redis/*

# Start all services
docker-compose up -d

# Wait for services to be healthy
sleep 30

# Verify
curl http://localhost:8081/actuator/health | jq

echo "Emergency restart completed"
```

## Maintenance Checklist

- [ ] Scheduled maintenance window announced
- [ ] All stakeholders notified
- [ ] Database backups created before maintenance
- [ ] Maintenance scripts tested in dev
- [ ] On-call engineer assigned
- [ ] Rollback plan documented
- [ ] Monitoring tools ready
- [ ] Status page prepared
- [ ] Post-maintenance verification done
- [ ] Maintenance report documented

## Key References

- [PostgreSQL Maintenance](https://www.postgresql.org/docs/current/routine-vacuuming.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- See also: [docs/operations/02-monitoring.md](02-monitoring.md)
- See also: [docs/operations/04-backup-recovery.md](04-backup-recovery.md)
