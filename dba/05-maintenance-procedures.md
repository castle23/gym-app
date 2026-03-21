# Database Maintenance Procedures

## Overview

Comprehensive guide to advanced maintenance procedures for production PostgreSQL systems at scale. Covers VACUUM strategies, ANALYZE optimization, index maintenance, table partitioning, bloat detection and remediation, automated maintenance scheduling, and monitoring for the Gym Platform. This guide focuses on DBA-level operational maintenance ensuring optimal database health, performance, and resource utilization.

## Table of Contents

- [VACUUM Fundamentals](#vacuum-fundamentals)
- [ANALYZE and Statistics](#analyze-and-statistics)
- [Bloat Detection and Remediation](#bloat-detection-and-remediation)
- [Index Maintenance](#index-maintenance)
- [Table Partitioning Maintenance](#table-partitioning-maintenance)
- [Maintenance Windows](#maintenance-windows)
- [Autovacuum Tuning](#autovacuum-tuning)
- [Scheduled Maintenance](#scheduled-maintenance)
- [Monitoring Maintenance](#monitoring-maintenance)
- [Troubleshooting Maintenance](#troubleshooting-maintenance)

---

## VACUUM Fundamentals

### Understanding VACUUM

PostgreSQL uses MVCC (Multi-Version Concurrency Control) which creates dead tuples when rows are updated/deleted. VACUUM reclaims this space.

**VACUUM Phases:**

```
Phase 1: HEAP SCAN
  ├─ Scan all pages
  ├─ Identify dead tuples
  └─ Build dead tuple map

Phase 2: INDEX SCAN
  ├─ Scan each index
  ├─ Remove index entries for dead tuples
  └─ Reclaim index space

Phase 3: HEAP CLEANUP
  ├─ Reclaim heap pages
  ├─ Update free space map
  └─ Truncate heap if possible

Phase 4: FSYNC
  └─ Sync to disk
```

### VACUUM Types

```sql
-- VACUUM (regular cleanup)
-- ✓ Reclaims space
-- ✓ Non-blocking (readers can work)
-- ✗ Slower than full vacuum
VACUUM workouts;

-- VACUUM FULL (aggressive)
-- ✓ Maximum space reclamation
-- ✗ Exclusive lock (blocks readers)
-- ✗ Requires table size in free space
VACUUM FULL workouts;

-- VACUUM ANALYZE (combine)
-- ✓ Vacuum + update statistics
-- ✓ Single pass
VACUUM ANALYZE workouts;

-- VACUUM FREEZE (aggressive freeze)
-- ✓ Prevents transaction wraparound
-- ✓ Freezes all xmin
VACUUM FREEZE workouts;

-- CONCURRENT VACUUM (non-blocking)
-- ✓ No exclusive lock
-- ✓ Concurrent readers allowed
VACUUM CONCURRENTLY workouts;
```

### VACUUM Configuration

```ini
# postgresql.conf - VACUUM tuning

# Autovacuum process settings
autovacuum = on                              # Enable autovacuum
autovacuum_naptime = 10s                     # Check every 10 seconds
autovacuum_max_workers = 3                   # 3 concurrent autovacuum workers

# Vacuum thresholds
autovacuum_vacuum_threshold = 50             # VACUUM after 50 dead rows
autovacuum_analyze_threshold = 50            # ANALYZE after 50 dead rows
autovacuum_vacuum_scale_factor = 0.02        # + 2% of table size
autovacuum_analyze_scale_factor = 0.01       # + 1% of table size

# Vacuum cost (I/O throttling)
autovacuum_vacuum_cost_delay = 20ms          # Wait 20ms between pages
autovacuum_vacuum_cost_limit = 200           # Max 200 cost units per vacuum

# Per-table settings for critical tables
ALTER TABLE accounts SET (
  autovacuum_vacuum_scale_factor = 0.001,
  autovacuum_analyze_scale_factor = 0.0005,
  autovacuum_vacuum_cost_delay = 5ms
);
```

### Manual VACUUM Strategy

```bash
#!/bin/bash
# maintenance-vacuum.sh - Scheduled VACUUM during maintenance window

DATABASES=("gym_auth" "gym_training" "gym_tracking" "gym_common")
MAINTENANCE_WINDOW="02:00"  # 2 AM
LOG_FILE="/var/log/postgresql/maintenance.log"

# Run during maintenance window
if [ "$(date +%H:%M)" = "$MAINTENANCE_WINDOW" ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting vacuum maintenance" >> "$LOG_FILE"
  
  for db in "${DATABASES[@]}"; do
    echo "Vacuuming $db..." >> "$LOG_FILE"
    
    # Regular vacuum with analyze
    vacuumdb -U postgres -d "$db" \
      --analyze \
      --verbose 2>&1 | tee -a "$LOG_FILE"
    
    # Aggressive vacuum for bloated tables
    psql -U postgres -d "$db" << SQL >> "$LOG_FILE"
-- Vacuum largest tables
VACUUM ANALYZE accounts;
VACUUM ANALYZE workouts;
VACUUM ANALYZE metrics;

-- Check bloat percentage
SELECT 
  schemaname,
  tablename,
  ROUND(100.0 * dead_tuples / (live_tuples + dead_tuples), 2) as dead_ratio
FROM pg_stat_user_tables
WHERE (live_tuples + dead_tuples) > 0
  AND dead_tuples > 100000
ORDER BY dead_ratio DESC
LIMIT 10;
SQL
  done
  
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Vacuum maintenance completed" >> "$LOG_FILE"
fi
```

---

## ANALYZE and Statistics

### Understanding ANALYZE

ANALYZE collects column statistics used by the query planner to estimate row counts and choose optimal execution plans.

**Statistics Collected:**

```sql
-- View statistics for table
SELECT * FROM pg_stats WHERE tablename = 'workouts';

-- Columns in statistics
-- - n_distinct: Estimated number of distinct values
-- - n_inherited: Values from inherited tables
-- - null_frac: Fraction of NULL values
-- - avg_width: Average value width in bytes
-- - correlation: Correlation between physical order and logical order
-- - histogram_bounds: Min/max ranges for values
```

### ANALYZE Strategy

```sql
-- Manual ANALYZE on specific table
ANALYZE gym_training.workouts;

-- ANALYZE with sampling (for large tables)
-- Faster, may be less accurate
ANALYZE (SAMPLE 1000) gym_training.workouts;

-- ANALYZE specific columns
ANALYZE gym_training.workouts (user_id, created_at, status);

-- Check freshness of statistics
SELECT 
  schemaname,
  tablename,
  last_analyze,
  last_autoanalyze,
  analyze_count,
  autoanalyze_count
FROM pg_stat_user_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY last_analyze DESC;
```

### Autovacuum Statistics

```sql
-- Autovacuum configuration for each table
SELECT 
  schemaname,
  tablename,
  autovacuum_vacuum_scale_factor,
  autovacuum_analyze_scale_factor,
  autovacuum_vacuum_cost_delay,
  autovacuum_vacuum_cost_limit
FROM pg_class c
JOIN pg_namespace n ON c.relnamespace = n.oid
WHERE relkind = 'r'
  AND schemaname NOT IN ('pg_catalog', 'information_schema');

-- Create aggressive ANALYZE schedule for hot tables
ALTER TABLE workouts SET (
  autovacuum_analyze_scale_factor = 0.0001,  -- Analyze after every 0.01% change
  autovacuum_analyze_threshold = 10           -- And after just 10 rows changed
);
```

---

## Bloat Detection and Remediation

### Bloat Detection

```sql
-- Estimate table bloat percentage
WITH bloat_data AS (
  SELECT 
    schemaname,
    tablename,
    live_tuples,
    dead_tuples,
    (dead_tuples::float / (live_tuples + dead_tuples)) * 100 as dead_ratio,
    pg_total_relation_size(schemaname||'.'||tablename) as total_size
  FROM pg_stat_user_tables
  WHERE (live_tuples + dead_tuples) > 0
)
SELECT 
  schemaname,
  tablename,
  ROUND(dead_ratio, 2) as bloat_percent,
  pg_size_pretty(total_size) as table_size,
  CASE 
    WHEN dead_ratio > 50 THEN 'CRITICAL'
    WHEN dead_ratio > 20 THEN 'HIGH'
    WHEN dead_ratio > 10 THEN 'MEDIUM'
    ELSE 'LOW'
  END as bloat_level
FROM bloat_data
WHERE dead_ratio > 5
ORDER BY dead_ratio DESC;

-- Index bloat estimation
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch,
  pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
  CASE 
    WHEN idx_scan = 0 THEN 'UNUSED'
    WHEN idx_tup_fetch::float / NULLIF(idx_tup_read, 0) < 0.1 THEN 'LOW_USAGE'
    ELSE 'ACTIVE'
  END as usage_status
FROM pg_stat_user_indexes
WHERE pg_relation_size(indexrelid) > 10*1024*1024  -- > 10MB
ORDER BY pg_relation_size(indexrelid) DESC;
```

### Bloat Remediation

```sql
-- Option 1: Regular VACUUM (no downtime)
-- Reclaims space but can't return to OS
VACUUM ANALYZE workouts;

-- Option 2: VACUUM FULL (requires downtime)
-- Returns space to OS, needs exclusive lock
BEGIN;
LOCK TABLE workouts IN EXCLUSIVE MODE;
VACUUM FULL workouts;
ANALYZE workouts;
COMMIT;

-- Option 3: CLUSTER (physical reorganization)
-- Reorganizes table, requires index
CREATE INDEX idx_workouts_created_temporary ON workouts(created_at DESC);
CLUSTER workouts USING idx_workouts_created_temporary;
DROP INDEX idx_workouts_created_temporary;

-- Option 4: Rewrite with pg_repack (online)
-- Install extension first
CREATE EXTENSION IF NOT EXISTS pg_repack;

-- Compact table without downtime
pg_repack -h localhost -U postgres -d gym_training -t workouts --no-analyze;

-- Option 5: Partition and archive old data
-- Move old partitions to separate tablespace
ALTER TABLE workouts_archived SET TABLESPACE archive_space;

-- Truncate oldest partitions
TRUNCATE TABLE workouts_2023_01;
```

---

## Index Maintenance

### Index Bloat and Maintenance

```sql
-- Find bloated indexes
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_blks_read,
  idx_blks_hit,
  CASE WHEN idx_blks_read + idx_blks_hit > 0
       THEN ROUND(100.0 * idx_blks_hit / (idx_blks_read + idx_blks_hit), 2)
       ELSE 0
  END as cache_hit_ratio,
  pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE idx_blks_read > 100
ORDER BY pg_relation_size(indexrelid) DESC;

-- Rebuild fragmented indexes
REINDEX INDEX CONCURRENTLY idx_workouts_user_created;

-- Rebuild all indexes on table (concurrent)
REINDEX TABLE CONCURRENTLY workouts;

-- Check for duplicate indexes
SELECT 
  t1.schemaname,
  t1.tablename,
  t1.indexname as index1,
  t2.indexname as index2,
  pg_size_pretty(pg_relation_size(t1.indexrelid)) as size1,
  pg_size_pretty(pg_relation_size(t2.indexrelid)) as size2
FROM pg_indexes t1
JOIN pg_indexes t2 ON 
  t1.schemaname = t2.schemaname 
  AND t1.tablename = t2.tablename 
  AND t1.indexdef LIKE t2.indexdef || '%'
  AND t1.indexname < t2.indexname
WHERE t1.schemaname NOT IN ('pg_catalog', 'information_schema');
```

### Unused Index Cleanup

```sql
-- Find truly unused indexes
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
  ROUND((pg_relation_size(indexrelid)::float / 
    pg_total_relation_size(schemaname||'.'||tablename)) * 100, 2) as pct_of_table
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE '%_pkey'  -- Exclude primary keys
  AND pg_relation_size(indexrelid) > 1*1024*1024  -- Only > 1MB
ORDER BY pg_relation_size(indexrelid) DESC;

-- Create DROP script (review before running!)
SELECT 
  'DROP INDEX CONCURRENTLY ' || schemaname || '.' || indexname || ';' as drop_statement
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE '%_pkey'
  AND pg_relation_size(indexrelid) > 1*1024*1024;
```

---

## Table Partitioning Maintenance

### Automatic Partition Management

```sql
-- Create procedure for monthly partition creation
CREATE OR REPLACE PROCEDURE create_monthly_partitions(
  p_table_name TEXT,
  p_base_schema TEXT DEFAULT 'public'
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_partition_name TEXT;
  v_start_date DATE;
  v_end_date DATE;
BEGIN
  v_start_date := DATE_TRUNC('month', CURRENT_DATE);
  v_end_date := v_start_date + INTERVAL '1 month';
  
  v_partition_name := p_table_name || '_' || 
    TO_CHAR(v_start_date, 'YYYY_MM');
  
  -- Create next 3 months worth of partitions
  FOR i IN 0..2 LOOP
    v_start_date := DATE_TRUNC('month', CURRENT_DATE + (i * INTERVAL '1 month'));
    v_end_date := v_start_date + INTERVAL '1 month';
    v_partition_name := p_table_name || '_' || 
      TO_CHAR(v_start_date, 'YYYY_MM');
    
    BEGIN
      EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        v_partition_name,
        p_table_name,
        v_start_date,
        v_end_date
      );
      RAISE NOTICE 'Created partition: %', v_partition_name;
    EXCEPTION WHEN duplicate_table THEN
      RAISE NOTICE 'Partition % already exists', v_partition_name;
    END;
  END LOOP;
END;
$$;

-- Schedule partition creation
SELECT create_monthly_partitions('metrics', 'gym_tracking');
SELECT create_monthly_partitions('logs', 'gym_common');
```

### Partition Maintenance Tasks

```sql
-- Check partition sizes
SELECT 
  schemaname,
  tablename,
  ROUND(100.0 * pg_total_relation_size(schemaname||'.'||tablename) /
    pg_total_relation_size('gym_tracking.metrics'), 2) as pct_of_parent,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as partition_size
FROM pg_tables
WHERE tablename LIKE 'metrics_%'
  AND schemaname = 'gym_tracking'
ORDER BY tablename DESC;

-- Archive old partitions
-- Move to lower-cost storage
ALTER TABLE metrics_2023_01 SET TABLESPACE archive_space;

-- Or truncate
TRUNCATE TABLE metrics_2023_01;

-- Detach and drop very old partitions
ALTER TABLE metrics DETACH PARTITION metrics_2022_12;
DROP TABLE metrics_2022_12;

-- Check for missing partitions
SELECT 
  tablename,
  pg_get_partition_constraintdef(oid) as constraint
FROM pg_class
WHERE relname LIKE 'metrics_%'
ORDER BY tablename;
```

---

## Maintenance Windows

### Scheduling Maintenance

```bash
#!/bin/bash
# maintenance-scheduler.sh - Automated maintenance window

MAINTENANCE_DAY="Sunday"
MAINTENANCE_START="02:00"
MAINTENANCE_DURATION="2 hours"
DB_USER="postgres"
LOG_FILE="/var/log/postgresql/maintenance.log"

# Check if it's maintenance day
if [ "$(date +%A)" = "$MAINTENANCE_DAY" ] && [ "$(date +%H:%M)" = "$MAINTENANCE_START" ]; then
  
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ===== MAINTENANCE WINDOW STARTED =====" >> "$LOG_FILE"
  
  # Step 1: Vacuum all databases
  vacuumdb -U "$DB_USER" -a --analyze 2>&1 | tee -a "$LOG_FILE"
  
  # Step 2: Reindex fragmented indexes
  psql -U "$DB_USER" -d postgres << SQL >> "$LOG_FILE"
-- Find and reindex indexes with poor hit ratio
WITH index_stats AS (
  SELECT 
    schemaname,
    tablename,
    indexname,
    CASE WHEN idx_blks_read + idx_blks_hit = 0 THEN 100
         ELSE ROUND(100.0 * idx_blks_hit / (idx_blks_hit + idx_blks_read), 2)
    END as cache_hit_ratio
  FROM pg_stat_user_indexes
)
SELECT 
  'REINDEX INDEX CONCURRENTLY ' || schemaname || '.' || indexname || ';'
FROM index_stats
WHERE cache_hit_ratio < 80
  AND indexname NOT LIKE '%_pkey'
LIMIT 10;
SQL
  
  # Step 3: Cluster critical tables (optional, very aggressive)
  # CLUSTER workouts USING idx_workouts_user_id;
  
  # Step 4: Archive old partitions
  psql -U "$DB_USER" -d gym_tracking << SQL >> "$LOG_FILE"
-- Archive metrics older than 6 months
ALTER TABLE metrics DETACH PARTITION metrics_2023_06 CONCURRENTLY;
DROP TABLE IF EXISTS metrics_2023_06;
SQL
  
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ===== MAINTENANCE WINDOW COMPLETED =====" >> "$LOG_FILE"
  
fi
```

### Monitoring Maintenance Progress

```bash
#!/bin/bash
# monitor-maintenance.sh - Watch maintenance operations

while true; do
  clear
  echo "=== PostgreSQL Maintenance Monitor ==="
  echo "Time: $(date)"
  echo ""
  
  psql -U postgres -d postgres << SQL
SELECT 
  pid,
  usename,
  CASE 
    WHEN query LIKE '%vacuum%' THEN 'VACUUM'
    WHEN query LIKE '%analyze%' THEN 'ANALYZE'
    WHEN query LIKE '%index%' THEN 'INDEX'
    ELSE 'OTHER'
  END as operation,
  EXTRACT(EPOCH FROM (NOW() - query_start))::int as duration_sec,
  query_start
FROM pg_stat_activity
WHERE query NOT LIKE 'SELECT%'
  AND pid != pg_backend_pid()
ORDER BY query_start;
SQL
  
  sleep 5
done
```

---

## Autovacuum Tuning

### Aggressive Autovacuum for High-Activity Tables

```sql
-- Identify high-activity tables
SELECT 
  schemaname,
  tablename,
  n_tup_ins + n_tup_upd + n_tup_del as total_changes,
  n_dead_tup,
  ROUND(100.0 * n_dead_tup / (n_live_tup + n_dead_tup), 2) as dead_ratio
FROM pg_stat_user_tables
WHERE n_live_tup + n_dead_tup > 0
ORDER BY total_changes DESC
LIMIT 20;

-- Create aggressive autovacuum settings for hot tables
ALTER TABLE workouts SET (
  autovacuum_vacuum_threshold = 10,
  autovacuum_analyze_threshold = 10,
  autovacuum_vacuum_scale_factor = 0.0001,
  autovacuum_analyze_scale_factor = 0.00005,
  autovacuum_vacuum_cost_delay = 5,
  autovacuum_vacuum_cost_limit = 500
);

-- For cold tables, relax autovacuum
ALTER TABLE archive_data SET (
  autovacuum_vacuum_threshold = 5000,
  autovacuum_analyze_threshold = 2500,
  autovacuum_vacuum_scale_factor = 0.1,
  autovacuum_analyze_scale_factor = 0.05,
  autovacuum_enabled = false
);

-- Run manual vacuum on startup to catch up
BEGIN;
VACUUM ANALYZE archive_data;
COMMIT;
```

---

## Scheduled Maintenance

### Daily Maintenance Cron Jobs

```bash
# /etc/cron.d/postgresql-maintenance

# Daily VACUUM at 3 AM
0 3 * * * postgres /usr/local/bin/maintenance-daily.sh >> /var/log/postgresql/maintenance-daily.log 2>&1

# Weekly full cluster cleanup on Sunday at 2 AM
0 2 * * 0 postgres /usr/local/bin/maintenance-weekly.sh >> /var/log/postgresql/maintenance-weekly.log 2>&1

# Monthly partition rotation first day at 1 AM
0 1 1 * * postgres psql -U postgres -d gym_common -c "SELECT create_monthly_partitions('metrics')" >> /var/log/postgresql/maintenance-monthly.log 2>&1

# Hourly index monitoring
0 * * * * postgres psql -U postgres -d postgres -c "SELECT pg_stat_reset_shared('block');" >> /var/log/postgresql/index-stats.log 2>&1
```

### Custom Maintenance Procedures

```sql
-- Create comprehensive maintenance function
CREATE OR REPLACE FUNCTION maintenance_full()
RETURNS TABLE (step TEXT, status TEXT, duration_ms BIGINT)
LANGUAGE plpgsql
AS $$
DECLARE
  v_start_time TIMESTAMP;
  v_end_time TIMESTAMP;
BEGIN
  -- Step 1: Vacuum all tables
  v_start_time := CLOCK_TIMESTAMP();
  VACUUM ANALYZE;
  v_end_time := CLOCK_TIMESTAMP();
  RETURN QUERY SELECT 
    'VACUUM ANALYZE'::TEXT, 
    'COMPLETED'::TEXT,
    EXTRACT(EPOCH FROM (v_end_time - v_start_time))::BIGINT * 1000;
  
  -- Step 2: Reindex all
  v_start_time := CLOCK_TIMESTAMP();
  REINDEX DATABASE CONCURRENTLY;
  v_end_time := CLOCK_TIMESTAMP();
  RETURN QUERY SELECT 
    'REINDEX DATABASE'::TEXT, 
    'COMPLETED'::TEXT,
    EXTRACT(EPOCH FROM (v_end_time - v_start_time))::BIGINT * 1000;
  
  -- Step 3: Clean up dead tuples in toast tables
  v_start_time := CLOCK_TIMESTAMP();
  VACUUM FREEZE;
  v_end_time := CLOCK_TIMESTAMP();
  RETURN QUERY SELECT 
    'VACUUM FREEZE'::TEXT, 
    'COMPLETED'::TEXT,
    EXTRACT(EPOCH FROM (v_end_time - v_start_time))::BIGINT * 1000;
  
END;
$$;

-- Execute maintenance
SELECT * FROM maintenance_full();
```

---

## Monitoring Maintenance

### Maintenance Health Dashboard

```sql
-- Maintenance Overview
SELECT 
  'Autovacuum Process Status' as metric,
  CASE WHEN autovacuum THEN 'ENABLED' ELSE 'DISABLED' END as status
FROM pg_settings
WHERE name = 'autovacuum'
UNION ALL
SELECT 
  'Tables needing urgent VACUUM',
  COUNT(*)::TEXT
FROM pg_stat_user_tables
WHERE n_dead_tup > 100000
UNION ALL
SELECT 
  'Tables with high bloat (>20%)',
  COUNT(*)::TEXT
FROM pg_stat_user_tables
WHERE (n_dead_tup::float / (n_live_tup + n_dead_tup)) > 0.2
UNION ALL
SELECT 
  'Unused indexes (>1MB)',
  COUNT(*)::TEXT
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND pg_relation_size(indexrelid) > 1*1024*1024;

-- Autovacuum activity
SELECT 
  datname,
  schemaname,
  relname,
  last_vacuum,
  last_autovacuum,
  EXTRACT(EPOCH FROM (NOW() - last_autovacuum))::INT as seconds_since_autovacuum,
  n_dead_tup,
  ROUND(100.0 * n_dead_tup / (n_live_tup + n_dead_tup), 2) as dead_ratio
FROM pg_stat_user_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY n_dead_tup DESC;
```

---

## Troubleshooting Maintenance

### Common Maintenance Issues

**Issue: Autovacuum not running on table**

```sql
-- Check autovacuum enabled
SELECT 
  schemaname,
  tablename,
  (SELECT setting::BOOLEAN FROM pg_settings WHERE name = 'autovacuum') as global_autovacuum,
  (SELECT reloptions FROM pg_class WHERE oid = to_regclass(schemaname||'.'||tablename)) as table_options
WHERE tablename = 'workouts';

-- Enable autovacuum if disabled
ALTER TABLE workouts SET (autovacuum_enabled = true);

-- Reset to defaults
ALTER TABLE workouts RESET (autovacuum_vacuum_threshold);
```

**Issue: VACUUM taking too long**

```sql
-- Monitor VACUUM progress
SELECT 
  pid,
  now() - pg_stat_get_xact_start_timestamp(pid) as duration,
  query
FROM pg_stat_activity
WHERE query LIKE '%VACUUM%';

-- Cancel long-running vacuum if needed
SELECT pg_terminate_backend(pid);

-- Use VACUUM with reduced cost
ALTER TABLE workouts SET (
  autovacuum_vacuum_cost_delay = 100ms,
  autovacuum_vacuum_cost_limit = 50
);
```

**Issue: Transaction wraparound warning**

```sql
-- Check transaction wraparound status
SELECT 
  datname,
  EXTRACT(EPOCH FROM (NOW() - query_start))::INT as query_age_sec,
  CASE 
    WHEN age(datfrozenxid) > 1500000000 THEN 'CRITICAL'
    WHEN age(datfrozenxid) > 1000000000 THEN 'WARNING'
    ELSE 'OK'
  END as wraparound_status
FROM pg_database
WHERE datallowconn
ORDER BY datfrozenxid;

-- Emergency VACUUM FREEZE
VACUUM FREEZE;
```

---

## Related Documentation

- [03-backup-recovery.md](03-backup-recovery.md) - Backup before maintenance
- [04-performance-tuning.md](04-performance-tuning.md) - Tuning after maintenance
- [02-database-architecture.md](02-database-architecture.md) - VACUUM internals

