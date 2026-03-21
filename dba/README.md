# DBA (Database Administrator) Resources

This section contains database administration tools, procedures, and reference materials specifically for DBAs.

## Contents

- **01-getting-started.md** - DBA quick start guide
- **02-database-architecture.md** - Database architecture overview
- **03-backup-recovery.md** - Backup and disaster recovery procedures
- **04-performance-tuning.md** - Performance optimization
- **05-maintenance-procedures.md** - Regular maintenance tasks
- **06-user-access-management.md** - Database user and permission management
- **07-monitoring-alerting.md** - Database monitoring setup
- **08-troubleshooting.md** - Database troubleshooting guide

## Subdirectories

- **procedures/** - Step-by-step procedures and runbooks
- **queries/** - Useful SQL queries and scripts
- **scripts/** - Database maintenance and automation scripts
- **maintenance/** - Maintenance tools and utilities

## Key Responsibilities

As a DBA for the Gym Platform:
1. Database availability and performance
2. Backup and disaster recovery
3. User access management
4. Schema maintenance and migrations
5. Performance monitoring and tuning
6. Security and compliance

## Quick Reference

### Database Connection
```bash
psql -U postgres -d gym_db -h localhost
```

### Database Credentials
See environment configuration (`.env` file).

### Database Port
PostgreSQL runs on port 5432.

### Current Status
- Database: `gym_db`
- Schemas: auth_schema, training_schema, tracking_schema, notification_schema
- Data: Production-ready

## Core Databases & Schemas

| Database | Schema | Purpose |
|----------|--------|---------|
| gym_db | auth_schema | Authentication & users |
| gym_db | training_schema | Training programs |
| gym_db | tracking_schema | Progress tracking |
| gym_db | notification_schema | Notifications |

## Common Tasks

### Daily
- Check database health (see **01-getting-started.md**)
- Verify backups completed
- Monitor log files
- Check storage space

### Weekly
- Analyze query performance
- Review slow queries
- Update table statistics
- Verify backup integrity

### Monthly
- Capacity planning
- Security audit
- Patch management
- Performance report

### Quarterly
- Full disaster recovery test
- Schema review and optimization
- Access control audit
- Compliance verification

## Backup & Recovery

Critical for business continuity:

**Backup Strategy**:
- Daily full backups
- Hourly transaction logs
- Off-site backup copies

**Recovery Time Objective (RTO)**: < 1 hour
**Recovery Point Objective (RPO)**: < 15 minutes

See **03-backup-recovery.md** for procedures.

## Performance Tuning

Key metrics to monitor:
- Query execution time
- Index usage
- Connection count
- Cache hit ratio
- Disk I/O

See **04-performance-tuning.md** for optimization techniques.

## User Access Management

Managing database users:
- Create and remove accounts
- Grant/revoke permissions
- Audit access logs
- Security best practices

See **06-user-access-management.md** for procedures.

## Useful Queries

Common database queries available in **queries/**:
- System information
- Table statistics
- Performance analysis
- User management
- Backup verification

## Monitoring

Set up alerts for:
- High CPU usage
- Disk space warnings
- Connection limits
- Slow queries
- Replication lag

See **07-monitoring-alerting.md** for setup.

## Disaster Recovery Plan

Recovery procedures in order:
1. Assess damage and impact
2. Prepare recovery environment
3. Restore from latest backup
4. Apply transaction logs
5. Verify data integrity
6. Resume services
7. Post-incident analysis

See **03-backup-recovery.md** for detailed procedures.

## Maintenance Windows

Recommended maintenance windows:
- Off-peak hours for backups
- Low-traffic periods for maintenance
- Scheduled downtime for major updates
- Notification to users in advance

See **05-maintenance-procedures.md** for scheduling.

## Tools & Utilities

Available DBA tools:
- pgAdmin - Web-based admin interface
- pg_stat_statements - Query performance analysis
- pg_upgrade - Major version upgrades
- pg_dump/pg_restore - Backup utilities

## Security

DBA security responsibilities:
- Secure access to database
- Encrypt sensitive data
- Audit access logs
- Manage user permissions
- Regular security updates

## For More Information

- **Database Documentation**: See [Database Documentation](../docs/database/)
- **Operations**: See [Operations Runbook](../docs/operations/)
- **Troubleshooting**: See [Troubleshooting Guide](../docs/troubleshooting/)
- **Architecture**: See [Architecture Documentation](../docs/arquitectura/)
