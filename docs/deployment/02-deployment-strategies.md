# Deployment Strategies

## Overview

This document covers different deployment approaches for Gym Platform microservices, including CI/CD pipeline strategies, blue-green deployments, rolling updates, and canary releases.

> **Note**: Blue-green, canary, rolling, and CI/CD pipeline strategies described in this document are aspirational/future deployment patterns. The current setup uses a single Docker Compose file (`docker-compose.yml`). See [01-deployment-runbook.md](01-deployment-runbook.md) for the actual deployment procedure.

## Deployment Strategies Comparison

| Strategy | Downtime | Rollback Speed | Complexity | Best For |
|----------|----------|----------------|-----------|----------|
| **Blue-Green** | None | Instant | Medium | Critical services, full version changes |
| **Rolling Update** | None | Slow | Medium | Regular updates, backward-compatible |
| **Canary** | None | Medium | High | Major changes, risk mitigation |
| **Shadow** | None | N/A (read-only) | High | Testing, validation |
| **Feature Flags** | None | Instant | Medium | Gradual rollouts, A/B testing |

## Blue-Green Deployment

### Overview

Two identical production environments (Blue and Green). Traffic switches between them during deployment.

```
Current State:
┌──────────────┐
│ Load Balancer│
└──────┬───────┘
       │
       ▼ (100%)
┌──────────────┐
│  Blue Env    │ ← Currently serving traffic
│ Services v1  │
└──────────────┘

┌──────────────┐
│  Green Env   │
│ Services v1  │ ← Idle
└──────────────┘

Deployment Phase:
1. Deploy new version to Green environment
2. Run smoke tests on Green
3. Switch traffic from Blue to Green
4. Keep Blue as rollback target

Rollback: Switch traffic back to Blue
```

### Implementation with Docker Compose

**Production setup:**
```yaml
# docker-compose-blue.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - gym_postgres_data_blue:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  auth-service-blue:
    image: gym-auth-service:v1.2.0
    container_name: gym-auth-service-blue
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVICE_LABEL: "blue"
    ports:
      - "8081:8081"

  training-service-blue:
    image: gym-training-service:v1.2.0
    container_name: gym-training-service-blue
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVICE_LABEL: "blue"
    ports:
      - "8082:8082"

  # More services...

volumes:
  gym_postgres_data_blue:

# docker-compose-green.yml
version: '3.8'

services:
  auth-service-green:
    image: gym-auth-service:v1.3.0
    container_name: gym-auth-service-green
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVICE_LABEL: "green"
    ports:
      - "8091:8081"

  # More services...
```

### Deployment Script

```bash
#!/bin/bash
# scripts/operational/blue-green-deploy.sh

set -e

NEW_VERSION=$1
CURRENT_ENV=$(cat /var/run/gym/current_env)
TARGET_ENV=$([[ "$CURRENT_ENV" == "blue" ]] && echo "green" || echo "blue")

echo "Current environment: $CURRENT_ENV"
echo "Deploying to: $TARGET_ENV"

# Pull latest images
docker pull gym-auth-service:$NEW_VERSION
docker pull gym-training-service:$NEW_VERSION
docker pull gym-tracking-service:$NEW_VERSION
docker pull gym-notification-service:$NEW_VERSION

# Deploy to target environment
echo "Starting $TARGET_ENV environment with version $NEW_VERSION..."
docker-compose -f docker-compose-${TARGET_ENV}.yml up -d --build

# Wait for services to be healthy
echo "Waiting for services to be healthy..."
for i in {1..30}; do
    if curl -f http://localhost:8091/actuator/health > /dev/null 2>&1; then
        echo "Services are healthy"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Run smoke tests
echo "Running smoke tests..."
./scripts/tests/smoke-tests.sh http://localhost:8091

# Switch traffic
echo "Switching traffic to $TARGET_ENV..."
docker exec gym-nginx nginx -s reload || true

# Update current environment marker
echo $TARGET_ENV > /var/run/gym/current_env

# Stop old environment (keep for quick rollback)
# docker-compose -f docker-compose-${CURRENT_ENV}.yml down

echo "Deployment complete. Current environment: $TARGET_ENV"
```

## Rolling Update Deployment

### Overview

Gradually replace instances of the old version with new version, maintaining service availability.

```
Initial State:
┌─────────────────────────┐
│ Load Balancer           │
├─────────────────────────┤
│ Service v1 (3 instances)│
│ ████████████            │
└─────────────────────────┘

Phase 1:
│ Service v1 (2 instances) + Service v2 (1 instance)
│ ████████ + ████

Phase 2:
│ Service v1 (1 instance) + Service v2 (2 instances)
│ ████ + ████████

Final State:
│ Service v2 (3 instances)
│ ████████████
```

### Implementation

```bash
#!/bin/bash
# scripts/operational/rolling-deploy.sh

set -e

NEW_VERSION=$1
SERVICE=$2
REPLICAS=${3:-3}

echo "Rolling deployment of $SERVICE:$NEW_VERSION with $REPLICAS replicas"

# Deploy new version alongside old
for i in $(seq 1 $REPLICAS); do
    echo "Starting $SERVICE-v2-$i..."
    docker run -d \
      --name $SERVICE-v2-$i \
      --network gym-network \
      -e SPRING_PROFILES_ACTIVE=prod \
      -e CONTAINER_ID=$i \
      gym-$SERVICE:$NEW_VERSION

    # Wait for new instance to be healthy
    echo "Waiting for $SERVICE-v2-$i to be healthy..."
    for j in {1..20}; do
        if curl -f http://$SERVICE-v2-$i:8081/actuator/health > /dev/null 2>&1; then
            echo "Instance $i is healthy"
            break
        fi
        sleep 2
    done

    # Stop old instance
    echo "Stopping $SERVICE-v1-$i..."
    docker stop $SERVICE-v1-$i || true
    docker rm $SERVICE-v1-$i || true

    # Small delay between replacements
    sleep 5
done

echo "Rolling deployment complete"
```

## Canary Deployment

### Overview

Deploy new version to small subset of users/traffic first, gradually increase traffic if no issues detected.

```
Traffic Distribution:
Phase 1: 95% v1 | 5% v2 (canary)
Phase 2: 80% v1 | 20% v2
Phase 3: 50% v1 | 50% v2
Phase 4: 0% v1 | 100% v2
```

### Implementation with Nginx

**nginx/canary.conf:**
```nginx
upstream gym_auth_v1 {
    server auth-service-v1:8081;
}

upstream gym_auth_v2 {
    server auth-service-v2:8081;
}

server {
    listen 80;
    server_name api.gym.local;

    location /api/v1/auth {
        # 5% traffic to canary (v2)
        if ($random < 5) {
            proxy_pass http://gym_auth_v2;
        }

        # 95% traffic to stable (v1)
        proxy_pass http://gym_auth_v1;

        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Version $upstream_addr;
    }
}
```

### Canary Script

```bash
#!/bin/bash
# scripts/operational/canary-deploy.sh

set -e

NEW_VERSION=$1
SERVICE=$2
STAGES=(5 20 50 100)  # Traffic percentage stages

echo "Starting canary deployment of $SERVICE:$NEW_VERSION"

# Deploy new version
echo "Deploying $SERVICE:$NEW_VERSION..."
docker run -d \
  --name $SERVICE-canary \
  --network gym-network \
  -e SPRING_PROFILES_ACTIVE=prod \
  gym-$SERVICE:$NEW_VERSION

# Wait for canary to be healthy
sleep 10

# Gradually increase traffic
for traffic in "${STAGES[@]}"; do
    echo "Setting traffic to $traffic% for $SERVICE:$NEW_VERSION"

    # Update Nginx configuration
    sed -i "s/canary_traffic = [0-9]*;/canary_traffic = $traffic;/" \
        /etc/nginx/conf.d/canary.conf

    # Reload Nginx
    docker exec gym-nginx nginx -s reload

    # Monitor for errors (5 minutes per stage)
    echo "Monitoring for errors... (5 minutes)"
    for i in {1..30}; do
        ERROR_RATE=$(curl -s http://localhost:9090/api/v1/metrics/error_rate | jq .value)

        if (( $(echo "$ERROR_RATE > 1.0" | bc -l) )); then
            echo "Error rate exceeded threshold: $ERROR_RATE%"
            echo "Rolling back canary..."
            docker stop $SERVICE-canary
            docker rm $SERVICE-canary
            exit 1
        fi

        echo "Error rate: $ERROR_RATE% (OK)"
        sleep 10
    done
done

echo "Canary deployment successful"
```

## Feature Flags (Feature Toggles)

### Overview

Control feature availability at runtime without redeployment.

```java
@Service
public class FeatureFlagService {

    @Autowired
    private FeatureFlagRepository repository;

    public boolean isEnabled(String featureName) {
        return repository.findByName(featureName)
            .map(FeatureFlag::isEnabled)
            .orElse(false);
    }

    public boolean isEnabledForUser(String featureName, UUID userId) {
        return repository.findByName(featureName)
            .map(flag -> flag.isEnabledForUser(userId))
            .orElse(false);
    }
}

@RestController
public class UserController {

    @Autowired
    private FeatureFlagService featureFlags;

    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable UUID id) {

        UserStatsDTO stats = new UserStatsDTO();

        if (featureFlags.isEnabledForUser("advanced_analytics", id)) {
            stats.setAdvancedMetrics(computeAdvancedMetrics(id));
        }

        return ResponseEntity.ok(stats);
    }
}
```

## Shadow Deployment

### Overview

Deploy new version alongside old version, mirror traffic to new version without affecting responses.

```
┌─────────────────────────┐
│ Client Request          │
└────────┬────────────────┘
         │
         ├─────────────────────┬──────────────────────┐
         │                     │                      │
         ▼                     ▼                      ▼
    ┌─────────┐          ┌─────────┐          ┌─────────┐
    │ v1 Prod │          │ v2 Shadow         │ Metrics │
    │Response │          │(No Response Used) │ Comparison
    └─────────┘          └─────────┘          └─────────┘
         │
         └─────────────────────────────────────────┐
                                                   │
                                                   ▼
                                            Return Response
                                            (from v1 only)
```

## CI/CD Pipeline Integration

### GitHub Actions Example

```yaml
name: Deploy to Production

on:
  push:
    branches:
      - main
  workflow_dispatch:

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Run tests
        run: mvn test

      - name: Build Docker images
        run: |
          docker build -f auth-service/Dockerfile -t gym-auth-service:${{ github.sha }} .
          docker build -f training-service/Dockerfile -t gym-training-service:${{ github.sha }} .

      - name: Push to registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push gym-auth-service:${{ github.sha }}

  deploy:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Deploy to production
        env:
          DEPLOY_KEY: ${{ secrets.DEPLOY_KEY }}
          SERVER_IP: ${{ secrets.SERVER_IP }}
        run: |
          mkdir -p ~/.ssh
          echo "$DEPLOY_KEY" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh -i ~/.ssh/id_rsa deploy@$SERVER_IP \
            "cd /opt/gym && \
             git pull origin main && \
             ./scripts/operational/blue-green-deploy.sh ${{ github.sha }}"
```

## Deployment Checklist

Before deploying:
- [ ] All tests passing
- [ ] Code reviewed and approved
- [ ] Release notes prepared
- [ ] Database migrations verified
- [ ] Configuration reviewed
- [ ] Rollback plan documented
- [ ] Team notified
- [ ] Monitoring configured
- [ ] Backup created
- [ ] Communication channel open

## Monitoring During Deployment

```bash
# Monitor service health
watch -n 2 'curl -s http://localhost:8081/auth/actuator/health | jq'

# Monitor logs
docker-compose logs -f auth-service

# Monitor resource usage
docker stats --no-stream

# Monitor error rates
curl http://localhost:9090/api/v1/metrics | jq '.errorRate'
```

### Rollback Procedures

### Immediate Rollback

```bash
# Checkout previous version and rebuild
git checkout <previous-commit-hash>
docker-compose up -d --build

# Verify services are responding
curl http://localhost:8080/actuator/health
```

### Database Rollback

```bash
# List backups
ls -lah /backups/

# Restore from backup
docker exec gym-postgres pg_restore -U gym_admin -d gym_db /backups/backup_timestamp.dump
```

## Key References

- [Blue-Green Deployments Guide](https://martinfowler.com/bliki/BlueGreenDeployment.html)
- [Canary Releases](https://martinfowler.com/bliki/CanaryRelease.html)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- See also: [docs/deployment/01-production-deployment-guide.md](01-production-deployment-guide.md)
- See also: [docs/deployment/03-health-checks.md](03-health-checks.md)
