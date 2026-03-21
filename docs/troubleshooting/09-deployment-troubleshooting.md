# Deployment Troubleshooting

## Overview

This guide addresses deployment-related issues in the Gym Platform: container startup failures, environment configuration problems, orchestration issues, and production deployment failures. Most deployment issues arise from configuration mismatches or resource constraints.

**Deployment Process:**
1. Build Docker image
2. Run docker-compose up
3. Wait for health checks
4. Execute database migrations
5. Verify service readiness
6. Direct traffic to new version

---

## Table of Contents

1. [Container Build Issues](#container-build-issues)
2. [Container Startup Issues](#container-startup-issues)
3. [Docker Compose Issues](#docker-compose-issues)
4. [Environment Configuration](#environment-configuration)
5. [Database Migration Issues](#database-migration-issues)
6. [Rolling Deployment Issues](#rolling-deployment-issues)

---

## Container Build Issues

### Issue: Docker Build Fails

**Symptoms:**
```
docker build failed with status 1
ERROR: Step X/Y failed
Base image not found
```

**Diagnostic Steps:**

1. **Check Dockerfile syntax:**
```bash
docker build -t gym/auth:test . 2>&1 | head -50
```

2. **Verify base image exists:**
```bash
docker pull openjdk:17-slim
```

3. **Check build context:**
```bash
ls -la
# Ensure Dockerfile and source files present
```

4. **Enable BuildKit for better errors:**
```bash
DOCKER_BUILDKIT=1 docker build -t gym/auth:test . 2>&1
```

**Resolution:**

**Fix Dockerfile:**
```dockerfile
# Before: Issues
FROM openjdk:17  # Might be outdated or not exist
COPY . /app

# After: Explicit and reproducible
FROM openjdk:17-slim-bullseye

WORKDIR /app

# Layer caching - copy pom first
COPY pom.xml .
RUN mvn dependency:resolve

# Then copy source
COPY src ./src

# Build
RUN mvn package -DskipTests

# Runtime stage
FROM openjdk:17-slim-bullseye
COPY --from=0 /app/target/gym-auth-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Fix base image:**
```dockerfile
# Use specific version tags
FROM openjdk:17-slim@sha256:abcdef123456...  # Reproducible

# Or at minimum use slim variant
FROM openjdk:17-slim
```

**Build with layers properly:**
```bash
# Enable BuildKit for better caching
export DOCKER_BUILDKIT=1

# Build with proper tag
docker build -t gym/auth:v1.0.0 -t gym/auth:latest .

# View build steps
docker build --progress=plain -t gym/auth:latest .
```

---

### Issue: Image Size Too Large

**Symptoms:**
```
Docker image >500MB
Slow push/pull to registry
Container startup takes >30 seconds
```

**Diagnostic Steps:**

1. **Check image size:**
```bash
docker images | grep gym
# Look at SIZE column
```

2. **Analyze image layers:**
```bash
docker history gym/auth:latest
# Identifies large layers
```

3. **Check for large files:**
```bash
docker run --rm gym/auth:latest du -sh / | sort -h | tail -10
```

**Resolution:**

**Use multi-stage builds:**
```dockerfile
# Before: Single stage, includes build tools
FROM openjdk:17-slim
COPY . /app
WORKDIR /app
RUN mvn package
ENTRYPOINT ["java", "-jar", "target/gym-auth-*.jar"]
# Size: ~400MB

# After: Multi-stage, only runtime in final image
FROM maven:3-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17-slim
COPY --from=builder /app/target/gym-auth-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# Size: ~150MB
```

**Clean up unnecessary files:**
```dockerfile
RUN mvn package && \
    rm -rf /root/.m2 && \
    rm -rf /app/target/*.jar.original
```

**Use distroless base image (minimal):**
```dockerfile
# For production - smallest image
FROM distroless/java17-debian11
COPY --from=builder /app/target/gym-auth-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# Size: ~80MB
```

---

## Container Startup Issues

### Issue: Container Exits Immediately

**Symptoms:**
```
Container exits with code 1, 127, or others
docker-compose up exits immediately
docker logs shows nothing helpful
```

**Diagnostic Steps:**

1. **Check exit code:**
```bash
docker run gym/auth:latest 2>&1
# Exit code indicates problem type:
# 1 = Application error
# 127 = Command not found
# 137 = Out of memory
# 143 = Killed
```

2. **Examine application logs:**
```bash
docker run -it gym/auth:latest /bin/bash
java -jar gym-auth-*.jar 2>&1 | head -100
```

3. **Check for Java errors:**
```bash
docker run --entrypoint "" gym/auth:latest java -version
# Should show Java version
```

4. **Test with verbose output:**
```bash
docker run -e JAVA_OPTS="-Xmx256m -XX:+PrintGC" gym/auth:latest
```

**Resolution:**

**Verify entrypoint:**
```dockerfile
# Ensure entrypoint is correct
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []

# Or explicitly specify
ENTRYPOINT exec java -jar app.jar
```

**Add health checks:**
```yaml
# docker-compose.yml
services:
  gym-auth:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 60s
```

**Debug container:**
```bash
# Run with override entrypoint
docker run -it --entrypoint /bin/bash gym/auth:latest

# Inside container
java -jar gym-auth-*.jar 2>&1
# See actual error
```

---

### Issue: Port Not Exposed

**Symptoms:**
```
Cannot connect to service
Connection refused on host port
docker port shows no mapping
```

**Diagnostic Steps:**

1. **Check port mapping:**
```bash
docker port gym-auth
# Should show: 8080/tcp -> 0.0.0.0:8080
```

2. **Verify service listening:**
```bash
docker exec gym-auth netstat -tlnp | grep 8080
docker exec gym-auth lsof -i :8080
```

3. **Test from inside container:**
```bash
docker exec gym-auth curl http://localhost:8080/actuator/health
# Should succeed
```

4. **Test from host:**
```bash
curl http://localhost:8080/actuator/health
# Should succeed
```

**Resolution:**

**Ensure port is exposed in Dockerfile:**
```dockerfile
EXPOSE 8080
```

**Ensure port is mapped in docker-compose:**
```yaml
services:
  gym-auth:
    ports:
      - "8080:8080"
```

**Check if port is available:**
```bash
# Host port not in use
netstat -tlnp | grep 8080
# If in use, either kill process or use different host port
```

---

## Docker Compose Issues

### Issue: docker-compose up Fails

**Symptoms:**
```
ERROR: could not parse docker-compose.yml
ERROR: service "X" depends_on service "Y" which is undefined
```

**Diagnostic Steps:**

1. **Validate YAML syntax:**
```bash
docker-compose config
# Outputs resolved config or shows error
```

2. **Check service dependencies:**
```bash
docker-compose config | grep -A 5 "depends_on"
```

3. **Verify all images available:**
```bash
docker-compose config | grep image:
# Check each image exists locally or is available on registry
```

**Resolution:**

**Fix YAML syntax:**
```yaml
# Before: Invalid YAML
services:
  gym-auth
    image: gym/auth

# After: Valid YAML (note indentation)
services:
  gym-auth:
    image: gym/auth
```

**Fix dependencies:**
```yaml
# Before: Invalid service name
services:
  gym-auth:
    depends_on:
      - database  # No such service

# After: Correct service name
services:
  gym-auth:
    depends_on:
      postgres:
        condition: service_healthy
```

**Pull images before up:**
```bash
docker-compose pull
docker-compose up
```

---

### Issue: Service Cannot Connect to Dependency

**Symptoms:**
```
Service starts but cannot reach database/rabbitmq
Connection timeout to postgres:5432
```

**Diagnostic Steps:**

1. **Check depends_on configuration:**
```yaml
# Must wait for health check, not just container start
depends_on:
  postgres:
    condition: service_healthy  # Important!
```

2. **Verify dependency health check:**
```bash
docker ps
# Check STATUS column - should show "(healthy)" not just "Up"
```

3. **Test connectivity from service:**
```bash
docker exec gym-auth nc -zv postgres 5432
```

**Resolution:**

**Add proper health check:**
```yaml
services:
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gym_user -d gym_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  gym-auth:
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gym_db
```

**Add startup delay:**
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        // Retry connecting to database
        int attempts = 0;
        while (attempts < 30) {
            try {
                return createDataSource();
            } catch (Exception e) {
                attempts++;
                if (attempts >= 30) throw e;
                try { Thread.sleep(1000); } catch (InterruptedException ex) {}
            }
        }
        return null;
    }
}
```

---

## Environment Configuration

### Issue: Wrong Configuration in Container

**Symptoms:**
```
Environment variables not set
Configuration values are defaults
Service connects to wrong database
```

**Diagnostic Steps:**

1. **Check environment variables:**
```bash
docker exec gym-auth env | sort
docker exec gym-auth env | grep SPRING_
```

2. **Verify .env file is loaded:**
```bash
ls -la .env
docker-compose config | grep SPRING_DATASOURCE
```

3. **Check application properties:**
```bash
docker exec gym-auth cat /app/application.properties | grep -i db
```

**Resolution:**

**Use .env file properly:**
```bash
# .env file (in same directory as docker-compose.yml)
JWT_SECRET=my-secret-here
DB_PASSWORD=postgres-password
SPRING_PROFILES_ACTIVE=prod
```

**Reference in docker-compose:**
```yaml
version: '3.8'

services:
  postgres:
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}

  gym-auth:
    environment:
      JWT_SECRET: ${JWT_SECRET}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
```

**Pass environment explicitly:**
```bash
# Override specific variables
DB_PASSWORD=prod-pass docker-compose up

# Or use .env.prod file
docker-compose --env-file .env.prod up
```

---

## Database Migration Issues

### Issue: Database Migrations Fail

**Symptoms:**
```
Flyway migration failed
Liquibase validation failed
SQL migration syntax error
```

**Diagnostic Steps:**

1. **Check migration logs:**
```bash
docker logs gym-auth | grep -i "migration\|flyway"
```

2. **Verify migration files:**
```bash
ls -la src/main/resources/db/migration/
```

3. **Check database state:**
```bash
docker exec postgres psql -U gym_user -d gym_db -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

**Resolution:**

**Fix migration syntax:**
```sql
-- Before: Incorrect DDL
CREATE TABLE gym_auth (
  id INT PRIMARY KEY,
  username VARCHAR,
  password VARCHAR
);

-- After: Correct DDL
CREATE TABLE IF NOT EXISTS gym_auth (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Use Flyway properly:**
```yaml
# docker-compose.yml
services:
  gym-auth:
    environment:
      SPRING_FLYWAY_ENABLED: "true"
      SPRING_FLYWAY_LOCATIONS: "classpath:db/migration"
      SPRING_JPA_HIBERNATE_DDL_AUTO: "validate"  # Don't let Hibernate change schema
```

**Skip failed migration (if necessary):**
```bash
# Only in emergency - can cause data corruption
docker exec postgres psql -U gym_user -d gym_db << EOF
DELETE FROM flyway_schema_history WHERE success = false;
EOF

# Re-run migration
docker-compose restart gym-auth
```

---

## Rolling Deployment Issues

### Issue: Zero-Downtime Deployment Fails

**Symptoms:**
```
Service unavailable during deployment
Requests fail during rolling update
Incomplete deployment
```

**Diagnostic Steps:**

1. **Check current version:**
```bash
curl http://localhost:8080/actuator/info | jq '.build.version'
```

2. **Monitor deployment progress:**
```bash
watch -n 1 'docker-compose ps'
```

3. **Check health of new version:**
```bash
docker-compose logs gym-auth-v2 | tail -20
curl http://localhost:8081/actuator/health  # If using different port
```

**Resolution:**

**Implement blue-green deployment:**
```yaml
# docker-compose.yml for green deployment
services:
  gym-auth-blue:
    image: gym/auth:v1.0.0
    ports:
      - "8080:8080"
    environment:
      - DEPLOYMENT_VERSION=blue

  gym-auth-green:
    image: gym/auth:v1.1.0  # New version
    ports:
      - "8081:8080"
    environment:
      - DEPLOYMENT_VERSION=green
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]

  nginx:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    # Update nginx.conf to point to green, then switch back to blue
```

**Use rolling deployment with health checks:**
```yaml
services:
  gym-auth-1:
    image: gym/auth:v1.1.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 5s
      timeout: 3s
      retries: 3
      start_period: 30s

  gym-auth-2:
    image: gym/auth:v1.0.0
    depends_on:
      gym-auth-1:
        condition: service_healthy
    # Stop after health check passes
```

**Verify deployment success:**
```bash
# Check all instances running
docker-compose ps | grep gym-auth

# Verify all healthy
for i in {8080..8082}; do
  echo "Checking $i..."
  curl -s http://localhost:$i/actuator/health | jq .status
done

# Monitor error rate during deployment
curl http://prometheus:9090/api/v1/query?query=rate(http_requests_total{status=~\"5..\"}\[5m\])
```

---

## Production Deployment Checklist

- [ ] Docker image builds successfully
- [ ] Image size reasonable (<200MB)
- [ ] All environment variables set
- [ ] Database migrations pass
- [ ] Health checks passing
- [ ] Service dependencies ready
- [ ] No secrets in logs
- [ ] Monitoring/logging enabled
- [ ] Rollback plan tested
- [ ] Zero-downtime deployment verified

---

## Related Documentation

- [02-debugging-techniques.md](02-debugging-techniques.md) - Container debugging
- [03-common-issues.md](03-common-issues.md) - Common deployment issues
- [04-diagnostic-procedures.md](04-diagnostic-procedures.md) - Container diagnostics
- docs/deployment/01-production-deployment-guide.md - Deployment guide
- docs/deployment/02-deployment-strategies.md - Deployment strategies
- docs/stack/05-deployment-docker.md - Docker configuration
