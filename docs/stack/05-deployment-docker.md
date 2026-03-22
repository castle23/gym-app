# Docker Deployment

## Overview

Gym Platform uses **Docker** containerization and **Docker Compose** for multi-container orchestration. This document covers Dockerfile best practices, Docker Compose configuration, container networking, and deployment workflows.

**Docker Stack:**
- Docker 20.10+
- Docker Compose 2.x
- Multi-stage builds
- Alpine Linux base images
- Container networking & volumes

## Dockerfile Best Practices

### Multi-Stage Build (Auth Service Example)

```dockerfile
# Stage 1: Build
FROM maven:3.8-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom files
COPY auth-service/pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline

# Copy source code
COPY auth-service/src ./src

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Gym Platform Team <ops@gym.local>"
LABEL description="Auth Service - JWT Authentication & User Management"

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/auth-service-*.jar auth-service.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -jar auth-service.jar --health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
```

### Optimized JVM Settings

```dockerfile
# Production-ready JVM configuration
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+UnlockDiagnosticVMOptions \
    -XX:G1SummarizeRSetStatsPeriod=1 \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar auth-service.jar"]
```

### Minimal Alpine Image

```dockerfile
FROM alpine:3.17

# Install Java runtime only (minimal footprint)
RUN apk add --no-cache openjdk17-jre

# Rest of Dockerfile...
```

## Docker Compose Configuration

### Actual Stack

The real `docker-compose.yml` runs 6 containers: `postgres`, `api-gateway`, `auth-service`, `training-service`, `tracking-service`, `notification-service`. All services share a single `gym_db` database with user `gym_admin`/`gym_password`.

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: gym-postgres
    environment:
      POSTGRES_USER: gym_admin
      POSTGRES_PASSWORD: ${DB_PASSWORD:-gym_password}
      POSTGRES_DB: gym_db
    volumes:
      - gym_postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gym_admin -d gym_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  api-gateway:
    build:
      context: ./api-gateway
    container_name: gym-api-gateway
    environment:
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-default}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  auth-service:
    build:
      context: ./auth-service
    container_name: gym-auth-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gym_db
      SPRING_DATASOURCE_USERNAME: gym_admin
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-gym_password}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/auth/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  training-service:
    build:
      context: ./training-service
    container_name: gym-training-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gym_db
      SPRING_DATASOURCE_USERNAME: gym_admin
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-gym_password}
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/training/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  gym_postgres_data:
```

(tracking-service on 8083 and notification-service on 8084 follow the same pattern.)

## Environment Configuration

### .env File

```bash
# Database
DB_PASSWORD=gym_password

# JWT
JWT_SECRET=your-secret-key-here

# Spring profile
SPRING_PROFILES_ACTIVE=default
```

See `.env.example` in the project root for the full template.

## Container Operations

### Build Images

```bash
# Build single service
docker build -f auth-service/Dockerfile -t gym-auth-service:latest .

# Build all services
docker-compose build

# Build with no cache
docker-compose build --no-cache

# Build specific service
docker-compose build auth-service
```

### Run Containers

```bash
# Start all services
docker-compose up -d

# Start with logs in foreground
docker-compose up

# Start specific service
docker-compose up -d auth-service

# Scale service (if supported)
docker-compose up -d --scale training-service=3
```

### Manage Containers

```bash
# View running containers
docker-compose ps

# View logs
docker-compose logs -f auth-service

# View logs with timestamps
docker-compose logs --timestamps -f auth-service

# Stop services
docker-compose stop

# Stop specific service
docker-compose stop auth-service

# Restart services
docker-compose restart

# Remove containers
docker-compose down

# Remove containers and volumes
docker-compose down -v
```

### Interactive Access

```bash
# Execute command in container
docker exec -it gym-auth-service /bin/sh

# View container details
docker inspect gym-auth-service

# Check resource usage
docker stats

# View network
docker network inspect gym-network
```

## Container Networking

All services communicate via the Docker internal network. There is **no service-to-service HTTP communication** — all client requests flow through the API Gateway (port 8080), which routes to the appropriate service.

## Volume Management

### Data Persistence

```yaml
volumes:
  # Named volume for PostgreSQL
  gym_postgres_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/postgres

  # Bind mount for backups
  - type: bind
    source: ./dba/backups
    target: /backups
```

### Backup Strategy

```bash
# Backup gym_db (single database, all schemas)
docker exec gym-postgres pg_dump -U gym_admin gym_db | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Restore from backup
gunzip < backup_20240101_120000.sql.gz | docker exec -i gym-postgres psql -U gym_admin gym_db
```

## Production Deployment

See `docker-compose.prod.yml` for the production configuration. Key differences from development:
- `restart: always` on all services
- PostgreSQL port not exposed externally
- `SPRING_PROFILES_ACTIVE: production`
- Build context is each service's own directory

```bash
# Deploy production stack
docker compose -f docker-compose.prod.yml up -d

# Verify health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/auth/actuator/health
curl http://localhost:8082/training/actuator/health
curl http://localhost:8083/tracking/actuator/health
curl http://localhost:8084/notifications/actuator/health
```

## Monitoring

### Container Metrics

```bash
# View real-time resource usage
docker stats --no-stream

# View logs with filtering
docker compose logs --tail=100 auth-service
docker compose logs -f auth-service 2>&1 | grep ERROR

# Inspect container details
docker inspect gym-auth-service
```

> **Note**: ELK stack (Elasticsearch, Logstash, Kibana) is not currently configured.

## Security Best Practices

1. Use specific image versions (not `latest`)
2. Run containers as non-root user
3. Use environment variables for secrets
4. Enable health checks
5. Implement resource limits
6. Use private Docker registries
7. Scan images for vulnerabilities
8. Keep Docker daemon updated
9. Use networks for isolation
10. Monitor container activity

## Key References

- [Docker Official Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Best Practices for Writing Dockerfiles](https://docs.docker.com/develop/dev-best-practices/dockerfile_best-practices/)
- See also: [docs/deployment/](../deployment/)
