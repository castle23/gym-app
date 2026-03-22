# Backup & Recovery

> **Note**: The advanced backup architecture described in this document (WAL archiving, streaming replication standby, S3 cloud storage, PITR, failover) is **not currently configured**. The current setup is a single PostgreSQL 15 container with a named Docker volume (`postgres_data`). Manual backups can be taken with `pg_dump`. This document describes the target backup architecture for future implementation.

## Overview

Comprehensive backup and recovery strategies for Gym Platform, including backup procedures, recovery strategies, RTO/RPO targets, and disaster recovery planning.

**Key Objectives:**
- RTO (Recovery Time Objective): < 1 hour
- RPO (Recovery Point Objective): < 15 minutes
- Backup frequency: Hourly incremental, daily full
- Retention period: 30 days local, 90 days archive

## Backup Strategy

### Backup Architecture

```
┌──────────────────────────────────────────────────┐
│         Production Database                      │
│     PostgreSQL (Primary Database)                │
└──────────────────┬───────────────────────────────┘
                   │
        ┌──────────┴───────────┐
        │                      │
        ▼                      ▼
    ┌────────────┐    ┌──────────────┐
    │   Local    │    │  Replication │
    │   Backups  │    │   Standby    │
    │ (30 days)  │    │   Database   │
    └────────────┘    └──────────────┘
        │
        ▼
    ┌────────────────────────┐
    │  Cloud Archive         │
    │ (AWS S3 / GCS)         │
    │ (90+ days retention)   │
    └────────────────────────┘
```

### Backup Types

| Type | Frequency | Size | Recovery Time | Use Case |
|------|-----------|------|---------------|----------|
| **Full** | Daily (2 AM) | 100% | Fastest | Weekly/Monthly archive |
| **Incremental** | Hourly | 10-30% | Moderate | Daily recovery |
| **Continuous** | Real-time | Stream | Very Fast | Hot standby |
| **Logical** | Weekly | 50-80% | Slower | Schema/data export |

## Automated Backup Scripts

### Full Backup Script

```bash
#!/bin/bash
# scripts/database/backup-full.sh

set -e

BACKUP_DIR="/backups/postgres/full"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/full_backup_${TIMESTAMP}.dump"
LOG_FILE="${BACKUP_DIR}/logs/backup_${TIMESTAMP}.log"

mkdir -p "${BACKUP_DIR}/logs"

echo "[$(date)] Starting full PostgreSQL backup..." | tee "${LOG_FILE}"

# Perform backup
if pg_dump \
    -h ${DB_HOST:-localhost} \
    -U ${DB_USER:-gym_admin} \
    -Fc \
    -b \
    -v \
    -j 4 \
    gym_db > "${BACKUP_FILE}" 2>> "${LOG_FILE}"; then

    FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "[$(date)] Backup completed successfully (${FILE_SIZE})" >> "${LOG_FILE}"

    # Calculate checksum
    md5sum "${BACKUP_FILE}" > "${BACKUP_FILE}.md5"

    # Upload to cloud (optional)
    if command -v aws &> /dev/null; then
        echo "[$(date)] Uploading to S3..." >> "${LOG_FILE}"
        aws s3 cp "${BACKUP_FILE}" s3://gym-backups/postgres/full/ \
            --region ${AWS_REGION:-us-east-1}
        echo "[$(date)] S3 upload completed" >> "${LOG_FILE}"
    fi

    # Remove backups older than 30 days
    find "${BACKUP_DIR}" -name "full_backup_*.dump" -mtime +30 -delete
    echo "[$(date)] Cleaned up old backups" >> "${LOG_FILE}"

else
    echo "[$(date)] Backup failed" >> "${LOG_FILE}"
    exit 1
fi
```

### Incremental Backup Script

```bash
#!/bin/bash
# scripts/database/backup-incremental.sh

set -e

BACKUP_DIR="/backups/postgres/incremental"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/incr_backup_${TIMESTAMP}.dump"

mkdir -p "${BACKUP_DIR}/logs"

# For PostgreSQL, we'll use WAL archiving instead
# This script demonstrates the concept

pg_dump \
    -h ${DB_HOST:-localhost} \
    -U ${DB_USER:-gym_admin} \
    -Fc \
    -b \
    gym_db > "${BACKUP_FILE}"

echo "Incremental backup created: ${BACKUP_FILE}"

# Clean up backups older than 7 days
find "${BACKUP_DIR}" -name "incr_backup_*.dump" -mtime +7 -delete
```

### Backup Scheduler (Cron)

```bash
# /etc/cron.d/gym-backup-schedule

# Full backup daily at 2 AM
0 2 * * * backup /opt/gym/scripts/database/backup-full.sh >> /var/log/gym/backup-full.log 2>&1

# Incremental backup every 4 hours
0 */4 * * * backup /opt/gym/scripts/database/backup-incremental.sh >> /var/log/gym/backup-incr.log 2>&1

# Backup verification every 6 hours
0 */6 * * * backup /opt/gym/scripts/database/verify-backup.sh >> /var/log/gym/backup-verify.log 2>&1

# Database statistics update daily
0 3 * * * backup /opt/gym/scripts/database/analyze-database.sh >> /var/log/gym/db-analyze.log 2>&1
```

## Backup Verification

### Backup Integrity Check

```bash
#!/bin/bash
# scripts/database/verify-backup.sh

set -e

BACKUP_DIR="/backups/postgres/full"
LATEST_BACKUP=$(ls -t "${BACKUP_DIR}"/full_backup_*.dump | head -1)

if [ ! -f "${LATEST_BACKUP}" ]; then
    echo "No backup found!"
    exit 1
fi

echo "Verifying backup: ${LATEST_BACKUP}"

# Check file size
FILE_SIZE=$(stat -c%s "${LATEST_BACKUP}")
if [ "$FILE_SIZE" -lt 1000000 ]; then
    echo "ERROR: Backup file too small (${FILE_SIZE} bytes)"
    exit 1
fi

# Verify MD5 checksum
if [ -f "${LATEST_BACKUP}.md5" ]; then
    if md5sum -c "${LATEST_BACKUP}.md5"; then
        echo "Checksum verification passed"
    else
        echo "ERROR: Checksum verification failed"
        exit 1
    fi
fi

# Test restore in isolated container
echo "Testing restore from backup..."
docker run --rm \
    -v "${LATEST_BACKUP}:/backup.dump" \
    -e PGPASSWORD="${DB_PASSWORD}" \
    postgres:15-alpine \
    pg_restore \
        -h gym-postgres \
        -U postgres \
        -d test_db \
        /backup.dump

echo "Backup verification completed successfully"
```

## Recovery Procedures

### Point-in-Time Recovery (PITR)

**Configuration (postgresql.conf):**
```ini
# Enable WAL archiving
wal_level = replica
archive_mode = on
archive_command = 'cp %p /mnt/server/archive/%f'
archive_timeout = 300
restore_command = 'cp /mnt/server/archive/%f %p'
```

**Restore procedure:**
```bash
#!/bin/bash
# scripts/database/restore-pitr.sh

BACKUP_FILE=$1
TARGET_TIME=${2:-"2024-01-15 14:30:00"}
RESTORE_DB="gym_db_restored"

echo "Restoring database to point in time: ${TARGET_TIME}"

# Create new database for restoration
psql -U postgres -c "DROP DATABASE IF EXISTS ${RESTORE_DB};"
psql -U postgres -c "CREATE DATABASE ${RESTORE_DB};"

# Restore from backup
pg_restore \
    -U postgres \
    -d "${RESTORE_DB}" \
    "${BACKUP_FILE}"

# Apply WAL files up to target time
echo "Applying WAL files up to ${TARGET_TIME}..."
# WAL recovery is handled by PostgreSQL during startup

echo "Restoration completed. Database: ${RESTORE_DB}"
echo "Verify data, then rename to production database:"
echo "  ALTER DATABASE gym_db RENAME TO gym_db_backup;"
echo "  ALTER DATABASE ${RESTORE_DB} RENAME TO gym_db;"
```

### Full Database Restore

```bash
#!/bin/bash
# scripts/database/restore-full.sh

BACKUP_FILE=$1

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "Backup file not found: ${BACKUP_FILE}"
    exit 1
fi

echo "Starting full restore from: ${BACKUP_FILE}"

# Verify backup
pg_restore -l "${BACKUP_FILE}" | head

# Drop current database (WARNING: Data loss!)
read -p "This will DELETE the current database. Continue? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    psql -U postgres -c "DROP DATABASE IF EXISTS gym_db;"
    psql -U postgres -c "CREATE DATABASE gym_db;"

    # Restore backup
    pg_restore \
        -U postgres \
        -d gym_db \
        --verbose \
        "${BACKUP_FILE}"

    echo "Database restoration completed"
else
    echo "Restore cancelled"
    exit 1
fi
```

## Disaster Recovery Plan

### RTO/RPO Matrix

```
Scenario              RTO    RPO    Recovery Method
─────────────────────────────────────────────────────────
Single table corrupt  30min  5min   Point-in-time restore
Database crash        1hour  15min  Full restore from backup
Server failure        2hour  1hour  Failover to replica
Datacenter loss       4hour  24hour Cloud backup restore
```

### Failover to Standby Database

```bash
#!/bin/bash
# scripts/database/failover-standby.sh

echo "Initiating failover to standby database..."

# Promote standby to primary
ssh standby-host "sudo pg_ctl promote -D /var/lib/postgresql/data"

# Update connection string in application
# Update environment variables in Docker containers
docker-compose down

# Modify .env to point to new primary
sed -i 's/DB_HOST=.*/DB_HOST=standby-host/' .env

docker-compose up -d

# Verify connectivity
sleep 10
curl -f http://localhost:8081/actuator/health || exit 1

echo "Failover completed successfully"
```

## Backup Monitoring

### Backup Health Dashboard

```promql
# Last backup duration
(time() - backup_completion_timestamp) / 3600

# Backup size trend
backup_size_bytes

# Backup failure rate
rate(backup_failures_total[24h])

# Backup coverage (% of data backed up)
backed_up_rows / total_rows * 100
```

### Alert Rules

```yaml
groups:
  - name: backup_alerts
    rules:
      - alert: BackupFailed
        expr: (time() - backup_last_success_timestamp) > 86400  # 24 hours
        for: 1h
        annotations:
          summary: "Database backup failed"

      - alert: HighBackupSize
        expr: backup_size_bytes > 500 * 1024 * 1024 * 1024  # 500 GB
        annotations:
          summary: "Backup size exceeds threshold"

      - alert: LowBackupSpace
        expr: backup_storage_available_bytes < 100 * 1024 * 1024 * 1024
        for: 5m
        annotations:
          summary: "Backup storage running low"

      - alert: RestoreTestFailed
        expr: backup_restore_test_success == 0
        for: 1h
        annotations:
          summary: "Backup restore test failed"
```

## Docker Compose Backup Configuration

```yaml
services:
  backup-scheduler:
    image: mcuadros/ofelia:latest
    container_name: gym-backup-scheduler
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./scripts/database:/scripts:ro
    command: daemon --docker --swarm
    networks:
      - gym-network
    environment:
      OFELIA_JOB_EXEC_BACKUP_FULL: |
        schedule: @daily
        container: gym-postgres
        command: >
          bash -c "pg_dump -U postgres -Fc gym_db > /backups/full_$(date +\%Y\%m\%d_\%H\%M\%S).dump"

      OFELIA_JOB_EXEC_BACKUP_VERIFY: |
        schedule: @hourly
        container: gym-postgres
        command: >
          bash -c "ls -lah /backups/ | tail -5"
```

## Backup Best Practices

1. **Automate backups** - Don't rely on manual processes
2. **Test restores regularly** - Verify backups are usable
3. **Store geographically** - Keep copies in different regions
4. **Encrypt backups** - Use encryption at rest and in transit
5. **Document procedures** - Keep runbooks up to date
6. **Monitor backup success** - Alert on failures
7. **Track retention** - Comply with data retention policies
8. **Version control configs** - Track backup configuration changes
9. **Capacity plan** - Ensure enough storage for growth
10. **Practice DR** - Run disaster recovery drills

## Key References

- [PostgreSQL Backup and Restore](https://www.postgresql.org/docs/current/backup.html)
- [pg_dump Documentation](https://www.postgresql.org/docs/current/app-pgdump.html)
- [WAL Archiving](https://www.postgresql.org/docs/current/wal-intro.html)
- [AWS S3 Backup Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/BestPractices.html)
- See also: [dba/initialization/README.md](../../dba/initialization/README.md)
- See also: [docs/troubleshooting/](../troubleshooting/)
