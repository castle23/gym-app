# Troubleshooting & Debugging

This section contains troubleshooting guides, debugging techniques, and solutions for common issues.

## Contents

- **01-troubleshooting-guide.md** - Comprehensive troubleshooting guide
- **02-debugging-techniques.md** - Debugging strategies and tools
- **03-common-issues.md** - Common problems and solutions
- **04-diagnostic-procedures.md** - System diagnostic procedures
- **05-performance-debugging.md** - Performance issue diagnosis
- **06-security-troubleshooting.md** - Security-related issues
- **07-database-troubleshooting.md** - Database issues
- **08-network-troubleshooting.md** - Network and connectivity issues
- **09-deployment-troubleshooting.md** - Deployment-related issues

## Subdirectories

- **faqs/** - Frequently asked questions and answers
- **diagnostic-tools/** - Diagnostic utilities and tools

## Quick Troubleshooting

### Service Won't Start
1. Check [Troubleshooting Guide](01-troubleshooting-guide.md)
2. Verify environment variables
3. Check port availability
4. Review service logs

### API Returns 401/403
See **06-security-troubleshooting.md** for:
- Authentication issues
- RBAC permission problems
- JWT token expiration

### Database Connection Failed
See **07-database-troubleshooting.md** for:
- Connection string issues
- Database availability
- Credential problems

### Performance Issues
See **05-performance-debugging.md** for:
- Slow query diagnosis
- Memory usage analysis
- CPU profiling

### Network Issues
See **08-network-troubleshooting.md** for:
- Service communication problems
- DNS resolution issues
- Firewall configuration

## Diagnostic Procedures

Quick diagnostic steps:

```bash
# 1. Check service health
./scripts/operational/health-check.sh

# 2. Check service logs (Docker)
docker logs <service-name>

# 3. Check database connectivity
psql -U postgres -d gym_db -h localhost

# 4. Test API endpoint
curl http://localhost:8081/swagger-ui.html
```

See **04-diagnostic-procedures.md** for detailed procedures.

## Common Issues & Solutions

Quick reference for common problems:

| Issue | Cause | Solution |
|-------|-------|----------|
| `Connection refused` | Service not running | Start service, check port |
| `401 Unauthorized` | Invalid token | Re-authenticate, check token |
| `403 Forbidden` | Insufficient permissions | Check role/permissions |
| `500 Internal Server Error` | Server error | Check logs, see Error Handling |
| `Timeout` | Slow query/network | See Performance/Network guides |

See **03-common-issues.md** for more examples.

## FAQ

See **faqs/** directory for frequently asked questions and answers.

## Debugging Tools

Available diagnostic tools in **diagnostic-tools/**:
- Log analysis scripts
- Performance profiling tools
- Database query analyzers
- Network diagnostic utilities

## Logging

To increase logging verbosity for debugging:

1. Set log level to DEBUG in configuration
2. Enable Spring Boot debug logging: `--debug`
3. Check log files for error messages
4. See **01-troubleshooting-guide.md** for log configuration

## Getting Help

If you can't resolve an issue:

1. Check [01-troubleshooting-guide.md](01-troubleshooting-guide.md)
2. Search [faqs/](faqs/) for similar questions
3. Review relevant documentation section
4. Check service logs
5. Consult with team leads or documentation maintainers

## Escalation

For escalation, provide:
1. **What** - What you were trying to do
2. **When** - When the issue occurred
3. **Error** - Exact error messages
4. **Steps** - How to reproduce
5. **Logs** - Relevant log excerpts
6. **Environment** - Dev/Test/Prod, configuration

## Related Documentation

- **Operations**: See [Operations Runbook](../operations/)
- **Development**: See [Development Documentation](../development/)
- **Deployment**: See [Deployment Guide](../deployment/)
- **Database**: See [Database Documentation](../database/)
- **API**: See [API Documentation](../api/)
