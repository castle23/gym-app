# Database Initialization Guide

This guide explains the database initialization scripts and how to use them.

## Overview

The database initialization scripts set up a complete, production-ready PostgreSQL database for the Gym Platform API. They create schemas, tables, indexes, and seed initial data for all four microservices.

## Directory Structure

```
dba/initialization/
├── schemas/
│   └── 01-init-schemas.sql          # Schema and table definitions
└── data/
    ├── 01-init-auth-data.sql        # Auth service initial data
    ├── 02-init-training-data.sql    # Training service data
    ├── 03-init-tracking-data.sql    # Tracking service data
    └── 04-init-notification-data.sql # Notification service data
```

## Scripts Overview

### 01-init-schemas.sql (Schemas)
**Size**: ~13 KB
**Purpose**: Creates all database schemas and table structures

**Schemas Created**:
- `auth_schema` - Authentication, users, roles, permissions
- `training_schema` - Training programs, exercises, workouts
- `tracking_schema` - Progress tracking, metrics, analytics
- `notification_schema` - Notifications, message queue, history

**Tables Created**: ~20+ tables across all schemas
**Includes**: Indexes, foreign keys, constraints, triggers

### 01-init-auth-data.sql (Auth Service)
**Size**: ~5.9 KB
**Purpose**: Seeds initial authentication and user data

**Data Includes**:
- Default admin user and roles
- RBAC roles: ADMIN, MANAGER, USER, TRAINER
- Initial permissions for each role
- User accounts for testing

### 02-init-training-data.sql (Training Service)
**Size**: ~11 KB
**Purpose**: Seeds training program templates and data

**Data Includes**:
- Sample training programs (strength, cardio, flexibility)
- Exercise definitions with proper form descriptions
- Workout templates
- Training schedules

### 03-init-tracking-data.sql (Tracking Service)
**Size**: ~9.2 KB
**Purpose**: Seeds tracking and metrics data

**Data Includes**:
- Sample workout logs
- Performance metrics
- Progress tracking data
- Analytics snapshots

### 04-init-notification-data.sql (Notification Service)
**Size**: ~5.0 KB
**Purpose**: Sets up notification templates and configuration

**Data Includes**:
- Notification templates (email, SMS, push)
- Notification preferences
- Message queue initial state
- Event notification configuration

## Execution Order

**Critical**: Scripts must be executed in this exact order:

```bash
1. 01-init-schemas.sql      # Create structure
2. 01-init-auth-data.sql    # Add users and roles
3. 02-init-training-data.sql # Add programs
4. 03-init-tracking-data.sql # Add metrics
5. 04-init-notification-data.sql # Add notifications
```

Why this order?
- `auth_schema` first: Creates users and roles that other services reference
- Schema tables must exist before inserting data
- Foreign key constraints require referenced tables

## How to Run

### Option 1: Docker Compose (Automatic)
```bash
docker-compose up -d
# Automatically runs all scripts in correct order
```

The `docker-compose.yml` references these scripts via volume mounts.

### Option 2: Manual Execution

**Connect to Database**:
```bash
psql -U postgres -h localhost -d gym_db
```

**Run Each Script**:
```bash
psql -U postgres -h localhost -d gym_db -f dba/initialization/schemas/01-init-schemas.sql
psql -U postgres -h localhost -d gym_db -f dba/initialization/data/01-init-auth-data.sql
psql -U postgres -h localhost -d gym_db -f dba/initialization/data/02-init-training-data.sql
psql -U postgres -h localhost -d gym_db -f dba/initialization/data/03-init-tracking-data.sql
psql -U postgres -h localhost -d gym_db -f dba/initialization/data/04-init-notification-data.sql
```

### Option 3: From SQL Shell
```sql
\i dba/initialization/schemas/01-init-schemas.sql
\i dba/initialization/data/01-init-auth-data.sql
\i dba/initialization/data/02-init-training-data.sql
\i dba/initialization/data/03-init-tracking-data.sql
\i dba/initialization/data/04-init-notification-data.sql
```

## Database Credentials

**Default Credentials** (see `.env`):
```
Host: localhost
Port: 5432
Database: gym_db
Username: postgres
Password: [see .env file]
```

**Connection String**:
```
postgresql://postgres:password@localhost:5432/gym_db
```

## Verification

After initialization, verify the setup:

### Check Schemas
```sql
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('auth_schema', 'training_schema', 'tracking_schema', 'notification_schema');
```

### Check Tables
```sql
SELECT table_name, table_schema 
FROM information_schema.tables 
WHERE table_schema IN ('auth_schema', 'training_schema', 'tracking_schema', 'notification_schema');
```

### Check Data
```sql
SELECT COUNT(*) as user_count FROM auth_schema.users;
SELECT COUNT(*) as program_count FROM training_schema.programs;
SELECT COUNT(*) as log_count FROM tracking_schema.workout_logs;
```

### Test Connections
Each microservice should be able to connect and query its schema:
```bash
# Auth Service
curl http://localhost:8081/swagger-ui.html

# Training Service
curl http://localhost:8082/swagger-ui.html

# Tracking Service
curl http://localhost:8083/swagger-ui.html

# Notification Service
curl http://localhost:8084/swagger-ui.html
```

## Adding New Data

To add new initial data:

1. **Create new SQL file** in appropriate directory:
   - `dba/initialization/data/05-init-custom-data.sql`

2. **Follow naming convention**:
   - Use sequential numbering
   - Include schema prefix: `auth_schema.table_name`

3. **Add foreign key checks**:
   ```sql
   INSERT INTO auth_schema.users (...)
   SELECT * FROM ...
   WHERE ... -- ensure referenced data exists
   ```

4. **Document the changes**:
   - Update this README with new file purpose
   - Add to execution order if dependencies exist
   - Test full initialization sequence

## Troubleshooting

### Script Fails with "Schema already exists"
**Problem**: Schema was created in a previous run
**Solution**: 
```bash
# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose up -d

# Or manually drop schema:
DROP SCHEMA IF EXISTS auth_schema CASCADE;
```

### Foreign Key Constraint Fails
**Problem**: Execution order is wrong or parent data missing
**Solution**: 
- Verify scripts execute in correct order
- Check parent table has required data
- Run `01-init-schemas.sql` and `01-init-auth-data.sql` first

### Data Not Appearing
**Problem**: Script ran but data not visible
**Solution**:
```sql
-- Verify transaction committed
COMMIT;

-- Check if data exists
SELECT COUNT(*) FROM auth_schema.users;

-- View recent data
SELECT * FROM auth_schema.users LIMIT 5;
```

### Cannot Connect to Database
**Problem**: PostgreSQL not running or wrong credentials
**Solution**:
```bash
# Check PostgreSQL running
docker ps | grep postgres

# Check logs
docker logs [container_name]

# Verify credentials in .env
cat .env | grep POSTGRES
```

## Backup Before Modifying

Always backup before running initialization on production:

```bash
# Backup current database
pg_dump -U postgres -h localhost -d gym_db > backup.sql

# Later restore if needed
psql -U postgres -h localhost -d gym_db < backup.sql
```

## Performance Considerations

- Initial run takes ~30 seconds
- Scripts include indexes for performance
- Foreign keys enabled for data integrity
- Triggers may increase insert time ~5-10%

## Security Notes

- Scripts use parameterized queries (no SQL injection)
- Default passwords should be changed in production
- Credentials stored in `.env`, not in scripts
- Sensitive data encrypted where applicable
- Audit logging can be added to tables

## Next Steps

After initialization:

1. **Verify Data**: See "Verification" section above
2. **Start Services**: `docker-compose up -d`
3. **Test APIs**: Use Postman collections in `tests/`
4. **Monitor Logs**: `docker logs [service-name]`
5. **Set Up Backups**: See [Backup Guide](../docs/database/03-backup-recovery.md)

## Related Documentation

- [Database Overview](../docs/database/01-database-overview.md)
- [Database Schema](../docs/database/02-schema-design.md)
- [DBA Getting Started](01-getting-started.md)
- [Backup & Recovery](03-backup-recovery.md)
- [Docker Deployment](../docs/stack/05-deployment-docker.md)
