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

### Full Stack (Production-Ready)

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: gym-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD:-changeMe}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=en_US.UTF-8"
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - gym_postgres_data:/var/lib/postgresql/data
      - ./dba/initialization/schemas:/docker-entrypoint-initdb.d
      - ./dba/backups:/backups
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    networks:
      - gym-network
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "10"

  # PgAdmin (Database Management)
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: gym-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@gym.local
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD:-admin}
      SCRIPT_NAME: /pgadmin
    ports:
      - "5050:80"
    networks:
      - gym-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  # Auth Service
  auth-service:
    build:
      context: .
      dockerfile: auth-service/Dockerfile
    container_name: gym-auth-service
    environment:
      SPRING_PROFILES_ACTIVE: ${PROFILE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
      SPRING_DATASOURCE_USERNAME: auth_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-changeMe}
      JWT_SECRET: ${JWT_SECRET:-your-super-secret-key-change-in-production}
      JWT_EXPIRATION: 3600
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    networks:
      - gym-network
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "10"

  # Training Service
  training-service:
    build:
      context: .
      dockerfile: training-service/Dockerfile
    container_name: gym-training-service
    environment:
      SPRING_PROFILES_ACTIVE: ${PROFILE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/training_db
      SPRING_DATASOURCE_USERNAME: training_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-changeMe}
      AUTH_SERVICE_URL: http://auth-service:8081
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
      auth-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    networks:
      - gym-network
    restart: unless-stopped

  # Tracking Service
  tracking-service:
    build:
      context: .
      dockerfile: tracking-service/Dockerfile
    container_name: gym-tracking-service
    environment:
      SPRING_PROFILES_ACTIVE: ${PROFILE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tracking_db
      SPRING_DATASOURCE_USERNAME: tracking_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-changeMe}
      AUTH_SERVICE_URL: http://auth-service:8081
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
    ports:
      - "8083:8083"
    depends_on:
      postgres:
        condition: service_healthy
      auth-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    networks:
      - gym-network
    restart: unless-stopped

  # Notification Service
  notification-service:
    build:
      context: .
      dockerfile: notification-service/Dockerfile
    container_name: gym-notification-service
    environment:
      SPRING_PROFILES_ACTIVE: ${PROFILE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/notification_db
      SPRING_DATASOURCE_USERNAME: notification_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-changeMe}
      AUTH_SERVICE_URL: http://auth-service:8081
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
    ports:
      - "8084:8084"
    depends_on:
      postgres:
        condition: service_healthy
      auth-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    networks:
      - gym-network
    restart: unless-stopped

  # Nginx Reverse Proxy
  nginx:
    image: nginx:1.24-alpine
    container_name: gym-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - auth-service
      - training-service
      - tracking-service
      - notification-service
    networks:
      - gym-network
    restart: unless-stopped

volumes:
  gym_postgres_data:
    driver: local

networks:
  gym-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

## Environment Configuration

### .env File (Development)

```bash
# Database
DB_PASSWORD=development_password
DB_USER=gym_user

# PgAdmin
PGADMIN_PASSWORD=admin

# Application Profile
PROFILE=dev

# JWT
JWT_SECRET=dev-secret-key-for-testing-only

# Service URLs
AUTH_SERVICE_URL=http://localhost:8081
TRAINING_SERVICE_URL=http://localhost:8082
TRACKING_SERVICE_URL=http://localhost:8083
NOTIFICATION_SERVICE_URL=http://localhost:8084

# Logging
LOG_LEVEL=DEBUG
```

### .env.prod File (Production)

```bash
# Database
DB_PASSWORD=${SECURE_PASSWORD_FROM_VAULT}
DB_USER=gym_service

# Application Profile
PROFILE=prod

# JWT (Generate with: openssl rand -base64 32)
JWT_SECRET=${SECURE_JWT_SECRET_FROM_VAULT}

# Service URLs (behind reverse proxy)
AUTH_SERVICE_URL=https://api.gym.local/auth
TRAINING_SERVICE_URL=https://api.gym.local/training
TRACKING_SERVICE_URL=https://api.gym.local/tracking
NOTIFICATION_SERVICE_URL=https://api.gym.local/notification

# Logging
LOG_LEVEL=INFO
```

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

### Service-to-Service Communication

```java
// auth-service calling training-service
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class TrainingServiceClient {

    @Value("${training.service.url:http://training-service:8082}")
    private String trainingServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    public Program getProgram(UUID programId) {
        String url = trainingServiceUrl + "/api/v1/programs/" + programId;
        return restTemplate.getForObject(url, Program.class);
    }
}
```

### Health Checks

```yaml
services:
  auth-service:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

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
# Backup PostgreSQL data
docker exec gym-postgres pg_dump -U postgres auth_db | gzip > backup_auth_$(date +%Y%m%d_%H%M%S).sql.gz

# Backup all databases
docker-compose exec -T postgres pg_dumpall -U postgres | gzip > backup_all_$(date +%Y%m%d_%H%M%S).sql.gz

# Restore from backup
gunzip < backup_auth_20240101_120000.sql.gz | docker exec -i gym-postgres psql -U postgres auth_db
```

## Production Deployment

### Nginx Configuration

**nginx/conf.d/gym-api.conf:**
```nginx
upstream gym_auth_service {
    server auth-service:8081;
}

upstream gym_training_service {
    server training-service:8082;
}

upstream gym_tracking_service {
    server tracking-service:8083;
}

upstream gym_notification_service {
    server notification-service:8084;
}

server {
    listen 80;
    server_name api.gym.local;
    client_max_body_size 10M;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.gym.local;

    ssl_certificate /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Auth Service
    location /auth {
        proxy_pass http://gym_auth_service/api/v1/auth;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    # Training Service
    location /training {
        proxy_pass http://gym_training_service/api/v1/training;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    # Tracking Service
    location /tracking {
        proxy_pass http://gym_tracking_service/api/v1/tracking;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    # Notification Service
    location /notification {
        proxy_pass http://gym_notification_service/api/v1/notification;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }
}
```

### Deploy Script

```bash
#!/bin/bash
# scripts/operational/deploy-docker-prod.sh

set -e

# Load environment
source .env.prod

# Validate environment
if [ -z "$JWT_SECRET" ]; then
    echo "Error: JWT_SECRET not set"
    exit 1
fi

# Build images
echo "Building Docker images..."
docker-compose build --no-cache

# Backup database
echo "Creating database backup..."
docker exec gym-postgres pg_dumpall -U postgres | \
    gzip > ./dba/backups/backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Stop current containers
echo "Stopping services..."
docker-compose stop

# Start new containers
echo "Starting services..."
docker-compose up -d

# Wait for services to be healthy
echo "Waiting for services to be healthy..."
for i in {1..30}; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo "Services are healthy"
        exit 0
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo "Error: Services failed to start"
exit 1
```

## Monitoring

### Container Metrics

```bash
# View real-time resource usage
docker stats --no-stream

# View logs with filtering
docker-compose logs --tail=100 auth-service
docker-compose logs -f auth-service 2>&1 | grep ERROR

# Inspect container details
docker inspect gym-auth-service | jq '.[] | {Name, State}'
```

### Log Aggregation

**docker-compose.yml (with ELK):**
```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  logstash:
    image: docker.elastic.co/logstash/logstash:8.0.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf

  kibana:
    image: docker.elastic.co/kibana/kibana:8.0.0
    ports:
      - "5601:5601"
```

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
