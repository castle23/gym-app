# Monitoring

> **Note**: The monitoring stack described in this document (Prometheus, Grafana, AlertManager, Node Exporter, cAdvisor, postgres-exporter) is **not currently configured** in the project. The current setup uses Docker Compose with Spring Boot Actuator health endpoints only. This document describes the target monitoring architecture for future implementation.

## Overview

Comprehensive monitoring setup for Gym Platform microservices including metrics collection, performance dashboards, alerting, and observability strategies.

**Monitoring Stack:**
- Prometheus (metrics collection)
- Grafana (visualization)
- AlertManager (alerting)
- Node Exporter (system metrics)
- Spring Boot Actuator (application metrics)

## Monitoring Architecture

```
┌──────────────────────────────────────────────────────┐
│              Gym Microservices                        │
│  (Auth, Training, Tracking, Notification + Database) │
└────────────────┬─────────────────────────────────────┘
                 │ Expose metrics
                 ▼
         ┌──────────────────┐
         │ Spring Actuator  │
         │ /actuator/metrics│
         │ /prometheus      │
         └────────┬─────────┘
                  │
         ┌────────▼──────────┐
         │   Prometheus      │
         │ (scrapes every    │
         │   15 seconds)     │
         └────────┬──────────┘
                  │
         ┌────────▼──────────────┐
         │      Grafana          │
         │ (dashboards & alerts) │
         └────────┬──────────────┘
                  │
         ┌────────▼──────────┐
         │   AlertManager    │
         │ (sends webhooks)  │
         └───────────────────┘
```

## Prometheus Configuration

### prometheus.yml Setup

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    environment: production
    region: us-east-1

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - '/etc/prometheus/rules/*.yml'

scrape_configs:
  # Auth Service
  - job_name: 'auth-service'
    metrics_path: '/auth/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
    static_configs:
      - targets: ['auth-service:8081']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance

  # Training Service
  - job_name: 'training-service'
    metrics_path: '/training/actuator/prometheus'
    static_configs:
      - targets: ['training-service:8082']

  # Tracking Service
  - job_name: 'tracking-service'
    metrics_path: '/tracking/actuator/prometheus'
    static_configs:
      - targets: ['tracking-service:8083']

  # Notification Service
  - job_name: 'notification-service'
    metrics_path: '/notifications/actuator/prometheus'
    static_configs:
      - targets: ['notification-service:8084']

  # PostgreSQL Exporter
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Node metrics
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']

  # Docker metrics
  - job_name: 'docker'
    static_configs:
      - targets: ['cadvisor:8080']
```

## Key Metrics to Monitor

### Application Metrics

```promql
# Request rate (requests per second)
rate(http_requests_total[5m])

# Request latency (p95)
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Error rate
rate(http_requests_total{status=~"5.."}[5m])

# Active threads
jvm_threads_live_threads

# Heap memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# GC time
rate(jvm_gc_memory_promoted_bytes_total[5m])

# Connection pool utilization
hikaricp_connections_active / hikaricp_connections_max * 100

# Database query duration
rate(database_query_duration_ms_bucket[5m])
```

### System Metrics

```promql
# CPU usage
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Memory usage
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# Disk usage
(1 - (node_filesystem_avail_bytes{fstype!~"tmpfs|fuse.lowerdir"} / 
       node_filesystem_size_bytes{fstype!~"tmpfs|fuse.lowerdir"})) * 100

# Network I/O
rate(node_network_receive_bytes_total[5m])
rate(node_network_transmit_bytes_total[5m])

# Disk I/O
rate(node_disk_read_bytes_total[5m])
rate(node_disk_write_bytes_total[5m])
```

## Docker Compose Monitoring Stack

```yaml
version: '3.8'

services:
  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: gym-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./prometheus/rules:/etc/prometheus/rules:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    networks:
      - gym-network
    restart: unless-stopped

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: gym-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
      GF_USERS_ALLOW_SIGN_UP: 'false'
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - gym-network
    restart: unless-stopped

  # AlertManager
  alertmanager:
    image: prom/alertmanager:latest
    container_name: gym-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    networks:
      - gym-network
    restart: unless-stopped

  # Node Exporter
  node-exporter:
    image: prom/node-exporter:latest
    container_name: gym-node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - gym-network
    restart: unless-stopped

  # PostgreSQL Exporter
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    container_name: gym-postgres-exporter
    environment:
      DATA_SOURCE_NAME: "postgresql://gym_admin:${DB_PASSWORD}@postgres:5432/gym_db?sslmode=disable"
    ports:
      - "9187:9187"
    networks:
      - gym-network
    depends_on:
      - postgres
    restart: unless-stopped

  # cAdvisor (Docker stats)
  cadvisor:
    image: gcr.io/cadvisor/cadvisor:latest
    container_name: gym-cadvisor
    ports:
      - "8080:8080"
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
    networks:
      - gym-network
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:

networks:
  gym-network:
    external: true
```

## Grafana Dashboards

### Dashboard 1: Service Health

```json
{
  "dashboard": {
    "title": "Service Health",
    "panels": [
      {
        "title": "Request Rate (req/s)",
        "targets": [
          {"expr": "sum(rate(http_requests_total[5m])) by (service)"}
        ]
      },
      {
        "title": "Error Rate (%)",
        "targets": [
          {"expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])) * 100"}
        ]
      },
      {
        "title": "Latency (p95, ms)",
        "targets": [
          {"expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) * 1000"}
        ]
      },
      {
        "title": "Service Availability",
        "targets": [
          {"expr": "(count(up{job=~\".*-service\"} == 1) / count(up{job=~\".*-service\"})) * 100"}
        ]
      }
    ]
  }
}
```

### Dashboard 2: Resource Usage

```json
{
  "dashboard": {
    "title": "Resource Usage",
    "panels": [
      {
        "title": "CPU Usage (%)",
        "targets": [
          {"expr": "rate(container_cpu_usage_seconds_total[5m]) * 100"}
        ]
      },
      {
        "title": "Memory Usage (GB)",
        "targets": [
          {"expr": "container_memory_usage_bytes / 1024 / 1024 / 1024"}
        ]
      },
      {
        "title": "Disk Space Available (GB)",
        "targets": [
          {"expr": "node_filesystem_avail_bytes{mountpoint=\"/\"} / 1024 / 1024 / 1024"}
        ]
      },
      {
        "title": "Network I/O (MB/s)",
        "targets": [
          {"expr": "rate(node_network_receive_bytes_total[5m]) / 1024 / 1024"}
        ]
      }
    ]
  }
}
```

## Alerting Rules

### prometheus/rules/alerts.yml

```yaml
groups:
  - name: service_alerts
    interval: 30s
    rules:
      # Service availability
      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 2m
        annotations:
          summary: "{{ $labels.job }} is down"
          description: "Service {{ $labels.job }} has been down for 2 minutes"

      # High error rate
      - alert: HighErrorRate
        expr: |
          (sum(rate(http_requests_total{status=~"5.."}[5m])) by (job) / 
           sum(rate(http_requests_total[5m])) by (job)) > 0.05
        for: 5m
        annotations:
          summary: "High error rate on {{ $labels.job }}"
          description: "Error rate is {{ $value | humanizePercentage }}"

      # High latency
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 5m
        annotations:
          summary: "High latency detected"
          description: "95th percentile latency is {{ $value | humanizeDuration }}"

      # Database down
      - alert: DatabaseDown
        expr: pg_up == 0
        for: 1m
        annotations:
          summary: "PostgreSQL database is down"
          description: "Database connection failed"

      # High CPU usage
      - alert: HighCPUUsage
        expr: rate(container_cpu_usage_seconds_total[5m]) * 100 > 80
        for: 5m
        annotations:
          summary: "High CPU usage on {{ $labels.container_name }}"
          description: "CPU usage is {{ $value | humanizePercentage }}"

      # High memory usage
      - alert: HighMemoryUsage
        expr: (container_memory_usage_bytes / 134217728) > 0.9
        for: 5m
        annotations:
          summary: "High memory usage on {{ $labels.container_name }}"
          description: "Memory usage is {{ $value | humanizePercentage }}"

      # Low disk space
      - alert: LowDiskSpace
        expr: (1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) > 0.85
        for: 5m
        annotations:
          summary: "Low disk space"
          description: "Only {{ $value | humanizePercentage }} free space remaining"

      # Connection pool saturation
      - alert: ConnectionPoolSaturation
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        annotations:
          summary: "Database connection pool is saturated"
          description: "{{ $value | humanizePercentage }} of connections in use"
```

## Monitoring Queries

### Useful PromQL Queries

```promql
# List all services and their status
up{job=~".*-service"}

# Request distribution by endpoint
sum(rate(http_requests_total[5m])) by (endpoint)

# Request distribution by status code
sum(rate(http_requests_total[5m])) by (status)

# Service load over time
sum(rate(http_requests_total[5m])) by (job)

# Database query performance
histogram_quantile(0.99, rate(database_query_duration_ms_bucket[5m]))

# Slowest endpoints
topk(10, rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m]))

# Memory leak detection (increasing over time)
rate(jvm_memory_used_bytes{area="heap"}[1h])

# Garbage collection pressure
rate(jvm_gc_pause_seconds_sum[5m])
```

## Custom Metrics

### Application Metrics Configuration

```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder customMetrics() {
        return (registry) -> {
            // Custom counter
            Counter.builder("gym.users.created")
                .description("Total users created")
                .register(registry);

            // Custom gauge
            Gauge.builder("gym.active.sessions", () -> getActiveSessions())
                .description("Active user sessions")
                .register(registry);

            // Custom timer
            Timer.builder("gym.workout.duration")
                .description("Workout duration")
                .register(registry);
        };
    }

    private long getActiveSessions() {
        // Implementation
        return 0;
    }
}
```

## Monitoring Best Practices

1. **Monitor the right metrics** - Focus on business and system metrics
2. **Set appropriate thresholds** - Based on SLA targets
3. **Alert on trends** - Not just absolute values
4. **Keep history** - Retain data for analysis
5. **Test alerts** - Verify alerting chain works
6. **Document runbooks** - Link alerts to resolution procedures
7. **Reduce alert fatigue** - Avoid too many false positives
8. **Monitor monitoring** - Ensure monitoring system is healthy

## Key References

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator Metrics](https://spring.io/blog/2018/03/16/micrometer-spring-boot-2-s-new-application-metrics-collector)
- See also: [docs/deployment/03-health-checks.md](../deployment/03-health-checks.md)
- See also: [docs/operations/03-logging.md](03-logging.md)
