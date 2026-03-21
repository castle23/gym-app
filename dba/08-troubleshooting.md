# Advanced Troubleshooting

## Overview

Advanced troubleshooting guide for production PostgreSQL systems, covering systematic diagnosis of complex issues, performance problems, replication failures, data corruption, connection issues, and critical incident response for the Gym Platform. This guide emphasizes methodical problem identification and root cause analysis for experienced DBAs.

## Table of Contents

- [Troubleshooting Methodology](#troubleshooting-methodology)
- [Connectivity Issues](#connectivity-issues)
- [Performance Problems](#performance-problems)
- [Replication Failures](#replication-failures)
- [Lock and Deadlock Issues](#lock-and-deadlock-issues)
- [Data Corruption](#data-corruption)
- [Crash and Recovery](#crash-and-recovery)
- [High Availability Failover](#high-availability-failover)
- [Emergency Procedures](#emergency-procedures)
- [Post-Incident Analysis](#post-incident-analysis)

---

## Troubleshooting Methodology

### Systematic Problem Solving

```
STEP 1: GATHER INFORMATION
├─ Check system health (CPU, Memory, Disk, I/O)
├─ Review PostgreSQL logs
├─ Check current activity (pg_stat_activity)
├─ Monitor metrics (Prometheus/Grafana)
└─ Review recent changes

STEP 2: REPRODUCE ISSUE
├─ Can you reproduce consistently?
├─ What's the frequency (always/intermittent)?
├─ Does it affect specific queries or all queries?
└─ When did it start?

STEP 3: ISOLATE ROOT CAUSE
├─ Narrow down affected component
├─ Identify related processes/queries
├─ Check for resource constraints
├─ Review error messages carefully

STEP 4: DEVELOP HYPOTHESIS
├─ What component is failing?
├─ What changed recently?
├─ Is it configuration, data, or system?
└─ Have you seen this before?

STEP 5: TEST SOLUTION
├─ Test in development/staging first
├─ Have rollback plan ready
├─ Document changes
└─ Monitor after fix

STEP 6: VERIFY FIX
├─ Confirm issue is resolved
├─ Check for side effects
├─ Monitor for recurrence
└─ Update documentation
```

### Diagnostic Queries Reference

```sql
-- ALWAYS START HERE
SELECT version();
SELECT now() - pg_postmaster_start_time() as uptime;
SELECT name, setting FROM pg_settings WHERE name = 'max_connections';

-- Check for obvious problems
SELECT COUNT(*) FROM pg_stat_activity;
SELECT COUNT(*) FROM pg_stat_activity WHERE state != 'idle';
SELECT schemaname, tablename, n_dead_tup FROM pg_stat_user_tables WHERE n_dead_tup > 100000;
SELECT * FROM pg_stat_replication;

-- Look at logs (tail -f /var/log/postgresql/postgresql.log)
-- Check disk space (df -h)
-- Monitor system load (top, vmstat, iostat)
```

---

## Connectivity Issues

### Connection Refused

```bash
# Symptom: "connection refused" or "port unreachable"

# Step 1: Verify PostgreSQL is running
ps aux | grep "postgres -D"

# If not running, check why
sudo systemctl status postgresql
sudo journalctl -xe

# Step 2: Check port is listening
netstat -tlnp | grep 5432
ss -tlnp | grep postgres

# Step 3: Check pg_hba.conf for access rules
cat /etc/postgresql/14/main/pg_hba.conf

# Should have line like:
# local   all             postgres                                trust
# host    all             all             127.0.0.1/32            md5

# Step 4: Verify bind address in postgresql.conf
grep "^listen_addresses" /etc/postgresql/14/main/postgresql.conf

# Should be: listen_addresses = 'localhost' or '*' for remote

# Step 5: Test connection
psql -h 127.0.0.1 -p 5432 -U postgres

# Step 6: Check firewall
sudo firewall-cmd --list-ports
sudo ufw status
```

### Authentication Failures

```bash
# Symptom: "role does not exist", "FATAL: Ident authentication failed"

# Step 1: Check user exists
sudo -u postgres psql -c "SELECT * FROM pg_roles WHERE rolname = 'app_user';"

# If not, create it
sudo -u postgres createuser app_user

# Step 2: Check pg_hba.conf authentication method
grep "app_user" /etc/postgresql/14/main/pg_hba.conf

# Common auth methods:
# trust - no password (local only)
# md5 - password required
# ident - OS user mapping
# peer - peer authentication

# Step 3: Check password is set
sudo -u postgres psql -c "ALTER USER app_user WITH PASSWORD 'new_password';"

# Step 4: Reload PostgreSQL to apply pg_hba.conf changes
sudo systemctl reload postgresql

# Step 5: Test authentication
PGPASSWORD='password' psql -h 127.0.0.1 -U app_user -d gym_training
```

### Connection Pool Issues

```sql
-- Symptom: "too many connections", connection timeouts

-- Step 1: Check current connection count
SELECT 
  datname,
  COUNT(*) as connections,
  pg_settings.setting as max_connections
FROM pg_stat_activity, pg_settings
WHERE pg_settings.name = 'max_connections'
GROUP BY datname, pg_settings.setting;

-- Step 2: Check connection limits per user/database
SELECT 
  rolname,
  rolconnlimit
FROM pg_roles
WHERE rolconnlimit > 0
ORDER BY rolconnlimit DESC;

-- Step 3: Increase max_connections if needed
-- Edit postgresql.conf:
# max_connections = 300

# Restart PostgreSQL:
sudo systemctl restart postgresql

-- Step 4: Kill idle connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'idle' 
  AND query_start < NOW() - INTERVAL '30 minutes';

-- Step 5: Check for connection leaks
SELECT 
  application_name,
  COUNT(*) as count
FROM pg_stat_activity
WHERE state = 'idle'
GROUP BY application_name
ORDER BY count DESC;
```

---

## Performance Problems

### Slow Query Analysis

```sql
-- Symptom: Query taking longer than expected

-- Step 1: Enable query logging
ALTER SYSTEM SET log_min_duration_statement = 500;
SELECT pg_reload_conf();

-- Step 2: Identify slow queries
SELECT 
  query,
  calls,
  mean_time,
  max_time,
  stddev_time
FROM pg_stat_statements
WHERE mean_time > 500
ORDER BY mean_time DESC
LIMIT 20;

-- Step 3: Analyze problematic query with EXPLAIN
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM large_table WHERE id = 123;

-- Look for:
-- - Seq Scan on large tables (missing index?)
-- - Hash Join with poor selectivity
-- - Actual vs Estimated rows mismatch (stale stats?)
-- - Many buffers read (cache miss?)

-- Step 4: Check if statistics are current
SELECT 
  schemaname,
  tablename,
  last_analyze,
  analyze_count
FROM pg_stat_user_tables
WHERE last_analyze < NOW() - INTERVAL '7 days';

-- Run ANALYZE if needed
ANALYZE large_table;

-- Step 5: Check for missing indexes
-- Run EXPLAIN again and look for Seq Scan
CREATE INDEX idx_large_table_id ON large_table(id);

-- Step 6: Verify improvement
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM large_table WHERE id = 123;
```

### High CPU Usage

```bash
# Symptom: PostgreSQL consuming 80%+ CPU

# Step 1: Identify CPU-consuming processes
top -p $(pgrep postgres | tr '\n' ',') -b -n 1

# Step 2: Find problematic query
psql -U postgres -c "
SELECT 
  pid,
  usename,
  state,
  query,
  query_start,
  EXTRACT(EPOCH FROM (NOW() - query_start)) as duration_sec
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_start;
"

# Step 3: Analyze query performance
psql -U postgres -d gym_training -c "
EXPLAIN (ANALYZE, BUFFERS)
SELECT ... (the problematic query)
"

# Step 4: Cancel long-running query if needed
psql -U postgres -c "SELECT pg_terminate_backend(PID);"

# Step 5: Check for missing indexes causing full table scans
psql -U postgres -d gym_training -c "
SELECT schemaname, tablename, seq_scan, seq_tup_read
FROM pg_stat_user_tables
WHERE seq_scan > 1000
ORDER BY seq_tup_read DESC;
"

# Step 6: Check autovacuum not running
psql -U postgres -c "SELECT * FROM pg_stat_activity WHERE query LIKE '%autovacuum%';"
```

### High Memory Usage

```bash
# Symptom: PostgreSQL taking 80%+ of system RAM

# Step 1: Check PostgreSQL process size
ps -eo pid,user,%mem,vsz,rss,comm | grep postgres | sort -k4 -rn

# Step 2: Check current memory configuration
psql -U postgres -c "
SELECT name, setting, unit
FROM pg_settings
WHERE name IN ('shared_buffers', 'effective_cache_size', 'work_mem')
ORDER BY name;
"

# Step 3: Identify high work_mem usage
psql -U postgres -c "
SELECT 
  pid,
  usename,
  query,
  state
FROM pg_stat_activity
WHERE query LIKE '%SORT%' OR query LIKE '%HASH%';
"

# These operations use work_mem per process!
# If 100 processes use 100MB work_mem each = 10GB!

# Step 4: Reduce work_mem if excessive
# Edit postgresql.conf:
# work_mem = 16MB  (for 200 connections)

# Step 5: Check for memory leaks (backend processes growing)
# If one backend process keeps growing, possible leak
# Solution: Restart that connection
```

### I/O Bottleneck

```bash
# Symptom: High disk I/O, slow queries

# Step 1: Monitor I/O statistics
iostat -x 1 5

# Look for:
# - High util% (> 80%)
# - High await (> 10ms avg)
# - High svctm (> 5ms)

# Step 2: Check WAL I/O vs data I/O
# WAL should be on separate fast disk if possible

# Step 3: Monitor specific queries
psql -U postgres -d gym_training -c "
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*) FROM large_table;
"

# Look at:
# - Shared Hit vs Read (should be >95% hits)
# - Temp Read/Write (sort to disk?)

# Step 4: Increase shared_buffers if lots of reads
# Edit postgresql.conf:
# shared_buffers = 8GB  (25-40% of available RAM)

# Step 5: Tune checkpoint settings to reduce I/O spikes
# Edit postgresql.conf:
# checkpoint_timeout = 15min
# checkpoint_completion_target = 0.9
```

---

## Replication Failures

### Replication Lag Growing

```sql
-- Symptom: Replica falling behind primary

-- Step 1: Check replication slot status
SELECT 
  slot_name,
  slot_type,
  active,
  restart_lsn,
  confirmed_flush_lsn,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn) / (1024*1024)) as lag_mb
FROM pg_replication_slots;

-- Step 2: Check replication connection
SELECT 
  pid,
  usename,
  client_addr,
  state,
  flush_lsn,
  replay_lsn,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), flush_lsn) / (1024*1024)) as sent_lag_mb
FROM pg_stat_replication;

-- Step 3: If replication stalled
-- Check replica logs for errors
-- Check network connectivity
ssh replica "tail -f /var/log/postgresql/postgresql.log | grep -i replica"

-- Step 4: Force WAL flush
SELECT pg_wal_flush();

-- Step 5: If lag continues growing
-- On primary, check WAL generation rate
SELECT 
  pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') / (1024*1024) as wal_mb,
  (SELECT setting::float FROM pg_settings WHERE name = 'max_wal_size') / 1024 as max_wal_mb;

-- If application doing massive writes, this is expected
-- Monitor replica can catch up when write rate reduces
```

### Replication Not Starting

```bash
# Symptom: Replication slot shows inactive

# Step 1: Check replica is connected
psql -h replica.ip -U replication -d postgres -c "SELECT 1"

# Step 2: Check replica standby.signal exists
ssh replica "ls -la /var/lib/postgresql/14/main/standby.signal"

# If not:
ssh replica "touch /var/lib/postgresql/14/main/standby.signal"
ssh replica "systemctl restart postgresql"

# Step 3: Check recovery.conf on replica
ssh replica "cat /var/lib/postgresql/14/main/recovery.conf"

# Should have:
# primary_conninfo = 'host=primary.ip port=5432 user=replication ...'
# primary_slot_name = 'replica_slot'

# Step 4: Check replica logs
ssh replica "tail -20 /var/log/postgresql/postgresql.log"

# Step 5: Manually trigger replication
ssh replica "psql -U postgres -d postgres << SQL
ALTER SYSTEM SET primary_conninfo = 'host=primary.ip port=5432 user=replication password=...';
SELECT pg_reload_conf();
SQL"

# Step 6: Force replication restart
ssh replica "systemctl restart postgresql"

# Monitor progress
watch -n 1 "psql -U postgres -c 'SELECT 
  slot_name, 
  active,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn) / (1024*1024))::int as lag_mb
FROM pg_replication_slots;'"
```

### Replication Slots Blocking WAL Cleanup

```sql
-- Symptom: Disk space growing, WAL not cleaned up

-- Step 1: Check replication slot status
SELECT 
  slot_name,
  slot_type,
  active,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / (1024*1024)) as retained_mb
FROM pg_replication_slots
ORDER BY retained_mb DESC;

-- Step 2: If slot inactive and retaining lots of WAL
-- Option A: Recreate the slot (force resync)
SELECT pg_drop_replication_slot('problematic_slot');

-- On replica, remove standby.signal and resync
ssh replica "rm /var/lib/postgresql/14/main/standby.signal"
ssh replica "rm -rf /var/lib/postgresql/14/main/base/*"

-- On primary, recreate base backup
pg_basebackup -h localhost -D /tmp/backup -Ft -z -v

# Transfer to replica
scp /tmp/backup/base.tar.gz replica:/tmp/
ssh replica "tar -xzf /tmp/base.tar.gz -C /var/lib/postgresql/14/main"
ssh replica "touch /var/lib/postgresql/14/main/standby.signal"
ssh replica "systemctl start postgresql"

-- Option B: Emergency drop inactive slot
-- WARNING: This means replica will need full resync!
SELECT pg_drop_replication_slot('old_inactive_slot');

-- Step 3: Monitor WAL cleanup after fix
SELECT 
  name,
  setting
FROM pg_settings
WHERE name IN ('wal_keep_size', 'max_wal_size');
```

---

## Lock and Deadlock Issues

### Detecting Locks

```sql
-- Symptom: Query hanging, no progress

-- Step 1: Check for blocking queries
SELECT 
  blocked_locks.pid AS blocked_pid,
  blocked_activity.usename AS blocked_user,
  blocking_locks.pid AS blocking_pid,
  blocking_activity.usename AS blocking_user,
  blocked_activity.query AS blocked_statement,
  blocking_activity.query AS blocking_statement,
  blocked_activity.application_name AS blocked_application,
  blocking_activity.application_name AS blocking_application
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
  AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
  AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
  AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
  AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
  AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
  AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
  AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
  AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
  AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
  AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- Step 2: Kill blocking query if safe
SELECT pg_terminate_backend(12345);  -- blocking_pid

-- Step 3: Check for row-level locks
SELECT 
  * 
FROM pg_locks 
WHERE NOT granted
ORDER BY pid;

-- Step 4: Check for table locks
SELECT 
  pg_locks.pid,
  pg_stat_activity.usename,
  pg_stat_activity.query,
  pg_locks.mode
FROM pg_locks
JOIN pg_stat_activity ON pg_locks.pid = pg_stat_activity.pid
WHERE pg_locks.locktype = 'relation'
  AND NOT pg_locks.granted;
```

### Deadlock Detection

```sql
-- Symptom: "ERROR: deadlock detected"

-- Step 1: Enable deadlock logging
ALTER SYSTEM SET log_lock_waits = on;
ALTER SYSTEM SET deadlock_timeout = 1000;  -- ms
SELECT pg_reload_conf();

-- Step 2: Review deadlock logs
grep "deadlock detected" /var/log/postgresql/postgresql.log | tail -20

-- Step 3: Analyze deadlock pattern
-- Look for queries A and B:
-- Transaction 1: locks table A, waits for table B
-- Transaction 2: locks table B, waits for table A
-- Result: DEADLOCK

-- Step 4: Fix deadlock (change lock order)
-- All transactions should lock in same order:
-- 1. Always lock table A first
-- 2. Then lock table B
-- 3. This prevents cycle

-- Step 5: Update application code
-- Instead of:
--   BEGIN;
--   UPDATE table_b SET ...;
--   UPDATE table_a SET ...;
--   COMMIT;
-- 
-- Do:
--   BEGIN;
--   UPDATE table_a SET ...;
--   UPDATE table_b SET ...;
--   COMMIT;

-- Step 6: Add retry logic in application
-- On deadlock error, retry transaction
```

---

## Data Corruption

### Detecting Corruption

```bash
# Symptom: "ERROR: invalid page header", index corruption

# Step 1: Run REINDEX to rebuild indexes
REINDEX INDEX CONCURRENTLY idx_name;
REINDEX TABLE CONCURRENTLY large_table;

# Step 2: Check table for corruption
psql -U postgres -d gym_training -c "
SELECT * FROM large_table LIMIT 1000;
"

# If errors on SELECT, data corruption

# Step 3: Use amcheck extension to detect corruption
CREATE EXTENSION IF NOT EXISTS amcheck;

SELECT bt_index_check('idx_name');

# Step 4: If data corrupted, try recovery
# Option A: Restore from backup
# Option B: pg_dump affected table, delete, restore

pg_dump -U postgres -d gym_training -t problematic_table --data-only > /tmp/data.sql
psql -U postgres -d gym_training -c "TRUNCATE problematic_table CASCADE;"
psql -U postgres -d gym_training -f /tmp/data.sql

# Step 5: Run ANALYZE after recovery
ANALYZE problematic_table;
```

### Transaction Wraparound

```sql
-- Symptom: "FATAL: could not access transaction status of transaction..."

-- Step 1: Check wraparound status
SELECT 
  datname,
  age(datfrozenxid),
  (2147483647 - age(datfrozenxid)) / 1000000 as txn_until_wraparound_millions
FROM pg_database
WHERE datallowconn
ORDER BY age(datfrozenxid) DESC;

-- Step 2: If < 10 million transactions left, CRITICAL
-- Perform emergency VACUUM FREEZE immediately

VACUUM FREEZE;  -- Don't wait!

-- Step 3: Monitor progress
SELECT 
  datname,
  age(datfrozenxid)
FROM pg_database
WHERE datname = current_database();

-- Check logs
tail -f /var/log/postgresql/postgresql.log | grep -i "vacuum"

-- Step 4: Once recovered, enable aggressive autovacuum
ALTER SYSTEM SET autovacuum_freeze_max_age = 100000000;  # Default
SELECT pg_reload_conf();

-- All tables will vacuum when reaching this age
```

---

## Crash and Recovery

### PostgreSQL Won't Start

```bash
# Symptom: systemctl start postgresql fails

# Step 1: Check what's wrong
sudo systemctl start postgresql
sudo journalctl -xe

# Common error messages:

# "Address already in use"
# - Another PostgreSQL running?
ps aux | grep postgres
# - Kill competing process
pkill -9 postgres

# "Permission denied"
# - PostgreSQL directory permissions
sudo chown postgres:postgres /var/lib/postgresql -R
sudo chmod 700 /var/lib/postgresql -R

# "Invalid argument"
# - Corrupted control file or WAL
# - Try recovery:
sudo -u postgres pg_ctl recover -D /var/lib/postgresql/14/main

# Step 2: Try single-user mode
sudo -u postgres postgres -D /var/lib/postgresql/14/main -P single

# Step 3: If still fails, force reset WAL
sudo -u postgres pg_resetwal -D /var/lib/postgresql/14/main

# WARNING: This can cause data loss!
# Use only as last resort before restoring from backup

# Step 4: Start again
sudo systemctl start postgresql
```

### Incomplete Recovery

```bash
# Symptom: Some data missing, or recovery incomplete

# Step 1: Check recovery progress
tail -f /var/log/postgresql/postgresql.log

# Look for "invalid page header" or recovery errors

# Step 2: Enable recovery info logging
sudo -u postgres bash -c '
cat >> /var/lib/postgresql/14/main/recovery.conf << EOF
recovery_verbose = on
recovery_min_apply_delay = '0'
EOF'

# Step 3: Restart for recovery
sudo systemctl restart postgresql

# Monitor recovery
watch -n 1 "psql -U postgres -c 'SELECT now() - pg_postmaster_start_time() as uptime;'"

# Step 4: After recovery completes, verify data
psql -U postgres -d gym_training -c "
SELECT 
  schemaname,
  tablename,
  n_live_tup,
  last_vacuum,
  last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC
LIMIT 10;"

# If row counts are wrong, may need restore from backup
```

---

## High Availability Failover

### Manual Failover Procedure

```bash
#!/bin/bash
# failover.sh - Manual HA failover

set -e

PRIMARY_HOST="primary.gym.local"
REPLICA_HOST="replica-1.gym.local"
REPLICA_ADMIN_PASS="replica_password"

echo "=== Starting Manual Failover ==="

# Step 1: Confirm primary is truly down
echo "Checking primary..."
if ! pg_isready -h "$PRIMARY_HOST" -p 5432; then
  echo "✓ Primary is unreachable (confirmed)"
else
  echo "✗ Primary is still responsive!"
  echo "Aborting - check if primary is really down"
  exit 1
fi

# Step 2: Stop applications connecting to database
echo "Notifying applications..."
# (Implement app notification here)

# Step 3: Promote replica to primary
echo "Promoting replica..."
ssh postgres@"$REPLICA_HOST" /usr/lib/postgresql/14/bin/pg_ctl promote \
  -D /var/lib/postgresql/14/main

# Step 4: Wait for promotion to complete
sleep 10

# Step 5: Verify new primary is writable
echo "Verifying new primary..."
psql -h "$REPLICA_HOST" -U postgres -d postgres -c "SELECT 1"

# Step 6: Reconfigure applications
echo "Reconfiguring connection strings..."
# Update app config to point to new primary
# (Implement app reconfiguration here)

# Step 7: Start replicating from new primary
echo "Setting up new replica from original primary (when ready)..."
# (Will configure old primary as replica once repaired)

echo "=== Failover Complete ==="
```

### Failover Verification

```sql
-- After failover, verify data integrity

-- Step 1: Check new primary role
SELECT pg_is_in_recovery();  -- Should return FALSE

-- Step 2: Verify all databases accessible
SELECT datname FROM pg_database WHERE datallowconn = true ORDER BY datname;

-- Step 3: Check data statistics
SELECT 
  datname,
  SUM(n_live_tup) as total_rows,
  SUM(n_dead_tup) as dead_rows
FROM pg_stat_user_tables
GROUP BY datname;

-- Step 4: Verify critical tables have data
SELECT COUNT(*) as user_count FROM auth.users;
SELECT COUNT(*) as workout_count FROM training.workouts;
SELECT COUNT(*) as metric_count FROM tracking.metrics;

-- Step 5: Check for replication slots and remove old primary slot
SELECT * FROM pg_replication_slots;
-- If old primary slot still exists, drop it:
-- SELECT pg_drop_replication_slot('slot_name');

-- Step 6: Monitor for consistency issues
SELECT 
  pid,
  usename,
  state,
  query
FROM pg_stat_activity
WHERE state != 'idle';
```

---

## Emergency Procedures

### Emergency Database Lockdown

```sql
-- If under attack or need immediate protection

-- Step 1: Disable all user connections
ALTER SYSTEM SET max_connections = 5;
SELECT pg_reload_conf();

-- Kill existing user connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE usename NOT IN ('postgres', 'system')
  AND pid != pg_backend_pid();

-- Step 2: Disable connections to specific databases
UPDATE pg_database SET datallowconn = false WHERE datname = 'gym_training';

-- Step 3: Enable read-only mode
ALTER DATABASE gym_training SET default_transaction_read_only = on;

-- Step 4: Pause autovacuum
ALTER SYSTEM SET autovacuum = off;
SELECT pg_reload_conf();

-- Step 5: Restrict network access
-- (Implement firewall rules at OS level)

-- Recovery steps:
ALTER SYSTEM RESET max_connections;
UPDATE pg_database SET datallowconn = true WHERE datname = 'gym_training';
ALTER DATABASE gym_training RESET default_transaction_read_only;
ALTER SYSTEM SET autovacuum = on;
SELECT pg_reload_conf();
```

### Out of Disk Space Emergency

```bash
# Symptom: "ERROR: could not write to file..."

# Step 1: Check disk usage
df -h

# Step 2: Find large files
du -sh /var/lib/postgresql/* | sort -rh | head -10

# Step 3: Identify large tables
psql -U postgres -d gym_training -c "
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;"

# Step 4: Emergency options:

# Option A: Truncate old archive data
psql -U postgres -d gym_tracking -c "TRUNCATE metrics_archive CASCADE;"

# Option B: Drop unused indexes
psql -U postgres -d gym_training -c "DROP INDEX idx_unused_index;"

# Option C: Move data to different disk
# Only if you have time/planning

# Step 5: Add disk space (if possible)
# Extend volume or add new disk

# Step 6: Resume normal operations
# Monitor disk usage going forward
```

---

## Post-Incident Analysis

### Incident Report Template

```
INCIDENT REPORT

Date/Time: 2024-03-21 14:30:00 UTC
Duration: 45 minutes
Severity: Critical
Status: RESOLVED

SUMMARY:
[Brief description of what happened]

IMPACT:
- Services affected: Training Service
- Users affected: ~1200
- Data lost: None
- Recovery time: 45 minutes

ROOT CAUSE:
[What actually caused the problem]

CONTRIBUTING FACTORS:
- No monitoring alert triggered
- Insufficient connection limit

TIMELINE:
14:30 - Issue detected
14:32 - Alert triggered
14:35 - Investigation started
14:45 - Root cause identified
14:50 - Fix implemented
15:15 - System fully recovered

RESOLUTION:
1. Increased connection pool limit
2. Added monitoring alert for connection threshold
3. Updated documentation

PREVENTION:
- [ ] Update runbook
- [ ] Add additional monitoring
- [ ] Schedule team training
- [ ] Implement preventive measure

FOLLOW-UP:
- [ ] Update alert thresholds
- [ ] Review similar systems
- [ ] Schedule post-mortem meeting
```

### Lessons Learned

```
After incident, ask:

1. What surprised us?
2. What went well?
3. What could we improve?
4. How do we prevent this?
5. What new risks did we discover?

Action items:
- Assign owners
- Set completion dates
- Track until resolved
```

---

## Related Documentation

- [07-monitoring-alerting.md](07-monitoring-alerting.md) - Detection and alerting
- [03-backup-recovery.md](03-backup-recovery.md) - Recovery procedures
- [04-performance-tuning.md](04-performance-tuning.md) - Performance issues

