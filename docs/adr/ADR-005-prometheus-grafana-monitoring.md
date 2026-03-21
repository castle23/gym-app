# ADR-005: Prometheus & Grafana for Monitoring

## Status
Accepted

## Date
2026-03-21

## Context

With microservices and Kubernetes (ADRs 001, 004), monitoring becomes critical:

1. **Distributed System**: Services running across multiple instances
2. **Performance Visibility**: Need to see what's happening at scale
3. **Alerting**: Must know when things go wrong
4. **Troubleshooting**: Need data to diagnose issues
5. **Metrics**: Want to track business and technical metrics

The team evaluated:
- Prometheus + Grafana (open source)
- Datadog (commercial, all-in-one)
- New Relic (commercial, APM focused)
- ELK Stack (open source logs focused)
- CloudWatch (AWS-native)

## Decision

We chose **Prometheus for metrics collection** and **Grafana for visualization**:

1. **Prometheus**: Collects metrics from all services
2. **Grafana**: Visualizes metrics, creates dashboards
3. **AlertManager**: Routes alerts
4. **Kubernetes-native**: Works well with Kubernetes

## Rationale

### 1. Open Source & Cost
- Free (no licensing costs)
- No vendor lock-in
- Can run on own infrastructure
- Community support

### 2. Kubernetes Native
- Scrapes Kubernetes metrics automatically
- ServiceMonitor for service discovery
- Native Prometheus Operator support
- Built for container environments

### 3. Powerful Querying
- PromQL for flexible metric queries
- Time-series database optimized for metrics
- Aggregations and alerting queries built-in

### 4. Scalability
- Federated Prometheus instances
- Thanos for long-term storage
- Handles millions of metrics

### 5. Alerting & Visualization
- AlertManager for sophisticated routing
- Grafana dashboards are beautiful and interactive
- Webhooks for integrations (PagerDuty, Slack, etc.)

### 6. Ecosystem
- 1000+ exporters available
- Integration with all major frameworks
- Community dashboards
- Good tooling

## Consequences

### Positive
- ✅ Free and open source
- ✅ Kubernetes-native
- ✅ Powerful querying
- ✅ Beautiful dashboards
- ✅ Good community
- ✅ No vendor lock-in

### Negative
- ❌ Metrics-focused (not logs)
- ❌ Short-term storage by default (need Thanos for long-term)
- ❌ Requires operational expertise
- ❌ Alerting less sophisticated than commercial tools
- ❌ Doesn't include APM (application performance monitoring)

## Alternatives Considered

### 1. Datadog
- **Pros**: All-in-one (metrics, logs, APM), easy setup
- **Cons**: Expensive, vendor lock-in, overkill for our needs
- **Why not**: Too expensive for current scale

### 2. New Relic
- **Pros**: Good APM, beautiful UI
- **Cons**: Expensive, vendor lock-in
- **Why not**: Prometheus + APM tool more cost-effective

### 3. ELK Stack (Elasticsearch, Logstash, Kibana)
- **Pros**: Good for logs, open source
- **Cons**: Focused on logs not metrics, more complex, more storage
- **Why not**: We need metrics-focused, not logs

### 4. CloudWatch (AWS-native)
- **Pros**: Works well with AWS
- **Cons**: AWS lock-in, more expensive, limited querying
- **Why not**: Want cloud-agnostic solution

## Related ADRs

- **Depends on**: ADR-004 (Monitoring Kubernetes)
- **Related to**: ADR-011 (Security of monitoring data)

## Implementation Strategy

### Metrics to Collect

**Application Metrics:**
- Request latency (P50, P95, P99)
- Request rate (requests/second)
- Error rate (5xx errors)
- HTTP status codes

**Business Metrics:**
- Users created
- Workouts completed
- Calories logged
- API calls per user

**Infrastructure Metrics:**
- CPU usage
- Memory usage
- Disk usage
- Network I/O

**Kubernetes Metrics:**
- Pod restarts
- Node capacity
- Pod resource usage

### Alert Examples

```
Alert: High Error Rate
  Trigger: error_rate > 5% for 5 minutes
  Action: Notify engineering team

Alert: Service Down
  Trigger: service_requests == 0 for 2 minutes
  Action: Page on-call engineer

Alert: High Latency
  Trigger: p95_latency > 1000ms for 10 minutes
  Action: Notify team, create incident
```

## Future Considerations

- Add Thanos for long-term metric storage and query
- Add distributed tracing (Jaeger) for APM
- Add centralized logging (ELK or Loki)
- Fine-tune alerting rules as we learn patterns
