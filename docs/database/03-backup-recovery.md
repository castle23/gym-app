# Backup & Recovery

> **Note**: This document describes target operational procedures. The current deployment is a single PostgreSQL instance in Docker. HA features (replicas, WAL archiving, S3 backups) are not yet configured.

## Overview

Comprehensive backup and disaster recovery procedures for PostgreSQL in the Gym Platform. This guide covers backup strategies, recovery scenarios, point-in-time recovery (PITR), backup verification, and disaster recovery best practices. The Gym Platform implements multiple backup layers to ensure data durability with configurable RTO/RPO targets.

## Table of Contents

- [Backup Strategy](#backup-strategy)
- [Backup Types](#backup-types)
- [Backup Scheduling](#backup-scheduling)
- [WAL Archiving](#wal-archiving)
- [Backup Implementation](#backup-implementation)
- [Backup Verification](#backup-verification)
- [Recovery Procedures](#recovery-procedures)
- [Point-in-Time Recovery](#point-in-time-recovery)
- [Disaster Recovery](#disaster-recovery)
- [RTO/RPO Configuration](#rto-rpo-configuration)
- [Backup Monitoring](#backup-monitoring)
- [Restoration Testing](#restoration-testing)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Backup Strategy

### Backup Architecture

The Gym Platform uses a multi-layered backup strategy:

```
┌──────────────────────────────────────────────────────────┐
│              Backup Strategy Architecture                 │
└──────────────────────────────────────────────────────────┘

├─ Layer 1: Full Backups (Daily)
│  ├─ Storage: AWS S3 / Azure Blob Storage
│  ├─ Retention: 30 days
│  └─ Size: ~2-3GB per database
│
├─ Layer 2: Incremental Backups (Every 6 hours)
│  ├─ Storage: AWS S3 / Azure Blob Storage
│  ├─ Retention: 7 days
│  └─ Built from WAL files
│
├─ Layer 3: WAL Archiving (Continuous)
│  ├─ Storage: S3 / Azure Blob / Filesystem
│  ├─ Retention: 30 days
│  └─ Enables PITR (Point-in-Time Recovery)
│
└─ Layer 4: Replication (Real-time)
   ├─ Storage: Replica servers
   ├─ Retention: Always active
   └─ RPO: ~1 second
```

### Backup Objectives

**Recovery Time Objective (RTO):**
- Full recovery: 30 minutes
- Replica promotion: 2 minutes
- Point-in-time recovery: 45 minutes

**Recovery Point Objective (RPO):**
- Synchronous replication: 0 seconds (no data loss)
- PITR recovery: 5 minutes (WAL archiving frequency)

---

## Backup Types

### 1. Full Backups

Complete database backup using `pg_basebackup` or Patroni backup commands.

**Full Backup Script:**

```bash
#!/bin/bash
# File: scripts/backup-full.sh

BACKUP_DIR="/var/lib/postgresql/backups"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="gym_db_full_${BACKUP_DATE}.tar.gz"
S3_BUCKET="gym-platform-backups"
LOG_FILE="/var/log/postgresql/backup_${BACKUP_DATE}.log"

# Colors for logging
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

log_info "Starting full database backup: $BACKUP_NAME"

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

# Create backup
log_info "Executing pg_basebackup..."
pg_basebackup \
  --pgdata="$BACKUP_DIR/gym_db_backup" \
  --format=tar \
  --gzip \
  --verbose \
  --progress \
  --label="gym_full_backup_${BACKUP_DATE}" \
  --no-password \
  2>&1 | tee -a "$LOG_FILE"

if [ $? -ne 0 ]; then
    log_error "pg_basebackup failed"
    exit 1
fi

# Move backup to final location
log_info "Compressing backup..."
cd "$BACKUP_DIR" || exit 1
tar -czf "$BACKUP_NAME" gym_db_backup/ && rm -rf gym_db_backup/

if [ $? -ne 0 ]; then
    log_error "Compression failed"
    exit 1
fi

# Calculate backup size and checksum
BACKUP_SIZE=$(du -sh "$BACKUP_NAME" | cut -f1)
BACKUP_CHECKSUM=$(sha256sum "$BACKUP_NAME" | awk '{print $1}')

log_info "Backup size: $BACKUP_SIZE"
log_info "Backup checksum: $BACKUP_CHECKSUM"

# Upload to S3
log_info "Uploading backup to S3..."
aws s3 cp "$BACKUP_NAME" "s3://$S3_BUCKET/full-backups/$BACKUP_NAME" \
  --metadata "date=$BACKUP_DATE,size=$BACKUP_SIZE,checksum=$BACKUP_CHECKSUM" \
  --storage-class STANDARD_IA \
  2>&1 | tee -a "$LOG_FILE"

if [ $? -ne 0 ]; then
    log_error "S3 upload failed"
    exit 1
fi

# Verify S3 upload
log_info "Verifying S3 upload..."
aws s3api head-object --bucket "$S3_BUCKET" --key "full-backups/$BACKUP_NAME" \
  2>&1 | tee -a "$LOG_FILE"

if [ $? -eq 0 ]; then
    log_info "Backup verification successful"
    # Clean up local backup
    rm -f "$BACKUP_NAME"
    log_info "Full backup completed successfully"
else
    log_error "S3 verification failed - keeping local backup"
    exit 1
fi

# Log backup completion
log_info "Full backup completed: $BACKUP_NAME"
log_info "Backup size: $BACKUP_SIZE"
log_info "Backup checksum: $BACKUP_CHECKSUM"
```

**Using Patroni:**

```bash
# Create backup through Patroni REST API
curl -X POST http://patroni-node:8008/backup

# Check backup status
curl http://patroni-node:8008/backup

# Response example:
# {
#   "backup": {
#     "backup_start_wal_lsn": "0/2000000",
#     "backup_wal_dir": "/var/lib/postgresql/wal_archive",
#     "backup_end_lsn": "0/3000000",
#     "backup_line_id": "20260321_150000",
#     "time_elapsed": "5 minutes 30 seconds"
#   }
# }
```

### 2. Incremental Backups

Backups based on WAL file changes since last full backup.

**Incremental Backup Script:**

```bash
#!/bin/bash
# File: scripts/backup-incremental.sh

BACKUP_DIR="/var/lib/postgresql/backups"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="gym_db_incremental_${BACKUP_DATE}.tar.gz"
S3_BUCKET="gym-platform-backups"
LAST_LSN_FILE="/var/lib/postgresql/.last_backup_lsn"

log_info() {
    echo "[INFO] $1" | tee -a "/var/log/postgresql/backup_incremental.log"
}

# Get current LSN
CURRENT_LSN=$(psql -U postgres -t -c "SELECT pg_current_wal_lsn();")
LAST_LSN=$(cat "$LAST_LSN_FILE" 2>/dev/null || echo "0/0")

log_info "Creating incremental backup from LSN: $LAST_LSN to $CURRENT_LSN"

# Compress WAL files from last backup
find /var/lib/postgresql/pg_wal -name "*.backup" -o -name "*.ready" | \
  tar -czf "$BACKUP_DIR/$BACKUP_NAME" --files-from - 2>&1

# Upload to S3
aws s3 cp "$BACKUP_DIR/$BACKUP_NAME" \
  "s3://$S3_BUCKET/incremental-backups/$BACKUP_NAME"

# Update last LSN
echo "$CURRENT_LSN" > "$LAST_LSN_FILE"

log_info "Incremental backup completed: $BACKUP_NAME"
```

### 3. Differential Backups

Backups of blocks changed since the last backup (full or incremental).

**Differential Backup Configuration:**

```bash
# Using pg_basebackup with manifest
pg_basebackup \
  --pgdata=/backup/differential \
  --format=plain \
  --manifest \
  --manifest-checksum=sha256 \
  --label="gym_differential_$(date +%Y%m%d_%H%M%S)"
```

---

## Backup Scheduling

### Cron Schedule Configuration

**File: /etc/postgresql/14/main/backup-schedule.cron**

```bash
# Full backup: Every day at 2:00 AM UTC
0 2 * * * /usr/local/bin/backup-full.sh >> /var/log/postgresql/backup_full.log 2>&1

# Incremental backup: Every 6 hours (02:00, 08:00, 14:00, 20:00 UTC)
0 2,8,14,20 * * * /usr/local/bin/backup-incremental.sh >> /var/log/postgresql/backup_incremental.log 2>&1

# WAL archiving: Continuous (managed by PostgreSQL)
# Verify WAL archiving status
*/5 * * * * /usr/local/bin/verify-wal-archiving.sh >> /var/log/postgresql/wal_archive_check.log 2>&1

# Backup cleanup: Remove backups older than 30 days
0 3 * * * /usr/local/bin/cleanup-old-backups.sh >> /var/log/postgresql/backup_cleanup.log 2>&1
```

### Systemd Timer Alternative

**File: /etc/systemd/system/postgresql-backup.service**

```ini
[Unit]
Description=PostgreSQL Full Backup for Gym Platform
After=postgresql.service
Requires=postgresql.service

[Service]
Type=oneshot
User=postgres
Group=postgres
ExecStart=/usr/local/bin/backup-full.sh
StandardOutput=journal
StandardError=journal
Environment="PGUSER=postgres"
Environment="PGHOST=localhost"
```

**File: /etc/systemd/system/postgresql-backup.timer**

```ini
[Unit]
Description=PostgreSQL Full Backup Timer (Daily at 02:00 UTC)
Requires=postgresql-backup.service

[Timer]
OnCalendar=daily
OnCalendar=*-*-* 02:00:00
Persistent=true
Unit=postgresql-backup.service
AccuracySec=1s

[Install]
WantedBy=timers.target
```

**Enable and start the timer:**

```bash
sudo systemctl daemon-reload
sudo systemctl enable postgresql-backup.timer
sudo systemctl start postgresql-backup.timer

# Verify
sudo systemctl status postgresql-backup.timer
sudo systemctl list-timers postgresql-backup.timer
```

---

## WAL Archiving

### WAL Archiving Configuration

**PostgreSQL Configuration: /etc/postgresql/14/main/postgresql.conf**

```ini
# WAL Archiving Settings
wal_level = replica                    # Enable archiving (replica or higher)
archive_mode = on                      # Enable archiving
archive_command = '/usr/local/bin/archive-wal.sh "%p" "%f"'
archive_timeout = 300                 # Archive every 5 minutes even if WAL not full
max_wal_senders = 5                    # Max streaming replication connections
wal_keep_size = 1GB                    # Keep 1GB of WAL for replicas

# WAL Settings for Backup
wal_compression = on                   # Compress WAL files
wal_buffers = 16MB                     # WAL buffer size
checkpoint_completion_target = 0.9     # Smooth checkpoint progress
```

### WAL Archive Script

**File: /usr/local/bin/archive-wal.sh**

```bash
#!/bin/bash
# Archive WAL files to S3 for PITR

WAL_FILE="$1"  # Full path to WAL file (e.g., /pg_wal/000000010000000000000001)
WAL_NAME="$2"  # WAL filename only (e.g., 000000010000000000000001)
S3_BUCKET="gym-platform-backups"
S3_PREFIX="wal-archive/$(date +%Y/%m/%d)"
RETRY_COUNT=5
RETRY_DELAY=5

log_archive() {
    logger -t postgresql-wal-archive "$1"
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" >> /var/log/postgresql/wal_archive.log
}

# Verify WAL file exists
if [ ! -f "$WAL_FILE" ]; then
    log_archive "ERROR: WAL file not found: $WAL_FILE"
    exit 1
fi

# Upload to S3 with retry
for i in $(seq 1 $RETRY_COUNT); do
    log_archive "Uploading $WAL_NAME (attempt $i/$RETRY_COUNT)"
    
    aws s3 cp "$WAL_FILE" "s3://$S3_BUCKET/$S3_PREFIX/$WAL_NAME" \
        --storage-class GLACIER \
        --metadata "upload_date=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        --sse AES256
    
    if [ $? -eq 0 ]; then
        log_archive "Successfully archived: $WAL_NAME"
        exit 0
    fi
    
    if [ $i -lt $RETRY_COUNT ]; then
        log_archive "Upload failed, retrying in ${RETRY_DELAY}s..."
        sleep $RETRY_DELAY
    fi
done

# Final failure
log_archive "ERROR: Failed to archive $WAL_NAME after $RETRY_COUNT attempts"
exit 1
```

### Monitor WAL Archiving

**Check archiving status:**

```sql
-- Current archiving stats
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    (SELECT count(*) FROM pg_stat_all_tables WHERE schemaname = 'pg_catalog') AS index_count
FROM pg_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- WAL archiving failures
SELECT * FROM pg_stat_archiver;

-- Expected output:
--  archived_count | failed_count | last_archived_wal | last_failed_wal | last_failed_time
--  12345          | 0            | 000000010...00123 | (null)          | (null)
```

**Verify archiving in S3:**

```bash
# Count archived WAL files
aws s3 ls s3://gym-platform-backups/wal-archive/ --recursive --summarize

# Check recent archiving
aws s3 ls s3://gym-platform-backups/wal-archive/ --recursive \
  --query 'Contents[?LastModified>=`2026-03-21`]' \
  --output table
```

---

## Backup Implementation

### Using pg_basebackup

**Manual full backup:**

```bash
# Create backup as tar
pg_basebackup \
  --pgdata=/var/lib/postgresql/backup \
  --format=tar \
  --gzip \
  --verbose \
  --progress \
  --label="manual_backup_$(date +%Y%m%d_%H%M%S)" \
  --slot=backup_slot \
  --checkpoint=fast \
  --no-password

# Backup statistics
tar -tzf backup.tar.gz | wc -l  # Number of files
tar -xzf backup.tar.gz --stat  # Detailed statistics
```

### Using Patroni

**Patroni backup commands:**

```bash
# Check Patroni status
patronictl -c /etc/patroni/patroni.yml list

# Create backup
patronictl -c /etc/patroni/patroni.yml backup gym-db-primary

# Monitor backup progress
curl -s http://localhost:8008/backup | jq .

# List recent backups
curl -s http://localhost:8008/backups | jq '.'

# Example response:
# {
#   "backups": [
#     {
#       "backup_start_lsn": "0/2000000",
#       "backup_end_lsn": "0/3000000",
#       "base_timeline_id": 1,
#       "backup_label": "gym_backup_20260321",
#       "backup_method": "pg_basebackup",
#       "backup_timeline": 1,
#       "database_system_identifier": "7182900961144268871",
#       "time_elapsed": "5 minutes",
#       "wal_segment_backup_info": "000000010000000000000002"
#     }
#   ]
# }
```

### Using pgBackRest

**Configuration: /etc/pgbackrest.conf**

```ini
[global]
# Repository settings
repo-path=/var/lib/pgbackrest
repo-retention-full=30
repo-retention-full-type=count
repo-s3-bucket=gym-platform-backups
repo-s3-endpoint=s3.amazonaws.com
repo-s3-key=YOUR_AWS_KEY
repo-s3-key-secret=YOUR_AWS_SECRET
repo-type=s3

# Database connection
db-path=/var/lib/postgresql/14/main
db-port=5432
db-user=postgres

# Backup settings
backup-standby=y              # Backup from replica
process-max=4                 # Parallel processes
buffer-size=16MB

# Archive settings
archive-push-queue-max=4GB
archive-timeout=60s
start-fast=y

[gym_db]
db-user=postgres
```

**pgBackRest backup commands:**

```bash
# Initialize repository
pgbackrest --stanza=gym_db stanza-create

# Full backup
pgbackrest --stanza=gym_db backup --type=full

# Incremental backup
pgbackrest --stanza=gym_db backup --type=incr

# Differential backup
pgbackrest --stanza=gym_db backup --type=diff

# Check backup status
pgbackrest --stanza=gym_db info

# Example output:
# stanza: gym_db
#     status: ok
#     wal archive min/max (14-1): 000000010000000000000001/000000010000000000001234
#
#     full backup: 20260321-150000F
#         timestamp start/stop: 2026-03-21 15:00:00 / 2026-03-21 15:05:30
#         wal display name: 000000010000000000000001
#         database size: 2.4 GB, backup size: 2.3 GB
#         repository size: 1.8 GB, repository backup size: 1.7 GB
```

---

## Backup Verification

### Verify Backup Integrity

**File: /usr/local/bin/verify-backup.sh**

```bash
#!/bin/bash
# Verify backup integrity and test restoration

BACKUP_FILE="$1"
BACKUP_NAME=$(basename "$BACKUP_FILE")
VERIFY_DIR="/tmp/backup_verify_${BACKUP_NAME}"
TEST_DB_PORT=5435

log_info() {
    echo "[INFO] $1"
}

log_error() {
    echo "[ERROR] $1"
    exit 1
}

# Extract backup
log_info "Extracting backup: $BACKUP_NAME"
mkdir -p "$VERIFY_DIR"
cd "$VERIFY_DIR" || exit 1
tar -xzf "$BACKUP_FILE" || log_error "Failed to extract backup"

# Verify backup_label file
log_info "Verifying backup label..."
if [ -f "$VERIFY_DIR/backup_label" ]; then
    log_info "Backup label found:"
    cat "$VERIFY_DIR/backup_label"
else
    log_error "Missing backup_label file"
fi

# Check for pg_control file
log_info "Verifying pg_control file..."
if [ -f "$VERIFY_DIR/global/pg_control" ]; then
    log_info "pg_control file found"
    # Check validity
    pg_controldata "$VERIFY_DIR" || log_error "pg_control validation failed"
else
    log_error "Missing pg_control file"
fi

# Verify manifest (if available)
if [ -f "$VERIFY_DIR/backup_manifest" ]; then
    log_info "Verifying backup manifest..."
    pg_verifybackup -P "$VERIFY_DIR" || log_error "Manifest verification failed"
fi

# Test restore
log_info "Starting test restore..."
mkdir -p "/var/lib/postgresql/test_restore"
cp -r "$VERIFY_DIR"/* "/var/lib/postgresql/test_restore/" 2>/dev/null

# Start PostgreSQL on test port
log_info "Starting test PostgreSQL instance on port $TEST_DB_PORT..."
pg_ctl -D "/var/lib/postgresql/test_restore" \
  -o "-p $TEST_DB_PORT" \
  -l "/var/log/postgresql/restore_test.log" \
  start

if [ $? -ne 0 ]; then
    log_error "Failed to start PostgreSQL with restored backup"
fi

sleep 5

# Run basic queries
log_info "Running verification queries..."
PGPORT=$TEST_DB_PORT psql -U postgres -d postgres -c "SELECT version();" \
    || log_error "Failed to connect to restored database"

PGPORT=$TEST_DB_PORT psql -U postgres -d postgres -c "SELECT datname FROM pg_database ORDER BY datname;" \
    || log_error "Failed to query restored database"

# Check database integrity
log_info "Running ANALYZE on all tables..."
PGPORT=$TEST_DB_PORT psql -U postgres -d postgres -c "ANALYZE;" \
    || log_error "ANALYZE failed"

# Stop test instance
log_info "Stopping test PostgreSQL instance..."
pg_ctl -D "/var/lib/postgresql/test_restore" stop

# Cleanup
log_info "Cleaning up test restore..."
rm -rf "/var/lib/postgresql/test_restore"
rm -rf "$VERIFY_DIR"

log_info "Backup verification completed successfully!"
```

**Run verification:**

```bash
chmod +x /usr/local/bin/verify-backup.sh
/usr/local/bin/verify-backup.sh /path/to/backup.tar.gz
```

### Backup Health Check

**Monitor backup age and success:**

```bash
#!/bin/bash
# Check backup freshness

BACKUP_DIR="/var/lib/postgresql/backups"
MAX_BACKUP_AGE_HOURS=26  # Alert if backup older than 26 hours
S3_BUCKET="gym-platform-backups"

# Get latest local backup
LATEST_BACKUP=$(ls -t "$BACKUP_DIR"/gym_db_full_*.tar.gz 2>/dev/null | head -1)

if [ -z "$LATEST_BACKUP" ]; then
    echo "CRITICAL: No backup found in $BACKUP_DIR"
    exit 2
fi

# Check backup age
BACKUP_TIME=$(stat -f%m "$LATEST_BACKUP" 2>/dev/null || stat -c%Y "$LATEST_BACKUP")
CURRENT_TIME=$(date +%s)
BACKUP_AGE_HOURS=$(( (CURRENT_TIME - BACKUP_TIME) / 3600 ))

if [ "$BACKUP_AGE_HOURS" -gt "$MAX_BACKUP_AGE_HOURS" ]; then
    echo "WARNING: Last backup is $BACKUP_AGE_HOURS hours old (limit: $MAX_BACKUP_AGE_HOURS)"
    exit 1
else
    echo "OK: Last backup is $BACKUP_AGE_HOURS hours old"
    echo "Backup size: $(du -sh "$LATEST_BACKUP" | cut -f1)"
    exit 0
fi
```

---

## Recovery Procedures

### Full Database Recovery

**Scenario: Complete data loss or primary database failure**

**Step 1: Stop PostgreSQL on affected server:**

```bash
sudo systemctl stop postgresql
# or
sudo pg_ctl -D /var/lib/postgresql/14/main stop -m fast
```

**Step 2: Download backup from S3:**

```bash
# List available backups
aws s3 ls s3://gym-platform-backups/full-backups/ --human-readable

# Download specific backup
aws s3 cp s3://gym-platform-backups/full-backups/gym_db_full_20260321_020000.tar.gz \
  /tmp/recovery_backup.tar.gz

# Verify download
sha256sum /tmp/recovery_backup.tar.gz
```

**Step 3: Prepare recovery environment:**

```bash
# Backup current (corrupted) data directory
sudo mv /var/lib/postgresql/14/main /var/lib/postgresql/14/main.corrupted

# Create new data directory
sudo mkdir -p /var/lib/postgresql/14/main
sudo chown postgres:postgres /var/lib/postgresql/14/main
sudo chmod 700 /var/lib/postgresql/14/main

# Extract backup
cd /tmp
tar -xzf recovery_backup.tar.gz
sudo mv gym_db_backup/* /var/lib/postgresql/14/main/
```

**Step 4: Recovery configuration:**

**File: /var/lib/postgresql/14/main/recovery.conf (PostgreSQL < 12)**

```ini
restore_command = 'aws s3 cp s3://gym-platform-backups/wal-archive/%f %p'
recovery_target_timeline = 'latest'
```

**For PostgreSQL 12+, use postgresql.conf:**

```ini
restore_command = 'aws s3 cp s3://gym-platform-backups/wal-archive/%f %p'
recovery_target_timeline = 'latest'
archive_mode = off   # Disable archiving during recovery
```

**Step 5: Start PostgreSQL:**

```bash
sudo systemctl start postgresql

# Monitor recovery progress
sudo tail -f /var/log/postgresql/postgresql.log

# Watch for messages like:
# LOG: started streaming WAL from primary at 0/2000000
# LOG: restored log file "00000001000000000000000F" from archive
# LOG: redo done at 0/3000000
# LOG: recovery complete at 0/3000000
```

**Step 6: Verify recovery:**

```bash
# Connect to database
sudo -u postgres psql

-- Verify recovery completion
SELECT datname, datcollate FROM pg_database ORDER BY datname;

-- Check table counts
SELECT schemaname, COUNT(*) FROM pg_tables GROUP BY schemaname;

-- Verify recent data (trainer records)
SELECT COUNT(*) FROM auth.users;
SELECT COUNT(*) FROM training.trainers;
SELECT COUNT(*) FROM tracking.workouts;
```

### Partial Recovery

**Scenario: Specific table corruption or data loss**

**Recover specific table from backup:**

```bash
# 1. Restore entire database to temporary location
mkdir -p /tmp/recovery_temp
cd /tmp/recovery_temp
tar -xzf /tmp/recovery_backup.tar.gz

# 2. Start PostgreSQL on alternative port with restored data
pg_ctl -D /tmp/recovery_temp/gym_db_backup \
  -o "-p 5433" \
  -l /tmp/recovery_recovery.log \
  start

# 3. Dump corrupted table
PGPORT=5433 pg_dump -U postgres gym_db -t auth.users \
  > /tmp/users_table_dump.sql

# 4. Stop recovery instance
pg_ctl -D /tmp/recovery_temp/gym_db_backup stop

# 5. Import table into production database
psql -U postgres gym_db < /tmp/users_table_dump.sql

# 6. Verify import
psql -U postgres -c "SELECT COUNT(*) FROM auth.users;"
```

### Replica Promotion

**Scenario: Primary failure with working replicas**

**Manual replica promotion:**

```bash
# 1. On replica server, promote to primary
sudo -u postgres pg_ctl promote -D /var/lib/postgresql/14/main

# Monitor promotion
sudo tail -f /var/log/postgresql/postgresql.log

# Expected log messages:
# LOG: received promote request
# LOG: redo done at 0/3000000
# LOG: selected new timeline ID: 2
# LOG: archive recovery complete
# LOG: database system is ready to accept connections
```

**Using Patroni:**

```bash
# Promote specific replica through Patroni
patronictl -c /etc/patroni/patroni.yml failover \
  --candidate gym-db-replica1 \
  --force

# Or trigger automatic failover
# (Patroni handles this automatically with quorum)

# Verify new primary
patronictl -c /etc/patroni/patroni.yml list

# Output shows new leader:
# + Cluster: gym-db (6980434808321234567) ----+
# | Member        | Host           | Role         | State   | TL | Lag in MB |
# +-----------+----+--------+--------+------+-----------+
# | gym-db-primary | 10.0.1.50      | Replica      | running | 2  | 0        |
# | gym-db-replica1| 10.0.1.51      | Leader       | running | 2  | 0        |
# | gym-db-replica2| 10.0.1.52      | Replica      | running | 2  | 0        |
```

---

## Point-in-Time Recovery

### PITR Using WAL Archives

**Scenario: Accidental data deletion at 15:30, need to recover to 15:25**

**Step 1: Identify recovery target:**

```bash
# Find LSN or timestamp
# Example: Need to recover to 2026-03-21 15:25:00 UTC

# Find corresponding WAL file
aws s3 ls s3://gym-platform-backups/wal-archive/ --recursive | grep "2026/03/21"

# Find LSN from WAL file name
# WAL filename: 000000010000000000000100
# LSN: 0/01000000
```

**Step 2: Prepare PITR recovery:**

```bash
# Extract full backup
tar -xzf gym_db_full_20260321_020000.tar.gz -C /var/lib/postgresql/recovery

# Configure recovery target
cat > /var/lib/postgresql/recovery/recovery.conf <<EOF
restore_command = 'aws s3 cp s3://gym-platform-backups/wal-archive/%f %p'
recovery_target_name = 'before_deletion'
recovery_target_timeline = 'latest'
recovery_target_type = 'immediate'
EOF

# Or specify exact timestamp
cat > /var/lib/postgresql/recovery/recovery.conf <<EOF
restore_command = 'aws s3 cp s3://gym-platform-backups/wal-archive/%f %p'
recovery_target_time = '2026-03-21 15:25:00 UTC'
recovery_target_timeline = 'latest'
EOF

# Or specify LSN
cat > /var/lib/postgresql/recovery/recovery.conf <<EOF
restore_command = 'aws s3 cp s3://gym-platform-backups/wal-archive/%f %p'
recovery_target_lsn = '0/01000000'
recovery_target_timeline = 'latest'
EOF
```

**Step 3: Start PITR:**

```bash
# Start PostgreSQL with recovery settings
pg_ctl -D /var/lib/postgresql/recovery start

# Monitor recovery
tail -f /var/log/postgresql/recovery.log

# Log output:
# LOG: database system was interrupted while in recovery at log time 2026-03-21 15:35:00
# LOG: starting point-in-time recovery to 2026-03-21 15:25:00
# LOG: restored log file "000000010000000000000100" from archive
# ...
# LOG: recovery stopping before commit of transaction 12345, time 2026-03-21 15:25:00
# LOG: database system is ready to accept read-only connections
```

**Step 4: Verify and switch:**

```bash
# Test queries on recovered database
psql -U postgres gym_db -c "SELECT * FROM auth.users ORDER BY created_at DESC LIMIT 5;"

# If satisfied, restart primary
pg_ctl -D /var/lib/postgresql/recovery stop

# Update production data directory
rm -rf /var/lib/postgresql/14/main.backup
mv /var/lib/postgresql/14/main /var/lib/postgresql/14/main.backup
mv /var/lib/postgresql/recovery /var/lib/postgresql/14/main

# Start production database
systemctl start postgresql
```

---

## Disaster Recovery

### DR Failover Plan

**Multi-site disaster recovery setup:**

```yaml
Primary Data Center (East):
├── gym-db-primary (10.0.1.50:5432)
├── gym-db-replica1 (10.0.1.51:5433)
└── WAL Archive (S3 with local redundancy)

Secondary Data Center (West):
├── gym-db-dr (10.1.1.50:5432) - Standby replica
├── Backup storage (S3 cross-region replication)
└── Recovery procedures documented
```

**Failover checklist:**

```
PHASE 1: Detection (0-5 minutes)
☐ Detect primary failure (health check timeout)
☐ Confirm loss of connectivity
☐ Verify replica status
☐ Check WAL archiving status
☐ Notify on-call team (PagerDuty)

PHASE 2: Failover Decision (5-10 minutes)
☐ Verify quorum (2 of 3 replicas available)
☐ Check replication lag (< 1 second)
☐ Review transaction consistency
☐ Approve failover (change advisory)

PHASE 3: Promotion (10-15 minutes)
☐ Promote best replica to primary
☐ Verify promotion success
☐ Update DNS/VIP to new primary
☐ Start streaming replication from new primary
☐ Verify application connectivity

PHASE 4: Recovery (15-30 minutes)
☐ Investigate primary failure
☐ Prepare old primary for re-join
☐ Rebuild old primary from WAL archive
☐ Re-add as replica/standby
☐ Verify replication health

PHASE 5: Cleanup (30+ minutes)
☐ Review transaction log for anomalies
☐ Verify data consistency across sites
☐ Update runbooks with lessons learned
☐ Conduct post-incident review
```

---

## RTO/RPO Configuration

### Targets by Service

**Auth Service (Critical):**

```yaml
RTO: 15 minutes
RPO: 5 minutes
Strategy:
  - Synchronous replication to 2 replicas
  - Hourly full backups
  - 6-hourly incremental backups
  - Continuous WAL archiving
  - Active-active (if feasible) or active-passive with quick promotion
```

**Training Service:**

```yaml
RTO: 30 minutes
RPO: 15 minutes
Strategy:
  - Asynchronous replication to 1 replica
  - Daily full backups
  - 12-hourly incremental backups
  - Continuous WAL archiving
  - Active-passive with Patroni failover
```

**Tracking Service:**

```yaml
RTO: 60 minutes
RPO: 1 hour
Strategy:
  - Asynchronous replication to replica
  - Daily full backups
  - WAL archiving (daily)
  - Point-in-time recovery capability
```

### Achieving RTO/RPO

**Configuration optimizations:**

```ini
# For low RPO (minimize data loss)
synchronous_commit = remote_apply     # Wait for replica flush to disk
fsync = on                             # Ensure disk writes
wal_sync_method = fsync                # WAL sync method
checkpoint_timeout = 15min             # Regular checkpoints
wal_buffers = 16MB                     # Larger WAL buffer
full_page_writes = on                  # Full page writes for safety

# For low RTO (fast recovery)
max_wal_senders = 10                   # More replication connections
wal_keep_size = 2GB                    # Keep more WAL for replicas
recovery_prefetch = try                # Prefetch during recovery (v14+)
recovery_max_workers = 4               # Parallel recovery processes
```

---

## Backup Monitoring

### Prometheus Metrics

**Backup success rate:**

```promql
# Backup completion rate
rate(postgresql_backup_total{status="success"}[1h])

# Backup failure rate
rate(postgresql_backup_total{status="failed"}[1h])

# Backup duration
postgresql_backup_duration_seconds

# Backup size
postgresql_backup_size_bytes

# WAL archiving lag
postgresql_wal_lsn_received - postgresql_wal_lsn_replayed
```

**Alert rules:**

```yaml
groups:
  - name: postgresql_backup
    rules:
      - alert: BackupFailure
        expr: increase(postgresql_backup_total{status="failed"}[1h]) > 0
        for: 5m
        annotations:
          summary: "PostgreSQL backup failed"
          description: "Backup failed in the last hour"

      - alert: BackupOlderThan24Hours
        expr: (time() - postgresql_last_backup_timestamp) > 86400
        for: 30m
        annotations:
          summary: "Last backup older than 24 hours"
          description: "Last successful backup: {{ $value | humanizeDuration }}"

      - alert: WALArchivingFailed
        expr: rate(postgresql_wal_archive_failed_total[5m]) > 0
        for: 10m
        annotations:
          summary: "WAL archiving failures detected"
          description: "WAL archiving is failing"

      - alert: ReplicationLagHigh
        expr: pg_replication_lag_bytes > 1073741824  # 1GB
        for: 5m
        annotations:
          summary: "Replication lag exceeds 1GB"
          description: "Replication lag: {{ $value | humanize1024 }}B"
```

### Grafana Dashboards

**Dashboard: PostgreSQL Backup Status**

```json
{
  "dashboard": {
    "title": "PostgreSQL Backup Status",
    "panels": [
      {
        "title": "Last Backup Time",
        "targets": [
          {
            "expr": "time() - postgresql_last_backup_timestamp"
          }
        ]
      },
      {
        "title": "Backup Success Rate (24h)",
        "targets": [
          {
            "expr": "rate(postgresql_backup_total{status=\"success\"}[24h])"
          }
        ]
      },
      {
        "title": "Latest Backup Size",
        "targets": [
          {
            "expr": "postgresql_backup_size_bytes"
          }
        ]
      },
      {
        "title": "WAL Archiving Status",
        "targets": [
          {
            "expr": "rate(postgresql_wal_archive_total[1m])"
          }
        ]
      }
    ]
  }
}
```

---

## Restoration Testing

### Regular Restore Drills

**Monthly restoration test procedure:**

```bash
#!/bin/bash
# File: scripts/monthly-restore-test.sh

TEST_DATE=$(date +%Y%m%d)
TEST_LOG="/var/log/postgresql/restore_test_${TEST_DATE}.log"
TEST_PORT=5435

log_test() {
    echo "[TEST] $1" | tee -a "$TEST_LOG"
}

log_test "Starting monthly restoration test..."

# 1. Get latest backup
BACKUP_FILE=$(aws s3 ls s3://gym-platform-backups/full-backups/ \
  --query 'Contents[-1].Key' --output text)

log_test "Testing backup: $BACKUP_FILE"

# 2. Download backup
aws s3 cp "s3://gym-platform-backups/$BACKUP_FILE" /tmp/test_restore.tar.gz

# 3. Extract to test directory
mkdir -p /tmp/restore_test
tar -xzf /tmp/test_restore.tar.gz -C /tmp/restore_test

# 4. Start test PostgreSQL
log_test "Starting test PostgreSQL instance..."
pg_ctl -D /tmp/restore_test \
  -o "-p $TEST_PORT" \
  -l "$TEST_LOG" \
  start

sleep 10

# 5. Run test queries
log_test "Running verification queries..."

PGPORT=$TEST_PORT psql -U postgres postgres << EOF 2>&1 | tee -a "$TEST_LOG"
SELECT version();
SELECT datname FROM pg_database ORDER BY datname;
SELECT COUNT(*) as user_count FROM auth.users;
SELECT COUNT(*) as trainer_count FROM training.trainers;
SELECT COUNT(*) as workout_count FROM tracking.workouts;
VACUUM ANALYZE;
EOF

TEST_RESULT=$?

# 6. Stop test instance
log_test "Stopping test PostgreSQL instance..."
pg_ctl -D /tmp/restore_test stop

# 7. Cleanup
rm -rf /tmp/restore_test
rm -f /tmp/test_restore.tar.gz

if [ $TEST_RESULT -eq 0 ]; then
    log_test "✓ Restoration test PASSED"
    exit 0
else
    log_test "✗ Restoration test FAILED"
    exit 1
fi
```

**Schedule monthly:**

```bash
# Add to crontab
0 4 1 * * /usr/local/bin/monthly-restore-test.sh
```

---

## Best Practices

### Backup Best Practices

1. **Backup Frequency:**
   - Full backup: Daily (2 AM UTC)
   - Incremental: Every 6 hours
   - WAL archiving: Continuous

2. **Backup Retention:**
   - Full backups: 30 days
   - Incremental backups: 7 days
   - WAL archives: 30 days
   - Off-site backups: 90 days

3. **Backup Storage:**
   - Primary location: S3 with versioning
   - Secondary location: Cross-region S3
   - Local cache: 2 recent backups on server
   - Encryption: AES-256 with S3 KMS

4. **Backup Verification:**
   - Monthly restore tests (production-like environment)
   - Daily backup integrity checks
   - Quarterly DR drills
   - Automated verification scripts

5. **Backup Monitoring:**
   - Alert on backup failures
   - Monitor backup age (> 26 hours = alert)
   - Track backup duration trends
   - Monitor S3 storage costs

### Recovery Best Practices

1. **Preparation:**
   - Document recovery procedures
   - Train team on recovery steps
   - Maintain recovery runbook
   - Test recovery at least monthly

2. **During Recovery:**
   - Follow documented procedures
   - Log all actions taken
   - Use staging environment first
   - Verify data integrity before cutover

3. **Post-Recovery:**
   - Investigate root cause of failure
   - Review transaction consistency
   - Update replica configuration
   - Conduct post-incident review
   - Update runbooks with lessons learned

### Backup Security

1. **Encryption:**
   - Enable encryption-in-transit (SSL)
   - Use KMS for S3 backups
   - Encrypt backups at application level

2. **Access Control:**
   - Restrict backup access to DBA team
   - Use IAM roles for AWS access
   - Audit backup access logs
   - Require MFA for S3 backup access

3. **Separation of Duties:**
   - Different credentials for backup/restore
   - Read-only access for verification
   - Write access only for authorized personnel

---

## Troubleshooting

### Common Backup Issues

**Issue: Backup fails with "Connection refused"**

```bash
# Check if PostgreSQL is running
systemctl status postgresql

# Check PostgreSQL is accepting connections
psql -U postgres -c "SELECT 1;"

# Check backup script permissions
ls -la /usr/local/bin/backup-full.sh
chmod +x /usr/local/bin/backup-full.sh

# Run backup with verbose output
bash -x /usr/local/bin/backup-full.sh
```

**Issue: WAL archiving failing**

```sql
-- Check archiver status
SELECT * FROM pg_stat_archiver;

-- Verify WAL archiving configuration
SELECT name, setting FROM pg_settings
WHERE name IN ('archive_mode', 'archive_command', 'archive_timeout');

-- Test archive command manually
SELECT pg_wal_replay_pause();
```

**Issue: Backup size increasing unexpectedly**

```sql
-- Check for table bloat
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;

-- Run VACUUM to reclaim space
VACUUM FULL ANALYZE;
```

---

**Related Documentation:**
- [01-database-overview.md](01-database-overview.md) - Database architecture and setup
- [02-schema-design.md](02-schema-design.md) - Schema design and structure
- See [docs/operations/](../operations/) for operational runbooks
- See [docs/troubleshooting/](../troubleshooting/) for diagnostic procedures
