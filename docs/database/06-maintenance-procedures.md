# Maintenance

## Overview

Comprehensive guide for regular database maintenance procedures for PostgreSQL in the Gym Platform. This guide covers daily/weekly/monthly maintenance tasks, VACUUM and ANALYZE procedures, index maintenance, table maintenance, replication monitoring, and automated maintenance scheduling. Proper maintenance is essential for data integrity, performance optimization, and long-term database health.

## Table of Contents

- [Maintenance Strategy](#maintenance-strategy)
- [Daily Maintenance Tasks](#daily-maintenance-tasks)
- [Weekly Maintenance Tasks](#weekly-maintenance-tasks)
- [Monthly Maintenance Tasks](#monthly-maintenance-tasks)
- [VACUUM Operations](#vacuum-operations)
- [ANALYZE Operations](#analyze-operations)
- [Index Maintenance](#index-maintenance)
- [Table Maintenance](#table-maintenance)
- [Replication Monitoring](#replication-monitoring)
- [Automated Maintenance](#automated-maintenance)
- [Monitoring & Alerting](#monitoring--alerting)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Maintenance Strategy

### Maintenance Objectives

```
┌──────────────────────────────────────────────┐
│      Database Maintenance Objectives          │
└──────────────────────────────────────────────┘

1. PERFORMANCE
   └─ Monitor query performance
   └─ Optimize slow queries
   └─ Update statistics for planner

2. AVAILABILITY
   └─ Ensure continuous operation
   └─ Minimize downtime/maintenance windows
   └─ Monitor replication health

3. DATA INTEGRITY
   └─ Verify data consistency
   └─ Repair corruption if found
   └─ Maintain audit trails

4. STORAGE EFFICIENCY
   └─ Reclaim bloated space
   └─ Manage index fragmentation
   └─ Archive old data

5. SECURITY
   └─ Monitor access logs
   └─ Verify permissions
   └─ Check for suspicious activity

6. PREPAREDNESS
   └─ Verify backup integrity
   └─ Test recovery procedures
   └─ Document all changes
```

### Maintenance Calendar

```
DAILY
├─ Monitor database health
├─ Check backup status
├─ Review error logs
├─ Check replication lag
└─ Verify connection counts

WEEKLY
├─ Analyze query performance
├─ Review slow query logs
├─ Monitor index usage
├─ Verify backup restoration
└─ Check disk space usage

MONTHLY
├─ Full maintenance window (3-4 hours)
├─ VACUUM FULL on large tables
├─ REINDEX if necessary
├─ Archive old data
├─ Update statistics
├─ Verify recovery procedures
└─ Document findings

QUARTERLY
├─ Disaster recovery drill
├─ Capacity planning review
├─ Performance trend analysis
├─ Schema review for improvements
└─ Update runbooks and procedures
```

---

## Daily Maintenance Tasks

### Health Check Script

**File: /usr/local/bin/daily-health-check.sh**

```bash
#!/bin/bash
# Daily database health check

DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="gym_db"
DB_USER="postgres"
LOG_FILE="/var/log/postgresql/daily_health_check.log"

log_check() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

log_check "=== Daily Health Check Started ==="

# 1. Database connectivity
log_check "1. Testing database connectivity..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" 2>/dev/null
if [ $? -eq 0 ]; then
    log_check "   ✓ Database responsive"
else
    log_check "   ✗ Database connection failed"
    exit 1
fi

# 2. Disk space
log_check "2. Checking disk space..."
DISK_USAGE=$(df -h /var/lib/postgresql | tail -1 | awk '{print $5}' | sed 's/%//')
log_check "   Disk usage: ${DISK_USAGE}%"
if [ "$DISK_USAGE" -gt 90 ]; then
    log_check "   ⚠ WARNING: Disk usage critical!"
fi

# 3. Backup status
log_check "3. Checking backup status..."
LATEST_BACKUP=$(ls -t /var/lib/postgresql/backups/gym_db_full_*.tar.gz 2>/dev/null | head -1)
if [ -f "$LATEST_BACKUP" ]; then
    BACKUP_SIZE=$(du -sh "$LATEST_BACKUP" | awk '{print $1}')
    BACKUP_AGE=$(($(date +%s) - $(stat -c%Y "$LATEST_BACKUP")))
    log_check "   ✓ Latest backup: $(basename $LATEST_BACKUP) (${BACKUP_SIZE}, $(($BACKUP_AGE / 3600))h old)"
    if [ "$BACKUP_AGE" -gt 86400 ]; then
        log_check "   ⚠ WARNING: Backup older than 24 hours!"
    fi
else
    log_check "   ✗ No recent backup found"
fi

# 4. Replication lag
log_check "4. Checking replication lag..."
LAG=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
  "SELECT EXTRACT(EPOCH FROM (NOW() - pg_last_xact_replay_timestamp()));" 2>/dev/null)
if [ ! -z "$LAG" ] && [ "$LAG" != "NULL" ]; then
    log_check "   Replication lag: ${LAG} seconds"
    if [ "${LAG%.*}" -gt 300 ]; then
        log_check "   ⚠ WARNING: Replication lag > 5 minutes!"
    fi
fi

# 5. Active connections
log_check "5. Checking active connections..."
ACTIVE_CONN=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
  "SELECT COUNT(*) FROM pg_stat_activity WHERE state != 'idle';" 2>/dev/null)
log_check "   Active connections: $ACTIVE_CONN"
if [ "$ACTIVE_CONN" -gt 50 ]; then
    log_check "   ⚠ WARNING: High number of active connections!"
fi

# 6. Cache hit ratio
log_check "6. Checking cache hit ratio..."
CACHE_HIT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
  "SELECT ROUND(100.0 * SUM(heap_blks_hit) / (SUM(heap_blks_hit) + SUM(heap_blks_read)), 2)
   FROM pg_statio_user_tables;" 2>/dev/null)
log_check "   Cache hit ratio: ${CACHE_HIT}%"
if [ "${CACHE_HIT%.*}" -lt 95 ]; then
    log_check "   ⚠ WARNING: Cache hit ratio below 95%"
fi

# 7. Deadlocks
log_check "7. Checking for deadlocks..."
DEADLOCK_COUNT=$(tail -100 /var/log/postgresql/postgresql.log | grep -c "deadlock detected")
if [ "$DEADLOCK_COUNT" -gt 0 ]; then
    log_check "   ⚠ WARNING: $DEADLOCK_COUNT deadlocks detected in recent logs"
fi

# 8. Error count
log_check "8. Checking for errors in logs..."
ERROR_COUNT=$(tail -500 /var/log/postgresql/postgresql.log | grep -c "ERROR")
if [ "$ERROR_COUNT" -gt 10 ]; then
    log_check "   ⚠ WARNING: $ERROR_COUNT errors in recent logs"
fi

log_check "=== Daily Health Check Completed ==="
```

**Schedule with cron:**

```bash
# Run daily at 3:00 AM UTC
0 3 * * * /usr/local/bin/daily-health-check.sh
```

---

## Weekly Maintenance Tasks

### Weekly Analysis

**File: /usr/local/bin/weekly-analysis.sh**

```bash
#!/bin/bash
# Weekly performance analysis

DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="gym_db"
REPORT_DIR="/var/log/postgresql/weekly_reports"
WEEK=$(date +%Y_week_%U)

mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/weekly_report_$WEEK.txt"

exec > "$REPORT_FILE" 2>&1

echo "=== Weekly Database Performance Report ==="
echo "Date: $(date)"
echo "Database: $DB_NAME"
echo ""

# 1. Table statistics
echo "1. TABLE STATISTICS"
echo "==================="
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows,
    ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) as dead_ratio,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
EOF

# 2. Index statistics
echo ""
echo "2. INDEX STATISTICS"
echo "==================="
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC
LIMIT 20;
EOF

# 3. Unused indexes
echo ""
echo "3. UNUSED INDEXES (candidate for removal)"
echo "=========================================="
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;
EOF

# 4. Slow queries (from logs)
echo ""
echo "4. SLOW QUERIES (> 1 second)"
echo "============================="
grep "duration: [1-9][0-9]{3,}" /var/log/postgresql/postgresql.log | \
  sed 's/.*duration: \([0-9.]*\) ms.*/\1/' | \
  sort -rn | head -20

# 5. Cache hit ratio trend
echo ""
echo "5. CACHE HIT RATIO"
echo "=================="
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF
SELECT
    datname,
    sum(heap_blks_read) as heap_read,
    sum(heap_blks_hit) as heap_hit,
    ROUND(100.0 * sum(heap_blks_hit) / 
      (sum(heap_blks_hit) + sum(heap_blks_read)), 2) as cache_hit_ratio
FROM pg_statio_user_tables
JOIN pg_database ON pg_database.oid = datid
GROUP BY datname;
EOF

echo ""
echo "=== Report saved to: $REPORT_FILE ==="
```

**Weekly cron schedule:**

```bash
# Run every Sunday at 1:00 AM UTC
0 1 * * 0 /usr/local/bin/weekly-analysis.sh
```

---

## Monthly Maintenance Tasks

### Monthly Maintenance Window

**File: /usr/local/bin/monthly-maintenance.sh**

```bash
#!/bin/bash
# Monthly maintenance procedure (3-4 hour window)

set -euo pipefail

DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="gym_db"
MAINTENANCE_LOG="/var/log/postgresql/monthly_maintenance_$(date +%Y%m%d).log"

log_maintenance() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$MAINTENANCE_LOG"
}

log_maintenance "=== MONTHLY MAINTENANCE STARTED ==="
START_TIME=$(date +%s)

# 1. Create backup
log_maintenance "1. Creating pre-maintenance backup..."
pg_basebackup --pgdata="/var/lib/postgresql/backup_pre_maintenance_$(date +%Y%m%d)" \
  --format=tar --gzip --verbose --progress \
  2>&1 | tee -a "$MAINTENANCE_LOG" || {
    log_maintenance "ERROR: Backup failed"
    exit 1
}

# 2. VACUUM on large tables
log_maintenance "2. Running VACUUM on large tables..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF 2>&1 | tee -a "$MAINTENANCE_LOG"
-- VACUUM (not FULL) - can run while DB is active
VACUUM (ANALYZE, VERBOSE) auth.users;
VACUUM (ANALYZE, VERBOSE) training.training_plans;
VACUUM (ANALYZE, VERBOSE) tracking.workouts;
VACUUM (ANALYZE, VERBOSE) training.trainers;

-- Check remaining bloat
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    n_dead_tup,
    ROUND(100.0 * n_dead_tup / (n_live_tup + n_dead_tup), 2) as dead_ratio
FROM pg_stat_user_tables
WHERE n_live_tup > 0
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
EOF

# 3. Reindex fragmented indexes
log_maintenance "3. Checking index fragmentation..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF 2>&1 | tee -a "$MAINTENANCE_LOG"
-- Check index stats
SELECT
    schemaname,
    tablename,
    indexname,
    idx_blks_read,
    idx_blks_hit,
    relpages
FROM pg_stat_user_indexes
JOIN pg_class ON pg_class.oid = indexrelid
WHERE relpages > 10000
ORDER BY relpages DESC;
EOF

# 4. Analyze all tables
log_maintenance "4. Running ANALYZE on all tables..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" -c "ANALYZE;" \
  2>&1 | tee -a "$MAINTENANCE_LOG"

# 5. Check table consistency
log_maintenance "5. Checking table constraints and consistency..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF 2>&1 | tee -a "$MAINTENANCE_LOG"
-- Check for constraint violations
SELECT
    table_name,
    constraint_name
FROM information_schema.table_constraints
WHERE constraint_type IN ('PRIMARY KEY', 'UNIQUE', 'FOREIGN KEY')
AND table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY table_name;
EOF

# 6. Check duplicate indexes
log_maintenance "6. Checking for duplicate/redundant indexes..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF 2>&1 | tee -a "$MAINTENANCE_LOG"
WITH index_data AS (
    SELECT
        schemaname,
        tablename,
        indexname,
        array_agg(attname ORDER BY attnum) as columns
    FROM pg_stat_user_indexes i
    JOIN pg_index idx ON i.indexrelid = idx.indexrelid
    JOIN pg_attribute a ON a.attrelid = idx.indrelid
        AND a.attnum = ANY(idx.indkey)
    GROUP BY schemaname, tablename, indexname, indexrelid
)
SELECT
    tablename,
    columns,
    count(*) as duplicate_count,
    string_agg(indexname, ', ') as indexes
FROM index_data
GROUP BY tablename, columns
HAVING count(*) > 1;
EOF

# 7. Archive old workouts
log_maintenance "7. Archiving old workout data..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF 2>&1 | tee -a "$MAINTENANCE_LOG"
-- Move workouts older than 2 years to archive
INSERT INTO tracking.workouts_archive
SELECT * FROM tracking.workouts
WHERE created_at < CURRENT_DATE - INTERVAL '2 years'
AND archived_at IS NULL;

UPDATE tracking.workouts SET archived_at = NOW()
WHERE created_at < CURRENT_DATE - INTERVAL '2 years'
AND archived_at IS NULL;

-- Verify archival
SELECT
    'Current workouts' as category,
    COUNT(*) as count
FROM tracking.workouts
WHERE archived_at IS NULL
UNION ALL
SELECT
    'Archived workouts',
    COUNT(*)
FROM tracking.workouts_archive;
EOF

# 8. Update table statistics
log_maintenance "8. Updating table statistics..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" -c "ANALYZE;" \
  2>&1 | tee -a "$MAINTENANCE_LOG"

# 9. Summary report
log_maintenance "9. Generating maintenance summary..."
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" << EOF 2>&1 | tee -a "$MAINTENANCE_LOG"
-- Database size
SELECT
    'Database size' as metric,
    pg_size_pretty(pg_database_size('gym_db')) as value;

-- Table statistics
SELECT
    'Largest table: ' || tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables t
JOIN pg_stat_user_tables s ON t.tablename = s.relname
WHERE t.table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 1;

-- Index statistics
SELECT
    'Total indexes' as metric,
    COUNT(*)::text as value
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');

-- Replication status
SELECT
    'Replication lag' as metric,
    EXTRACT(EPOCH FROM (NOW() - pg_last_xact_replay_timestamp()))::text || ' seconds' as value;
EOF

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

log_maintenance "=== MONTHLY MAINTENANCE COMPLETED ==="
log_maintenance "Total duration: $(($DURATION / 60)) minutes $(($DURATION % 60)) seconds"
log_maintenance "Log saved to: $MAINTENANCE_LOG"
```

**Schedule monthly maintenance window:**

```bash
# First Sunday of each month at 2:00 AM UTC (3-4 hour window)
# Notify stakeholders in advance
0 2 * * 0 [ $(date +\%d) -le 07 ] && /usr/local/bin/monthly-maintenance.sh
```

---

## VACUUM Operations

### Understanding VACUUM

**VACUUM types:**

```sql
-- Standard VACUUM (safe, concurrent)
VACUUM [ANALYZE] [table_name];

-- VACUUM FULL (reclaims disk space, locks table)
VACUUM FULL [table_name];

-- VACUUM with options
VACUUM (ANALYZE, VERBOSE, SKIP_LOCKED) table_name;
```

**VACUUM behavior:**

```
Standard VACUUM:
├─ Marks dead tuples as available for reuse
├─ Does NOT return disk space to OS
├─ Can run concurrently with other operations
├─ Recommended for regular maintenance
└─ Runtime: Minutes to hours

VACUUM FULL:
├─ Reclaims disk space back to OS
├─ Locks table (no reads/writes)
├─ Rewrites entire table
├─ Should be used sparingly
└─ Runtime: Hours to days
```

### VACUUM Configuration

**PostgreSQL autovacuum settings:**

```ini
# In postgresql.conf

# Enable autovacuum
autovacuum = on

# Number of autovacuum workers
autovacuum_max_workers = 3

# Time between autovacuum runs
autovacuum_naptime = 30s

# Threshold for autovacuum
autovacuum_vacuum_threshold = 50        # min 50 dead tuples
autovacuum_analyze_threshold = 50       # min 50 tuples changed

# Scale factor (percentage of table size)
autovacuum_vacuum_scale_factor = 0.1    # 10% of table size
autovacuum_analyze_scale_factor = 0.05  # 5% of table size

# Cost settings (lower = more aggressive)
autovacuum_vacuum_cost_delay = 2ms      # Pause between vacuum operations
autovacuum_vacuum_cost_limit = 200      # Cost limit per cycle
```

### Manual VACUUM

**Regular VACUUM on large tables:**

```sql
-- Start time: Monitor duration
SELECT NOW();

-- VACUUM with progress reporting
VACUUM (ANALYZE, VERBOSE) tracking.workouts;

-- Expected output:
-- INFO: vacuuming "tracking.workouts"
-- INFO: "workouts": removed 50000 row versions in 1234 pages
-- INFO: "workouts": index "idx_workouts_user_id" now contains 1000000 row versions in 5000 pages
-- INFO: "workouts": free space map: 100 pages used
-- ANALYZE - scanning heap
-- ANALYZE - done

-- Check completion time
SELECT NOW();
```

**Concurrent VACUUM (less impact):**

```bash
# Run VACUUM with reduced I/O impact
psql -c "VACUUM (ANALYZE, SKIP_LOCKED) tracking.workouts;"

# Or use a separate session with lower priority
nice -n 10 psql -c "VACUUM ANALYZE tracking.workouts;"
```

---

## ANALYZE Operations

### Understanding ANALYZE

**ANALYZE updates table statistics for query planner:**

```sql
-- Analyze single table
ANALYZE auth.users;

-- Analyze specific columns (for large tables)
ANALYZE auth.users(email, created_at);

-- Analyze with verbose output
ANALYZE VERBOSE training.training_plans;

-- Expected output:
-- INFO: analyzing "training.training_plans"
-- INFO: "training_plans": scanned 500 of 500 pages, 10000 live rows, 100 dead rows
```

### When to ANALYZE

**Run ANALYZE after:**

```
├─ Large data loads (BULK INSERT)
├─ Many DELETE operations (removed significant data)
├─ Data type changes or migrations
├─ After full VACUUM
├─ After index creation
└─ When query plans seem suboptimal
```

**Monitor ANALYZE status:**

```sql
-- Check when tables were last analyzed
SELECT
    schemaname,
    tablename,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze,
    n_live_tup,
    n_dead_tup
FROM pg_stat_user_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY last_analyze DESC;
```

---

## Index Maintenance

### Regular Index Checks

**Monitor index bloat:**

```sql
-- Check index size and fragmentation
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    ROUND(100.0 * idx_blks_hit / (idx_blks_hit + idx_blks_read), 2) as cache_ratio
FROM pg_stat_user_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_relation_size(indexrelid) DESC;
```

**Remove unused indexes:**

```sql
-- Find unused indexes (safe to remove)
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_relation_size(indexrelid) DESC;

-- Drop unused index (after verification)
DROP INDEX CONCURRENTLY auth.idx_users_old_field;
```

**Rebuild fragmented indexes:**

```bash
# Concurrent index rebuild (doesn't lock table)
psql -c "REINDEX INDEX CONCURRENTLY idx_users_email;"

# Force rebuild with lock (use during maintenance window)
psql -c "REINDEX INDEX idx_users_email;"
```

---

## Table Maintenance

### Check Table Integrity

**Verify table constraints:**

```sql
-- Check for constraint violations
SELECT
    constraint_name,
    table_name,
    constraint_type
FROM information_schema.table_constraints
WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY table_name;

-- Verify all constraints are valid
SELECT * FROM pg_constraint WHERE convalidated = FALSE;
```

**Detect bloated tables:**

```sql
-- Tables with high dead row ratio
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    n_live_tup,
    n_dead_tup,
    ROUND(100.0 * n_dead_tup / (n_live_tup + n_dead_tup), 2) as dead_ratio
FROM pg_stat_user_tables
WHERE n_live_tup > 0
AND n_dead_tup > n_live_tup * 0.1  -- More than 10% dead
ORDER BY n_dead_tup DESC;
```

### Table Cleanup

**Archive old data:**

```sql
-- Create archive table if not exists
CREATE TABLE IF NOT EXISTS tracking.workouts_archive (LIKE tracking.workouts);

-- Move old data to archive
INSERT INTO tracking.workouts_archive
SELECT * FROM tracking.workouts
WHERE created_at < CURRENT_DATE - INTERVAL '2 years';

-- Mark as archived
UPDATE tracking.workouts SET
  archived_at = NOW(),
  is_archived = TRUE
WHERE created_at < CURRENT_DATE - INTERVAL '2 years'
  AND archived_at IS NULL;

-- Verify archive
SELECT
    COUNT(*) as current_workouts
FROM tracking.workouts WHERE archived_at IS NULL;

SELECT
    COUNT(*) as archived_workouts
FROM tracking.workouts_archive;
```

---

## Replication Monitoring

### Check Replication Health

**Monitor replication status:**

```sql
-- Check replica lag
SELECT
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    pg_wal_lsn_diff(sent_lsn, replay_lsn) as replication_lag_bytes,
    EXTRACT(EPOCH FROM (NOW() - pg_last_xact_replay_timestamp())) as lag_seconds
FROM pg_stat_replication;

-- Expected output:
-- client_addr | state    | sent_lsn | write_lsn | flush_lsn | replay_lsn | lag_bytes | lag_seconds
-- 10.0.1.51   | streaming| 0/500000 | 0/500000  | 0/500000  | 0/500000   | 0         | 0.123
```

**Monitor WAL archiving:**

```sql
-- Check WAL archiving status
SELECT
    datname,
    archived_count,
    failed_count,
    last_archived_wal,
    last_failed_wal,
    last_failed_time
FROM pg_stat_archiver;

-- Expected: failed_count should be 0
```

**Verify replica catches up:**

```bash
#!/bin/bash
# Monitor replica lag until caught up

while true; do
    LAG=$(psql -t -c "SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) FROM pg_stat_replication LIMIT 1;")
    if [ "$LAG" == " " ] || [ "$LAG" -lt 1000 ]; then
        echo "✓ Replica caught up (lag: ${LAG} bytes)"
        break
    else
        echo "Replication lag: ${LAG} bytes"
        sleep 5
    fi
done
```

---

## Automated Maintenance

### Systemd Timer

**File: /etc/systemd/system/postgresql-maintenance.service**

```ini
[Unit]
Description=PostgreSQL Database Maintenance
After=postgresql.service
Requires=postgresql.service

[Service]
Type=oneshot
User=postgres
Group=postgres
ExecStart=/usr/local/bin/monthly-maintenance.sh
StandardOutput=journal
StandardError=journal
Environment="PGUSER=postgres"
```

**File: /etc/systemd/system/postgresql-maintenance.timer**

```ini
[Unit]
Description=PostgreSQL Maintenance Timer (Monthly)
Requires=postgresql-maintenance.service

[Timer]
OnCalendar=monthly
OnCalendar=Sun *-*-1..7 02:00:00
Persistent=true
Unit=postgresql-maintenance.service

[Install]
WantedBy=timers.target
```

**Enable and start:**

```bash
sudo systemctl daemon-reload
sudo systemctl enable postgresql-maintenance.timer
sudo systemctl start postgresql-maintenance.timer

# Check status
sudo systemctl status postgresql-maintenance.timer
sudo systemctl list-timers postgresql-maintenance.timer
```

---

## Monitoring & Alerting

### Prometheus Metrics

**Key maintenance metrics:**

```promql
# Dead tuple ratio
pg_stat_user_tables_n_dead_tup / (pg_stat_user_tables_n_live_tup + pg_stat_user_tables_n_dead_tup)

# Last vacuum age
time() - pg_stat_user_tables_last_vacuum

# Last analyze age
time() - pg_stat_user_tables_last_analyze

# Table bloat
pg_total_relation_size / pg_relation_size
```

**Alert rules:**

```yaml
groups:
  - name: postgresql_maintenance
    rules:
      - alert: TableBloatHigh
        expr: |
          (pg_stat_user_tables_n_dead_tup / 
           (pg_stat_user_tables_n_live_tup + pg_stat_user_tables_n_dead_tup)) > 0.25
        for: 1h
        annotations:
          summary: "Table {{ $labels.tablename }} has > 25% dead tuples"
          description: "Run VACUUM ANALYZE on {{ $labels.tablename }}"

      - alert: TableNotAnalyzed
        expr: (time() - pg_stat_user_tables_last_analyze) > 604800  # 1 week
        for: 1h
        annotations:
          summary: "Table {{ $labels.tablename }} not analyzed for 7+ days"
          description: "Run ANALYZE {{ $labels.tablename }}"

      - alert: UnusedIndexes
        expr: pg_stat_user_indexes_idx_scan == 0
        for: 7d
        annotations:
          summary: "Index {{ $labels.indexname }} unused for 7+ days"
          description: "Consider dropping {{ $labels.indexname }}"
```

---

## Best Practices

### Maintenance Best Practices

1. **Regular Schedule:**
   - Daily health checks
   - Weekly performance analysis
   - Monthly full maintenance window
   - Quarterly capacity review

2. **Proactive Monitoring:**
   - Watch table bloat metrics
   - Monitor index usage
   - Track replication lag
   - Alert on high dead tuple ratios

3. **Safe Operations:**
   - Always backup before maintenance
   - Use CONCURRENTLY for indexes (no locks)
   - Schedule during low-traffic windows
   - Have rollback plan ready

4. **Documentation:**
   - Log all maintenance operations
   - Document decision for index removal
   - Update runbooks regularly
   - Share findings with team

### Operational Best Practices

1. **Autovacuum Tuning:**
   - Monitor autovacuum activity
   - Adjust thresholds for high-churn tables
   - Balance with system load

2. **Archive Strategy:**
   - Archive workouts > 2 years old
   - Archive metrics > 1 year old
   - Verify before deleting
   - Keep archive tables indexed

3. **Index Strategy:**
   - Review index usage monthly
   - Remove unused indexes quarterly
   - Add missing indexes as needed
   - Document index purpose and date

---

## Troubleshooting

### High Dead Tuple Ratio

```bash
# Check dead tuple percentage
psql -c "SELECT schemaname, tablename, n_dead_tup, n_live_tup,
  ROUND(100.0 * n_dead_tup / (n_live_tup + n_dead_tup), 2) as dead_ratio
FROM pg_stat_user_tables
WHERE n_live_tup > 0
ORDER BY n_dead_tup DESC LIMIT 20;"

# Solution: Run VACUUM
psql -c "VACUUM ANALYZE table_name;"

# If still high after VACUUM: Run VACUUM FULL
psql -c "VACUUM FULL ANALYZE table_name;"
```

### Slow VACUUM

```bash
# Check if VACUUM is stuck
psql -c "SELECT * FROM pg_stat_progress_vacuum;"

# Kill if necessary (use with caution)
SELECT pg_terminate_backend(pid) FROM pg_stat_progress_vacuum;

# Resume: Restart PostgreSQL
systemctl restart postgresql

# Prevention: Tune autovacuum settings
# Reduce autovacuum_vacuum_cost_delay to run more frequently but faster
```

### Replication Lag

```bash
# Check lag status
psql -c "SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) as lag_bytes FROM pg_stat_replication;"

# Check for blocking queries on replica
psql -h replica -c "SELECT * FROM pg_stat_activity WHERE state != 'idle';"

# Check WAL archiving
psql -c "SELECT * FROM pg_stat_archiver;"

# Solution: Wait for replica to catch up or restart if stuck
```

---

**Related Documentation:**
- [01-database-overview.md](01-database-overview.md) - Database architecture
- [02-schema-design.md](02-schema-design.md) - Schema structure
- [03-backup-recovery.md](03-backup-recovery.md) - Backup procedures
- [04-performance-tuning.md](04-performance-tuning.md) - Performance optimization
- [05-migration-guide.md](05-migration-guide.md) - Migration procedures
- See [docs/operations/](../operations/) for operational procedures
