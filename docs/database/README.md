# Database Documentation

This section contains database schema, maintenance procedures, and DBA reference documentation.

## Contents

- **01-database-overview.md** - Database architecture and overview
- **02-schema-design.md** - Database schema and data models
- **03-backup-recovery.md** - Backup and recovery procedures
- **04-performance-tuning.md** - Query optimization and performance
- **05-migration-guide.md** - Database migration procedures
- **06-maintenance-procedures.md** - Routine maintenance tasks

## Subdirectories

- **queries/** - SQL queries and useful commands
- **maintenance/** - Maintenance scripts and tools

## Database Information

**Type**: PostgreSQL
**Port**: 5432
**Connection**: `postgresql://localhost:5432/gym_db`

### Schemas

- `auth_schema` - Authentication and user data
- `training_schema` - Training programs and data
- `tracking_schema` - Progress tracking data
- `notification_schema` - Notification queue and history

## Quick Reference

### Connect to Database
```bash
psql -U postgres -d gym_db -h localhost
```

### Check Database Status
See **01-database-overview.md** for connection and status verification.

### Common Queries
See **queries/** directory for frequently used SQL queries.

### Backups
See **03-backup-recovery.md** for backup procedures and scripts.

## For DBAs

This documentation is tailored for database administrators:
1. Start with **01-database-overview.md** for context
2. Review **02-schema-design.md** to understand data structures
3. Use **06-maintenance-procedures.md** for regular tasks
4. Refer to **queries/** for common operations

### Key Responsibilities

- Database backups (see **03-backup-recovery.md**)
- Performance monitoring (see **04-performance-tuning.md**)
- Schema migrations (see **05-migration-guide.md**)
- Maintenance tasks (see **06-maintenance-procedures.md**)

## For Developers

Developers should review:
- **02-schema-design.md** - Understanding data models
- **queries/** - Common database queries
- See [API Documentation](../api/) for how services use the database

## Schema Overview

The database consists of 4 logical schemas:

| Schema | Purpose | Tables |
|--------|---------|--------|
| auth_schema | User auth and permissions | users, roles, permissions |
| training_schema | Training programs | programs, exercises, workouts |
| tracking_schema | Progress tracking | workout_logs, metrics, analytics |
| notification_schema | Notifications | notification_queue, notification_history |

See **02-schema-design.md** for detailed schema diagrams.

## Connection Strings

### Development
```
jdbc:postgresql://localhost:5432/gym_db?user=postgres&password=password
```

### Production
See environment variables in deployment configuration.

## Performance Considerations

See **04-performance-tuning.md** for:
- Query optimization
- Index strategies
- Connection pooling
- Caching strategies

## Backup Strategy

Regular backups are critical:
- **Daily**: Full database backup
- **Hourly**: Transaction logs (for point-in-time recovery)
- **Weekly**: Backup verification and testing

See **03-backup-recovery.md** for procedures.

## Disaster Recovery

Recovery procedures:
1. Restore from latest backup
2. Apply transaction logs up to point-in-time
3. Verify data integrity
4. Resume services

See **03-backup-recovery.md** for detailed procedures.

## Monitoring & Alerts

Key metrics to monitor:
- Connection count
- Query performance
- Disk space usage
- Cache hit ratio
- Lock contention

See [Operations Runbook](../operations/) for monitoring setup.

## For More Information

- **Operations**: See [Operations Runbook](../operations/)
- **DBA Tools**: See [DBA Documentation](../../dba/)
- **Architecture**: See [Architecture Documentation](../arquitectura/)
- **Troubleshooting**: See [Troubleshooting Guide](../troubleshooting/)
