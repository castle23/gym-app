# Operations Documentation

This section contains operational runbooks and standard operating procedures for running the Gym Platform API in production.

## Contents

- **01-operational-runbook.md** - Complete operational runbook for production management
- **02-monitoring.md** - Service monitoring and alerting setup
- **03-logging.md** - Log aggregation and analysis
- **04-backup-recovery.md** - Backup and disaster recovery procedures
- **05-performance-tuning.md** - Performance optimization
- **06-incident-management.md** - Incident response procedures
- **07-maintenance.md** - Scheduled maintenance procedures
- **08-updates-patches.md** - Updates and patch management

## Subdirectories

- **sops/** - Standard Operating Procedures
- **diagnostic-tools/** - Diagnostic and troubleshooting tools

## Quick Reference

### Service Status
```bash
./scripts/operational/health-check.sh
```

### Running Services
- API Gateway: `http://localhost:8080`
- Auth Service: `http://localhost:8081`
- Training Service: `http://localhost:8082`
- Tracking Service: `http://localhost:8083`
- Notification Service: `http://localhost:8084`
- PostgreSQL: `localhost:5432`

### Common Tasks

**Check Service Health**
See `01-operational-runbook.md` for detailed health check procedures.

**View Logs**
See `03-logging.md` for log aggregation and viewing procedures.

**Perform Backup**
See `04-backup-recovery.md` for backup procedures.

**Scale Services**
See `05-performance-tuning.md` for scaling instructions.

## Monitoring

Service monitoring includes:
- Health check endpoints
- Log monitoring
- Database monitoring
- Performance metrics
- Alerting rules

See `02-monitoring.md` for setup details.

## Incident Response

In case of an issue:
1. Check [Troubleshooting Guide](../troubleshooting/)
2. Review incident procedures in `06-incident-management.md`
3. Follow service recovery steps
4. Document incident for post-mortem

## Scheduled Maintenance

Regular maintenance tasks:
- Daily: Health checks, log rotation
- Weekly: Performance analysis, backup verification
- Monthly: Patch updates, capacity planning
- Quarterly: Major version updates, disaster recovery drills

See `07-maintenance.md` for detailed schedules.

## Documentation Updates

Keep operational procedures current:
1. Review procedures quarterly
2. Update based on lessons learned
3. Add new procedures as needed
4. Document any manual interventions

## Support & Escalation

For operational issues:
1. Check `01-operational-runbook.md`
2. Review incident response in `06-incident-management.md`
3. Consult [Troubleshooting Guide](../troubleshooting/)
4. Escalate to appropriate team

## For More Information

- **Deployment**: See [Deployment Guide](../deployment/)
- **Troubleshooting**: See [Troubleshooting Guide](../troubleshooting/)
- **Monitoring Scripts**: See [Scripts](../../scripts/)
