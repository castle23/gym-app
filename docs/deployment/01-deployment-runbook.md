# Deployment Runbook - Gym Platform

Step-by-step procedures for deploying, monitoring, and rolling back the platform using Docker Compose.

**Environment**: Single server, Docker Compose  
**Database**: PostgreSQL 15 (single instance, `gym_db`, 4 schemas)  
**Services**: api-gateway (8080), auth-service (8081), training-service (8082), tracking-service (8083), notification-service (8084)

---

## Pre-Deployment Checklist

```
☐ Code
  □ All tests passing (mvn clean test)
  □ No hardcoded secrets or credentials

☐ Configuration
  □ .env file configured (see .env.example)
  □ JWT_SECRET set (minimum 32 characters)
  □ SMTP credentials configured
  □ firebase-config.json present at project root

☐ Database
  □ Backup created if upgrading existing deployment
```

---

## First-Time Deployment

```bash
# 1. Clone repository
git clone <repo-url>
cd gym

# 2. Configure environment
cp .env.example .env
# Edit .env: set JWT_SECRET, SMTP_*, DB_PASSWORD

# 3. Build and start all services
docker-compose up -d --build

# 4. Verify all containers are running
docker-compose ps
```

Expected output:
```
NAME                      STATUS          PORTS
gym-postgres              Up (healthy)    0.0.0.0:5432->5432/tcp
gym-api-gateway           Up (healthy)    0.0.0.0:8080->8080/tcp
gym-auth-service          Up (healthy)    0.0.0.0:8081->8081/tcp
gym-training-service      Up (healthy)    0.0.0.0:8082->8082/tcp
gym-tracking-service      Up (healthy)    0.0.0.0:8083->8083/tcp
gym-notification-service  Up (healthy)    0.0.0.0:8084->8084/tcp
```

---

## Updating an Existing Deployment

```bash
# 1. Pull latest code
git pull origin main

# 2. Backup database
docker exec gym-postgres pg_dump -U gym_admin gym_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 3. Rebuild and restart services (zero-downtime for stateless services)
docker-compose up -d --build

# Docker Compose replaces containers one by one; postgres data is preserved in volume
```

---

## Health Verification

```bash
# Check all service health endpoints
for entry in "api-gateway:8080:" "auth:8081:auth" "training:8082:training" "tracking:8083:tracking" "notification:8084:notifications"; do
    IFS=':' read -r name port prefix <<< "$entry"
    path="${prefix:+/$prefix}/actuator/health"
    echo -n "$name: "
    curl -s "http://localhost:$port$path" | grep -o '"status":"[^"]*"'
done
```

### Manual smoke test

```bash
# 1. Register user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@test.com","password":"Test123!","firstName":"Smoke","lastName":"Test","role":"ROLE_USER"}'

# 2. Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@test.com","password":"Test123!"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 3. Verify token works
curl -s http://localhost:8080/training/exercises \
  -H "Authorization: Bearer $TOKEN" | grep -o '"totalElements":[0-9]*'
```

---

## Rollback Procedure

### Application rollback (code change caused issue)

```bash
# 1. Identify last working commit
git log --oneline -10

# 2. Checkout previous version
git checkout <previous-commit-hash>

# 3. Rebuild and restart
docker-compose up -d --build

# 4. Verify health
docker-compose ps
```

### Database rollback (schema change caused issue)

```bash
# 1. Stop all app services (keep postgres running)
docker-compose stop api-gateway auth-service training-service tracking-service notification-service

# 2. Restore backup
cat backup_YYYYMMDD_HHMMSS.sql | docker exec -i gym-postgres psql -U gym_admin gym_db

# 3. Checkout previous code version
git checkout <previous-commit-hash>

# 4. Restart services
docker-compose up -d api-gateway auth-service training-service tracking-service notification-service
```

---

## Monitoring During Deployment

```bash
# Watch container status
watch -n 2 docker-compose ps

# Follow logs for all services
docker-compose logs -f

# Follow logs for a specific service
docker-compose logs -f auth-service

# Check for errors
docker-compose logs --since=5m | grep -i "error\|exception\|caused by"

# Resource usage
docker stats --no-stream
```

---

## Troubleshooting

### Container not starting

```bash
# Check logs
docker-compose logs <service-name>

# Common causes:
# - Database not ready yet → wait for postgres healthcheck, then: docker-compose restart <service>
# - Missing env variable → check .env file
# - Port already in use → lsof -i :<port>
```

### Database connection refused

```bash
# Verify postgres is healthy
docker-compose ps postgres

# Test connection manually
docker exec gym-postgres psql -U gym_admin -d gym_db -c "SELECT 1;"

# Check schemas exist
docker exec gym-postgres psql -U gym_admin -d gym_db -c "\dn"
# Expected: auth_schema, training_schema, tracking_schema, notification_schema
```

### Service returns 401/403 unexpectedly

```bash
# Verify JWT_SECRET is identical across all services in .env
grep JWT_SECRET .env

# Check gateway logs for JWT validation errors
docker-compose logs api-gateway | grep -i "jwt\|token\|auth"
```

### Out of disk space

```bash
# Check disk usage
df -h

# Remove unused Docker images and stopped containers
docker system prune -f

# Check volume sizes
docker system df -v
```

---

## Production Deployment (`docker-compose.prod.yml`)

```bash
# Uses docker-compose.prod.yml which has restart: always and no exposed postgres port
docker-compose -f docker-compose.prod.yml up -d --build

# Note: prod compose builds from individual service directories (not monorepo context)
# Ensure each service directory is self-contained before using prod compose
```

Key differences from dev compose:
- `restart: always` on all services
- PostgreSQL port not exposed externally
- Uses `SPRING_PROFILES_ACTIVE: production`

---

## Post-Deployment Checklist

```
☐ All 6 containers in "Up (healthy)" state
☐ All health endpoints return {"status":"UP"}
☐ Smoke test login succeeds
☐ No ERROR lines in logs (docker-compose logs --since=5m)
☐ Database schemas present (auth_schema, training_schema, tracking_schema, notification_schema)
```
