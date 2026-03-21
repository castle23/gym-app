# Database Troubleshooting

## Overview

This guide addresses database-specific issues in the Gym Platform: connection problems, query performance, data consistency, backup/recovery, and replication issues. Most Gym Platform issues trace back to database problems, making systematic database troubleshooting essential.

**Database Context:**
- System: PostgreSQL 15+
- Backup Strategy: Daily pg_dump + WAL archiving
- Replication: Streaming replication (optional)
- Monitoring: pg_stat_statements extension
- Connection Pooling: HikariCP (10-20 connections per service)

---

## Table of Contents

1. [Connection Issues](#connection-issues)
2. [Query Performance](#query-performance)
3. [Data Consistency](#data-consistency)
4. [Backup and Recovery](#backup-and-recovery)
5. [Replication Issues](#replication-issues)
6. [Maintenance Tasks](#maintenance-tasks)

---

## Connection Issues

### Issue: Connection Pool Exhausted

**Symptoms:**
```
HikariPool-1 - Connection is not available, request timed out after 30000ms
Connection timeout error in application logs
```

**Diagnostic Steps:**

1. **Check active connections:**
```sql
SELECT count(*) as total_connections FROM pg_stat_activity;
SELECT datname, count(*) as connections 
FROM pg_stat_activity 
GROUP BY datname;
```

2. **Identify long-running connections:**
```sql
SELECT pid, usename, application_name, state, query_start, NOW() - query_start as duration
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start;
```

3. **Check pool configuration:**
```bash
docker exec gym-auth env | grep HIKARI
docker exec postgres psql -U gym_user -d gym_db -c "SHOW max_connections;"
```

4. **Monitor connections in real-time:**
```bash
watch -n 2 'docker exec postgres psql -U gym_user -d gym_db -c \
  "SELECT count(*) as connections FROM pg_stat_activity;"'
```

**Resolution:**

**Increase connection pool:**
```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20  # Increased from 10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

**Close idle connections:**
```sql
-- Kill idle connections (careful in production!)
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'idle' AND query_start < NOW() - INTERVAL '30 minutes';
```

**Find connection leaks in code:**
```bash
# Look for unclosed connections/statements
grep -r "getConnection\|createStatement" src/ --include="*.java" | \
  grep -v "try-with-resources\|try ("
```

---

### Issue: Cannot Connect to Database

**Symptoms:**
```
psql: could not connect to server: Connection refused
java.sql.SQLTransientConnectionException: Connection refused
```

**Diagnostic Steps:**

1. **Verify PostgreSQL is running:**
```bash
docker ps | grep postgres
docker logs postgres | tail -20
```

2. **Test network connectivity:**
```bash
docker exec gym-auth nc -zv postgres 5432
docker exec gym-auth ping postgres
```

3. **Verify environment variables:**
```bash
docker exec gym-auth env | grep -i spring_datasource
```

4. **Test direct connection:**
```bash
docker exec postgres psql -U gym_user -d gym_db -c "SELECT 1"
```

**Resolution:**

**Verify docker-compose configuration:**
```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:15
    container_name: postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: gym_db
      POSTGRES_USER: gym_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gym_user -d gym_db"]

  gym-auth:
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gym_db
      SPRING_DATASOURCE_USERNAME: gym_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
```

**Ensure database exists:**
```bash
docker exec postgres createdb -U gym_user gym_db 2>/dev/null || echo "Database already exists"
```

---

## Query Performance

### Issue: Slow Queries

**Symptoms:**
```
Database query takes >1000ms
"Query took 2500ms" in logs
Application timeout waiting for results
```

**Diagnostic Steps:**

1. **Enable slow query logging:**
```sql
ALTER DATABASE gym_db SET log_min_duration_statement = 1000;
```

2. **Find slow queries:**
```bash
docker logs postgres | grep "duration:" | tail -20
```

3. **Get execution plan:**
```bash
docker exec postgres psql -U gym_user -d gym_db << 'EOF'
EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
SELECT s.id, s.name, u.username, COUNT(e.id) as exercise_count
FROM training_sessions s
JOIN users u ON s.user_id = u.id
LEFT JOIN exercises e ON s.id = e.session_id
WHERE s.created_at > NOW() - INTERVAL '7 days'
GROUP BY s.id, u.id, u.username
ORDER BY s.created_at DESC
LIMIT 20;
EOF
```

4. **Check for sequential scans:**
```sql
SELECT schemaname, tablename, seq_scan, idx_scan, 
       ROUND(100.0 * seq_scan / (seq_scan + idx_scan), 2) as seq_pct
FROM pg_stat_user_tables
WHERE seq_scan + idx_scan > 0
ORDER BY seq_pct DESC
LIMIT 10;
```

**Resolution:**

**Create missing indexes:**
```sql
-- Single column index
CREATE INDEX idx_sessions_user_id ON training_sessions(user_id);

-- Composite index
CREATE INDEX idx_sessions_user_created 
ON training_sessions(user_id, created_at DESC);

-- Partial index (if most records are active)
CREATE INDEX idx_active_sessions 
ON training_sessions(user_id) 
WHERE status = 'active';

-- Verify statistics are updated
ANALYZE training_sessions;
```

**Optimize query structure:**
```sql
-- Before: Subquery
SELECT * FROM users WHERE id IN (
  SELECT DISTINCT user_id FROM training_sessions WHERE created_at > NOW() - INTERVAL '7 days'
);

-- After: JOIN
SELECT DISTINCT u.* 
FROM users u
INNER JOIN training_sessions s ON u.id = s.user_id
WHERE s.created_at > NOW() - INTERVAL '7 days';
```

**Reduce result set:**
```sql
-- Add WHERE clause to limit data
SELECT * FROM training_sessions WHERE created_at > NOW() - INTERVAL '30 days' LIMIT 1000;

-- Use pagination instead of loading all
SELECT * FROM training_sessions ORDER BY created_at DESC LIMIT 20 OFFSET 0;
```

---

### Issue: Lock Timeouts

**Symptoms:**
```
ERROR: canceling statement due to lock timeout
Process was terminated
Transaction deadlock detected
```

**Diagnostic Steps:**

1. **Check for blocking queries:**
```sql
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_query,
       blocking_activity.query AS blocking_query
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks 
  ON blocking_locks.locktype = blocked_locks.locktype
  AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
  AND blocking_locks.pid != blocked_locks.pid
JOIN pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

2. **Identify deadlock details:**
```bash
docker logs postgres | grep -A 10 "deadlock detected"
```

3. **Check lock types:**
```sql
SELECT pid, usename, locktype, relation, mode, granted
FROM pg_locks
WHERE NOT granted;
```

**Resolution:**

**Terminate blocking transaction:**
```sql
-- Carefully identify and terminate blocking transaction
SELECT pg_terminate_backend(pid) FROM pg_stat_activity 
WHERE pid = <blocking_pid>;
```

**Adjust lock timeout:**
```properties
# application.properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.lock.timeout=5000
```

**Reduce transaction scope:**
```java
// Before: Long transaction
@Transactional
public void processAllUsers() {
    List<User> users = userRepository.findAll();
    for (User user : users) {
        user.setLastProcessed(LocalDateTime.now());
        userRepository.save(user);
        // External API call - holds lock entire time!
        sendNotification(user);
    }
}

// After: Short transactions
public void processAllUsers() {
    List<Long> userIds = userRepository.findAllIds();
    for (Long userId : userIds) {
        processSingleUser(userId);
    }
}

@Transactional
private void processSingleUser(Long userId) {
    User user = userRepository.findById(userId);
    user.setLastProcessed(LocalDateTime.now());
    userRepository.save(user);
    
    // Outside transaction
    sendNotification(user);
}
```

---

## Data Consistency

### Issue: Constraint Violations

**Symptoms:**
```
Duplicate key value violates unique constraint
Foreign key constraint violation
CHECK constraint violation
```

**Diagnostic Steps:**

1. **Review error message:**
```
ERROR: duplicate key value violates unique constraint "users_email_key"
DETAIL: Key (email)=(user@example.com) already exists.
```

2. **Check constraint definitions:**
```sql
-- List all constraints on table
SELECT constraint_name, constraint_type, table_name
FROM information_schema.table_constraints
WHERE table_name = 'users';
```

3. **Find violating data:**
```sql
-- Find duplicate emails
SELECT email, COUNT(*) as count FROM users GROUP BY email HAVING COUNT(*) > 1;

-- Find orphaned foreign keys
SELECT s.id FROM training_sessions s
LEFT JOIN users u ON s.user_id = u.id
WHERE u.id IS NULL;
```

**Resolution:**

**Fix duplicate data:**
```sql
-- Find and remove duplicates (keep first occurrence)
DELETE FROM users a
WHERE a.id NOT IN (
  SELECT MIN(id) FROM users b 
  WHERE a.email = b.email
  GROUP BY b.email
);
```

**Fix orphaned foreign keys:**
```sql
-- Check constraints
SELECT * FROM training_sessions WHERE user_id NOT IN (SELECT id FROM users);

-- Delete orphaned records
DELETE FROM training_sessions 
WHERE user_id NOT IN (SELECT id FROM users);

-- Or cascade delete properly in future
ALTER TABLE training_sessions 
DROP CONSTRAINT IF EXISTS training_sessions_user_id_fkey;

ALTER TABLE training_sessions 
ADD CONSTRAINT training_sessions_user_id_fkey 
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
```

---

### Issue: Data Inconsistency

**Symptoms:**
```
Totals don't match
Counts are incorrect
Data appears lost or duplicated
```

**Diagnostic Steps:**

1. **Verify referential integrity:**
```sql
-- Check for orphaned records
SELECT COUNT(*) FROM training_sessions WHERE user_id NOT IN (SELECT id FROM users);
SELECT COUNT(*) FROM exercises WHERE session_id NOT IN (SELECT id FROM training_sessions);
```

2. **Compare counts:**
```sql
SELECT 
  (SELECT COUNT(*) FROM users) as user_count,
  (SELECT COUNT(*) FROM training_sessions) as session_count,
  (SELECT COUNT(*) FROM exercises) as exercise_count;
```

3. **Check transaction logs:**
```bash
docker logs gym-auth | grep -i "rollback\|transaction failed"
```

**Resolution:**

**Rebuild consistency:**
```sql
-- Reset counters to correct values
UPDATE users SET session_count = (
  SELECT COUNT(*) FROM training_sessions WHERE user_id = users.id
);

-- Remove orphaned data
DELETE FROM training_sessions WHERE user_id NOT IN (SELECT id FROM users);
DELETE FROM exercises WHERE session_id NOT IN (SELECT id FROM training_sessions);

-- Verify consistency
SELECT COUNT(*) FROM training_sessions WHERE user_id IN (SELECT id FROM users);
```

---

## Backup and Recovery

### Issue: Backup Failures

**Symptoms:**
```
Backup script exits with error
Backup file not created or incomplete
Backup disk space full
```

**Diagnostic Steps:**

1. **Check backup script:**
```bash
ls -lah /backup/gym_db_*.sql.gz | tail -5
# Check file sizes - should be reasonable
```

2. **Verify backup permissions:**
```bash
docker exec postgres ls -la /var/lib/postgresql/backup/
```

3. **Test backup command:**
```bash
docker exec postgres pg_dump -U gym_user -d gym_db | gzip > /tmp/test_backup.sql.gz
ls -lah /tmp/test_backup.sql.gz
```

4. **Check disk space:**
```bash
docker exec postgres df -h /var/lib/postgresql/
```

**Resolution:**

**Create backup script:**
```bash
#!/bin/bash
# backup.sh
BACKUP_DIR="/var/lib/postgresql/backup"
DB_NAME="gym_db"
DB_USER="gym_user"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p $BACKUP_DIR

# Full backup
pg_dump -U $DB_USER -d $DB_NAME | gzip > $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "Backup successful: $BACKUP_FILE"
    # Keep only last 7 days
    find $BACKUP_DIR -name "${DB_NAME}_*.sql.gz" -mtime +7 -delete
else
    echo "Backup failed!"
    exit 1
fi
```

**Schedule backup in Docker:**
```yaml
# docker-compose.yml
services:
  postgres:
    volumes:
      - postgres_backup:/var/lib/postgresql/backup
    environment:
      POSTGRES_INITDB_ARGS: "-c log_statement=all"

volumes:
  postgres_backup:
    driver: local
```

---

### Issue: Restore Failures

**Symptoms:**
```
Restore hangs or fails
"Permission denied" errors
"Relation already exists"
```

**Diagnostic Steps:**

1. **Verify backup file integrity:**
```bash
gunzip -t /backup/gym_db_20240321.sql.gz
# No output means file is OK
```

2. **Check database state before restore:**
```bash
docker exec postgres psql -U gym_user -d gym_db -c "SELECT COUNT(*) FROM users;"
```

3. **Test restore on test database:**
```bash
# Create test DB
docker exec postgres createdb -U gym_user -d gym_db_test

# Attempt restore
docker exec postgres psql -U gym_user -d gym_db_test < gym_db_backup.sql
```

**Resolution:**

**Restore from backup:**
```bash
# Full database restore
docker exec postgres psql -U gym_user -d gym_db < backup.sql

# Or restore from gzip
gunzip -c backup.sql.gz | docker exec postgres psql -U gym_user -d gym_db

# Or using pipe
cat backup.sql.gz | docker exec -i postgres gunzip | docker exec -i postgres psql -U gym_user -d gym_db
```

**Point-in-time recovery:**
```bash
# Requires WAL archiving configured
# Use pg_basebackup + WAL files to recover to specific time
docker exec postgres pg_basebackup -D /var/lib/postgresql/backup/recovery \
  -X stream -v -P

# Configure recovery_target_time in postgresql.conf
# Restart PostgreSQL to apply recovery
```

---

## Replication Issues

### Issue: Replica Lag

**Symptoms:**
```
Replica significantly behind primary
Replication lag > 1 second
User sees stale data
```

**Diagnostic Steps:**

1. **Check replication status on primary:**
```sql
SELECT client_addr, backend_start, state, sync_state, replay_lag
FROM pg_stat_replication;
```

2. **Check replica lag:**
```sql
SELECT pg_last_xact_replay_timestamp() as last_replay_time,
       NOW() - pg_last_xact_replay_timestamp() as replication_lag;
```

3. **Monitor WAL production:**
```sql
SELECT pg_current_xlog_location() as xlog_location;
```

**Resolution:**

**Increase network bandwidth for replication:**
```yaml
# docker-compose.yml - if using replication
services:
  postgres-replica:
    environment:
      PRIMARY_CONNINFO: "host=postgres port=5432 user=replication password=${REPLICATION_PASSWORD}"
      RECOVERY_TARGET_TIMELINE: latest
```

**Optimize replication settings:**
```properties
# postgresql.conf on primary
wal_level = replica
max_wal_senders = 3
wal_keep_segments = 64
hot_standby = on  # On replica
```

---

## Maintenance Tasks

### Vacuum and Analyze

**Purpose:** Reclaim space and optimize query planning

**Run regular maintenance:**
```sql
-- Remove dead tuples (space reclamation)
VACUUM gym_db;

-- Aggressive vacuum (locks table)
VACUUM FULL gym_db;

-- Update statistics for query planner
ANALYZE gym_db;

-- Combined (recommended)
VACUUM ANALYZE gym_db;
```

**Schedule automatic maintenance:**
```sql
-- Check autovacuum settings
SHOW autovacuum;
SHOW autovacuum_vacuum_scale_factor;

-- Recommended settings
ALTER SYSTEM SET autovacuum = on;
ALTER SYSTEM SET autovacuum_vacuum_scale_factor = 0.05;  # 5% of table
ALTER SYSTEM SET autovacuum_analyze_scale_factor = 0.02;  # 2% of table
```

### Monitor Table Sizes

```sql
-- Find largest tables
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;
```

### Remove Unused Indexes

```sql
-- Find unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- Remove if truly unused
DROP INDEX idx_unused_index;
```

---

## Database Troubleshooting Checklist

- [ ] Connection pool not exhausted (<80% active)
- [ ] No slow queries (p95 < 1000ms)
- [ ] Referential integrity maintained
- [ ] Recent backups exist and tested
- [ ] Autovacuum running
- [ ] Replication lag < 1 second (if applicable)
- [ ] Disk space available (>20% free)
- [ ] WAL archiving working (if PITR needed)

---

## Related Documentation

- [02-debugging-techniques.md](02-debugging-techniques.md) - Database debugging
- [03-common-issues.md](03-common-issues.md) - Common database issues
- [04-diagnostic-procedures.md](04-diagnostic-procedures.md) - Database diagnostics
- docs/operations/04-backup-recovery.md - Backup procedures
- docs/database/ - Database documentation
- docs/stack/02-database-postgresql.md - PostgreSQL setup
