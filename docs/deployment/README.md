# Deployment Documentation

This section contains DevOps and deployment procedures for the Gym Platform API.

## Contents

- **01-docker-deployment-results.md** - Docker deployment results and verification
- **01-production-deployment-guide.md** - Step-by-step production deployment
- **02-deployment-strategies.md** - Different deployment approaches
- **03-health-checks.md** - Service health monitoring
- **04-scaling.md** - Horizontal and vertical scaling

## Subdirectories

- **checklists/** - Pre-deployment checklists and verification lists
- **environments/** - Environment-specific configurations

## Quick Start: Deploying to Production

1. Review the [Production Readiness Checklist](checklists/production-readiness-checklist.md)
2. Follow [Production Deployment Guide](01-production-deployment-guide.md)
3. Verify deployment with [Health Checks](03-health-checks.md)
4. Monitor with [Operations Runbook](../operations/01-operational-runbook.md)

## Deployment Status

✅ **Current**: 4 services running on ports 8081-8084
- Auth Service: `http://localhost:8081`
- Training Service: `http://localhost:8082`
- Tracking Service: `http://localhost:8083`
- Notification Service: `http://localhost:8084`

✅ **Database**: PostgreSQL running on port 5432

## Supported Environments

- **Development**: Local Docker Compose setup
- **Testing**: Containerized test environment
- **Production**: Docker Compose or Kubernetes-ready

Configuration files:
- `docker-compose.yml` - Development
- `docker-compose.prod.yml` - Production

## Key Services

| Service | Port | Role |
|---------|------|------|
| Auth Service | 8081 | Authentication & Authorization |
| Training Service | 8082 | Training Program Management |
| Tracking Service | 8083 | Progress Tracking & Analytics |
| Notification Service | 8084 | User Notifications |
| PostgreSQL | 5432 | Data Persistence |

## Deployment Scripts

Available deployment scripts in `scripts/operational/`:
- `deploy-production.sh` - Automated production deployment
- `health-check.sh` - Service health verification
- `run_postman_tests.sh` - API testing after deployment

## Pre-Deployment Requirements

1. Docker and Docker Compose installed
2. PostgreSQL backup (if upgrading)
3. Environment variables configured
4. SSL certificates (for HTTPS)
5. Database migration plan

## Post-Deployment Tasks

1. Run health checks
2. Verify API endpoints
3. Test RBAC functionality
4. Monitor logs for errors
5. Set up monitoring/alerting

See [Operations Runbook](../operations/01-operational-runbook.md) for ongoing management.

## Troubleshooting Deployment

If deployment fails:
1. Check [Troubleshooting Guide](../troubleshooting/)
2. Review deployment logs
3. Verify prerequisites
4. Consult [Production Readiness Checklist](checklists/production-readiness-checklist.md)

## For More Information

- **Architecture**: See [Architecture Documentation](../arquitectura/)
- **Configuration**: See [Stack Documentation](../stack/)
- **Operations**: See [Operations Runbook](../operations/)
