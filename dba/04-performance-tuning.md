# Performance Tuning for DBAs

## Overview

Advanced performance tuning guide for PostgreSQL in production environments. Covers query optimization at scale, index design strategies, memory configuration, I/O optimization, query parallelization, workload analysis, and systematic tuning approaches for DBAs managing the Gym Platform's critical databases. This guide goes beyond basic configuration into deep performance analysis and optimization techniques.

## Table of Contents

- [Performance Tuning Methodology](#performance-tuning-methodology)
- [Query Performance Analysis](#query-performance-analysis)
- [Index Design and Optimization](#index-design-and-optimization)
- [Memory Configuration](#memory-configuration)
- [I/O Performance Tuning](#io-performance-tuning)
- [Query Parallelization](#query-parallelization)
- [Table Partitioning](#table-partitioning)
- [Statistics and Query Planning](#statistics-and-query-planning)
- [Connection Pooling](#connection-pooling)
- [Workload-Specific Tuning](#workload-specific-tuning)
- [Monitoring and Baselines](#monitoring-and-baselines)
- [Troubleshooting Performance](#troubleshooting-performance)

---

## Performance Tuning Methodology

### The EXPLAIN ANALYZE Workflow

**Always Start with EXPLAIN ANALYZE:**

```sql
-- Enable timing and detailed output
SET log_min_duration_statement = 0;
SET log_statement = 'all';

-- Analyze problematic query
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING)
SELECT u.id, u.email, COUNT(w.id) as workout_count
FROM users u
LEFT JOIN workouts w ON u.id = w.user_id
WHERE u.created_at > NOW() - INTERVAL '30 days'
GROUP BY u.id
HAVING COUNT(w.id) > 0
ORDER BY workout_count DESC
LIMIT 100;

-- Output shows:
-- - Actual vs Estimated rows
-- - Buffer hits (memory vs disk)
-- - Node timing breakdown
-- - Filter effectiveness
```

### Performance Tuning Checklist

```
STEP 1: MEASUREMENT
  ☐ Enable query logging (log_min_duration_statement)
  ☐ Capture baseline metrics
  ☐ Identify slow queries
  ☐ Check current system resources

STEP 2: ANALYSIS
  ☐ Run EXPLAIN ANALYZE on slow queries
  ☐ Identify sequential scans on large tables
  ☐ Check index usage statistics
  ☐ Review join orders and filter selectivity

STEP 3: HYPOTHESIS
  ☐ Formulate tuning hypothesis
  ☐ Identify missing indexes
  ☐ Check statistics accuracy
  ☐ Consider query rewrite opportunities

STEP 4: IMPLEMENTATION
  ☐ Create new indexes in test environment
  ☐ Rewrite queries if needed
  ☐ Test with EXPLAIN ANALYZE
  ☐ Measure improvement

STEP 5: VALIDATION
  ☐ Run in staging environment
  ☐ Monitor for regression
  ☐ Deploy to production
  ☐ Document changes
```

---

## Query Performance Analysis

### Identifying Slow Queries

**Enable Query Logging:**

```ini
# postgresql.conf - Query performance logging

# Log all queries slower than 100ms
log_min_duration_statement = 100

# Log all queries (for analysis)
log_statement = 'all'

# Log connections/disconnections
log_connections = on
log_disconnections = on

# Include query details
log_duration = on
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '

# Log lock waits
log_lock_waits = on
deadlock_timeout = 1s
```

**Query Analysis Script:**

```bash
#!/bin/bash
# analyze-slow-queries.sh - Extract and analyze slow queries

LOGFILE="/var/log/postgresql/postgresql.log"
SLOW_THRESHOLD_MS=100
ANALYSIS_FILE="/tmp/slow_queries_$(date +%Y%m%d_%H%M%S).txt"

echo "=== PostgreSQL Slow Query Analysis ===" > "$ANALYSIS_FILE"
echo "Generated: $(date)" >> "$ANALYSIS_FILE"
echo "Threshold: ${SLOW_THRESHOLD_MS}ms" >> "$ANALYSIS_FILE"
echo "" >> "$ANALYSIS_FILE"

# Extract slow queries
grep "duration: [0-9]*\.[0-9]*ms" "$LOGFILE" | \
  awk -F'duration: ' '{print $2}' | \
  awk -F'ms' '{print $1}' | \
  awk -v thresh="$SLOW_THRESHOLD_MS" '$1 > thresh' | \
  sort -rn | \
  head -20 >> "$ANALYSIS_FILE"

echo "" >> "$ANALYSIS_FILE"
echo "=== Top 10 Most Frequent Queries ===" >> "$ANALYSIS_FILE"

# Find most common slow queries
grep "duration: [0-9]*\.[0-9]*ms" "$LOGFILE" | \
  awk '{
    # Extract query (simplified)
    for(i=1; i<=NF; i++) {
      if($i ~ /^SELECT|^UPDATE|^INSERT|^DELETE/) {
        query=$(i) " " $(i+1) " " $(i+2);
        break;
      }
    }
    print query;
  }' | \
  sort | uniq -c | sort -rn | head -10 >> "$ANALYSIS_FILE"

cat "$ANALYSIS_FILE"
```

### Auto-Explain for Problem Queries

**Setup auto_explain module:**

```sql
-- Create extension
CREATE EXTENSION IF NOT EXISTS auto_explain;

-- Configure auto_explain
ALTER SYSTEM SET shared_preload_libraries = 'auto_explain';
ALTER SYSTEM SET auto_explain.log_min_duration = 100;  -- Log queries > 100ms
ALTER SYSTEM SET auto_explain.log_analyze = on;
ALTER SYSTEM SET auto_explain.log_verbose = on;
ALTER SYSTEM SET auto_explain.log_buffers = on;
ALTER SYSTEM SET auto_explain.sample_rate = 0.1;  -- Sample 10% of slow queries

-- Apply settings
SELECT pg_reload_conf();

-- Verify
SELECT name, setting FROM pg_settings WHERE name LIKE 'auto_explain%';
```

### Analysis with pg_stat_statements

```sql
-- Install extension (if not already installed)
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Top 20 slowest queries by total time
SELECT 
  query,
  calls,
  total_time,
  mean_time,
  max_time,
  rows
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
  AND mean_time > 50  -- Queries averaging > 50ms
ORDER BY total_time DESC
LIMIT 20;

-- Most frequently called queries
SELECT 
  query,
  calls,
  total_time,
  mean_time,
  stddev_time
FROM pg_stat_statements
ORDER BY calls DESC
LIMIT 20;

-- Queries with high planning time
SELECT 
  query,
  calls,
  total_plan_time,
  mean_plan_time,
  total_exec_time,
  mean_exec_time
FROM pg_stat_statements
WHERE mean_plan_time > 10  -- Average planning > 10ms
ORDER BY total_plan_time DESC
LIMIT 10;

-- Reset statistics to focus on recent activity
SELECT pg_stat_statements_reset();
```

---

## Index Design and Optimization

### Index Selection Strategy

**Multi-Column Index Design:**

```sql
-- Pattern: Most restrictive columns first

-- GOOD: Highly selective columns first
CREATE INDEX idx_workouts_user_created_status 
  ON workouts(user_id, created_at DESC, status)
  WHERE deleted_at IS NULL;

-- QUERY benefits from all columns
EXPLAIN ANALYZE
SELECT * FROM workouts
WHERE user_id = 123 
  AND created_at > NOW() - INTERVAL '30 days'
  AND status = 'completed';
```

**Index Strategy by Query Pattern:**

```sql
-- 1. FILTER + ORDER + LIMIT
-- Index: (filter_column, order_column)
CREATE INDEX idx_users_status_created 
  ON users(status, created_at DESC);

-- Efficient query
EXPLAIN ANALYZE
SELECT id, email, created_at 
FROM users
WHERE status = 'active'
ORDER BY created_at DESC
LIMIT 100;

-- 2. JOIN + FILTER
-- Index: (join_column, filter_column)
CREATE INDEX idx_workouts_user_type
  ON workouts(user_id, workout_type);

-- 3. GROUP BY + AGGREGATE
-- Index: (group_column, aggregate_column)
CREATE INDEX idx_metrics_user_timestamp
  ON metrics(user_id, timestamp DESC);

-- 4. DISTINCT + ORDER
-- Index: (distinct_column, order_column)
CREATE INDEX idx_sessions_user_created
  ON sessions(user_id, created_at DESC);
```

### Covering Indexes (Include Clause)

```sql
-- Index covers all columns needed for query
-- Avoids heap lookup (heap-only tuples)

-- WITHOUT covering index:
-- Index lookup → Heap lookup for data
-- 2 I/O operations

CREATE INDEX idx_users_email_covering
  ON users(email)
  INCLUDE (id, name, status, created_at);

-- Query needs ONLY index, no heap lookup
-- 1 I/O operation (much faster!)
EXPLAIN ANALYZE
SELECT id, name, status, created_at
FROM users
WHERE email = 'user@gym.local';
```

### Partial Indexes

```sql
-- Index only relevant rows (reduce size, improve speed)

-- Only active users
CREATE INDEX idx_active_users_email
  ON users(email)
  WHERE status = 'active';

-- Only recent deleted records
CREATE INDEX idx_soft_deleted_recent
  ON workouts(user_id, created_at)
  WHERE deleted_at IS NOT NULL
    AND deleted_at > NOW() - INTERVAL '90 days';

-- Query using partial index
EXPLAIN ANALYZE
SELECT COUNT(*) FROM users
WHERE status = 'active' AND email LIKE '%@gmail.com';
```

### Expression Indexes

```sql
-- Index computed values for fast filtering

-- Search by email domain
CREATE INDEX idx_users_email_domain
  ON users(LOWER(SUBSTRING(email FROM '@' FOR 100)));

-- Date-based search
CREATE INDEX idx_workouts_date
  ON workouts(DATE(created_at));

-- Range search optimization
CREATE INDEX idx_metrics_range
  ON metrics((value - 0.5) * 100);  -- Normalized range

-- Query using expression index
EXPLAIN ANALYZE
SELECT COUNT(*) FROM users
WHERE LOWER(SUBSTRING(email FROM '@' FOR 100)) = 'gmail.com';
```

### BRIN Indexes (Large Tables)

```sql
-- BRIN: Block Range Index - fast on sorted data, small size

-- For time-series data (naturally sorted)
CREATE INDEX idx_metrics_timestamp_brin
  ON metrics USING BRIN (timestamp DESC)
  WITH (pages_per_range = 64);

-- For large tables with locality
CREATE INDEX idx_logs_created_brin
  ON logs USING BRIN (created_at)
  WITH (pages_per_range = 128);

-- Size comparison:
-- B-tree index: ~200MB
-- BRIN index: ~2MB (100x smaller!)

-- Query performance still good for range scans
EXPLAIN ANALYZE
SELECT * FROM metrics
WHERE timestamp > NOW() - INTERVAL '7 days'
ORDER BY timestamp DESC
LIMIT 10000;
```

### Index Maintenance

```sql
-- Identify unused indexes
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch,
  pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE 'pg_toast%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Duplicate indexes
WITH index_info AS (
  SELECT 
    schemaname,
    tablename,
    indexname,
    array_agg(attname ORDER BY attnum) as columns,
    pg_size_pretty(pg_relation_size(indexrelid)) as size
  FROM pg_indexes
  JOIN pg_index ON pg_indexes.indexname = pg_index.indexname::text
  JOIN pg_attribute ON pg_attribute.attrelid = pg_index.indrelid
    AND pg_attribute.attnum = ANY(pg_index.indkey)
  GROUP BY schemaname, tablename, indexname, indexrelid
)
SELECT * FROM index_info
WHERE (tablename, columns) IN (
  SELECT tablename, columns FROM index_info GROUP BY tablename, columns HAVING COUNT(*) > 1
);

-- REINDEX during maintenance window
REINDEX INDEX CONCURRENTLY idx_old_index;
DROP INDEX IF EXISTS idx_duplicate_index;

-- Rebuild fragmented indexes
SELECT 
  schemaname,
  tablename,
  indexname,
  CASE WHEN idx_blks_hit + idx_blks_read > 0 
       THEN ROUND(100.0 * idx_blks_hit / (idx_blks_hit + idx_blks_read), 2)
       ELSE 0
  END as cache_hit_ratio
FROM pg_statio_user_indexes
WHERE idx_blks_read > 100
ORDER BY cache_hit_ratio ASC;
```

---

## Memory Configuration

### Shared Buffers Optimization

**Calculation:**

```bash
#!/bin/bash
# Calculate optimal shared_buffers size

TOTAL_RAM_GB=$(free -g | awk '/^Mem:/{print $2}')
WORKLOAD_TYPE=$1  # "transactional" or "analytics"

if [ "$WORKLOAD_TYPE" = "analytics" ]; then
  # Analytics: Use more shared buffers (60-70%)
  SHARED_BUFFERS_GB=$((TOTAL_RAM_GB * 70 / 100))
else
  # Transactional: Use 25% (standard rule of thumb)
  SHARED_BUFFERS_GB=$((TOTAL_RAM_GB * 25 / 100))
fi

# Min 256MB, reasonable max 40GB
if [ "$SHARED_BUFFERS_GB" -lt 1 ]; then
  SHARED_BUFFERS_GB=1
elif [ "$SHARED_BUFFERS_GB" -gt 40 ]; then
  SHARED_BUFFERS_GB=40
fi

echo "Total RAM: ${TOTAL_RAM_GB}GB"
echo "Recommended shared_buffers: ${SHARED_BUFFERS_GB}GB"
echo "In postgresql.conf: shared_buffers = $(($SHARED_BUFFERS_GB * 1024))MB"
```

**Advanced Configuration:**

```ini
# postgresql.conf - Memory tuning for different workloads

# ===== TRANSACTIONAL WORKLOAD (OLTP) =====
# Small, frequent transactions
shared_buffers = 4GB              # 25% of 16GB server
effective_cache_size = 12GB       # 75% of RAM
work_mem = 16MB                   # RAM / (max_connections * 2)
maintenance_work_mem = 1GB
max_connections = 200
random_page_cost = 1.1            # Fast SSD

# ===== ANALYTICS WORKLOAD (OLAP) =====
# Large queries, complex joins
shared_buffers = 10GB             # 65% of 16GB server
effective_cache_size = 14GB       # 90% of RAM
work_mem = 64MB                   # Larger for sort/hash operations
maintenance_work_mem = 2GB
max_connections = 50              # Fewer, heavier connections
random_page_cost = 1.1            # Fast SSD

# ===== MIXED WORKLOAD =====
shared_buffers = 5GB              # 30% of 16GB
effective_cache_size = 13GB       # 80% of RAM
work_mem = 32MB
maintenance_work_mem = 1GB
max_connections = 150

# ===== COMMON TUNING PARAMETERS =====
max_parallel_workers_per_gather = 4    # Enable parallelization
max_parallel_workers = 8                # Total parallel workers
max_worker_processes = 8
max_parallel_maintenance_workers = 4

# WAL tuning
wal_buffers = 16MB
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9
```

### Memory Monitoring

```sql
-- Check actual memory usage
SELECT 
  name,
  setting,
  unit,
  (setting::bigint * CASE unit
    WHEN 'B' THEN 1
    WHEN 'kB' THEN 1024
    WHEN 'MB' THEN 1024*1024
    WHEN 'GB' THEN 1024*1024*1024
    ELSE 1
  END) / (1024*1024*1024.0) as size_gb
FROM pg_settings
WHERE name IN (
  'shared_buffers',
  'effective_cache_size',
  'work_mem',
  'maintenance_work_mem',
  'wal_buffers'
)
ORDER BY size_gb DESC;

-- Memory pressure indicators
SELECT 
  'Shared buffers hit ratio' as metric,
  ROUND(100.0 * blks_hit / (blks_hit + blks_read), 2)::text || '%' as value
FROM pg_stat_database
WHERE datname = current_database()
UNION ALL
SELECT 'Buffer usage', 
  pg_size_pretty(sum(relpages * 8192)) 
FROM pg_class;
```

---

## I/O Performance Tuning

### Identifying I/O Bottlenecks

```sql
-- Tables with most sequential scans (I/O heavy)
SELECT 
  schemaname,
  tablename,
  seq_scan,
  seq_tup_read,
  idx_scan,
  (seq_tup_read::float / NULLIF(seq_scan, 0))::numeric as avg_tuples_per_seqscan,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;

-- Index usage statistics
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch,
  (100.0 * idx_tup_fetch / NULLIF(idx_tup_read, 0))::numeric as fetch_ratio
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC
LIMIT 20;

-- Identify full table scans in queries
SELECT 
  schemaname,
  tablename,
  seq_scan - idx_scan as seq_only_scans,
  seq_tup_read,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_stat_user_tables
WHERE (seq_scan - idx_scan) > 100
  AND pg_total_relation_size(schemaname||'.'||tablename) > 10*1024*1024  -- >10MB
ORDER BY seq_tup_read DESC;
```

### Filesystems and Disk Layout

```bash
#!/bin/bash
# disk-io-analysis.sh - Monitor I/O performance

# Current I/O statistics
iostat -x 1 5

# Disk utilization
df -h /var/lib/postgresql

# Identify slow disks
echo "=== Disk Performance Check ==="
fio --name=seqread --rw=read --bs=4k --runtime=10 \
    --filename=/var/lib/postgresql/test_file --direct=1 \
    --numjobs=1 --ioengine=libaio --iodepth=32

# Monitor WAL I/O specifically
iostat -x -d -k /dev/sda 1 | grep -E "sda|avgrq-sz|await"

# Check page cache efficiency
echo "=== Page Cache Statistics ==="
cat /proc/pressure/io

# Monitor dirty pages
echo "=== Dirty Pages ===" 
vmstat 1 5
```

### I/O Configuration

```ini
# postgresql.conf - I/O tuning

# Checkpoint tuning (reduce sudden I/O spikes)
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9  # Spread checkpoints over 90% of interval
max_wal_size = 4GB                  # Checkpoint frequency
min_wal_size = 80MB

# BGwriter (background writer process)
bgwriter_delay = 200ms              # Check every 200ms
bgwriter_lru_maxpages = 100         # Write up to 100 pages
bgwriter_lru_multiplier = 2.0       # Anticipate future writes

# WAL writer
wal_writer_delay = 200ms
wal_writer_flush_after = 1MB

# Autovacuum I/O throttling
autovacuum_vacuum_cost_delay = 20ms  # Throttle vacuum I/O
autovacuum_vacuum_cost_limit = 200   # MB/s budget

# Random I/O optimization (SSD)
random_page_cost = 1.1  # Default 4.0 (for HDD)

# Sequential I/O prefetching
effective_io_concurrency = 200  # SSD: high, HDD: 2-4
```

---

## Query Parallelization

### Enabling Parallel Queries

```sql
-- Check parallel query settings
SELECT name, setting FROM pg_settings 
WHERE name LIKE '%parallel%'
ORDER BY name;

-- Enable parallelization
ALTER SYSTEM SET max_parallel_workers_per_gather = 4;
ALTER SYSTEM SET max_parallel_workers = 8;
ALTER SYSTEM SET max_worker_processes = 8;
ALTER SYSTEM SET max_parallel_maintenance_workers = 4;

-- Set cost threshold (queries > 1MB estimated work use parallel)
ALTER SYSTEM SET parallel_tuple_cost = 0.05;
ALTER SYSTEM SET parallel_setup_cost = 500;
ALTER SYSTEM SET min_parallel_table_scan_size = 8MB;
ALTER SYSTEM SET min_parallel_index_scan_size = 512kB;

SELECT pg_reload_conf();
```

### Force Parallelization

```sql
-- For specific query: force parallel plan
SET max_parallel_workers_per_gather = 4;
SET parallel_tuple_cost = 0.01;
SET parallel_setup_cost = 100;

-- Query automatically uses parallel workers
EXPLAIN ANALYZE
SELECT user_id, COUNT(*) as workout_count, AVG(duration) as avg_duration
FROM workouts
WHERE created_at > NOW() - INTERVAL '90 days'
GROUP BY user_id
HAVING COUNT(*) > 10
ORDER BY avg_duration DESC
LIMIT 1000;

-- Analyze shows: Gather → Parallel Seq Scan → GroupAggregate
```

### Parallel Index Scan

```sql
-- Create parallel-friendly indexes
CREATE INDEX CONCURRENTLY idx_workouts_user_created
  ON workouts(user_id, created_at DESC);

-- Query with parallel index scan
EXPLAIN (ANALYZE, BUFFERS)
SELECT w.id, w.user_id, w.duration, u.name
FROM workouts w
JOIN users u ON w.user_id = u.id
WHERE w.created_at > NOW() - INTERVAL '7 days'
  AND w.duration > 30
ORDER BY w.created_at DESC
LIMIT 10000;
```

---

## Table Partitioning

### Partition Strategy for Time-Series Data

```sql
-- CREATE PARTITIONED TABLE
CREATE TABLE metrics (
  id BIGSERIAL,
  user_id INT NOT NULL,
  metric_type VARCHAR(50) NOT NULL,
  value NUMERIC NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

-- Create monthly partitions
CREATE TABLE metrics_2024_01 PARTITION OF metrics
  FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE metrics_2024_02 PARTITION OF metrics
  FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE metrics_2024_03 PARTITION OF metrics
  FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

-- Create indexes on each partition
CREATE INDEX idx_metrics_2024_01_user_time
  ON metrics_2024_01 (user_id, timestamp DESC);

-- Auto-create partitions
CREATE TABLE metrics_default PARTITION OF metrics DEFAULT;

-- INSERT automatically routed to correct partition
INSERT INTO metrics (user_id, metric_type, value, timestamp)
VALUES (123, 'heart_rate', 72.5, NOW());

-- Query benefits from partition pruning
EXPLAIN (ANALYZE)
SELECT AVG(value) FROM metrics
WHERE timestamp > '2024-03-01' AND timestamp < '2024-03-15';
```

### List Partitioning (By Status/Type)

```sql
-- Partition by status
CREATE TABLE workouts (
  id BIGSERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT ck_status CHECK (status IN ('draft', 'active', 'completed', 'archived'))
) PARTITION BY LIST (status);

-- Create partition for each status
CREATE TABLE workouts_draft PARTITION OF workouts
  FOR VALUES IN ('draft');

CREATE TABLE workouts_active PARTITION OF workouts
  FOR VALUES IN ('active');

CREATE TABLE workouts_completed PARTITION OF workouts
  FOR VALUES IN ('completed');

CREATE TABLE workouts_archived PARTITION OF workouts
  FOR VALUES IN ('archived');

-- Query automatically uses partition pruning
EXPLAIN (ANALYZE)
SELECT COUNT(*) FROM workouts WHERE status = 'completed';
-- Only scans workouts_completed partition!
```

---

## Statistics and Query Planning

### ANALYZE and Statistics

```sql
-- Manual ANALYZE on table
ANALYZE gym_training.workouts;

-- ANALYZE with sample for large tables
ANALYZE (SAMPLE 10000) gym_training.workouts;

-- Check statistics accuracy
SELECT 
  schemaname,
  tablename,
  last_analyze,
  last_autoanalyze,
  analyze_count,
  autoanalyze_count
FROM pg_stat_user_tables
ORDER BY last_analyze DESC;

-- View column statistics
SELECT * FROM pg_stats
WHERE tablename = 'workouts'
ORDER BY attname;

-- Histogram statistics (for range queries)
SELECT 
  attname,
  n_distinct,
  most_common_vals,
  most_common_freqs,
  histogram_bounds
FROM pg_stats
WHERE tablename = 'workouts' AND attname = 'created_at';
```

### Autovacuum Tuning

```ini
# postgresql.conf - Autovacuum optimization

autovacuum = on
autovacuum_naptime = 10s              # Check every 10 seconds
autovacuum_vacuum_threshold = 50      # VACUUM after 50 dead rows
autovacuum_analyze_threshold = 50     # ANALYZE after 50 dead rows
autovacuum_vacuum_scale_factor = 0.02 # + 2% of table size
autovacuum_analyze_scale_factor = 0.01

# Per-table autovacuum settings
ALTER TABLE workouts SET (
  autovacuum_vacuum_scale_factor = 0.001,
  autovacuum_analyze_scale_factor = 0.0005
);
```

---

## Connection Pooling

### PgBouncer Configuration for Performance

```ini
# pgbouncer.ini - Production configuration

[databases]
gym_auth = host=localhost port=5432 dbname=gym_auth pool_size=20
gym_training = host=localhost port=5432 dbname=gym_training pool_size=30
gym_tracking = host=localhost port=5432 dbname=gym_tracking pool_size=25

[pgbouncer]
pool_mode = transaction     # Connection per transaction (balanced)
max_client_conn = 1000      # Max client connections
default_pool_size = 25      # Connections per database
min_pool_size = 5           # Keep minimum connections ready
reserve_pool_size = 5       # Extra connections for spikes

# Performance tuning
server_lifetime = 3600      # Close backend connections after 1 hour
server_idle_timeout = 600   # Close idle connections after 10 min
server_connect_timeout = 15
server_login_retry = 15
query_timeout = 0           # No query timeout in pgbouncer

# Memory and buffer
pkt_buf = 4096
listen_backlog = 2048

# Statistics
stats_period = 60           # Report stats every 60s
verbose = 1                 # Log level
```

### Connection Pool Monitoring

```sql
-- Check PgBouncer stats (from pgbouncer console)
-- Connect: psql -h 127.0.0.1 -p 6432 pgbouncer

SHOW STATS;        -- Overall statistics
SHOW POOLS;        -- Per-database/user statistics
SHOW CLIENTS;      -- Active client connections
SHOW SERVERS;      -- Backend connections to PostgreSQL
SHOW CONFIG;       -- Current configuration

-- Common issues:
-- Too many idle connections: Increase server_idle_timeout
-- Connection pool exhausted: Increase pool_size or reserve_pool_size
-- Slow connections: Check server_lifetime, tune timeout parameters
```

---

## Workload-Specific Tuning

### OLTP (Transactional) Workload

```sql
-- Small, frequent transactions
-- Characteristics: Fast response time, high throughput

CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  account_id INT NOT NULL,
  amount NUMERIC(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

-- Indexes for quick lookups
CREATE INDEX idx_transactions_account_created
  ON transactions(account_id, created_at DESC);

CREATE INDEX idx_transactions_status_created
  ON transactions(status, created_at)
  WHERE status IN ('pending', 'processing');

-- Tuning for OLTP:
-- - Smaller shared_buffers (reduce lock contention)
-- - High work_mem (multiple concurrent queries)
-- - Aggressive autovacuum (keep tables clean)
-- - Lower max_wal_size (frequent checkpoints, lower recovery time)
```

### OLAP (Analytics) Workload

```sql
-- Large, complex queries on historical data
-- Characteristics: Throughput over response time

CREATE TABLE metrics_archive (
  id BIGSERIAL,
  user_id INT NOT NULL,
  metric_name VARCHAR(100) NOT NULL,
  value NUMERIC NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

-- Tuning for OLAP:
-- - Large shared_buffers (cache entire datasets)
-- - High work_mem (sort/hash on large datasets)
-- - Parallel query execution enabled
-- - Partitioning for partition pruning
-- - BRIN indexes for time-series
-- - Column compression (if applicable)

-- Parallel aggregation query
SET max_parallel_workers_per_gather = 8;

EXPLAIN (ANALYZE)
SELECT 
  DATE_TRUNC('day', timestamp) as day,
  metric_name,
  COUNT(*) as metric_count,
  AVG(value) as avg_value,
  STDDEV(value) as stddev_value,
  MIN(value) as min_value,
  MAX(value) as max_value
FROM metrics_archive
WHERE timestamp > NOW() - INTERVAL '90 days'
GROUP BY DATE_TRUNC('day', timestamp), metric_name
ORDER BY day DESC, metric_name;
```

---

## Monitoring and Baselines

### Establishing Performance Baselines

```bash
#!/bin/bash
# baseline-performance.sh - Create performance baseline

BASELINE_DIR="/var/lib/postgresql/baselines"
BASELINE_DATE=$(date +%Y%m%d_%H%M%S)
BASELINE_FILE="${BASELINE_DIR}/baseline_${BASELINE_DATE}.txt"

mkdir -p "$BASELINE_DIR"

echo "=== PostgreSQL Performance Baseline ===" > "$BASELINE_FILE"
echo "Generated: $(date)" >> "$BASELINE_FILE"
echo "" >> "$BASELINE_FILE"

# System metrics
echo "=== System Metrics ===" >> "$BASELINE_FILE"
echo "CPU cores: $(nproc)" >> "$BASELINE_FILE"
echo "Total RAM: $(free -h | awk '/^Mem:/{print $2}')" >> "$BASELINE_FILE"
echo "Disk: $(df -h / | awk 'NR==2 {print $2}')" >> "$BASELINE_FILE"

# PostgreSQL configuration
echo "" >> "$BASELINE_FILE"
echo "=== PostgreSQL Configuration ===" >> "$BASELINE_FILE"
psql -U postgres -c "SELECT name, setting FROM pg_settings WHERE name IN ('shared_buffers', 'work_mem', 'maintenance_work_mem', 'random_page_cost');" >> "$BASELINE_FILE"

# Query performance
echo "" >> "$BASELINE_FILE"
echo "=== Query Performance ===" >> "$BASELINE_FILE"
psql -U postgres << SQL >> "$BASELINE_FILE"
SELECT 
  query,
  calls,
  mean_time,
  stddev_time,
  rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
SQL

# Index usage
echo "" >> "$BASELINE_FILE"
echo "=== Index Usage ===" >> "$BASELINE_FILE"
psql -U postgres << SQL >> "$BASELINE_FILE"
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  pg_size_pretty(pg_relation_size(indexrelid))
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;
SQL

echo "Baseline saved to: $BASELINE_FILE"
cat "$BASELINE_FILE"
```

### Real-Time Performance Monitoring

```sql
-- Real-time query monitoring
SELECT 
  pid,
  usename,
  application_name,
  state,
  query,
  query_start,
  EXTRACT(EPOCH FROM (NOW() - query_start))::int as runtime_seconds,
  rows_scanned,
  blks_hit,
  blks_read
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY runtime_seconds DESC;

-- Real-time lock monitoring
SELECT 
  pid,
  usename,
  locktype,
  database,
  relation::regclass,
  mode,
  granted
FROM pg_locks
WHERE NOT granted
ORDER BY pid;

-- Slow transaction detection
SELECT 
  pid,
  usename,
  xact_start,
  EXTRACT(EPOCH FROM (NOW() - xact_start))::int as txn_duration_sec,
  query_start,
  query
FROM pg_stat_activity
WHERE xact_start < NOW() - INTERVAL '5 minutes'
ORDER BY xact_start;
```

---

## Troubleshooting Performance

### Common Performance Issues

**Issue: High CPU Usage**

```sql
-- Find CPU-intensive queries
SELECT 
  pid,
  usename,
  query,
  query_start,
  pg_blocking_pids(pid) as blocked_by
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_start;

-- Check for full table scans on large tables
SELECT 
  schemaname,
  tablename,
  seq_scan,
  seq_tup_read,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_stat_user_tables
WHERE seq_scan > 1000
  AND pg_total_relation_size(schemaname||'.'||tablename) > 100*1024*1024
ORDER BY seq_tup_read DESC;

-- Check for missing indexes
SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;
```

**Issue: Memory Exhaustion**

```bash
# Check system memory
free -h
top -b -n 1 | head -20

# Check PostgreSQL process size
ps -eo pid,user,%mem,vsz,rss,cmd | grep postgres | sort -k4 -rn

# Check shared buffers pressure
psql -U postgres -c "
SELECT 
  'Shared buffers hit ratio',
  ROUND(100.0 * heap_blks_hit / (heap_blks_hit + heap_blks_read), 2)::text || '%'
FROM pg_stat_database
WHERE datname = current_database();
"
```

**Issue: Slow Joins**

```sql
-- Analyze join performance
EXPLAIN (ANALYZE, BUFFERS)
SELECT a.id, a.name, b.count
FROM table_a a
JOIN table_b b ON a.id = b.a_id
WHERE a.created_at > NOW() - INTERVAL '30 days';

-- Check for missing indexes on join columns
CREATE INDEX idx_table_b_a_id ON table_b(a_id);

-- Consider rewriting join
-- FROM table_a a CROSS JOIN LATERAL (...) subquery
```

---

## Related Documentation

- [02-database-architecture.md](02-database-architecture.md) - Low-level architecture
- [03-backup-recovery.md](03-backup-recovery.md) - Backup strategies
- [01-getting-started.md](01-getting-started.md) - DBA fundamentals

