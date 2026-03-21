# Scripts

This directory contains executable scripts organized by purpose.

## Quick Navigation

- **[operational/](operational/)** - Deployment, health checks, and production management
- **[development/](development/)** - Build, test, and development utilities
- **[database/](database/)** - Database maintenance and migration scripts
- **[monitoring/](monitoring/)** - Service monitoring and alerting scripts

## Available Scripts

### Operational Scripts

| Script | Purpose |
|--------|---------|
| `operational/deploy-production.sh` | Automated production deployment |
| `operational/health-check.sh` | Verify service health status |
| `operational/run_postman_tests.sh` | Run API test suite |

### Development Scripts

Development tools for local development (currently empty, add as needed):
- Build scripts
- Test runners
- Local environment setup

### Database Scripts

Database management scripts (currently empty, add as needed):
- Schema migrations
- Data backups
- Maintenance tasks

### Monitoring Scripts

Monitoring utilities (currently empty, add as needed):
- Performance monitoring
- Log aggregation
- Alerting

## Running Scripts

All scripts use proper shell headers and can be executed directly:

```bash
# Make script executable (if not already)
chmod +x scripts/operational/deploy-production.sh

# Run the script
./scripts/operational/deploy-production.sh
```

## Script Development Standards

When creating new scripts:
1. Add proper shell header: `#!/bin/bash`
2. Set error handling: `set -e`
3. Add documentation at the top
4. Include usage examples
5. Use meaningful variable names
6. Add error messages and logging
7. Handle edge cases
8. Place in appropriate subdirectory

Example:
```bash
#!/bin/bash
# Script: my-script.sh
# Purpose: Brief description
# Usage: ./my-script.sh [options]

set -e

echo "Starting task..."
# Script logic here
```

## For More Information

- **Deployment**: See [Deployment Documentation](../docs/deployment/)
- **Operations**: See [Operations Runbook](../docs/operations/)
- **Development**: See [Development Documentation](../docs/development/)
