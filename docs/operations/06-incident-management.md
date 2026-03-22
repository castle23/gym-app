# Incident Management

## Overview

Incident response procedures, escalation paths, communication protocols, and post-incident analysis for Gym Platform operations.

## Incident Severity Levels

| Severity | Impact | Response Time | Example |
|----------|--------|----------------|---------|
| **Critical (P1)** | Service down, data loss risk | 5 minutes | All services down, database failure |
| **High (P2)** | Degraded performance | 15 minutes | 50% error rate, latency > 5s |
| **Medium (P3)** | Limited functionality | 1 hour | Single endpoint failing, 10% error rate |
| **Low (P4)** | Minor issue | 8 hours | Cosmetic bug, documentation issue |

## Incident Response Flow

```
┌─────────────────────────────────────────────────────┐
│  Incident Detected (Alert / User Report)            │
└─────────────────┬───────────────────────────────────┘
                  │
        ┌─────────▼──────────┐
        │   Page On-Call     │
        │  (via PagerDuty)   │
        └─────────┬──────────┘
                  │
        ┌─────────▼──────────────┐
        │  Initial Assessment    │
        │  - Severity level      │
        │  - Affected services   │
        │  - Customer impact     │
        └─────────┬──────────────┘
                  │
        ┌─────────▼──────────────┐
        │ Escalate if needed     │
        │ - Open war room        │
        │ - Notify stakeholders  │
        └─────────┬──────────────┘
                  │
        ┌─────────▼──────────────┐
        │  Investigation &       │
        │  Root Cause Analysis   │
        │  - Check logs          │
        │  - Query metrics       │
        │  - Review changes      │
        └─────────┬──────────────┘
                  │
        ┌─────────▼──────────────┐
        │  Implement Fix/        │
        │  Mitigation            │
        │  - Deploy hotfix       │
        │  - Scale resources     │
        │  - Route traffic       │
        └─────────┬──────────────┘
                  │
        ┌─────────▼──────────────┐
        │  Verify Resolution     │
        │  - Check health checks │
        │  - Monitor metrics     │
        │  - Confirm functionality
        └─────────┬──────────────┘
                  │
        ┌─────────▼──────────────┐
        │  Update Status Page    │
        │  - Close incident      │
        │  - Notify users        │
        └─────────┬──────────────┘
                  │
        ┌─────────▼──────────────┐
        │  Post-Incident Review  │
        │  - Document timeline   │
        │  - RCA meeting         │
        │  - Action items        │
        └────────────────────────┘
```

## Incident Response Runbook

### On-Call Responsibilities

**First Response (< 5 min):**
```bash
1. Check if you're in war room
2. Acknowledge alert/page
3. Review alert details and dashboard
4. Check status page: is it already updated?
5. Initial severity assessment
6. Post initial status to Slack #incidents
```

**Investigation (< 15 min):**
```bash
# Check service health
curl http://localhost:8081/auth/actuator/health | jq

# View recent logs
docker-compose logs -f --tail=100 auth-service

# Check CPU/memory
docker stats --no-stream

# Query database
docker exec gym-postgres psql -U gym_admin -d gym_db -c "SELECT NOW();"

# Check Prometheus metrics (if configured)
# curl 'http://localhost:9090/api/v1/query?query=up{job="auth-service"}'
```

### Common Incidents & Resolutions

**Incident: Service Down (P1)**

```bash
#!/bin/bash
# scripts/incident-response/investigate-service-down.sh

SERVICE=$1  # e.g., "auth-service"
PORT=$2     # e.g., "8081"

echo "=== Investigating $SERVICE down ==="

# 1. Check container status
echo "Container status:"
docker ps | grep "$SERVICE"

# 2. Check if service is running
echo "Service health:"
curl -s -f "http://localhost:$PORT/actuator/health" && echo "OK" || echo "FAILED"
# 3. Check recent logs for errors
echo "Recent errors:"
docker logs "$SERVICE" | grep ERROR | tail -20

# 4. Check resource usage
echo "Resource usage:"
docker stats --no-stream | grep "$SERVICE"

# 5. Check database connectivity
echo "Database connectivity:"
docker exec gym-postgres psql -U gym_admin -d gym_db -c "SELECT 1;"

# 6. Restart service
echo "Attempting restart..."
docker restart "$SERVICE"
sleep 10

# 7. Verify recovery
echo "Verifying recovery..."
for i in {1..30}; do
    if curl -s -f "http://localhost:$PORT/actuator/health" > /dev/null; then
        echo "Service recovered successfully"
        exit 0
    fi
    echo "Waiting... ($i/30)"
    sleep 1
done

echo "Service failed to recover"
exit 1
```

**Incident: High Error Rate (P2)**

```bash
#!/bin/bash
# scripts/incident-response/investigate-high-error-rate.sh

echo "=== Investigating high error rate ==="

# Check error rate by endpoint (requires Prometheus)
# curl -s 'http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[5m]) by (endpoint)'

# Check recent errors
echo "Recent errors in logs:"
docker-compose logs -f --tail=50 auth-service | grep ERROR

# Check database performance
echo "Database query performance:"
docker exec gym-postgres psql -U gym_admin -d gym_db -c \
    "SELECT query, mean_exec_time FROM pg_stat_statements WHERE mean_exec_time > 1000 ORDER BY mean_exec_time DESC LIMIT 10;"

# Check service dependencies
echo "Dependency health:"
curl -s http://localhost:8081/actuator/health/dependencies | jq

# If database is slow: restart vacuum
echo "Running VACUUM ANALYZE..."
docker exec gym-postgres psql -U postgres -d gym_db -c "VACUUM ANALYZE;"

# If service is struggling: increase resources
echo "Check if we need to scale up..."
docker stats --no-stream | grep -E "auth|training|tracking"
```

**Incident: Database Connection Pool Exhausted (P1)**

```bash
#!/bin/bash
# scripts/incident-response/investigate-connection-pool.sh

echo "=== Investigating connection pool issues ==="

# Check active connections
echo "Active connections:"
docker exec gym-postgres psql -U gym_admin -d gym_db -c \
    "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"

# Check idle connections
echo "Idle connections (potential leak):"
docker exec gym-postgres psql -U gym_admin -d gym_db -c \
    "SELECT pid, usename, state, query FROM pg_stat_activity WHERE state = 'idle' AND query_start < NOW() - INTERVAL '5 minutes';"

# Terminate idle connections
echo "Terminating idle connections..."
docker exec gym-postgres psql -U gym_admin -d gym_db -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < NOW() - INTERVAL '30 minutes';"

# Check connection pool settings
echo "Connection pool settings:"
docker exec gym-auth-service grep "maximum-pool-size\|minimum-idle" /app/application.yml

# Increase connection pool temporarily
echo "Consider scaling service replicas..."
```

## Communication Protocol

### Incident Slack Channel

```
#incidents channel:

[09:15] @oncall: 🚨 P1 INCIDENT: Auth Service Down
- Status: INVESTIGATING
- Affected: Login endpoints
- Users impacted: ~500 active users
- Last successful request: 09:14
- On-call: @alice, @bob

[09:17] @alice: Found issue: Docker container restarted
- Checking logs for root cause

[09:20] @alice: Root cause identified
- Database connection timeout
- Running VACUUM on affected tables

[09:23] @bob: Mitigation deployed
- Restarted auth service
- Health check: OK
- Monitoring error rate

[09:25] @alice: ✅ RESOLVED
- Service fully recovered
- Error rate: < 1%
- All endpoints responding normally
- War room closing

[09:30] @alice: 📋 POST-INCIDENT SCHEDULED
- RCA meeting: Tomorrow 10:00 AM
- War room: Slack thread
```

### Status Page Updates

```
Initial notification:
"We are investigating an issue with the authentication service. 
Some users may experience login failures. Estimated impact: TBD"

Updated (investigating):
"We have identified the issue related to database connections. 
Our team is implementing a fix. ETA: 15 minutes"

Updated (fixing):
"A fix has been deployed. We are monitoring the system for stability. 
Service should be fully restored shortly."

Resolution:
"The issue has been resolved. All services are operating normally. 
We apologize for the inconvenience and will provide a full incident 
report within 24 hours."
```

## Post-Incident Review

### Incident Report Template

```markdown
# Incident Report: [Title]

## Executive Summary
Brief 1-2 sentence summary of the incident

## Timeline
- **09:15** - Alert triggered: Auth service health check failed
- **09:16** - On-call responded, war room opened
- **09:20** - Root cause identified: Connection pool exhaustion
- **09:23** - Mitigation deployed: Service restarted
- **09:25** - All systems normal, incident resolved
- **09:30** - Status page updated

## Root Cause Analysis
During peak traffic, the database experienced high load. Query performance 
degraded, causing connections to accumulate in the pool. The pool was exhausted,
causing all new connections to fail.

## Contributing Factors
1. Increase in traffic (20% above baseline)
2. Missing query optimization on new endpoint
3. Connection pool timeout too aggressive

## Impact
- Duration: 10 minutes
- Users affected: ~500 active users
- Login attempts failed: 2,451
- Revenue impact: $0 (free tier)

## Resolution
1. Restarted auth service to clear connection pool
2. Identified slow query in user lookup
3. Added database index on frequently filtered column
4. Increased connection pool timeout

## Preventive Actions
- [ ] Optimize queries identified in slow query log
- [ ] Add automated query performance monitoring
- [ ] Increase connection pool max size (Ticket: #123)
- [ ] Add circuit breaker for database connections
- [ ] Load test new endpoints before release

## Lessons Learned
1. Need better pre-production load testing
2. Connection pool metrics not monitored
3. Incident response time was good, but root cause took longer

## Owner
@alice (DevOps)

## Timeline Review
- **Created**: 2024-01-15
- **Reviewed**: 2024-01-16 (Team standup)
- **Closed**: 2024-01-17
```

## Incident Metrics

### Track These Metrics

```yaml
Metrics:
  - MTTR (Mean Time To Recovery): < 15 min
  - MTTD (Mean Time To Detect): < 5 min
  - MTBF (Mean Time Between Failures): > 720 hours
  - False alert ratio: < 5%
  - RCA completion rate: 100% within 48 hours
```

### Dashboard Query

```promql
# Incident frequency
rate(incidents_total[30d])

# Average MTTR
avg(mttr_seconds)

# Service availability
(1 - (incidents_total / business_hours)) * 100
```

## Escalation Path

```
Level 1: On-Call Engineer
  ├─ P1/P2: Immediate investigation
  ├─ P3: Within 1 hour
  └─ P4: Within 8 hours

Level 2: Senior Engineer / Team Lead
  ├─ Triggered if: P1 for > 10 minutes
  ├─ OR: P2 for > 30 minutes
  └─ Actions: Additional investigation, technical guidance

Level 3: Engineering Manager
  ├─ Triggered if: P1 for > 30 minutes
  ├─ Actions: Customer communication, resource allocation
  └─ Calls: CTO, Product Manager

Level 4: Executive
  ├─ Triggered if: P1 for > 1 hour
  ├─ Actions: Customer communication, transparency
  └─ Calls: VP Ops, CEO
```

## Tools & Resources

**Monitoring & Alerting** (if configured):
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- AlertManager: http://localhost:9093

**Debugging Tools:**
```bash
# jstack for thread dumps
jstack <pid> > thread_dump.txt

# jmap for heap dumps
jmap -dump:live,format=b,file=heap.bin <pid>

# jps for process listing
jps -lmv

# Database profiling
pg_stat_statements
pg_stat_activity
EXPLAIN ANALYZE
```

## Key References

- [Google SRE Handbook - Incident Response](https://sre.google/sre-book/managing-incidents/)
- [PagerDuty Incident Response](https://response.pagerduty.com/)
- See also: [docs/operations/01-operational-runbook.md](01-operational-runbook.md)
- See also: [docs/troubleshooting/](../troubleshooting/)
