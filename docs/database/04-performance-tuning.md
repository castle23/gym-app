# Performance Tuning

> **Note**: This document describes PostgreSQL performance tuning best practices. Table and index names in examples may not match the current schema — refer to [Schema Design](02-schema-design.md) for actual table names.

## Overview

Comprehensive guide to optimizing PostgreSQL performance for the Gym Platform. This guide covers query optimization, index tuning, configuration parameter optimization, monitoring and diagnostic techniques, and workload-specific performance improvements. The Gym Platform handles multiple concurrent workloads across authentication, training planning, and workout tracking, requiring careful performance analysis and tuning.

## Table of Contents

- [Performance Tuning Strategy](#performance-tuning-strategy)
- [Query Optimization](#query-optimization)
- [Index Optimization](#index-optimization)
- [Configuration Tuning](#configuration-tuning)
- [Connection Pool Tuning](#connection-pool-tuning)
- [Monitoring & Profiling](#monitoring--profiling)
- [Slow Query Analysis](#slow-query-analysis)
- [Workload-Specific Tuning](#workload-specific-tuning)
- [Maintenance Procedures](#maintenance-procedures)
- [Capacity Planning](#capacity-planning)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Performance Tuning Strategy

### Performance Tuning Methodology

```
┌─────────────────────────────────────────────┐
│    Performance Tuning Methodology            │
└─────────────────────────────────────────────┘

1. BASELINE ESTABLISHMENT
   ├─ Measure current performance
   ├─ Identify bottlenecks
   ├─ Document metrics
   └─ Set targets

2. PROFILING & ANALYSIS
   ├─ Identify slow queries
   ├─ Analyze query plans
   ├─ Check index usage
   ├─ Monitor resource utilization
   └─ Identify contention points

3. OPTIMIZATION
   ├─ Rewrite queries
   ├─ Add/modify indexes
   ├─ Tune parameters
   ├─ Adjust workload distribution
   └─ Archive old data

4. TESTING
   ├─ Measure improvement
   ├─ Test in staging
   ├─ Verify no regression
   ├─ Load test
   └─ Benchmark

5. DEPLOYMENT
   ├─ Apply changes to production
   ├─ Monitor during deployment
   ├─ Verify performance improvement
   ├─ Update runbooks
   └─ Document changes
```

### Key Performance Metrics

**System Metrics:**

```sql
-- CPU Usage (query performance impact)
SELECT 
    datname,
    usename,
    state,
    query_start,
    backend_start,
    state_change
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start;

-- Memory Usage
SELECT
    datname,
    sum(heap_blks_read) as heap_blks_read,
    sum(heap_blks_hit) as heap_blks_hit,
    round(100.0 * sum(heap_blks_hit) / 
      (sum(heap_blks_hit) + sum(heap_blks_read)), 2) as cache_hit_ratio
FROM pg_statio_user_tables
GROUP BY datname;

-- I/O Performance
SELECT
    schemaname,
    tablename,
    seq_scan,
    idx_scan,
    seq_tup_read,
    seq_tup_read / NULLIF(seq_scan, 0) as avg_seq_read
FROM pg_stat_user_tables
ORDER BY seq_scan DESC
LIMIT 20;

-- Lock Contention
SELECT
    pid,
    usename,
    application_name,
    query,
    state,
    wait_event_type,
    wait_event
FROM pg_stat_activity
WHERE wait_event_type IS NOT NULL
ORDER BY query_start;
```

**Query Metrics:**

```
Response Time: < 100ms for OLTP queries
Throughput: > 5000 queries/second for reads
Cache Hit Ratio: > 99% for hot data
Index Efficiency: > 95% queries use indexes
Connection Efficiency: < 100ms connection time
```

---

## Query Optimization

### EXPLAIN ANALYZE

**Basic EXPLAIN usage:**

```sql
-- Show query plan without executing
EXPLAIN SELECT * FROM auth.users WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- Show query plan with actual execution statistics
EXPLAIN ANALYZE SELECT * FROM auth.users WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- Show plan in JSON format for programmatic analysis
EXPLAIN (ANALYZE, FORMAT JSON) 
SELECT * FROM auth.users WHERE id = '550e8400-e29b-41d4-a716-446655440000';
```

**Interpreting EXPLAIN output:**

```
Seq Scan on auth.users (cost=0.00..35.00 rows=1 width=248)
├─ cost: 0.00..35.00     ← Startup cost (0.00) to total cost (35.00)
├─ rows: 1               ← Estimated rows returned
├─ width: 248            ← Average row width in bytes
└─ Filter: (id = '...')  ← Filter condition

Key Indicators:
- Seq Scan: Full table scan (inefficient for large tables)
- Index Scan: Using index (efficient)
- cost=0.00..35.00: Total cost units (lower is better)
- rows: Estimated rows (actual vs estimated difference = query plan problem)
- buffers: Shared buffers used (with ANALYZE BUFFERS option)
```

**EXPLAIN with BUFFERS:**

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING)
SELECT u.id, u.email, COUNT(w.id) as workout_count
FROM auth.users u
LEFT JOIN tracking.workouts w ON u.id = w.user_id
WHERE u.created_at > CURRENT_DATE - INTERVAL '30 days'
GROUP BY u.id, u.email
ORDER BY workout_count DESC;

-- Output shows:
-- Planning Time: 0.234 ms
-- Execution Time: 45.123 ms
-- Buffers: shared hit=1234 read=56 dirtied=0 written=0
```

### Query Rewriting Patterns

**Pattern 1: Avoid subqueries in SELECT**

```sql
-- BAD: Subquery in SELECT clause (runs for each row)
SELECT 
    id,
    email,
    (SELECT COUNT(*) FROM tracking.workouts WHERE user_id = u.id) as workout_count
FROM auth.users u;

-- GOOD: Use LEFT JOIN with GROUP BY
SELECT 
    u.id,
    u.email,
    COUNT(w.id) as workout_count
FROM auth.users u
LEFT JOIN tracking.workouts w ON u.id = w.user_id
GROUP BY u.id, u.email;
```

**Pattern 2: Use window functions instead of GROUP BY + subquery**

```sql
-- BAD: Multiple queries or complex subqueries
SELECT user_id, total_workouts
FROM (
    SELECT 
        user_id,
        COUNT(*) as total_workouts,
        ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC) as rank
    FROM tracking.workouts
    GROUP BY user_id
) ranked
WHERE rank <= 10;

-- GOOD: Single query with window function
SELECT 
    user_id,
    COUNT(*) as total_workouts,
    ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC) as rank
FROM tracking.workouts
GROUP BY user_id
LIMIT 10;
```

**Pattern 3: Optimize IN clauses**

```sql
-- BAD: Too many IN values (> 100)
SELECT * FROM auth.users 
WHERE id IN (uuid1, uuid2, uuid3, ... uuid1000);

-- GOOD: Use JOIN instead
SELECT u.* FROM auth.users u
INNER JOIN (VALUES (uuid1), (uuid2), ... (uuid1000)) as ids(id)
ON u.id = ids.id;

-- Or use a temporary table
CREATE TEMP TABLE user_ids AS
SELECT id FROM auth.users WHERE email LIKE 'test%';

SELECT * FROM tracking.workouts 
WHERE user_id IN (SELECT id FROM user_ids);
```

**Pattern 4: Filter early**

```sql
-- BAD: Filter after expensive JOIN
SELECT u.*, w.*
FROM auth.users u
JOIN tracking.workouts w ON u.id = w.user_id
WHERE w.created_at > CURRENT_DATE - INTERVAL '30 days';

-- GOOD: Filter tables before JOIN
SELECT u.*, w.*
FROM auth.users u
JOIN (
    SELECT * FROM tracking.workouts 
    WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
) w ON u.id = w.user_id;
```

**Pattern 5: Avoid DISTINCT when possible**

```sql
-- BAD: DISTINCT on large result set
SELECT DISTINCT user_id FROM tracking.workouts;

-- GOOD: Use GROUP BY or SELECT DISTINCT for small result
SELECT DISTINCT ON (user_id) user_id FROM tracking.workouts
ORDER BY user_id, created_at DESC;
```

---

## Index Optimization

### Index Analysis

**Find unused indexes:**

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- Action: Drop unused indexes to reduce I/O and storage
-- Before dropping, verify the index is truly unused:
-- 1. Monitor for 2-3 weeks during normal workload
-- 2. Check application code for explicit INDEX HINT
-- 3. Verify in staging environment first
```

**Find missing indexes:**

```sql
-- Queries with high seq_scan / low idx_scan ratio
SELECT
    schemaname,
    tablename,
    seq_scan,
    idx_scan,
    seq_tup_read,
    idx_tup_read,
    seq_tup_read / NULLIF(seq_scan, 0) as avg_seq_tup,
    CASE 
        WHEN seq_scan > idx_scan * 10 THEN 'MISSING_INDEX'
        ELSE 'OK'
    END as recommendation
FROM pg_stat_user_tables
WHERE seq_scan > 1000
ORDER BY seq_scan - idx_scan DESC;

-- Example output:
-- Table: tracking.workouts
-- seq_scan: 15000 | idx_scan: 150 | Recommendation: MISSING_INDEX
-- Action: Add index on frequently filtered columns
```

**Duplicate indexes:**

```sql
-- Find duplicate or redundant indexes
WITH index_data AS (
    SELECT
        schemaname,
        tablename,
        indexname,
        array_agg(attname ORDER BY attnum) as columns,
        pg_size_pretty(pg_relation_size(indexrelid)) as index_size
    FROM pg_stat_user_indexes i
    JOIN pg_index idx ON i.indexrelid = idx.indexrelid
    JOIN pg_attribute a ON a.attrelid = idx.indrelid
        AND a.attnum = ANY(idx.indkey)
    GROUP BY schemaname, tablename, indexname, indexrelid
)
SELECT
    tablename,
    columns,
    string_agg(indexname, ', ') as duplicate_indexes,
    string_agg(index_size, ', ') as sizes,
    COUNT(*) as duplicate_count
FROM index_data
GROUP BY tablename, columns
HAVING COUNT(*) > 1
ORDER BY tablename;
```

### Index Creation Best Practices

**Analyze before creating:**

```sql
-- Check table statistics
ANALYZE auth.users;
ANALYZE tracking.workouts;

-- View estimated index benefit
EXPLAIN SELECT * FROM auth.users WHERE email = 'user@example.com';
-- If Seq Scan is shown, index could help
```

**Recommended indexes for Gym Platform:**

```sql
-- Auth Schema
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_users_created_at ON auth.users(created_at);
CREATE INDEX idx_users_updated_at ON auth.users(updated_at);
CREATE INDEX idx_user_roles_user_id ON auth.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON auth.user_roles(role_id);
CREATE INDEX idx_tokens_user_id ON auth.tokens(user_id);
CREATE INDEX idx_tokens_token_hash ON auth.tokens(token_hash);
CREATE INDEX idx_tokens_expires_at ON auth.tokens(expires_at);

-- Training Schema
CREATE INDEX idx_trainers_user_id ON training.trainers(user_id);
CREATE INDEX idx_trainers_created_at ON training.trainers(created_at);
CREATE INDEX idx_training_plans_trainer_id ON training.training_plans(trainer_id);
CREATE INDEX idx_training_plans_user_id ON training.training_plans(user_id);
CREATE INDEX idx_training_plans_status ON training.training_plans(status);
CREATE INDEX idx_training_plans_created_at ON training.training_plans(created_at);
CREATE INDEX idx_exercises_created_by ON training.exercises(created_by);
CREATE INDEX idx_plan_exercises_plan_id ON training.plan_exercises(plan_id);
CREATE INDEX idx_plan_exercises_exercise_id ON training.plan_exercises(exercise_id);

-- Tracking Schema
CREATE INDEX idx_workouts_user_id ON tracking.workouts(user_id);
CREATE INDEX idx_workouts_training_plan_id ON tracking.workouts(training_plan_id);
CREATE INDEX idx_workouts_created_at ON tracking.workouts(created_at);
CREATE INDEX idx_workouts_completed_at ON tracking.workouts(completed_at);
CREATE INDEX idx_workout_exercises_workout_id ON tracking.workout_exercises(workout_id);
CREATE INDEX idx_workout_exercises_exercise_id ON tracking.workout_exercises(exercise_id);
CREATE INDEX idx_metrics_user_id ON tracking.metrics(user_id);
CREATE INDEX idx_metrics_recorded_at ON tracking.metrics(recorded_at);
CREATE INDEX idx_personal_records_user_id ON tracking.personal_records(user_id);
CREATE INDEX idx_personal_records_exercise_id ON tracking.personal_records(exercise_id);

-- Composite indexes for common queries
CREATE INDEX idx_workouts_user_created ON tracking.workouts(user_id, created_at DESC);
CREATE INDEX idx_training_plans_user_status ON training.training_plans(user_id, status);
```

**Partial indexes for performance:**

```sql
-- Index only active records
CREATE INDEX idx_users_active ON auth.users(id) WHERE deleted_at IS NULL;

-- Index only recent workouts
CREATE INDEX idx_recent_workouts ON tracking.workouts(user_id, created_at DESC)
WHERE created_at > CURRENT_DATE - INTERVAL '90 days';

-- Index for specific user types
CREATE INDEX idx_premium_trainers ON training.trainers(user_id)
WHERE subscription_tier = 'premium';
```

### Index Maintenance

**Monitor index bloat:**

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    idx_blks_read,
    idx_blks_hit,
    ROUND(100.0 * idx_blks_hit / (idx_blks_hit + idx_blks_read), 2) as cache_ratio,
    pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
ORDER BY idx_blks_read DESC;

-- Rebuild indexes if cache ratio is low or index is bloated
REINDEX INDEX CONCURRENTLY idx_users_email;
```

**Analyze index structure:**

```bash
# Check index fragmentation
psql -U postgres gym_db -c "
SELECT
    schemaname,
    indexname,
    idx_blks_read,
    idx_blks_hit,
    relpages
FROM pg_stat_user_indexes
JOIN pg_class ON pg_class.oid = indexrelid
ORDER BY relpages DESC
LIMIT 20;"
```

---

## Configuration Tuning

### Critical Parameters

**PostgreSQL Configuration: /etc/postgresql/14/main/postgresql.conf**

```ini
# MEMORY SETTINGS (most important for performance)

# Shared buffers: 25% of available RAM (for server dedicated to PostgreSQL)
shared_buffers = 16GB

# Effective cache size: 50-75% of available RAM (hints to planner)
effective_cache_size = 32GB

# Work memory: Per-operation working memory (sorts, hash aggregation)
# Formula: (RAM - shared_buffers) / (max_connections * 4)
work_mem = 512MB

# Maintenance work memory: Used by VACUUM, ANALYZE, CREATE INDEX
maintenance_work_mem = 2GB

# WAL buffers: 16MB typically sufficient
wal_buffers = 16MB

# Temporary buffers: Per-session temp table buffer
temp_buffers = 64MB

# CONNECTION SETTINGS

# Max connections
max_connections = 200

# Superuser reserved connections
superuser_reserved_connections = 10

# CONNECTION POOLING (handled by PgBouncer in production)
# These settings work with connection pooling
idle_in_transaction_session_timeout = 60000  # 60 seconds
statement_timeout = 300000                   # 5 minutes

# QUERY PLANNING

# Random page cost: Lower for SSD, higher for HDD
# AWS RDS SSD: 1.1, On-premises HDD: 4.0
random_page_cost = 1.1

# Effective IO concurrency: Number of concurrent I/O operations
# SSD/Cloud: 200, HDD: 2-4
effective_io_concurrency = 200

# JIT (Just-In-Time compilation) - for complex queries
jit = on
jit_above_cost = 100000
jit_inline_above_cost = 500000
jit_optimize_above_cost = 500000

# CHECKPOINT & WAL

# Checkpoint timeout and target time
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9

# WAL settings
max_wal_size = 4GB
min_wal_size = 1GB

# PARALLELIZATION (PostgreSQL 9.6+)

# Enable parallel query execution
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
max_parallel_maintenance_workers = 4

# Cost threshold for parallel queries
parallel_tuple_cost = 0.05
parallel_setup_cost = 500.0

# AUTOVACUUM

# Autovacuum settings (keep aggressive for 24/7 operation)
autovacuum = on
autovacuum_max_workers = 3
autovacuum_naptime = 30s
autovacuum_vacuum_threshold = 50
autovacuum_analyze_threshold = 50
autovacuum_vacuum_scale_factor = 0.1
autovacuum_analyze_scale_factor = 0.05

# LOGGING (for performance analysis)

# Log slow queries
log_min_duration_statement = 1000  # 1 second
log_statement = 'all'              # Log all statements
log_duration = off                 # Don't log query duration

# Log lock waits
log_lock_waits = on
deadlock_timeout = 1s
```

**Dynamic parameter changes (without restart):**

```sql
-- Current settings
SHOW shared_buffers;
SHOW effective_cache_size;
SHOW work_mem;

-- Change settings (connection-level)
SET work_mem = '256MB';

-- Change settings (server-wide, requires superuser)
ALTER SYSTEM SET log_min_duration_statement = 500;

-- Reload configuration
SELECT pg_reload_conf();

-- Verify changes
SHOW log_min_duration_statement;
```

---

## Connection Pool Tuning

### PgBouncer Configuration

**File: /etc/pgbouncer/pgbouncer.ini**

```ini
[databases]
# Define database connections
gym_db = host=10.0.1.50 port=5432 dbname=gym_db

[pgbouncer]
# Listen address and port
listen_addr = 127.0.0.1
listen_port = 6432

# Pool mode: session, transaction, or statement
pool_mode = transaction

# Pool size settings
min_pool_size = 10              # Minimum connections per database
default_pool_size = 25          # Default connections per database
reserve_pool_size = 5           # Extra connections for temporary spikes
reserve_pool_timeout = 3        # Timeout for reserve pool connections

# Total connections limit
max_client_conn = 1000          # Max client connections
max_db_conn = 50                # Max server connections per database
max_user_conn = 100             # Max connections per user

# Timeouts and cleanup
server_lifetime = 3600          # Close server connection after 1 hour
server_idle_timeout = 600       # Close idle connections after 10 mins
client_idle_timeout = 300       # Close idle client connections

# Maintenance
server_connect_timeout = 15
query_wait_timeout = 120
query_timeout = 0               # Disable query timeout (handled by app)

# Verbose logging
logfile = /var/log/pgbouncer/pgbouncer.log
pidfile = /var/run/pgbouncer/pgbouncer.pid
```

**Monitor PgBouncer:**

```bash
# Check pool status
psql -h 127.0.0.1 -p 6432 -U pgbouncer pgbouncer << EOF
SHOW POOLS;
SHOW CLIENTS;
SHOW SERVERS;
SHOW STATS;
EOF

# Example SHOW POOLS output:
# database | user | cl_active | cl_waiting | sv_active | sv_idle | sv_used | sv_tested | sv_login | maxwait | pool_mode | in_use | paused
# gym_db   | app  | 10        | 0          | 10        | 15      | 0       | 0         | 0        | 0      | transaction | 0 | 0
```

---

## Monitoring & Profiling

### Performance Monitoring Queries

**Active queries:**

```sql
-- Find currently running queries
SELECT
    pid,
    usename,
    application_name,
    state,
    query_start,
    state_change,
    backend_start,
    EXTRACT(EPOCH FROM (NOW() - query_start)) as duration_seconds,
    query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start DESC;
```

**Query statistics:**

```sql
-- Most time-consuming queries
SELECT
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 20;

-- Most frequently called queries
SELECT
    query,
    calls,
    total_time,
    mean_time
FROM pg_stat_statements
ORDER BY calls DESC
LIMIT 20;
```

**Table statistics:**

```sql
-- Table access patterns
SELECT
    schemaname,
    tablename,
    seq_scan,
    idx_scan,
    seq_tup_read,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    CASE
        WHEN seq_scan > 0 THEN (seq_tup_read / seq_scan)::NUMERIC(10, 2)
        ELSE 0
    END as avg_rows_per_seq_scan
FROM pg_stat_user_tables
ORDER BY seq_scan - idx_scan DESC;
```

### Prometheus Integration

**PostgreSQL exporter metrics:**

```yaml
# Prometheus scrape config
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'postgresql'
    static_configs:
      - targets: ['localhost:9187']
    metrics_path: '/metrics'
```

**Key metrics to monitor:**

```promql
# Query latency
pg_stat_statements_mean_exec_time_seconds

# Cache hit ratio
pg_stat_database_heap_blks_hit / (pg_stat_database_heap_blks_hit + pg_stat_database_heap_blks_read)

# Active connections
count(pg_stat_activity_state{state="active"})

# Sequential scans (indicator of missing indexes)
rate(pg_stat_user_tables_seq_scan[5m])

# Tuple throughput
rate(pg_stat_database_tup_inserted[5m])
rate(pg_stat_database_tup_updated[5m])
rate(pg_stat_database_tup_deleted[5m])
```

---

## Slow Query Analysis

### Enable Query Logging

**Configuration:**

```ini
# Log slow queries (> 1 second)
log_min_duration_statement = 1000

# Log all statements (careful in production!)
log_statement = 'all'

# Log statement details
log_duration = off
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '

# Query logging directory
log_directory = '/var/log/postgresql'
logging_collector = on
log_filename = 'postgresql-%Y-%m-%d.log'
log_rotation_age = 1d
log_rotation_size = 100MB
```

### Analyze Slow Queries

**Find slow queries from log:**

```bash
# Extract queries > 1 second execution time
grep "duration: [1-9][0-9]{3,}" /var/log/postgresql/postgresql.log

# Sort by execution time
grep "duration:" /var/log/postgresql/postgresql.log | \
  sed 's/.*duration: \([0-9.]*\).*/\1/' | \
  sort -rn | \
  head -20

# Find most frequent slow queries
grep "duration:" /var/log/postgresql/postgresql.log | \
  awk '{print $NF}' | \
  sort | uniq -c | sort -rn | head -20
```

**Pgbadger analysis:**

```bash
# Analyze PostgreSQL logs with pgbadger
pgbadger /var/log/postgresql/postgresql.log -o report.html

# This generates HTML report with:
# - Query distribution
# - Top slow queries
# - Connection patterns
# - Detailed statistics
```

---

## Workload-Specific Tuning

### Authentication Service

**Query pattern:** Point lookups by email/ID

```sql
-- Index strategy
CREATE INDEX idx_users_email_active ON auth.users(email) 
WHERE deleted_at IS NULL;

CREATE INDEX idx_users_id_active ON auth.users(id) 
WHERE deleted_at IS NULL;

-- Tune for fast single-row lookups
SET random_page_cost = 1.1;    # Assume SSD
SET effective_cache_size = '32GB';
```

### Training Service

**Query pattern:** Complex planning queries with aggregations

```sql
-- Composite indexes for common joins
CREATE INDEX idx_training_plans_trainer_user ON training.training_plans(trainer_id, user_id, status);

-- Partial indexes for current plans
CREATE INDEX idx_active_plans ON training.training_plans(user_id)
WHERE status IN ('active', 'pending')
AND deleted_at IS NULL;

-- Tune for complex queries
SET work_mem = '512MB';
SET effective_cache_size = '32GB';
```

### Tracking Service

**Query pattern:** Time-series data with aggregations

```sql
-- Time-based indexes for recent workouts
CREATE INDEX idx_recent_workouts_user ON tracking.workouts(user_id, created_at DESC)
WHERE created_at > CURRENT_DATE - INTERVAL '90 days';

-- Metrics aggregation index
CREATE INDEX idx_metrics_user_recorded ON tracking.metrics(user_id, recorded_at DESC)
WHERE deleted_at IS NULL;

-- Tune for time-series workloads
SET random_page_cost = 1.1;
SET effective_cache_size = '32GB';
```

---

## Maintenance Procedures

### ANALYZE

**Manual ANALYZE:**

```sql
-- Analyze single table
ANALYZE auth.users;

-- Analyze specific columns
ANALYZE auth.users(email, created_at);

-- Analyze all tables in schema
ANALYZE auth;

-- Analyze entire database
ANALYZE;
```

**Monitor ANALYZE:**

```sql
-- Last ANALYZE time
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
ORDER BY last_analyze DESC;
```

### VACUUM

**Manual VACUUM:**

```sql
-- Vacuum single table
VACUUM ANALYZE auth.users;

-- Aggressive vacuum (locks table, full reclamation)
VACUUM FULL ANALYZE auth.users;

-- Vacuum specific percentage of table
VACUUM (ANALYZE, VERBOSE) auth.users;
```

**Monitor VACUUM:**

```bash
# Watch VACUUM progress
watch -n 1 'psql -c "SELECT * FROM pg_stat_progress_vacuum;"'

# Check vacuum locks
psql -c "SELECT * FROM pg_locks WHERE NOT granted;"
```

---

## Capacity Planning

### Growth Projections

**Monitor table growth:**

```sql
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - 
                   pg_relation_size(schemaname||'.'||tablename)) as indexes_size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Disk space planning:**

```
Current database size: 50GB
Monthly growth rate: 2GB
Projected size in 1 year: 50GB + (2GB * 12) = 74GB
Projected size in 3 years: 50GB + (2GB * 36) = 122GB

Recommendation:
- Current allocation: 500GB (allows 10x growth)
- Add storage if usage exceeds 60% (300GB)
```

---

## Best Practices

### Query Best Practices

1. **Use parameterized queries:**
   ```sql
   -- BAD: String concatenation (SQL injection risk, cache miss)
   SELECT * FROM auth.users WHERE email = '$email';
   
   -- GOOD: Parameterized (safe, cache reuse)
   SELECT * FROM auth.users WHERE email = $1;
   ```

2. **Batch operations when possible:**
   ```sql
   -- BAD: Multiple single inserts
   INSERT INTO tracking.workouts (user_id, training_plan_id, ...) VALUES (...);
   INSERT INTO tracking.workouts (user_id, training_plan_id, ...) VALUES (...);
   
   -- GOOD: Single batch insert
   INSERT INTO tracking.workouts (user_id, training_plan_id, ...) VALUES
     (...),
     (...),
     (...);
   ```

3. **Limit result sets:**
   ```sql
   -- BAD: Fetch all rows
   SELECT * FROM tracking.metrics WHERE user_id = $1;
   
   -- GOOD: Paginate results
   SELECT * FROM tracking.metrics WHERE user_id = $1
   ORDER BY recorded_at DESC
   LIMIT 100 OFFSET 0;
   ```

### Operational Best Practices

1. **Regular VACUUM/ANALYZE schedule**
2. **Monitor query plans and slow queries**
3. **Test index changes in staging**
4. **Document index purpose and creation date**
5. **Review index usage quarterly**
6. **Archive old data** (move workouts > 1 year to archive)

---

## Troubleshooting

### High CPU Usage

```bash
# Check running queries
psql -c "SELECT * FROM pg_stat_activity WHERE state != 'idle';"

# Check for missing indexes
psql -c "SELECT * FROM pg_stat_user_tables WHERE seq_scan > 1000 ORDER BY seq_scan DESC;"

# Analyze query plan
psql -c "EXPLAIN ANALYZE SELECT ..."

# Possible solutions:
# 1. Kill long-running query (if safe)
# 2. Add missing index
# 3. Rewrite query
# 4. Increase work_mem temporarily
```

### High I/O Usage

```bash
# Check sequential scans
psql -c "SELECT * FROM pg_stat_user_tables ORDER BY seq_scan - idx_scan DESC LIMIT 20;"

# Check full table scans
psql -c "SELECT * FROM pg_stat_user_tables WHERE seq_scan > idx_scan * 10;"

# Solutions:
# 1. Add indexes for frequently scanned tables
# 2. Partition large tables
# 3. Archive old data
```

### Connection Pool Exhaustion

```bash
# Check connection count
psql -c "SELECT count(*) FROM pg_stat_activity;"

# Check per-database connections
psql -c "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"

# Solutions:
# 1. Increase max_connections
# 2. Tune PgBouncer pool size
# 3. Kill idle connections
# 4. Add connection retry logic in application
```

---

**Related Documentation:**
- [01-database-overview.md](01-database-overview.md) - Database architecture
- [02-schema-design.md](02-schema-design.md) - Schema structure
- [03-backup-recovery.md](03-backup-recovery.md) - Backup procedures
- See [docs/operations/](../operations/) for operational procedures
