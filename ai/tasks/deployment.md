# Task: Deployment

## Prerequisites
- [ ] All tests passing: `mvn clean test`
- [ ] `.env` file configured with correct values
- [ ] Database backup taken (if updating existing deployment)
- [ ] Docker and Docker Compose installed and running
- [ ] No uncommitted changes: `git status`

## Workflow

### 1. Pre-flight Checks
```bash
# Run full test suite
mvn clean test

# Verify git state
git status
git log --oneline -5
```
- All tests must pass — do NOT deploy with failing tests
- Confirm you are on the correct branch

### 2. Backup Database
```bash
docker exec gym-postgres pg_dump -U gym_admin gym_db > backup_$(date +%Y%m%d_%H%M%S).sql
```
- Always backup before deploying changes that modify schema or data
- Store backup file outside the project directory or in a designated backups folder
- Verify backup file is non-empty

### 3. Build and Deploy
```bash
# Development
docker-compose up -d --build

# Production
docker-compose -f docker-compose.prod.yml up -d --build
```

### 4. Verify Containers
```bash
docker-compose ps
```
- All containers should show status `Up`
- Check for restart loops: containers should not be `Restarting`

### 5. Check Health Endpoints
```bash
curl -s http://localhost:8081/actuator/health | jq .
curl -s http://localhost:8082/actuator/health | jq .
curl -s http://localhost:8083/actuator/health | jq .
curl -s http://localhost:8084/actuator/health | jq .
```
- Each service should return `{"status": "UP"}`
- Check database connectivity in health details

### 6. Smoke Test
```bash
# Register a test user
curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"smoketest","password":"Test1234!","email":"smoke@test.com"}'

# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"smoketest","password":"Test1234!"}' | jq -r '.token')

# Use token on a protected endpoint
curl -s http://localhost:8082/api/v1/trainings \
  -H "Authorization: Bearer $TOKEN" | jq .
```
- Registration should succeed (or return conflict if user exists)
- Login should return a valid JWT token
- Protected endpoint should return data (not 401/403)

### 7. Monitor Logs
```bash
# Check for errors in the last 5 minutes
docker-compose logs --since=5m | grep -i error

# Follow logs for a specific service
docker-compose logs -f auth-service

# Check all services
docker-compose logs --tail=50
```
- No unhandled exceptions
- No connection errors
- No repeated error patterns

## Rollback Procedure
If deployment fails or issues are found:

```bash
# 1. Stop current deployment
docker-compose down

# 2. Restore database (if schema was changed)
docker exec -i gym-postgres psql -U gym_admin -d gym_db < backup_YYYYMMDD_HHMMSS.sql

# 3. Checkout previous working version
git checkout [previous-tag-or-commit]

# 4. Rebuild and deploy
docker-compose up -d --build

# 5. Verify health
curl -s http://localhost:8081/actuator/health | jq .
```

## Production Differences
| Aspect           | Development                  | Production                              |
|------------------|------------------------------|-----------------------------------------|
| Compose file     | `docker-compose.yml`         | `docker-compose -f docker-compose.prod.yml` |
| Restart policy   | none                         | `restart: always`                       |
| DB port          | Exposed (5432)               | NOT exposed externally                  |
| Log level        | DEBUG                        | INFO or WARN                            |
| Profiles         | `default`                    | `prod`                                  |

## Completion Checklist
- [ ] All tests passed before deployment
- [ ] Database backed up (if applicable)
- [ ] All containers running and healthy
- [ ] All health endpoints return `UP`
- [ ] Smoke test passed (register → login → use token)
- [ ] No errors in recent logs
- [ ] Rollback plan verified and documented

## References
- `ai/rules/project-overview.md` — service ports and architecture
- `docker-compose.yml` — development compose configuration
- `docker-compose.prod.yml` — production compose configuration
- `.env.example` — required environment variables
