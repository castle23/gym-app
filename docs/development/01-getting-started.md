# Getting Started with Development

## Quick Start (5 minutes)

### Prerequisites

```bash
# Check you have these installed
java -version          # Java 17 or higher
mvn -version          # Maven 3.8+
docker -v             # Docker 24+
docker-compose -v     # Docker Compose 2.0+
git --version         # Git
```

### One-Command Setup

```bash
# Clone and start
git clone https://github.com/your-org/gym-platform-api.git
cd gym-platform-api
cp .env.example .env   # configure JWT_SECRET, SMTP_*, etc.
docker-compose up -d

# Wait ~40 seconds for services to start
sleep 40

# Verify services via Swagger UI
curl http://localhost:8081/swagger-ui.html                    # Auth
curl http://localhost:8082/training/swagger-ui.html           # Training
curl http://localhost:8083/tracking/swagger-ui.html           # Tracking
curl http://localhost:8084/notifications/swagger-ui.html      # Notification
```

That's it! All services running ✅

---

## Detailed Setup

### Step 1: Install Prerequisites

#### Java 17+

```bash
# macOS (Homebrew)
brew install openjdk@17

# Windows (Chocolatey)
choco install openjdk17

# Linux (Ubuntu)
sudo apt-get install openjdk-17-jdk

# Verify installation
java -version
# openjdk version "17.x.x" 2024-...
```

#### Maven 3.8+

```bash
# macOS
brew install maven

# Windows
choco install maven

# Linux
sudo apt-get install maven

# Verify
mvn -version
# Apache Maven 3.8.x
```

#### Docker & Docker Compose

```bash
# Install Docker Desktop (includes Docker Compose v2)
# macOS & Windows: Download from https://docker.com/products/docker-desktop
# Linux: https://docs.docker.com/engine/install/

# Verify
docker --version
# Docker version 24.x.x
docker-compose --version
# Docker Compose version 2.x.x
```

### Step 2: Clone Repository

```bash
git clone https://github.com/your-org/gym-platform-api.git
cd gym-platform-api

# Verify structure
ls -la
# Expected:
# ├── auth-service/
# ├── training-service/
# ├── tracking-service/
# ├── notification-service/
# ├── common/
# ├── docker-compose.yml
# ├── pom.xml
# └── ... (docs, scripts, etc)
```

### Step 3: Build Locally (Optional, Docker handles this)

```bash
# Build all modules
mvn clean install

# Expected output after ~2-3 minutes:
# BUILD SUCCESS
# Total time: 2m 30s

# Build specific service
mvn clean install -pl auth-service
```

### Step 4: Start Services with Docker Compose

```bash
# Start all services in background
docker-compose up -d

# Watch startup logs
docker-compose logs -f

# Expected output:
# auth-service | 2024-03-21 10:30:45 - Started AuthServiceApplication in 8.234 seconds
# training-service | 2024-03-21 10:30:47 - Started TrainingServiceApplication in 8.123 seconds
# tracking-service | 2024-03-21 10:30:49 - Started TrackingServiceApplication in 8.456 seconds
# notification-service | 2024-03-21 10:30:50 - Started NotificationServiceApplication in 7.989 seconds
```

### Step 5: Verify Services Running

```bash
# Check Docker containers
docker-compose ps

# Expected:
# NAME                      STATUS          PORTS
# gym-postgres              Up (healthy)    0.0.0.0:5432->5432/tcp
# gym-api-gateway           Up (healthy)    0.0.0.0:8080->8080/tcp
# gym-auth-service          Up (healthy)    0.0.0.0:8081->8081/tcp
# gym-training-service      Up (healthy)    0.0.0.0:8082->8082/tcp
# gym-tracking-service      Up (healthy)    0.0.0.0:8083->8083/tcp
# gym-notification-service  Up (healthy)    0.0.0.0:8084->8084/tcp

# Check health endpoints
for entry in "auth:8081:auth" "training:8082:training" "tracking:8083:tracking" "notification:8084:notifications"; do
  IFS=':' read -r name port prefix <<< "$entry"
  echo "$name: $(curl -s http://localhost:$port/$prefix/actuator/health | grep -o '"status":"[^"]*"')"
done
```

### Step 6: Access Services

**API Gateway** (single entry point for all requests):
- http://localhost:8080

**Auth Service** (direct access):
- Swagger UI: http://localhost:8081/swagger-ui.html
- API Docs: http://localhost:8081/v3/api-docs
- Health: http://localhost:8081/auth/actuator/health

**Training Service** (direct access):
- Swagger UI: http://localhost:8082/training/swagger-ui.html
- API Docs: http://localhost:8082/training/v3/api-docs
- Health: http://localhost:8082/training/actuator/health

**Tracking Service** (direct access):
- Swagger UI: http://localhost:8083/tracking/swagger-ui.html
- Health: http://localhost:8083/tracking/actuator/health

**Notification Service** (direct access):
- Swagger UI: http://localhost:8084/notifications/swagger-ui.html
- Health: http://localhost:8084/notifications/actuator/health

**Database**:
- Host: localhost
- Port: 5432
- Database: gym_db
- Username: gym_admin
- Password: gym_password (see `.env`)

---

## First API Call

### Step 1: Register a User

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_USER"
  }'

# Response 201:
# {
#   "userId": 1,
#   "email": "user@example.com",
#   "message": "User registered successfully"
# }
```

### Step 2: Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'

# Response 200:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "userId": 1,
#   "email": "user@example.com",
#   "message": "Login successful"
# }

# Save token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Step 3: Make Authenticated Request

```bash
# Get exercises (via API Gateway)
curl -X GET http://localhost:8080/training/exercises \
  -H "Authorization: Bearer $TOKEN"

# Response 200:
# {
#   "content": [...],
#   "totalElements": 10,
#   "totalPages": 1,
#   "number": 0
# }
```

---

## Development Workflow

### 1. Make Code Changes

```bash
# Edit a file
vim auth-service/src/main/java/com/gym/auth/controller/AuthController.java
```

### 2. Test Changes (Hot Reload with Docker)

```bash
# In another terminal, watch for changes
docker-compose up -d --build auth-service

# Or manually rebuild
mvn clean install -pl auth-service
docker-compose up -d --build auth-service
```

### 3. Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Specific service
mvn test -pl auth-service
```

### 4: Check Your Code

```bash
# Build to catch errors
mvn clean install

# Format code
mvn spotless:apply

# Run linting
mvn checkstyle:check
```

---

## Stopping Services

```bash
# Stop all services (keep data)
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove volumes (delete all data!)
docker-compose down -v

# View logs after stopping
docker-compose logs auth-service
```

---

## Common Tasks

### Run Tests

```bash
# Run all tests
mvn test

# Run single test class
mvn test -Dtest=AuthControllerTest

# Run single test method
mvn test -Dtest=AuthControllerTest#testLogin
```

### Build & Deploy

```bash
# Build all services
mvn clean install

# Build Docker images
docker-compose build

# Push to registry
docker tag gym:auth-service docker.io/yourorg/gym-auth:latest
docker push docker.io/yourorg/gym-auth:latest
```

### Database Operations

```bash
# Connect to database
psql -U gym_admin -d gym_db -h localhost

# Or via Docker
docker exec -it gym-postgres psql -U gym_admin -d gym_db

# Backup database
pg_dump -U gym_admin -d gym_db > backup.sql

# Restore database
psql -U gym_admin -d gym_db < backup.sql
```

### View Logs

```bash
# All services
docker-compose logs -f

# Single service
docker-compose logs -f auth-service

# Last 100 lines
docker-compose logs --tail=100 training-service

# With timestamps
docker-compose logs -t
```

---

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8081
lsof -i :8081
# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

### Database Connection Failed

```bash
# Check PostgreSQL container
docker ps | grep postgres

# Check logs
docker logs <postgres_container_id>

# Verify credentials in .env
cat .env | grep POSTGRES
```

### Service Won't Start

```bash
# Check logs
docker logs auth-service

# Rebuild
docker-compose build --no-cache auth-service
docker-compose up -d auth-service
```

### Out of Memory

```bash
# Increase Docker memory
# Docker Desktop > Settings > Resources > Memory: 4GB+

# Or set JVM memory in docker-compose.yml:
environment:
  - JAVA_OPTS=-Xmx1024m
```

---

## Next Steps

1. **Explore APIs**: Open Swagger UI at `http://localhost:8081/swagger-ui.html`
2. **Read Code**: Start with `auth-service/src/main/java`
3. **Run Tests**: `mvn test`
4. **Make Changes**: Edit code and see hot reload
5. **Study Documentation**: Read [Development Environment Guide](02-development-environment.md)
6. **Join a Task**: Find an issue and submit a PR

---

## Resources

- [Project README](../../README.md)
- [Development Environment Setup](02-development-environment.md)
- [Coding Standards](03-coding-standards.md)
- [API Documentation](../api/01-api-overview.md)
- [Architecture Overview](../arquitectura/01-overview.md)
- [Contributing Guide](../../CONTRIBUTING.md)

## Getting Help

- **Documentation**: See [docs/README.md](../README.md)
- **Issues**: Check GitHub issues for solutions
- **Team**: Ask team leads in Slack/Discord
- **Docs**: Search [docs/troubleshooting/](../troubleshooting/)
