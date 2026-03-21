# Backup and Disaster Recovery

## Overview

Comprehensive guide to backup strategies, point-in-time recovery (PITR), disaster recovery procedures, and recovery time/point objectives (RTO/RPO) for PostgreSQL in the Gym Platform. This guide covers continuous WAL archiving, streaming replication backups, cross-region replication, verification procedures, and advanced recovery scenarios for production DBA operations.

## Table of Contents

- [Backup Architecture](#backup-architecture)
- [Backup Strategies](#backup-strategies)
- [Continuous WAL Archiving](#continuous-wal-archiving)
- [Physical Backups](#physical-backups)
- [Logical Backups](#logical-backups)
- [Point-in-Time Recovery (PITR)](#point-in-time-recovery-pitr)
- [Backup Verification](#backup-verification)
- [Cross-Region Replication](#cross-region-replication)
- [Disaster Recovery Scenarios](#disaster-recovery-scenarios)
- [RTO/RPO Planning](#rto-rpo-planning)
- [Backup Encryption and Security](#backup-encryption-and-security)
- [Recovery Testing](#recovery-testing)
- [Troubleshooting](#troubleshooting)

---

## Backup Architecture

### RPO/RTO Strategy Matrix

For Gym Platform production systems:

```
┌─────────────────────────────────────────────────────┐
│           Backup Strategy Comparison                 │
├──────────────┬──────────┬────────┬─────────────────┤
│   Strategy   │   RPO    │  RTO   │    Overhead     │
├──────────────┼──────────┼────────┼─────────────────┤
│ Full + WAL   │ ~5 min   │ ~30min │ Low-Medium      │
│ Streaming    │ ~1 sec   │ ~5 min │ Medium          │
│ Replication  │ ~0 sec   │ ~2 min │ Medium-High     │
│ S3 WAL arch  │ ~5 min   │ ~1 hour│ Low             │
└──────────────┴──────────┴────────┴─────────────────┘
```

### Tiered Backup Architecture

```
┌──────────────────────────────────────────────────────────┐
│           Gym Platform Backup Architecture               │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Primary Database (PostgreSQL 14+)                 │  │
│  │  - Active transactions                             │  │
│  │  - Real-time WAL generation                        │  │
│  └────────────────────────────────────────────────────┘  │
│         │                          │                      │
│         │ Continuous WAL Archiving │                      │
│         │                          │                      │
│    ┌────▼────┐   ┌────────────┐  ┌┴──────────────┐       │
│    │S3 WAL   │   │WAL Archiver│  │Local WAL Dest │       │
│    │Archive  │   │(pg_archiver)  │                 │       │
│    └────┬────┘   └────────────┘  └┬──────────────┘       │
│         │                          │                      │
│         │    Physical Backup       │                      │
│         │  (pg_basebackup stream)  │                      │
│         │                          │                      │
│    ┌────▼──────────────────────────▼────┐                │
│    │  Backup Repository (S3/GCS/Local)   │                │
│    │  - Base backups (compressed)        │                │
│    │  - WAL files (continuous stream)    │                │
│    │  - Metadata (recovery info)         │                │
│    └────┬──────────────────────────────┘                 │
│         │                                                 │
│         │  On-demand recovery                             │
│         │                                                 │
│    ┌────▼──────────────────────────┐                     │
│    │  Recovery/Standby Database    │                     │
│    │  - Replay WAL                 │                     │
│    │  - Test restoration           │                     │
│    │  - Promote if needed          │                     │
│    └────────────────────────────────┘                     │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

### Backup Components

**WAL Files:**
- Default: 16MB files in `pg_wal/` directory
- Contain all transaction information needed for recovery
- Continuous archiving copies these to persistent storage
- Retention determined by RPO requirements

**Base Backups:**
- Consistent snapshot of entire cluster
- Created using `pg_basebackup` or backup tools
- Includes: data files, index files, configuration
- Compressed for efficient storage

**Backup Metadata:**
- `backup_label` file with backup start/stop information
- Timeline history files for recovery
- Backup manifest for integrity verification

---

## Backup Strategies

### Strategy 1: Full Backup + Continuous WAL Archiving (Recommended)

**Use Case:** Production systems with moderate RTO/RPO requirements

**Setup:**

```bash
#!/bin/bash
# daily-backup.sh - Daily full backup with continuous WAL archiving

BACKUP_DIR="/backup/postgresql/full"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="gym_full_${BACKUP_DATE}.tar.gz"
REMOTE_BUCKET="s3://gym-backups/postgresql/full"

# Create full backup
pg_basebackup \
  -h localhost \
  -U backup_user \
  -D /tmp/backup_${BACKUP_DATE} \
  -Ft \
  -z \
  -P \
  -v

# Verify backup
if [ $? -eq 0 ]; then
  echo "Backup successful: $BACKUP_NAME"
  
  # Upload to S3
  aws s3 cp /tmp/backup_${BACKUP_DATE}/base.tar.gz \
    ${REMOTE_BUCKET}/${BACKUP_NAME}
  
  # Create backup metadata
  cat > ${BACKUP_DIR}/${BACKUP_NAME}.metadata << EOF
backup_name=${BACKUP_NAME}
backup_date=${BACKUP_DATE}
backup_size=$(du -h /tmp/backup_${BACKUP_DATE} | cut -f1)
backup_status=success
wal_start=$(grep '^START WAL LOCATION' /tmp/backup_${BACKUP_DATE}/backup_label | awk '{print $4}')
wal_stop=$(grep '^STOP WAL LOCATION' /tmp/backup_${BACKUP_DATE}/backup_label | awk '{print $4}')
EOF
  
  # Cleanup
  rm -rf /tmp/backup_${BACKUP_DATE}
else
  echo "Backup failed!"
  exit 1
fi
```

**PostgreSQL Configuration:**

```ini
# postgresql.conf - Continuous WAL Archiving

# Enable WAL archiving
wal_level = replica
archive_mode = on
archive_command = '/opt/scripts/archive-wal.sh "%p" "%f"'
archive_timeout = 300  # Force archiving every 5 minutes

# WAL retention
wal_keep_size = 1GB
max_wal_size = 4GB
min_wal_size = 80MB
```

**WAL Archiving Script:**

```bash
#!/bin/bash
# archive-wal.sh - Archive WAL segment to S3

WAL_PATH=$1
WAL_FILE=$2
S3_BUCKET="s3://gym-backups/postgresql/wal"
ARCHIVE_DIR="/var/lib/postgresql/archive"

# Local archive (for rapid replay)
cp "$WAL_PATH" "$ARCHIVE_DIR/$WAL_FILE"

# S3 archive (for long-term retention)
aws s3 cp "$WAL_PATH" "${S3_BUCKET}/${WAL_FILE}" --storage-class GLACIER

if [ $? -eq 0 ]; then
  echo "$(date '+%Y-%m-%d %H:%M:%S') Archived $WAL_FILE to S3" >> /var/log/postgresql/archive.log
  exit 0
else
  echo "$(date '+%Y-%m-%d %H:%M:%S') Failed to archive $WAL_FILE" >> /var/log/postgresql/archive.log
  exit 1
fi
```

**Monitoring:**

```sql
-- Check WAL archiving status
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Monitor replication slots
SELECT 
  slot_name,
  slot_type,
  active,
  restart_lsn,
  confirmed_flush_lsn
FROM pg_replication_slots;

-- Check WAL archiving progress
SELECT 
  name,
  setting
FROM pg_settings
WHERE name LIKE '%archive%' OR name LIKE '%wal_level%';
```

---

### Strategy 2: Streaming Replication Backup

**Use Case:** Continuous standby with automated failover

**Setup:**

```bash
#!/bin/bash
# streaming-backup.sh - Backup with streaming replication

REPLICA_HOST="standby.gym.local"
REPLICA_DATA_DIR="/var/lib/postgresql/14/main"
BACKUP_USER="replication"

# On primary: Configure replication user
psql -U postgres << EOF
CREATE ROLE $BACKUP_USER WITH REPLICATION ENCRYPTED PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE postgres TO $BACKUP_USER;
EOF

# Configure pg_hba.conf
echo "host  replication  $BACKUP_USER  10.0.0.0/8  md5" >> /etc/postgresql/14/main/pg_hba.conf
systemctl reload postgresql

# On replica: Create streaming replica
pg_basebackup \
  -h primary.gym.local \
  -U $BACKUP_USER \
  -D $REPLICA_DATA_DIR \
  -Pv \
  -R

# Configure standby.signal
touch $REPLICA_DATA_DIR/standby.signal

# Configure recovery parameters
cat > $REPLICA_DATA_DIR/postgresql.auto.conf << EOF
primary_conninfo = 'host=primary.gym.local port=5432 user=$BACKUP_USER password=secure_password application_name=standby1'
recovery_target_timeline = 'latest'
EOF

# Start replica
systemctl start postgresql
```

**Replication Configuration (postgresql.conf):**

```ini
# Primary node settings
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
wal_keep_size = 2GB

# Replication slot configuration
slot_name = 'standby1_slot'

# Synchronous replication (if needed)
synchronous_standby_names = 'standby1'
synchronous_commit = 'local'
```

---

### Strategy 3: Logical Backups with pg_dump

**Use Case:** Schema changes, selective database migration, lightweight backups

**Setup:**

```bash
#!/bin/bash
# logical-backup.sh - Daily schema and data backup

BACKUP_DIR="/backup/postgresql/logical"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
DATABASES=("auth" "training" "tracking" "common")
S3_BUCKET="s3://gym-backups/postgresql/logical"

for db in "${DATABASES[@]}"; do
  # Full database dump
  pg_dump \
    -h localhost \
    -U backup_user \
    -d $db \
    --no-acl \
    --no-owner \
    --compress=9 \
    --format=custom \
    -f "${BACKUP_DIR}/${db}_${BACKUP_DATE}.dump"
  
  # Schema-only dump
  pg_dump \
    -h localhost \
    -U backup_user \
    -d $db \
    --no-acl \
    --no-owner \
    --compress=9 \
    --schema-only \
    --format=custom \
    -f "${BACKUP_DIR}/${db}_schema_${BACKUP_DATE}.dump"
  
  # SQL text dump for version control
  pg_dump \
    -h localhost \
    -U backup_user \
    -d $db \
    --no-acl \
    --no-owner \
    -f "${BACKUP_DIR}/${db}_${BACKUP_DATE}.sql"
  
  # Upload to S3
  aws s3 cp \
    "${BACKUP_DIR}/${db}_${BACKUP_DATE}.dump" \
    "${S3_BUCKET}/${db}/"
done
```

**Restore Logical Backup:**

```bash
#!/bin/bash
# restore-logical.sh - Restore database from logical backup

BACKUP_FILE=$1  # e.g., training_20240321_120000.dump
DB_NAME=${BACKUP_FILE%_*}

# Create target database
createdb -U postgres $DB_NAME

# Restore
pg_restore \
  -h localhost \
  -U postgres \
  -d $DB_NAME \
  --no-acl \
  --no-owner \
  -v \
  "$BACKUP_FILE"

# Verify
psql -U postgres -d $DB_NAME -c "SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema');"
```

---

## Continuous WAL Archiving

### WAL Archiving Configuration

**Production Setup:**

```ini
# postgresql.conf

# Enable archiving
wal_level = replica
archive_mode = on
archive_command = '/opt/scripts/s3-archive-wal.sh %p %f'
archive_timeout = 300

# WAL retention
wal_keep_size = 2GB          # Keep 2GB of WAL locally
max_wal_size = 4GB           # Checkpoint when reaches 4GB
min_wal_size = 80MB          # Don't checkpoint below 80MB

# Replication slots
max_replication_slots = 10

# For streaming replication
max_wal_senders = 20
```

### Advanced Archiving Script

```bash
#!/bin/bash
# s3-archive-wal.sh - Production WAL archiving to S3

set -e

WAL_PATH=$1
WAL_FILE=$2
S3_BUCKET="s3://gym-backups-prod/wal"
LOCAL_ARCHIVE="/var/lib/postgresql/wal_archive"
LOG_FILE="/var/log/postgresql/wal_archive.log"
MAX_RETRIES=5
RETRY_DELAY=5

# Ensure local archive directory exists
mkdir -p "$LOCAL_ARCHIVE"

# Local copy (fast)
if cp "$WAL_PATH" "$LOCAL_ARCHIVE/$WAL_FILE" 2>/dev/null; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Local archive successful: $WAL_FILE" >> "$LOG_FILE"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Local archive failed: $WAL_FILE" >> "$LOG_FILE"
  exit 1
fi

# S3 upload with retry logic
ATTEMPT=1
while [ $ATTEMPT -le $MAX_RETRIES ]; do
  if aws s3 cp "$WAL_PATH" "${S3_BUCKET}/${WAL_FILE}" \
       --storage-class STANDARD_IA \
       --metadata "archive_date=$(date +%s)" 2>/dev/null; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] S3 archive successful: $WAL_FILE" >> "$LOG_FILE"
    exit 0
  fi
  
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] S3 archive attempt $ATTEMPT failed: $WAL_FILE" >> "$LOG_FILE"
  
  if [ $ATTEMPT -lt $MAX_RETRIES ]; then
    sleep $RETRY_DELAY
    ATTEMPT=$((ATTEMPT + 1))
  else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] S3 archive failed after $MAX_RETRIES attempts: $WAL_FILE" >> "$LOG_FILE"
    exit 1
  fi
done
```

### Monitoring WAL Archiving

```sql
-- Check if archiving is running
SELECT name, setting FROM pg_settings 
WHERE name IN ('wal_level', 'archive_mode', 'archive_command');

-- Estimate WAL generation rate (check every 5 minutes)
WITH wal_info AS (
  SELECT 
    NOW() as check_time,
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') as current_lsn_bytes
)
SELECT 
  check_time,
  current_lsn_bytes,
  (current_lsn_bytes / (1024*1024))::int as lsn_mb;

-- Monitor replication slots
SELECT 
  slot_name,
  slot_type,
  active,
  restart_lsn,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / (1024*1024))::int as retained_mb
FROM pg_replication_slots;

-- Check for slots blocking WAL cleanup
SELECT 
  slot_name,
  CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END as status,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / (1024*1024))::int as retained_mb,
  CASE WHEN pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) > 4*1024*1024*1024 
       THEN 'WARNING: Slot retaining >4GB'
       ELSE 'OK'
  END as status_check
FROM pg_replication_slots
ORDER BY retained_mb DESC;
```

---

## Physical Backups

### pg_basebackup with Streaming

**Base Backup Command:**

```bash
# Streaming format (TAR)
pg_basebackup \
  -h primary.gym.local \
  -U backup_user \
  -D /backup/pg_basebackup \
  -Ft \
  -z \
  -P \
  -v \
  --wal-method=stream \
  --backup-label="gym_backup_$(date +%Y%m%d_%H%M%S)" \
  -l "gym_backup_$(date +%Y%m%d_%H%M%S)"

# Output files:
# base.tar.gz - data files
# pg_wal.tar.gz - WAL files included
# backup_label - recovery info
```

**Advanced pg_basebackup Script:**

```bash
#!/bin/bash
# production-basebackup.sh - Production-grade physical backup

set -e

BACKUP_USER="backup_user"
BACKUP_HOST="primary.gym.local"
BACKUP_PORT="5432"
BACKUP_DIR="/backup/postgres"
REMOTE_S3="s3://gym-backups-prod/physical"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="gym_physical_${BACKUP_DATE}"
LOG_FILE="/var/log/postgresql/backup.log"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting backup: $BACKUP_NAME" >> "$LOG_FILE"

# Create backup directory
mkdir -p "$BACKUP_DIR/$BACKUP_NAME"
cd "$BACKUP_DIR/$BACKUP_NAME"

# Create base backup
if pg_basebackup \
    -h "$BACKUP_HOST" \
    -p "$BACKUP_PORT" \
    -U "$BACKUP_USER" \
    -D . \
    -Ft \
    -z \
    -P \
    -v \
    --wal-method=stream \
    --label="$BACKUP_NAME" \
    2>"$LOG_FILE.basebackup"; then
  
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Basebackup completed successfully" >> "$LOG_FILE"
  
  # Verify backup integrity
  if tar -tzf base.tar.gz >/dev/null 2>&1; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup integrity verified" >> "$LOG_FILE"
  else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup integrity check FAILED" >> "$LOG_FILE"
    exit 1
  fi
  
  # Create backup manifest
  cat > "${BACKUP_NAME}.manifest" << EOF
backup_name=$BACKUP_NAME
backup_timestamp=$BACKUP_DATE
backup_host=$BACKUP_HOST
backup_size=$(du -sh . | cut -f1)
base_backup_size=$(du -sh base.tar.gz | cut -f1)
wal_backup_size=$(du -sh pg_wal.tar.gz | cut -f1)
backup_status=success
EOF
  
  # Compress entire backup directory
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Compressing backup..." >> "$LOG_FILE"
  tar -czf "${BACKUP_NAME}.tar.gz" base.tar.gz pg_wal.tar.gz "${BACKUP_NAME}.manifest"
  
  # Upload to S3
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Uploading to S3..." >> "$LOG_FILE"
  aws s3 cp "${BACKUP_NAME}.tar.gz" \
    "${REMOTE_S3}/" \
    --storage-class GLACIER \
    --metadata "backup-date=$BACKUP_DATE,hostname=$BACKUP_HOST"
  
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup completed successfully" >> "$LOG_FILE"
  
  # Cleanup local files (keep manifest)
  rm -f base.tar.gz pg_wal.tar.gz
  
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup FAILED" >> "$LOG_FILE"
  exit 1
fi
```

---

## Logical Backups

### pg_dump Strategies

**Schema Backup:**

```bash
# Schema-only backup (useful for version control)
pg_dump \
  -h localhost \
  -U postgres \
  -d gym_training \
  --schema-only \
  --no-owner \
  --no-acl \
  -f schema_backup.sql

# Data-only backup (for selective restoration)
pg_dump \
  -h localhost \
  -U postgres \
  -d gym_training \
  --data-only \
  --compress=9 \
  -f data_backup.dump

# Custom format (best compression, parallel restore)
pg_dump \
  -h localhost \
  -U postgres \
  -d gym_training \
  --format=custom \
  --compress=9 \
  --verbose \
  -f gym_training_custom.dump
```

**Parallel Dump (Large Databases):**

```bash
#!/bin/bash
# parallel-dump.sh - Multi-job backup for large tables

DATABASE="gym_training"
DUMP_DIR="/backup/parallel_dump"
JOBS=8

mkdir -p "$DUMP_DIR"

# Parallel jobs dump
pg_dump \
  -h localhost \
  -U postgres \
  -d "$DATABASE" \
  --jobs=$JOBS \
  --format=directory \
  --verbose \
  -f "$DUMP_DIR"

# Compress TOC
gzip -9 "$DUMP_DIR/toc.dat"

# Create summary
cat > "$DUMP_DIR/backup_info.txt" << EOF
Database: $DATABASE
Backup Date: $(date)
Backup Size: $(du -sh "$DUMP_DIR" | cut -f1)
Parallel Jobs: $JOBS
Format: directory
EOF
```

---

## Point-in-Time Recovery (PITR)

### PITR Prerequisites

1. Continuous WAL archiving enabled
2. Base backup with known recovery position
3. WAL files from backup start to recovery target point
4. Recovery timeline identified

### PITR Procedure

**Step 1: Identify Recovery Point:**

```bash
# List available backups with metadata
aws s3 ls s3://gym-backups-prod/physical/ --recursive --human-readable | sort

# Find backup metadata
BACKUP_MANIFEST=$(aws s3 ls s3://gym-backups-prod/physical/ | tail -1 | awk '{print $4}')
aws s3 cp "s3://gym-backups-prod/physical/${BACKUP_MANIFEST}" - | tar -xz
cat gym_physical_*.manifest
```

**Step 2: Restore Base Backup:**

```bash
#!/bin/bash
# restore-base-backup.sh - Restore base backup for PITR

BACKUP_FILE="gym_physical_20240321_120000.tar.gz"
RECOVERY_DIR="/var/lib/postgresql/recovery"
ARCHIVE_DIR="/backup/wal_archive"

# Cleanup any existing recovery directory
rm -rf "$RECOVERY_DIR"
mkdir -p "$RECOVERY_DIR"

# Extract backup
tar -xzf "$BACKUP_FILE" -C "$RECOVERY_DIR"

# Extract base tarball
cd "$RECOVERY_DIR"
tar -xzf base.tar.gz
tar -xzf pg_wal.tar.gz

# Create recovery configuration
cat > recovery.conf << EOF
restore_command = 'cp $ARCHIVE_DIR/%f %p'
recovery_target_timeline = 'latest'
recovery_target_xid = '12345678'  # Target transaction
recovery_target_time = '2024-03-21 14:30:00'
recovery_target_name = 'before_incident'
pause_at_recovery_target = true
EOF

# Set permissions
chown -R postgres:postgres "$RECOVERY_DIR"
chmod 700 "$RECOVERY_DIR"
```

**Step 3: Configure Recovery Parameters:**

```ini
# postgresql.conf for recovery

# Location of WAL archives
restore_command = 'cp /backup/wal_archive/%f %p'

# Recovery target options (choose one)
# By timestamp (recommended)
recovery_target_time = '2024-03-21 14:30:00'

# By transaction ID
recovery_target_xid = '12345678'

# By name (recovery points)
recovery_target_name = 'before_incident'

# Recovery timeline
recovery_target_timeline = 'latest'

# Pause before finishing recovery
pause_at_recovery_target = true

# Validate recovery
recovery_target_inclusive = true
```

**Step 4: Execute Recovery:**

```bash
#!/bin/bash
# execute-pitr.sh - Execute point-in-time recovery

RECOVERY_DATA_DIR="/var/lib/postgresql/recovery_data"
ARCHIVE_DIR="/backup/wal_archive"
PG_BIN="/usr/lib/postgresql/14/bin"

# Verify WAL files available
echo "Available WAL files:"
ls -lh "$ARCHIVE_DIR" | tail -20

# Create PostgreSQL data directory with recovery config
mkdir -p "$RECOVERY_DATA_DIR"
cp -r /var/lib/postgresql/14/main/* "$RECOVERY_DATA_DIR/"

# Create recovery.conf
cat > "$RECOVERY_DATA_DIR/recovery.conf" << EOF
restore_command = 'cp $ARCHIVE_DIR/%f %p'
recovery_target_time = '2024-03-21 14:30:00'
recovery_target_timeline = 'latest'
pause_at_recovery_target = true
EOF

# Start PostgreSQL in recovery mode
sudo -u postgres "$PG_BIN/postgres" \
  -D "$RECOVERY_DATA_DIR" \
  -c log_recovery_conflict_waits=true

# Monitor recovery progress
sleep 5
tail -f /var/log/postgresql/postgresql.log

# Once recovery completes and paused, promote
echo "SELECT pg_wal_replay_resume();" | \
  psql -h localhost -U postgres -d postgres
```

**Monitoring PITR Progress:**

```sql
-- Check recovery progress
SELECT 
  pg_is_wal_replay_paused() as replay_paused,
  pg_last_wal_receive_lsn() as receive_lsn,
  pg_last_wal_replay_lsn() as replay_lsn,
  (pg_wal_lsn_diff(pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn()) / (1024*1024))::int as backlog_mb;

-- Show current recovery target
SELECT name, setting FROM pg_settings 
WHERE name LIKE 'recovery%' OR name LIKE 'restore%';

-- Resume from pause
SELECT pg_wal_replay_resume();
```

---

## Backup Verification

### Backup Integrity Testing

**Automated Verification Script:**

```bash
#!/bin/bash
# verify-backup.sh - Comprehensive backup verification

BACKUP_FILE=$1
TEMP_VERIFY_DIR="/tmp/backup_verify_$$"
LOG_FILE="/var/log/postgresql/backup_verify.log"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting backup verification: $BACKUP_FILE" >> "$LOG_FILE"

# Create verification directory
mkdir -p "$TEMP_VERIFY_DIR"
trap "rm -rf $TEMP_VERIFY_DIR" EXIT

# Test 1: Archive integrity
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Test 1: Archive integrity check..." >> "$LOG_FILE"
if tar -tzf "$BACKUP_FILE" >/dev/null 2>&1; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] PASS: Archive is valid" >> "$LOG_FILE"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] FAIL: Archive is corrupted" >> "$LOG_FILE"
  exit 1
fi

# Test 2: Extract and verify structure
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Test 2: Verifying backup structure..." >> "$LOG_FILE"
tar -xzf "$BACKUP_FILE" -C "$TEMP_VERIFY_DIR"

if [ -f "$TEMP_VERIFY_DIR/base.tar.gz" ] && [ -f "$TEMP_VERIFY_DIR/pg_wal.tar.gz" ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] PASS: Backup structure is valid" >> "$LOG_FILE"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] FAIL: Backup structure invalid" >> "$LOG_FILE"
  exit 1
fi

# Test 3: Base backup integrity
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Test 3: Validating base backup..." >> "$LOG_FILE"
if tar -tzf "$TEMP_VERIFY_DIR/base.tar.gz" >/dev/null 2>&1; then
  FILE_COUNT=$(tar -tzf "$TEMP_VERIFY_DIR/base.tar.gz" | wc -l)
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] PASS: Base backup valid ($FILE_COUNT files)" >> "$LOG_FILE"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] FAIL: Base backup corrupted" >> "$LOG_FILE"
  exit 1
fi

# Test 4: WAL backup integrity
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Test 4: Validating WAL backup..." >> "$LOG_FILE"
if tar -tzf "$TEMP_VERIFY_DIR/pg_wal.tar.gz" >/dev/null 2>&1; then
  WAL_COUNT=$(tar -tzf "$TEMP_VERIFY_DIR/pg_wal.tar.gz" | wc -l)
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] PASS: WAL backup valid ($WAL_COUNT files)" >> "$LOG_FILE"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] FAIL: WAL backup corrupted" >> "$LOG_FILE"
  exit 1
fi

# Test 5: Metadata verification
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Test 5: Checking backup metadata..." >> "$LOG_FILE"
MANIFEST=$(find "$TEMP_VERIFY_DIR" -name "*.manifest" -type f)
if [ -f "$MANIFEST" ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] PASS: Backup metadata found" >> "$LOG_FILE"
  cat "$MANIFEST" >> "$LOG_FILE"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: No backup metadata found" >> "$LOG_FILE"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup verification completed successfully" >> "$LOG_FILE"
```

### Test Restoration

```bash
#!/bin/bash
# test-restore.sh - Restore backup to test environment

BACKUP_FILE="gym_physical_20240321_120000.tar.gz"
TEST_DB_DIR="/tmp/test_restore_$$"
TEST_PORT=5433

# Cleanup
rm -rf "$TEST_DB_DIR"
mkdir -p "$TEST_DB_DIR"

# Extract backup
tar -xzf "$BACKUP_FILE" -C "$TEST_DB_DIR"
cd "$TEST_DB_DIR"
tar -xzf base.tar.gz
tar -xzf pg_wal.tar.gz

# Create minimal recovery config
cat > recovery.conf << EOF
recovery_target = 'immediate'
recovery_target_timeline = 'latest'
EOF

# Initialize cluster for recovery
initdb -D "$TEST_DB_DIR" --allow-group-access

# Start test instance
postgres -D "$TEST_DB_DIR" -p "$TEST_PORT" &
POSTGRES_PID=$!

sleep 5

# Run verification queries
psql -p "$TEST_PORT" -U postgres << EOF
-- Check database status
SELECT datname, state FROM pg_stat_activity;

-- Verify key tables
SELECT tablename FROM pg_tables 
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY tablename;

-- Check row counts
SELECT 
  schemaname, 
  tablename, 
  n_live_tup 
FROM pg_stat_user_tables 
ORDER BY n_live_tup DESC 
LIMIT 10;
EOF

# Cleanup
kill $POSTGRES_PID
rm -rf "$TEST_DB_DIR"
```

---

## Cross-Region Replication

### Setup Cross-Region Standby

```bash
#!/bin/bash
# setup-cross-region-replica.sh - Configure cross-region standby

PRIMARY_HOST="primary.gym.local"
REPLICA_HOST="replica-dr.gym-dr.local"
REPLICA_USER="replica_user"
REPLICA_DATA_DIR="/var/lib/postgresql/14/main"
REPLICA_SSH_KEY="/root/.ssh/id_rsa_replica"

# Step 1: On primary - create replication user with SSL
ssh "$PRIMARY_HOST" << EOF
psql -U postgres << SQL
CREATE ROLE $REPLICA_USER WITH REPLICATION ENCRYPTED PASSWORD 'secure_password';
ALTER ROLE $REPLICA_USER SET search_path = public;
GRANT CONNECT ON DATABASE postgres TO $REPLICA_USER;
SQL
EOF

# Step 2: Configure pg_hba.conf on primary
ssh "$PRIMARY_HOST" << EOF
echo "hostssl  replication  $REPLICA_USER  0.0.0.0/0  md5" >> /etc/postgresql/14/main/pg_hba.conf
systemctl reload postgresql
EOF

# Step 3: Setup cross-region replication slot
ssh "$PRIMARY_HOST" << EOF
psql -U postgres << SQL
SELECT * FROM pg_create_physical_replication_slot('dr_replica_slot');
SQL
EOF

# Step 4: On replica - base backup over network (may take time)
ssh "$REPLICA_HOST" << EOF
mkdir -p "$REPLICA_DATA_DIR"
pg_basebackup \
  -h "$PRIMARY_HOST" \
  -U "$REPLICA_USER" \
  -D "$REPLICA_DATA_DIR" \
  -Pv \
  -R \
  --slot=dr_replica_slot \
  --write-recovery-conf

touch "$REPLICA_DATA_DIR/standby.signal"

cat >> "$REPLICA_DATA_DIR/postgresql.auto.conf" << CONFIG
primary_slot_name = 'dr_replica_slot'
wal_receiver_keep_alive_interval = 10
wal_receiver_timeout = 60
wal_receiver_status_interval = 10
CONFIG

chown -R postgres:postgres "$REPLICA_DATA_DIR"
chmod 700 "$REPLICA_DATA_DIR"

systemctl start postgresql
EOF

# Step 5: Monitor replication lag
ssh "$PRIMARY_HOST" << EOF
psql -U postgres << SQL
SELECT 
  slot_name,
  active,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / (1024*1024))::int as retained_mb,
  write_lsn,
  flush_lsn,
  replay_lsn
FROM pg_stat_replication;
SQL
EOF
```

---

## Disaster Recovery Scenarios

### Scenario 1: Data Corruption on Primary

```bash
#!/bin/bash
# dr-data-corruption.sh - Handle data corruption

PRIMARY_HOST="primary.gym.local"
REPLICA_HOST="replica.gym.local"
INCIDENT_TIME="2024-03-21 14:30:00"

# Step 1: Verify data corruption
ssh "$PRIMARY_HOST" << EOF
psql -U postgres << SQL
-- Run data integrity checks
REINDEX DATABASE gym_training;

-- Check for corrupted pages
SELECT 
  relname,
  COUNT(*) as error_count
FROM pg_class, pg_stat_user_tables
WHERE pg_class.oid = pg_stat_user_tables.relid
  AND pg_stat_user_tables.last_vacuum IS NULL
GROUP BY relname;
SQL
EOF

# Step 2: Promote replica to primary
echo "Promoting replica to primary..."
ssh "$REPLICA_HOST" << EOF
psql -U postgres << SQL
SELECT pg_wal_replay_resume();
-- Replica is now read-write
SQL

# Promote standby
pg_ctl promote -D /var/lib/postgresql/14/main
EOF

# Step 3: Update application connection strings
echo "Update application configs to use:"
echo "  host=$REPLICA_HOST"
echo "  port=5432"

# Step 4: Perform full REINDEX on corrupted tables
ssh "$REPLICA_HOST" << EOF
psql -U postgres -d gym_training << SQL
-- Identify and fix corruption
REINDEX TABLE CONCURRENTLY accounts;
REINDEX TABLE CONCURRENTLY workouts;

-- Verify
ANALYZE;
SELECT relname, last_analyze FROM pg_stat_user_tables;
SQL
EOF

echo "DR procedure completed"
```

### Scenario 2: Ransomware / Data Deletion

```bash
#!/bin/bash
# dr-ransomware.sh - Recover from data deletion

BACKUP_DATE="20240321_100000"  # Before incident
RECOVERY_POINT="2024-03-21 09:59:00"
RECOVERY_DB="/backup/recovery_db"

# Step 1: Download backup
echo "Downloading backup from S3..."
aws s3 cp "s3://gym-backups-prod/physical/gym_physical_${BACKUP_DATE}.tar.gz" .

# Step 2: Setup recovery environment
mkdir -p "$RECOVERY_DB"
tar -xzf "gym_physical_${BACKUP_DATE}.tar.gz" -C "$RECOVERY_DB"

cd "$RECOVERY_DB"
tar -xzf base.tar.gz
tar -xzf pg_wal.tar.gz

# Step 3: Configure PITR
cat > recovery.conf << EOF
restore_command = 'aws s3 cp s3://gym-backups-prod/wal/%f %p'
recovery_target_time = '$RECOVERY_POINT'
recovery_target_timeline = 'latest'
recovery_target_inclusive = true
pause_at_recovery_target = true
EOF

# Step 4: Start recovery
echo "Starting PITR recovery..."
postgres -D "$RECOVERY_DB" &

# Monitor recovery
sleep 10
tail -f /var/log/postgresql/postgresql.log

# Step 5: Resume and validate
echo "Validating recovered data..."
psql -h localhost -U postgres << SQL
SELECT COUNT(*) as record_count FROM accounts;
SELECT MAX(created_at) as latest_record FROM workouts;
SQL

# Step 6: Dump recovered data
pg_dumpall -h localhost -U postgres > recovered_data_backup.sql

echo "Recovery completed. Data backed up to recovered_data_backup.sql"
```

### Scenario 3: Cluster-Wide Failure

```bash
#!/bin/bash
# dr-cluster-failure.sh - Full cluster recovery

BACKUP_DIR="/backup/cluster_recovery"
S3_BUCKET="s3://gym-backups-prod"

# Step 1: Download all backups
echo "Downloading cluster backups..."
mkdir -p "$BACKUP_DIR"
aws s3 sync "${S3_BUCKET}/physical/" "$BACKUP_DIR/physical/"
aws s3 sync "${S3_BUCKET}/wal/" "$BACKUP_DIR/wal/"

# Step 2: Select latest base backup
LATEST_BACKUP=$(ls -t "$BACKUP_DIR/physical"/*.tar.gz | head -1)
echo "Using backup: $LATEST_BACKUP"

# Step 3: Prepare new cluster
NEW_CLUSTER_DATA="/var/lib/postgresql/14/main_recovery"
mkdir -p "$NEW_CLUSTER_DATA"
cd "$NEW_CLUSTER_DATA"

# Extract base backup
tar -xzf "$LATEST_BACKUP"
tar -xzf base.tar.gz
tar -xzf pg_wal.tar.gz

# Step 4: Collect all WAL files after backup
mkdir -p wal_recovery
cp "$BACKUP_DIR/wal"/* wal_recovery/

# Step 5: Create recovery configuration
cat > recovery.conf << EOF
restore_command = 'cp wal_recovery/%f %p'
recovery_target_timeline = 'latest'
recovery_target = 'immediate'
EOF

# Step 6: Initialize and start recovery
chown -R postgres:postgres "$NEW_CLUSTER_DATA"
sudo -u postgres postgres -D "$NEW_CLUSTER_DATA" &

# Wait for recovery to complete
sleep 30

# Step 7: Verify cluster
psql -h localhost -U postgres << SQL
SELECT datname, state FROM pg_stat_activity;
SELECT schemaname, COUNT(*) as table_count FROM pg_tables GROUP BY schemaname;
SQL

echo "Cluster recovery completed"
```

---

## RTO/RPO Planning

### RTO/RPO Matrix for Gym Platform

```
┌──────────────────────────────────────────────────────────────┐
│     RTO/RPO Targets by Service Component                      │
├──────────────────┬──────────┬──────────┬──────────────────────┤
│   Service        │   RPO    │   RTO    │   Strategy           │
├──────────────────┼──────────┼──────────┼──────────────────────┤
│ Auth Service     │ < 1 min  │ < 5 min  │ Cross-region replica │
│ Training Service │ < 5 min  │ < 10 min │ Full backup + WAL    │
│ Tracking Service │ < 10 min │ < 20 min │ Base backup only     │
│ Metadata         │ < 1 sec  │ < 2 min  │ Streaming replica    │
└──────────────────┴──────────┴──────────┴──────────────────────┘
```

### Calculating Backup Frequency

```bash
#!/bin/bash
# calculate-backup-frequency.sh

# Formula: RPO_SECONDS / (BACKUP_SIZE_MB / NETWORK_SPEED_MBPS)

RPO_MINUTES=5
RPO_SECONDS=$((RPO_MINUTES * 60))

BACKUP_SIZE_MB=2048  # 2GB backup
NETWORK_SPEED_MBPS=1000  # 1Gbps network

# Time to upload backup
UPLOAD_TIME=$((BACKUP_SIZE_MB / NETWORK_SPEED_MBPS))

# Safe backup interval (50% of RPO)
BACKUP_INTERVAL=$((RPO_SECONDS / 2))

echo "RPO Target: $RPO_MINUTES minutes"
echo "Backup Size: ${BACKUP_SIZE_MB}MB"
echo "Network Speed: ${NETWORK_SPEED_MBPS}Mbps"
echo "Upload Time: ${UPLOAD_TIME}s"
echo "Recommended Backup Interval: ${BACKUP_INTERVAL}s ($(($BACKUP_INTERVAL / 60)) minutes)"

# Cron schedule (e.g., every 2.5 minutes)
echo "*/2 * * * * /opt/scripts/daily-backup.sh"
```

---

## Backup Encryption and Security

### Encrypting Backups

**Using GPG for Encryption:**

```bash
#!/bin/bash
# encrypt-backup.sh - Encrypt backup with GPG

BACKUP_FILE=$1
GPG_RECIPIENT="dba@gym.local"
ENCRYPTED_FILE="${BACKUP_FILE}.gpg"

# Encrypt backup
gpg --encrypt \
  --recipient "$GPG_RECIPIENT" \
  --trust-model always \
  "$BACKUP_FILE"

# Verify encryption
gpg --list-packets "$ENCRYPTED_FILE" | head -5

# Remove unencrypted backup
rm -f "$BACKUP_FILE"

echo "Backup encrypted: $ENCRYPTED_FILE"
```

**Using OpenSSL for Encryption:**

```bash
#!/bin/bash
# ssl-encrypt-backup.sh - Encrypt with OpenSSL

BACKUP_FILE=$1
PASSPHRASE_FILE="/secure/backup_passphrase.txt"

# Encrypt backup
openssl enc -aes-256-cbc \
  -salt \
  -in "$BACKUP_FILE" \
  -out "${BACKUP_FILE}.enc" \
  -pass "file:${PASSPHRASE_FILE}"

# Verify encrypted file
openssl enc -aes-256-cbc \
  -d \
  -in "${BACKUP_FILE}.enc" \
  -pass "file:${PASSPHRASE_FILE}" | tar -tzf - | head -10

echo "Backup encrypted with OpenSSL"
```

### Backup Storage Security

```ini
# S3 Bucket Policy - Restrict backup access

{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyUnencryptedObjectUploads",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::gym-backups-prod/*",
      "Condition": {
        "StringNotEquals": {
          "s3:x-amz-server-side-encryption": "AES256"
        }
      }
    },
    {
      "Sid": "RestrictIPAccess",
      "Effect": "Allow",
      "Principal": {"AWS": "arn:aws:iam::ACCOUNT:role/DBArole"},
      "Action": "s3:*",
      "Resource": "arn:aws:s3:::gym-backups-prod/*",
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": "10.0.0.0/8"
        }
      }
    }
  ]
}
```

---

## Recovery Testing

### Quarterly Recovery Drill

```bash
#!/bin/bash
# quarterly-recovery-drill.sh - Full recovery procedure test

DRILL_DATE=$(date +%Y%m%d)
DRILL_DIR="/backup/recovery_drills/${DRILL_DATE}"
DRILL_LOG="/var/log/postgresql/recovery_drill_${DRILL_DATE}.log"

echo "Starting quarterly recovery drill" > "$DRILL_LOG"

# Step 1: Find backups suitable for test
echo "Finding test backups..." >> "$DRILL_LOG"
BACKUPS=$(aws s3 ls s3://gym-backups-prod/physical/ | tail -3)
echo "$BACKUPS" >> "$DRILL_LOG"

# Step 2: For each backup, test restoration
for BACKUP_LINE in $BACKUPS; do
  BACKUP_FILE=$(echo "$BACKUP_LINE" | awk '{print $4}')
  echo "Testing backup: $BACKUP_FILE" >> "$DRILL_LOG"
  
  # Download backup
  mkdir -p "$DRILL_DIR/$BACKUP_FILE"
  aws s3 cp "s3://gym-backups-prod/physical/$BACKUP_FILE" "$DRILL_DIR/"
  
  # Extract and verify
  cd "$DRILL_DIR"
  tar -xzf "$BACKUP_FILE" || {
    echo "FAILED: Backup extraction failed" >> "$DRILL_LOG"
    continue
  }
  
  # Test PITR capability
  echo "Testing PITR capability..." >> "$DRILL_LOG"
  # [PITR test commands here]
  
  echo "Backup $BACKUP_FILE: PASS" >> "$DRILL_LOG"
done

echo "Recovery drill completed. Results: $DRILL_LOG"
```

---

## Troubleshooting

### Common Issues

**Issue: WAL archiving failing**

```sql
-- Check archive status
SELECT 
  schemaname,
  name,
  setting
FROM pg_settings
WHERE name IN ('archive_mode', 'archive_command', 'archive_timeout')
ORDER BY name;

-- Check for stuck WAL segments
SELECT * FROM pg_stat_archiver;

-- Manual archive WAL segment
\! cp /var/lib/postgresql/14/pg_wal/000000010000000000000001 /backup/wal_archive/
```

**Issue: Replication slot blocking WAL cleanup**

```sql
-- List problematic replication slots
SELECT 
  slot_name,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / (1024*1024))::int as retained_mb
FROM pg_replication_slots
WHERE active = false;

-- Drop inactive slot
SELECT pg_drop_replication_slot('slot_name');

-- Restart active slots
SELECT pg_drop_replication_slot('slot_name');
-- [Recreate on replica]
```

**Issue: PITR recovery too slow**

```bash
# Monitor recovery progress
tail -f /var/log/postgresql/postgresql.log | grep -i "redo"

# Check WAL replay rate
SELECT 
  pg_last_wal_replay_lsn() as replay_lsn,
  now() as current_time;

-- Run frequently to track progress
```

---

## Related Documentation

- [02-database-architecture.md](02-database-architecture.md) - PostgreSQL internals
- [04-performance-tuning.md](04-performance-tuning.md) - Performance optimization
- [05-maintenance-procedures.md](05-maintenance-procedures.md) - Maintenance operations
- [07-monitoring-alerting.md](07-monitoring-alerting.md) - Monitoring backup status
