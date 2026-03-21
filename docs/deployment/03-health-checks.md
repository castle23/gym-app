# Health Checks

## Overview

Service health check endpoints, monitoring strategies, and readiness probes for Gym Platform microservices. Proper health checks ensure reliable deployments and enable orchestration platforms to make intelligent routing decisions.

## Health Check Endpoints

### Spring Boot Actuator Health

All microservices expose health check endpoints via Spring Boot Actuator.

**Base URL Structure:**
```
http://{service-host}:{service-port}/actuator/health
```

### Service Health Endpoints

| Service | Port | Health Endpoint | Status Endpoint |
|---------|------|-----------------|-----------------|
| Auth | 8081 | `http://localhost:8081/actuator/health` | `http://localhost:8081/actuator/health/liveness` |
| Training | 8082 | `http://localhost:8082/actuator/health` | `http://localhost:8082/actuator/health/liveness` |
| Tracking | 8083 | `http://localhost:8083/actuator/health` | `http://localhost:8083/actuator/health/liveness` |
| Notification | 8084 | `http://localhost:8084/actuator/health` | `http://localhost:8084/actuator/health/liveness` |

### Health Check Response

**Healthy Service (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "status": 1880276582400,
        "total": 2199023255552,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Unhealthy Service (503 Service Unavailable):**
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused: connect"
      }
    }
  }
}
```

## Spring Boot Actuator Configuration

### application.yml Configuration

```yaml
management:
  # Enable all health indicators
  health:
    enabled-by-default: true
    circuitbreaker:
      enabled: true
    defaults:
      enabled: true

  # Health endpoint configuration
  endpoint:
    health:
      enabled: true
      show-details: when-authorized  # Show details only when authorized
      show-components: when-authorized
      probes:
        enabled: true

  # Endpoint exposure
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
      path-mapping:
        health: health

  # Metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      service: auth-service
      environment: production
```

### Custom Health Indicator

```java
@Component
public class CustomDatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Health health() {
        try {
            // Test database connection
            long userCount = userRepository.count();

            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("userCount", userCount)
                .withDetail("connection", "OK")
                .build();

        } catch (Exception ex) {
            return Health.down()
                .withException(ex)
                .withDetail("error", "Database connection failed")
                .build();
        }
    }
}

@Component
public class CacheHealthIndicator implements HealthIndicator {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public Health health() {
        try {
            boolean available = cacheManager.getCacheNames()
                .stream()
                .allMatch(name -> cacheManager.getCache(name) != null);

            if (available) {
                return Health.up()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "Available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "Unavailable")
                    .build();
            }
        } catch (Exception ex) {
            return Health.down()
                .withException(ex)
                .build();
        }
    }
}
```

## Kubernetes Probes

### Liveness Probe

Determines if container should be restarted. Checks if service is alive but possibly stuck.

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### Readiness Probe

Determines if service is ready to accept traffic. Checks if service is up and responding to requests.

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 2
```

### Startup Probe

Allows service time to start before liveness/readiness checks begin.

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/startup
    port: 8081
  failureThreshold: 30
  periodSeconds: 10
```

## Docker Health Checks

### Dockerfile Health Check

```dockerfile
# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1
```

### Docker Compose Health Check

```yaml
services:
  auth-service:
    image: gym-auth-service:latest
    ports:
      - "8081:8081"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    restart: on-failure

  postgres:
    image: postgres:15-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
```

## Monitoring Health Checks

### Manual Health Check

```bash
# Check single service
curl http://localhost:8081/actuator/health | jq

# Check all services
for port in 8081 8082 8083 8084; do
    echo "Checking port $port..."
    curl -s http://localhost:$port/actuator/health | jq '.status'
done

# Check with detailed output
curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8081/actuator/health | jq '.components'
```

### Continuous Monitoring Script

```bash
#!/bin/bash
# scripts/operational/health-check-monitor.sh

SERVICES=(
    "auth-service:8081"
    "training-service:8082"
    "tracking-service:8083"
    "notification-service:8084"
)

while true; do
    echo "=== Health Check Report ($(date)) ==="

    for service in "${SERVICES[@]}"; do
        IFS=':' read -r name port <<< "$service"
        response=$(curl -s -w "\n%{http_code}" http://localhost:$port/actuator/health)
        status=$(echo "$response" | tail -1)
        body=$(echo "$response" | head -1)

        if [ "$status" = "200" ]; then
            health=$(echo "$body" | jq -r '.status')
            echo "✓ $name ($port): $health"
        else
            echo "✗ $name ($port): HTTP $status"
        fi
    done

    echo ""
    sleep 30
done
```

### Prometheus Integration

**prometheus/prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']

  - job_name: 'training-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8082']

  - job_name: 'tracking-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8083']

  - job_name: 'notification-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8084']
```

## Health Status Indicators

### HTTP Status Codes

| Code | Status | Meaning |
|------|--------|---------|
| 200 | UP | Service is healthy and ready |
| 503 | DOWN | Service is unhealthy or not ready |
| 429 | OUT_OF_SERVICE | Service is temporarily unavailable |

### Health Status Values

```
- UP: Service is healthy
- DOWN: Service is not healthy
- OUT_OF_SERVICE: Service is available but cannot handle requests
- UNKNOWN: Health status cannot be determined
```

## Troubleshooting Health Checks

### Database Connection Issues

```bash
# Check database connectivity from container
docker exec gym-auth-service curl -s http://localhost:8081/actuator/health | jq '.components.db'

# Manual database test
docker exec gym-postgres psql -U postgres -c "SELECT 1;"

# Test from host machine
psql -h localhost -U postgres -d postgres -c "SELECT 1;"
```

### Service Not Responding

```bash
# Check if container is running
docker ps | grep gym-auth-service

# View container logs
docker logs gym-auth-service

# Check port binding
netstat -tuln | grep 8081

# Check connectivity from host
curl -v http://localhost:8081/actuator/health
```

### Timeout Issues

```bash
# Increase timeout in health check
curl --max-time 20 http://localhost:8081/actuator/health

# Check service performance metrics
docker exec gym-auth-service curl -s http://localhost:8081/actuator/metrics | jq '.names' | grep 'http'

# Monitor service during health check
docker stats gym-auth-service --no-stream
```

## Advanced Health Checks

### Circuit Breaker Health

```java
@Component
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers()
            .forEach(cb -> details.put(
                cb.getName(),
                cb.getState().toString()
            ));

        return Health.up()
            .withDetails(details)
            .build();
    }
}
```

### External Service Health Check

```java
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Health health() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "https://external-api.com/health",
                Map.class,
                2000
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("external-api", "OK")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }

        return Health.unknown().build();
    }
}
```

## Health Check Alerting

### Alert Rules (Prometheus)

```yaml
groups:
  - name: health_checks
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 2m
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service has been down for more than 2 minutes"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate on {{ $labels.job }}"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: DatabaseDown
        expr: pg_up{job="postgres"} == 0
        for: 1m
        annotations:
          summary: "Database is down"
```

## Key References

- [Spring Boot Actuator Documentation](https://spring.io/guides/gs/actuator-service/)
- [Kubernetes Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Docker Health Checks](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck)
- [Prometheus Alerting](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
- See also: [docs/operations/monitoring.md](../operations/monitoring.md)
- See also: [docs/troubleshooting/service-diagnostics.md](../troubleshooting/service-diagnostics.md)
