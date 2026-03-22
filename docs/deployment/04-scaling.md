# Scaling Guide

> **Note**: This document describes scaling strategies for future reference. The current setup is a single-server Docker Compose deployment with no load balancer, Redis, or read replicas. Kubernetes HPA, PgBouncer, and multi-region configurations are not currently implemented.

## Overview

Horizontal and vertical scaling strategies, load balancing, and capacity planning for Gym Platform microservices to handle growth and maintain performance.

## Scaling Strategies

### Vertical Scaling (Scale Up)

Increase resources on existing servers (CPU, RAM).

**Pros:**
- Simple to implement
- No code changes required
- Better for single-threaded applications

**Cons:**
- Limited by hardware ceiling
- Single point of failure
- Downtime may be required

**Implementation:**

```bash
# Increase JVM memory
export JAVA_OPTS="-Xms2G -Xmx4G -XX:+UseG1GC"
java $JAVA_OPTS -jar auth-service.jar

# Increase database connection pool
# In application.yml:
# spring:
#   datasource:
#     hikari:
#       maximum-pool-size: 50  # was 20
```

### Horizontal Scaling (Scale Out)

Add more servers/containers to distribute load.

**Pros:**
- No hardware ceiling
- High availability
- Better fault tolerance
- Cost-effective

**Cons:**
- Requires load balancer
- More complex architecture
- Session/state management needed

**Implementation with Docker Compose:**

```bash
# Scale service to 3 replicas
docker-compose up -d --scale auth-service=3

# Scale multiple services
docker-compose up -d \
  --scale auth-service=3 \
  --scale training-service=3 \
  --scale tracking-service=2 \
  --scale notification-service=1
```

## Load Balancing

### Nginx Load Balancer

**nginx/conf.d/load-balancing.conf:**

```nginx
upstream auth_service_cluster {
    least_conn;  # Load balancing algorithm
    server auth-service-1:8081 weight=1 max_fails=3 fail_timeout=30s;
    server auth-service-2:8081 weight=1 max_fails=3 fail_timeout=30s;
    server auth-service-3:8081 weight=1 max_fails=3 fail_timeout=30s;
}

upstream training_service_cluster {
    least_conn;
    server training-service-1:8082 weight=1;
    server training-service-2:8082 weight=1;
    server training-service-3:8082 weight=1;
}

server {
    listen 80;
    server_name api.gym.local;

    # Auth service endpoints
    location /auth/ {
        proxy_pass http://auth_service_cluster;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Connection settings
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # Buffering
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        proxy_busy_buffers_size 8k;

        # Keepalive
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }

    # Training service endpoints
    location /training/ {
        proxy_pass http://training_service_cluster;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}
```

### Load Balancing Algorithms

| Algorithm | Best For | Configuration |
|-----------|----------|----------------|
| **Round Robin** | Uniform server capacity | `upstream { server a; server b; }` |
| **Least Conn** | Variable response times | `upstream { least_conn; ... }` |
| **IP Hash** | Session persistence | `upstream { ip_hash; ... }` |
| **Weighted** | Heterogeneous servers | `server a weight=3;` |

### Session Persistence

```nginx
upstream auth_service_cluster {
    # Sticky cookie for session persistence
    sticky cookie srv_id expires=1h path=/ httponly secure;

    server auth-service-1:8081 route=srv1;
    server auth-service-2:8081 route=srv2;
    server auth-service-3:8081 route=srv3;
}
```

## Auto-Scaling

### Kubernetes Auto-Scaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Max
```

### Docker Compose with CPU/Memory Limits

```yaml
services:
  auth-service-1:
    image: gym-auth-service:latest
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M
```

## Performance Monitoring

### Key Metrics to Track

```
CPU Usage: < 70% (warning) / < 85% (critical)
Memory: < 80% (warning) / < 90% (critical)
Response Time: < 500ms (p95) / < 1000ms (p99)
Error Rate: < 1% / > 5% (critical)
Throughput: Requests per second
Connection Pool: Utilized connections
```

### Prometheus Queries

```promql
# CPU usage by service
container_cpu_usage_seconds_total{pod=~".*-service"}

# Memory usage
container_memory_usage_bytes{pod=~".*-service"}

# Response time percentiles
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Error rate
rate(http_requests_total{status=~"5.."}[5m])

# Active connections
pg_connections_used

# Database query performance
pg_query_duration_ms_bucket
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Gym Platform Services",
    "panels": [
      {
        "title": "CPU Usage",
        "targets": [
          {
            "expr": "rate(container_cpu_usage_seconds_total[5m]) * 100"
          }
        ]
      },
      {
        "title": "Memory Usage",
        "targets": [
          {
            "expr": "container_memory_usage_bytes / 1024 / 1024"
          }
        ]
      },
      {
        "title": "Request Latency (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m])"
          }
        ]
      }
    ]
  }
}
```

## Capacity Planning

### Growth Projections

```
Current State:
- 1000 active users
- Auth Service: 8081 handling ~100 req/s
- 4 GB RAM usage
- 2 CPUs at 45% utilization

Growth Scenario (6 months):
- 10,000 active users (10x growth)
- Need: ~1000 req/s capacity
- Projected: 20-30 GB RAM, 8 CPUs
- Recommendation: 3 service replicas minimum, larger instances
```

### Scaling Timeline

```
Phase 1 (Current): Single server
┌─────────────────────┐
│ 4 services + 1 DB   │
│ 1 server (4 CPU)    │
└─────────────────────┘

Phase 2 (3-6 months): Service separation
┌──────────────┐  ┌──────────────┐
│ App Server   │  │ DB Server    │
│ 4 services   │  │ PostgreSQL   │
│ 8 CPU        │  │ 16 GB RAM    │
└──────────────┘  └──────────────┘

Phase 3 (6-12 months): Service clustering
┌────────────────────────────────────────┐
│  Load Balancer (Nginx)                 │
├────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │Auth x3   │ │Training  │ │Tracking  │ │
│ │Notif x2  │ │x3        │ │x3        │ │
│ └──────────┘ └──────────┘ └──────────┘ │
├────────────────────────────────────────┤
│  PostgreSQL Cluster (Primary + Replica)│
└────────────────────────────────────────┘
```

## Database Scaling

### Read Replicas

```
Master DB (Write)
    │
    ├─→ Replica 1 (Read)
    ├─→ Replica 2 (Read)
    └─→ Replica 3 (Read)
```

**Configuration:**

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource writeDataSource() {
        return createDataSource(
            "jdbc:postgresql://db-master:5432/gym_db",
            "gym_user",
            "password"
        );
    }

    @Bean
    public DataSource readDataSource() {
        return createDataSource(
            "jdbc:postgresql://db-replica-1:5432/gym_db",
            "gym_user",
            "password"
        );
    }

    @Bean
    public RoutingDataSource routingDataSource() {
        RoutingDataSource dataSource = new RoutingDataSource();
        dataSource.setDefaultTargetDataSource(writeDataSource());
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("READ", readDataSource());
        targetDataSources.put("WRITE", writeDataSource());
        
        dataSource.setTargetDataSources(targetDataSources);
        return dataSource;
    }

    private DataSource createDataSource(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        return new HikariDataSource(config);
    }
}
```

### Sharding Strategy

```
Users database:
├── Shard 1: users 0000-3FFF
├── Shard 2: users 4000-7FFF
├── Shard 3: users 8000-BFFF
└── Shard 4: users C000-FFFF

Sharding key: userId % 4
```

## Caching Strategy

### Distributed Caching (Redis)

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    networks:
      - gym-network

volumes:
  redis_data:
```

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisCacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        return RedisCacheManager.create(connectionFactory);
    }
}
```

## Cost Optimization

### Resource Analysis

```bash
# Calculate resource usage per service
docker stats --no-stream --format \
  "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | \
  awk 'NR>1 {
    gsub(/m$/, "", $2); gsub(/%/, "", $2);
    gsub(/[MG]B.*/, "", $3);
    cost = ($2 * 0.05) + ($3 * 0.10);
    print $1, "CPU Cost: $" $2 * 0.05, "Memory Cost: $" $3 * 0.10, "Total: $" cost
  }'
```

### Resource Optimization Tips

1. **Right-size containers** - Don't over-allocate resources
2. **Use spot instances** - For non-critical workloads
3. **Schedule scaling** - Scale down during off-peak hours
4. **Monitor waste** - Identify unused resources
5. **Optimize images** - Smaller image = faster deployment

## Scaling Checklist

- [ ] Load balancer configured
- [ ] Health checks enabled
- [ ] Service discovery configured
- [ ] Database scaling plan ready
- [ ] Cache strategy implemented
- [ ] Monitoring and alerting set up
- [ ] Auto-scaling rules defined
- [ ] Failover procedures documented
- [ ] Capacity projections made
- [ ] Cost analysis completed

## Key References

- [Nginx Load Balancing Documentation](https://nginx.org/en/docs/http/load_balancing.html)
- [Kubernetes Horizontal Pod Autoscaler](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
- [Docker Compose Scaling](https://docs.docker.com/compose/compose-file/compose-file-v3/#deploy)
- [PostgreSQL Replication](https://www.postgresql.org/docs/current/warm-standby.html)
- [Redis Clustering](https://redis.io/topics/cluster-tutorial)
- See also: [docs/operations/performance-tuning.md](../operations/performance-tuning.md)
- See also: [docs/deployment/03-health-checks.md](03-health-checks.md)
