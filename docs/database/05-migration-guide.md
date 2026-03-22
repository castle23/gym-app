# Migration Guide

> **Note**: The current project uses Hibernate `ddl-auto: update` in development and `validate` in production. Flyway/Liquibase are not yet configured. This document describes the target migration strategy for production use.

## Overview

Comprehensive guide for executing database schema migrations in the Gym Platform. This guide covers migration strategies, tools (Flyway/Liquibase), version control procedures, zero-downtime migrations, rollback strategies, and migration testing. The Gym Platform uses Flyway for SQL-based migrations and Liquibase for complex scenarios, with a robust change control process for production safety.

## Table of Contents

- [Migration Strategy](#migration-strategy)
- [Migration Tools](#migration-tools)
- [Migration Planning](#migration-planning)
- [Creating Migrations](#creating-migrations)
- [Testing Migrations](#testing-migrations)
- [Production Deployment](#production-deployment)
- [Rollback Procedures](#rollback-procedures)
- [Data Migrations](#data-migrations)
- [Zero-Downtime Migrations](#zero-downtime-migrations)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Migration Strategy

### Migration Principles

```
┌──────────────────────────────────────────────┐
│   Database Migration Principles               │
└──────────────────────────────────────────────┘

1. REVERSIBILITY
   └─ All migrations must be reversible
   └─ Maintain rollback scripts
   └─ Test rollbacks regularly

2. ATOMICITY
   └─ Migrations either fully succeed or fail
   └─ No partial/broken state
   └─ Use transactions where possible

3. IDEMPOTENCY
   └─ Migrations are safe to run multiple times
   └─ Use "IF NOT EXISTS" / "IF EXISTS"
   └─ Handle edge cases

4. VELOCITY
   └─ Minimize lock times
   └─ Use concurrent operations
   └─ Avoid full table rewrites

5. VISIBILITY
   └─ Track all migrations
   └─ Version control
   └─ Audit trail
   └─ Clear commit messages

6. TESTABILITY
   └─ Test in staging first
   └─ Load test if large changes
   └─ Document expected impact
```

### Migration Types

**Schema Migrations:** Add/modify/drop structures
- Create tables, indexes, columns
- Modify column types
- Drop deprecated structures

**Data Migrations:** Transform data without schema changes
- Backfill new columns
- Normalize existing data
- Cleanup invalid data

**Behavioral Migrations:** Change how data is handled
- Add constraints
- Modify triggers
- Update stored procedures

---

## Migration Tools

### Flyway

**Setup:**

```bash
# Install Flyway
wget https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/9.0.0/flyway-commandline-9.0.0-linux-x64.tar.gz
tar -xzf flyway-commandline-9.0.0-linux-x64.tar.gz
sudo mv flyway-9.0.0 /opt/flyway
sudo ln -s /opt/flyway/flyway /usr/local/bin

# Verify installation
flyway -version
# Output: Flyway Community Edition 9.0.0
```

**Configuration: conf/flyway.conf**

```properties
# Database connection
flyway.driver=org.postgresql.Driver
flyway.url=jdbc:postgresql://10.0.1.50:5432/gym_db
flyway.user=flyway_user
flyway.password=secure_password

# Migration location and naming
flyway.locations=filesystem:sql/migrations
flyway.baselineVersion=1
flyway.sqlMigrationPrefix=V
flyway.undoSqlMigrationPrefix=U
flyway.sqlMigrationSeparator=__

# Baseline migration
flyway.baselineOnMigrate=false
flyway.baselineDescription=Initial schema

# Validation and repair
flyway.validateOnMigrate=true
flyway.outOfOrder=false
flyway.cleanDisabled=true

# Schema management
flyway.schemas=auth,training,tracking,common
flyway.table=schema_migrations
flyway.defaultSchema=public
```

**Spring Boot integration:**

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baselineOnMigrate: false
    validateOnMigrate: true
    locations:
      - classpath:db/migration
    outOfOrder: false
    schemas:
      - auth
      - training
      - tracking
      - common
```

**Flyway commands:**

```bash
# Info: Show migration status
flyway info
# Output:
# Schema version history (gym_db.public.schema_migrations)
# +-----+-------------------+-----------+----------+---------------------+------+
# | ... | Migration         | Checksum  | Installed On        | ...  |
# +-----+-------------------+-----------+----------+---------------------+------+
# | ... | 1 | Create auth schema  | 1234567890| 2026-01-15 10:00:00 | ... |

# Migrate: Apply pending migrations
flyway migrate

# Validate: Check migration status
flyway validate

# Undo: Revert last migration (Flyway Pro only)
flyway undo

# Repair: Fix migration issues
flyway repair

# Clean: Wipe all objects (DANGER - development only)
flyway clean

# Baseline: Set initial version
flyway baseline -baselineVersion=1 -baselineDescription="Initial schema"
```

### Liquibase

**Setup:**

```bash
# Install Liquibase
wget https://github.com/liquibase/liquibase/releases/download/v4.20.0/liquibase-4.20.0.tar.gz
tar -xzf liquibase-4.20.0.tar.gz
sudo mv liquibase /opt/liquibase
sudo chmod +x /opt/liquibase/liquibase

# Add to PATH
export PATH=/opt/liquibase:$PATH
```

**Configuration: liquibase.properties**

```properties
# Database
url=jdbc:postgresql://10.0.1.50:5432/gym_db
username=liquibase_user
password=secure_password
driver=org.postgresql.Driver

# Changeset location
changeLogFile=db/changelog/db.changelog-master.yaml

# Schema
defaultSchemaName=public
liquibaseSchemaName=public
liquibaseTableName=databasechangelog

# Validation
contexts=prod
labels=gym-platform

# Output
outputFile=liquibase-status.txt
```

**Changeset example: db/changelog/db.changelog-master.yaml**

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/auth/001-create-auth-schema.yaml
  - include:
      file: db/changelog/auth/002-create-users-table.yaml
  - include:
      file: db/changelog/auth/003-add-user-indexes.yaml
```

**Liquibase commands:**

```bash
# Status: Check pending changes
liquibase status

# Update: Apply all pending changes
liquibase update

# Rollback: Revert to specific tag
liquibase rollback --tag=v1.2.0

# Dry run: Preview changes without applying
liquibase updateSQL

# History: Show applied changes
liquibase history
```

---

## Migration Planning

### Pre-Migration Checklist

```
PHASE 1: Planning (1-2 weeks before)
☐ Identify all affected systems
☐ Design schema changes
☐ Estimate data volume impact
☐ Identify potential locks/bottlenecks
☐ Plan rollback procedure
☐ Create change advisory ticket

PHASE 2: Development (1-2 weeks)
☐ Write migration scripts
☐ Create rollback scripts
☐ Write data transformation scripts (if needed)
☐ Document changes in detail
☐ Get code review approval

PHASE 3: Testing (1 week)
☐ Test in development environment
☐ Load test in staging
☐ Test rollback procedure
☐ Verify zero-downtime (if applicable)
☐ Document test results
☐ Sign-off from team lead

PHASE 4: Staging (3-5 days)
☐ Run full migration in staging
☐ Verify application compatibility
☐ Run end-to-end tests
☐ Measure performance impact
☐ Verify backup/recovery capability

PHASE 5: Production Preparation (1-2 days)
☐ Create database backup
☐ Disable automated backups during migration
☐ Prepare rollback script
☐ Brief on-call team
☐ Set maintenance window
☐ Notify stakeholders

PHASE 6: Production Execution (maintenance window)
☐ Final backup
☐ Execute migration
☐ Validate success
☐ Re-enable backups
☐ Monitor for issues

PHASE 7: Post-Migration (1-3 days)
☐ Analyze performance impact
☐ Verify data consistency
☐ Check application functionality
☐ Update documentation
☐ Conduct post-incident review
☐ Remove temporary code/tables
```

### Risk Assessment

**Low Risk:**
- Adding columns with defaults
- Adding indexes
- Adding constraints to valid data

**Medium Risk:**
- Dropping columns (data loss potential)
- Modifying column types (data conversion)
- Renaming columns (application compatibility)

**High Risk:**
- Changing primary keys
- Renaming tables
- Large data transformations
- Adding NOT NULL constraints on existing columns

---

## Creating Migrations

### Flyway SQL Migrations

**File naming convention:** V{version}__{description}.sql

```
sql/migrations/
├── V001__initial_schema.sql
├── V002__create_auth_users_table.sql
├── V003__create_training_schema.sql
├── V004__add_user_email_index.sql
└── V005__backfill_user_roles.sql
```

**Example: V001__initial_schema.sql**

```sql
-- Flyway migration: Initial schema setup
-- Version: 1
-- Date: 2026-01-15
-- Author: DBA Team
-- Description: Create auth, training, tracking, common schemas

-- Create schemas
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS training;
CREATE SCHEMA IF NOT EXISTS tracking;
CREATE SCHEMA IF NOT EXISTS common;

-- Create extensions (idempotent)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set search_path for current session
SET search_path TO auth, training, tracking, common, public;

-- Grant schema permissions
GRANT USAGE ON SCHEMA auth TO app_user;
GRANT USAGE ON SCHEMA training TO app_user;
GRANT USAGE ON SCHEMA tracking TO app_user;
GRANT USAGE ON SCHEMA common TO app_user;

-- Mark migration as complete
SELECT 1; -- Ensure successful execution
```

**Example: V002__create_auth_users_table.sql**

```sql
-- Flyway migration: Create auth.users table
-- Version: 2
-- Date: 2026-01-15
-- Description: Create users table with indexes

CREATE TABLE IF NOT EXISTS auth.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT email_not_empty CHECK (email != ''),
    CONSTRAINT username_not_empty CHECK (username != '')
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON auth.users(email) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_username ON auth.users(username) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_created_at ON auth.users(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_status ON auth.users(status) WHERE deleted_at IS NULL;

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON auth.users TO app_user;
GRANT USAGE, SELECT ON SEQUENCE auth.users_id_seq TO app_user;

-- Set table owner
ALTER TABLE auth.users OWNER TO postgres;
```

### Liquibase YAML Migrations

**Example: db/changelog/auth/002-create-users-table.yaml**

```yaml
databaseChangeLog:
  - changeSet:
      id: 002-create-users-table
      author: DBA Team
      objectQuotingStrategy: QUOTE_ALL_OBJECTS
      changes:
        - createTable:
            tableName: users
            schemaName: auth
            columns:
              - column:
                  name: id
                  type: UUID
                  defaultValueComputed: gen_random_uuid()
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: email
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: username
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: password_hash
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              - column:
                  name: full_name
                  type: VARCHAR(255)
              - column:
                  name: status
                  type: VARCHAR(50)
                  defaultValue: active
              - column:
                  name: created_at
                  type: TIMESTAMP WITH TIME ZONE
                  defaultValueComputed: CURRENT_TIMESTAMP
              - column:
                  name: updated_at
                  type: TIMESTAMP WITH TIME ZONE
                  defaultValueComputed: CURRENT_TIMESTAMP
              - column:
                  name: deleted_at
                  type: TIMESTAMP WITH TIME ZONE
        
        - createIndex:
            indexName: idx_users_email
            schemaName: auth
            tableName: users
            columns:
              - column: email
            where: deleted_at IS NULL
        
        - createIndex:
            indexName: idx_users_status
            schemaName: auth
            tableName: users
            columns:
              - column: status
            where: deleted_at IS NULL
        
        - grantPermissions:
            permissions: SELECT,INSERT,UPDATE,DELETE
            grantee: app_user
            schemaName: auth
            tableName: users
      rollback:
        - dropTable:
            tableName: users
            schemaName: auth
```

---

## Testing Migrations

### Local Testing

**Test in development environment:**

```bash
# 1. Create fresh database from backup
createdb -h localhost -U postgres gym_db_test
psql -h localhost -U postgres -d gym_db_test < /backup/gym_db_schema.sql

# 2. Run migration
flyway migrate -configFiles=conf/flyway.conf

# 3. Verify schema
psql -h localhost -U postgres -d gym_db_test << EOF
SELECT * FROM information_schema.tables
WHERE table_schema NOT IN ('pg_catalog', 'information_schema');
EOF

# 4. Verify data (if data migration)
SELECT COUNT(*) FROM auth.users;
SELECT COUNT(*) FROM training.trainers;

# 5. Test rollback
flyway undo
```

### Staging Testing

**Test full migration in staging (mirror of production):**

```bash
#!/bin/bash
# File: scripts/test-migration.sh

STAGING_DB="gym_db_staging"
LOG_FILE="/var/log/migration_test_$(date +%Y%m%d_%H%M%S).log"

log_info() {
    echo "[INFO] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo "[ERROR] $1" | tee -a "$LOG_FILE"
    exit 1
}

# 1. Backup current schema
log_info "Creating pre-migration backup..."
pg_dump -h staging-db -U postgres "$STAGING_DB" > "/tmp/${STAGING_DB}_pre_migration.sql" 2>&1 || log_error "Backup failed"

# 2. Apply migration
log_info "Applying migration..."
flyway migrate -configFiles=conf/flyway-staging.conf >> "$LOG_FILE" 2>&1 || log_error "Migration failed"

# 3. Validate schema
log_info "Validating schema..."
psql -h staging-db -U postgres -d "$STAGING_DB" << EOF >> "$LOG_FILE" 2>&1
-- Validate table count
SELECT COUNT(*) as table_count FROM information_schema.tables
WHERE table_schema NOT IN ('pg_catalog', 'information_schema');

-- Validate new tables/columns exist
\d auth.users
\d training.trainers

-- Validate indexes
SELECT * FROM pg_stat_user_indexes;
EOF

if [ $? -ne 0 ]; then
    log_error "Schema validation failed"
fi

# 4. Validate data integrity
log_info "Validating data integrity..."
psql -h staging-db -U postgres -d "$STAGING_DB" << EOF >> "$LOG_FILE" 2>&1
-- Check for constraint violations
SELECT COUNT(*) FROM auth.users WHERE email IS NULL;
SELECT COUNT(*) FROM training.trainers WHERE user_id IS NULL;
EOF

# 5. Run application tests
log_info "Running application integration tests..."
cd /app
./mvnw test -Dspring.datasource.url=jdbc:postgresql://staging-db:5432/$STAGING_DB \
    >> "$LOG_FILE" 2>&1 || log_error "Application tests failed"

# 6. Performance test
log_info "Running performance tests..."
psql -h staging-db -U postgres -d "$STAGING_DB" << EOF >> "$LOG_FILE" 2>&1
-- Test query performance on new schema
EXPLAIN ANALYZE SELECT * FROM auth.users WHERE email = 'test@example.com';
EXPLAIN ANALYZE SELECT * FROM training.trainers WHERE user_id = 'test-uuid';
EOF

log_info "Migration testing completed successfully!"
```

---

## Production Deployment

### Pre-Deployment

**Final checks (1 hour before):**

```bash
#!/bin/bash
# File: scripts/pre-migration-check.sh

DB_HOST="10.0.1.50"
DB_PORT="5432"
DB_NAME="gym_db"

echo "=== PRE-MIGRATION CHECKS ==="

# 1. Check connection
echo -n "1. Database connectivity... "
psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" -c "SELECT 1;" > /dev/null && echo "✓" || echo "✗"

# 2. Check disk space
echo -n "2. Disk space... "
DISK_USED=$(df /var/lib/postgresql | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USED" -lt 80 ]; then
    echo "✓ ($DISK_USED% used)"
else
    echo "✗ ($DISK_USED% used - critical!)"
    exit 1
fi

# 3. Check active connections
echo -n "3. Active connections... "
CONN_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -d "$DB_NAME" -t -c "SELECT count(*) FROM pg_stat_activity WHERE state != 'idle';")
echo "$CONN_COUNT"
if [ "$CONN_COUNT" -gt 10 ]; then
    echo "   ⚠ Warning: $CONN_COUNT active connections"
fi

# 4. Check recent errors in logs
echo -n "4. Recent errors in logs... "
ERROR_COUNT=$(tail -1000 /var/log/postgresql/postgresql.log | grep -c "ERROR")
echo "$ERROR_COUNT errors"

# 5. Verify backup
echo -n "5. Latest backup status... "
LATEST_BACKUP=$(ls -t /var/lib/postgresql/backups/gym_db_full_*.tar.gz 2>/dev/null | head -1)
if [ -f "$LATEST_BACKUP" ]; then
    BACKUP_AGE=$(($(date +%s) - $(stat -c%Y "$LATEST_BACKUP")))
    if [ "$BACKUP_AGE" -lt 3600 ]; then
        echo "✓ ($(($BACKUP_AGE / 60)) minutes old)"
    else
        echo "⚠ ($(($BACKUP_AGE / 3600)) hours old)"
    fi
else
    echo "✗ (No backup found)"
    exit 1
fi

# 6. Test migration script
echo -n "6. Migration script syntax... "
flyway validate -configFiles=conf/flyway.conf > /dev/null && echo "✓" || echo "✗"

echo "=== PRE-MIGRATION CHECKS COMPLETE ==="
```

### Execute Migration

**Production migration procedure:**

```bash
#!/bin/bash
# File: scripts/run-production-migration.sh

set -euo pipefail

MIGRATION_START=$(date +%s)
LOG_FILE="/var/log/postgresql/migration_$(date +%Y%m%d_%H%M%S).log"

log_info() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [ERROR] $1" | tee -a "$LOG_FILE"
    # Trigger rollback here if needed
    exit 1
}

log_success() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [SUCCESS] $1" | tee -a "$LOG_FILE"
}

# 1. Final backup
log_info "Creating final backup before migration..."
pg_basebackup \
  --pgdata=/var/lib/postgresql/backup_pre_migration \
  --format=tar \
  --gzip \
  --verbose \
  --progress \
  --label="pre_migration_$(date +%Y%m%d_%H%M%S)" \
  >> "$LOG_FILE" 2>&1 || log_error "Final backup failed"

# 2. Disable automated backups
log_info "Disabling automated backups..."
systemctl stop postgresql-backup.timer >> "$LOG_FILE" 2>&1 || log_error "Failed to stop backup timer"

# 3. Record baseline
log_info "Recording schema baseline..."
pg_dump -s > /tmp/schema_pre_migration.sql 2>&1 || log_error "Schema dump failed"

# 4. Execute migration
log_info "Starting migration..."
flyway migrate -configFiles=conf/flyway.conf >> "$LOG_FILE" 2>&1 || log_error "Migration failed"

# 5. Validate migration success
log_info "Validating migration..."
psql -d gym_db << EOF >> "$LOG_FILE" 2>&1
-- Check schema migration table
SELECT * FROM schema_migrations ORDER BY version DESC LIMIT 5;

-- Validate new objects
\dt
\di

-- Check for errors
SELECT * FROM pg_stat_activity WHERE state = 'disabled' OR wait_event IS NOT NULL;
EOF

if [ $? -ne 0 ]; then
    log_error "Migration validation failed"
fi

# 6. Re-enable automated backups
log_info "Re-enabling automated backups..."
systemctl start postgresql-backup.timer >> "$LOG_FILE" 2>&1 || log_error "Failed to start backup timer"

# 7. Calculate duration
MIGRATION_END=$(date +%s)
DURATION=$((MIGRATION_END - MIGRATION_START))

log_success "Migration completed successfully in ${DURATION} seconds"
log_success "Migration log saved to: $LOG_FILE"
```

### Post-Deployment Validation

**Verify production migration:**

```sql
-- 1. Check migration history
SELECT * FROM schema_migrations ORDER BY version DESC LIMIT 10;

-- 2. Validate schema consistency
SELECT COUNT(*) FROM information_schema.tables
WHERE table_schema NOT IN ('pg_catalog', 'information_schema', 'pg_toast');

-- 3. Check for errors
SELECT * FROM pg_stat_activity WHERE state != 'idle';

-- 4. Verify indexes are present
SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0 LIMIT 10;

-- 5. Test key queries
EXPLAIN ANALYZE SELECT * FROM auth.users WHERE email = 'test@example.com';

-- 6. Monitor replication lag (if applicable)
SELECT 
    pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) as replication_lag_bytes,
    EXTRACT(EPOCH FROM (NOW() - pg_last_xact_replay_timestamp())) as replication_lag_seconds;
```

---

## Rollback Procedures

### Rollback Planning

**Rollback decision tree:**

```
Migration failed?
├─ YES
│  ├─ Is database still responsive?
│  │  ├─ YES: Attempt Flyway undo
│  │  └─ NO: Restore from backup
│  └─ Notify team, start investigation
│
└─ NO
   ├─ Is application working?
   │  ├─ YES: Continue monitoring
   │  └─ NO: Determine cause
   │     ├─ Data issue: Execute data rollback
   │     ├─ Schema issue: Flyway undo or manual revert
   │     └─ Compatibility issue: Deploy previous app version
```

### Flyway Undo

```bash
# Note: Requires Flyway Pro

# 1. List available undo migrations
flyway info

# 2. Undo last migration
flyway undo

# 3. Undo to specific version (requires manual undo SQL)
# Edit undo migration and execute manually
psql -d gym_db -f sql/migrations/U002__undo_create_users_table.sql
```

### Restore from Backup

```bash
#!/bin/bash
# File: scripts/rollback-from-backup.sh

set -euo pipefail

BACKUP_FILE="/var/lib/postgresql/backup_pre_migration/base.tar.gz"
RESTORE_DATE=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/postgresql/rollback_${RESTORE_DATE}.log"

log_info() {
    echo "[INFO] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo "[ERROR] $1" | tee -a "$LOG_FILE"
    exit 1
}

# 1. Stop PostgreSQL
log_info "Stopping PostgreSQL..."
systemctl stop postgresql || log_error "Failed to stop PostgreSQL"

# 2. Backup current data directory (for analysis)
log_info "Backing up failed migration state..."
mv /var/lib/postgresql/14/main "/var/lib/postgresql/14/main_failed_migration_${RESTORE_DATE}" \
    || log_error "Failed to backup data directory"

# 3. Extract backup
log_info "Extracting backup to data directory..."
mkdir -p /var/lib/postgresql/14/main
cd /tmp
tar -xzf "$BACKUP_FILE" -C /var/lib/postgresql/14/main/ \
    || log_error "Failed to extract backup"

# 4. Fix permissions
log_info "Fixing directory permissions..."
chown -R postgres:postgres /var/lib/postgresql/14/main
chmod 700 /var/lib/postgresql/14/main

# 5. Start PostgreSQL
log_info "Starting PostgreSQL..."
systemctl start postgresql || log_error "Failed to start PostgreSQL"

# 6. Verify restore
log_info "Verifying restore..."
sleep 5
psql -U postgres -c "SELECT version();" || log_error "Restore verification failed"

log_info "Rollback completed successfully"
log_info "Failed migration state saved to: /var/lib/postgresql/14/main_failed_migration_${RESTORE_DATE}"
log_info "Review migration failure and create updated migration script"
```

---

## Data Migrations

### Data Transformation Patterns

**Pattern 1: Backfill new column**

```sql
-- Migration: Add column with default
ALTER TABLE auth.users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

-- Data migration: Backfill from audit table
UPDATE auth.users u SET
  last_login_at = (
    SELECT MAX(event_timestamp)
    FROM common.audit_events
    WHERE entity_type = 'user'
    AND entity_id = u.id
    AND event_type = 'login'
  )
WHERE last_login_at IS NULL;

-- Add constraint after data validation
ALTER TABLE auth.users ALTER COLUMN last_login_at SET NOT NULL;
```

**Pattern 2: Data normalization**

```sql
-- Before: Email stored in multiple formats
SELECT * FROM auth.users WHERE email LIKE '%Example%';

-- Migration: Create update script
UPDATE auth.users SET
  email = LOWER(TRIM(email))
WHERE email != LOWER(TRIM(email));

-- Verify changes
SELECT DISTINCT email FROM auth.users ORDER BY email;
```

**Pattern 3: Denormalization for performance**

```sql
-- Add denormalized column
ALTER TABLE training.training_plans ADD COLUMN total_exercises INTEGER DEFAULT 0;

-- Backfill counts
UPDATE training.training_plans tp SET
  total_exercises = (
    SELECT COUNT(*)
    FROM training.plan_exercises
    WHERE plan_id = tp.id
  );

-- Keep synchronized via trigger
CREATE OR REPLACE FUNCTION update_plan_exercise_count()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE training.training_plans SET
    total_exercises = (
      SELECT COUNT(*) FROM training.plan_exercises
      WHERE plan_id = NEW.plan_id
    )
  WHERE id = NEW.plan_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_plan_exercises_count
AFTER INSERT OR DELETE ON training.plan_exercises
FOR EACH ROW EXECUTE FUNCTION update_plan_exercise_count();
```

---

## Zero-Downtime Migrations

### Strategy for High-Availability

**Goal: Minimize service interruption during schema changes**

**Step 1: Add column with default**

```sql
-- Backward compatible: Application ignores new column
ALTER TABLE auth.users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- Application still works (column is optional)
```

**Step 2: Deploy application update**

```java
// Application code updated to use new column
@Column
private LocalDateTime lastLoginAt;

// Hibernate automatically handles new column
// Backward compatible: If null, uses default behavior
```

**Step 3: Backfill data**

```sql
-- Run during off-peak or with low-impact backfill
UPDATE auth.users SET
  last_login_at = (SELECT MAX(event_timestamp) FROM audit_events ...)
WHERE last_login_at IS NULL;
```

**Step 4: Add constraints (if needed)**

```sql
-- After data is backfilled and application updated
ALTER TABLE auth.users ALTER COLUMN last_login_at SET NOT NULL;
CREATE INDEX idx_users_last_login ON auth.users(last_login_at DESC);
```

### Example: Rename Column (Zero-Downtime)

**Instead of direct rename:**

```sql
-- BAD: Requires application downtime
ALTER TABLE auth.users RENAME COLUMN phone TO phone_number;
```

**Better approach:**

```sql
-- Step 1: Create new column (backward compatible)
ALTER TABLE auth.users ADD COLUMN phone_number VARCHAR(20);

-- Step 2: Create bidirectional triggers
CREATE OR REPLACE FUNCTION sync_phone_to_phone_number()
RETURNS TRIGGER AS $$
BEGIN
  NEW.phone_number := NEW.phone;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_phone_sync BEFORE INSERT OR UPDATE ON auth.users
FOR EACH ROW EXECUTE FUNCTION sync_phone_to_phone_number();

-- Step 3: Deploy updated application
-- Application uses phone_number, trigger keeps phone synchronized

-- Step 4: Backfill existing data
UPDATE auth.users SET phone_number = phone;

-- Step 5: Drop old column (after application fully migrated)
ALTER TABLE auth.users DROP COLUMN phone;
ALTER TABLE auth.users RENAME COLUMN phone_number TO phone;
```

---

## Troubleshooting

### Migration Failures

**Issue: Migration locks table too long**

```bash
# Check long-running queries
psql -c "SELECT * FROM pg_stat_activity WHERE state != 'idle' ORDER BY query_start;"

# Cancel long-running query (if safe)
SELECT pg_terminate_backend(pid);

# Use CONCURRENTLY for indexes
CREATE INDEX CONCURRENTLY idx_new_column ON table(new_column);

# Add LOCK TIMEOUT
SET lock_timeout = '30 seconds';
ALTER TABLE auth.users ADD COLUMN new_col VARCHAR(100);
```

**Issue: Rollback fails**

```bash
# Check rollback migration syntax
psql -d gym_db -f sql/migrations/U002__undo.sql --echo-all

# Manually execute rollback if migration corrupted
psql -d gym_db << EOF
-- Manually revert schema
DROP TABLE IF EXISTS new_table;
DROP INDEX IF EXISTS idx_new_index;

-- Update migration history
DELETE FROM schema_migrations WHERE version = 2;
EOF
```

**Issue: Data migration too slow**

```sql
-- Disable unnecessary triggers
ALTER TABLE tracking.workouts DISABLE TRIGGER ALL;

-- Perform bulk operations
UPDATE tracking.workouts SET status = 'completed' WHERE created_at < NOW() - INTERVAL '1 year';

-- Re-enable triggers
ALTER TABLE tracking.workouts ENABLE TRIGGER ALL;

-- Analyze table statistics
ANALYZE tracking.workouts;
```

---

## Best Practices

### Migration Safety

1. **Always test in staging first**
   - Use production data volume
   - Run with production traffic patterns
   - Verify rollback procedure

2. **Keep migrations small and focused**
   - One logical change per migration
   - Easier to understand and review
   - Simpler to rollback if needed

3. **Use version control**
   - Commit migration scripts with application code
   - Tie migration to feature branch
   - Track who created migration and when

4. **Document all migrations**
   ```sql
   -- Clear description of what is changing
   -- Why this change is needed
   -- Impact on application
   -- Rollback procedure (if not automatic)
   -- Estimated duration
   ```

5. **Use transactions wisely**
   ```sql
   -- Most DDL can be wrapped in transactions
   BEGIN;
   ALTER TABLE auth.users ADD COLUMN new_col INTEGER;
   CREATE INDEX idx_new_col ON auth.users(new_col);
   COMMIT;
   -- If any statement fails, entire transaction rolls back
   ```

### Deployment Best Practices

1. **Run during maintenance window**
   - Notify users in advance
   - Schedule during low-traffic period
   - Have on-call team available

2. **Monitor during deployment**
   - Watch query logs for errors
   - Monitor connection count
   - Check replication lag

3. **Have rollback ready**
   - Test rollback procedure beforehand
   - Document rollback decision criteria
   - Have rollback script ready to execute

4. **Verify after deployment**
   - Run integration tests
   - Verify data integrity
   - Monitor for unusual errors
   - Check performance metrics

---

**Related Documentation:**
- [01-database-overview.md](01-database-overview.md) - Database architecture
- [02-schema-design.md](02-schema-design.md) - Current schema structure
- [03-backup-recovery.md](03-backup-recovery.md) - Backup procedures
- [04-performance-tuning.md](04-performance-tuning.md) - Performance considerations
- See [docs/deployment/](../deployment/) for application deployment
