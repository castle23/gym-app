# ADR-010: Disaster Recovery & High Availability

## Status
Accepted

## Date
2026-03-21

## Context

The Gym Platform is a critical service. User expectations:
- Service should be available 24/7
- Data should never be lost
- Recovery should be fast

Potential disasters:
1. **Service Failure**: Bug causes service to crash
2. **Data Loss**: Database corruption or failure
3. **Hardware Failure**: Server/disk failure
4. **Network Failure**: Connectivity issues
5. **Entire Data Center**: Fire, flood, natural disaster
6. **Ransomware/Attack**: Security breach

Traditional approach: "Hope it doesn't happen." But we need better.

Requirements:
1. **Availability**: 99.9% (3 nines) = ~8.6 hours downtime/year max
2. **Recovery Time** (RTO): <= 1 hour to recover
3. **Recovery Point** (RPO): <= 1 day of data loss acceptable
4. **Geographic Redundancy**: Survive single data center failure
5. **Automated**: Don't rely on manual intervention

## Decision

We implemented **multi-layered HA strategy**:

1. **Application Level HA**: Multiple instances, auto-restart
2. **Database HA**: Primary + read replica in same region
3. **Backup & Recovery**: S3 backups (ADR-009) for point-in-time
4. **Geographic Redundancy**: Multi-region failover capability
5. **Health Monitoring**: Continuous health checks (ADR-005)

## Rationale

### 1. Application Level (Kubernetes)
Kubernetes provides:
- Multiple pod replicas (auto-restart on failure)
- Rolling updates (zero downtime deployments)
- Load balancing (distribute traffic)
- Service discovery (automatic)

```
Without Kubernetes:
  Server Down → Customers impacted immediately

With Kubernetes:
  Server Down → Automatically restarted → No customer impact
```

### 2. Database HA (Primary + Replica)
PostgreSQL streaming replication provides:
- Standby replica in same region
- Automatic failover if primary fails
- Read scaling (read-heavy queries use replica)
- Low RPO (replica is nearly real-time copy)

```
Scenario: Database server fails
  Primary (crashed) → failover → Replica takes over
  Downtime: ~30 seconds
```

### 3. Backups & Recovery
S3 backups enable:
- Recovery from data corruption (restore old version)
- Compliance (audit trail, retention)
- Disaster recovery (from S3 in different region)
- Testing (restore to test environment)

### 4. Multi-Region Failover
For extreme disasters (entire region down):
- Kubernetes cluster replicates to different region
- Route traffic to healthy region
- Database restored from S3 backup

```
Region 1 (Primary)
├── Kubernetes cluster
├── PostgreSQL primary
└── Services running

Region 2 (Standby)
├── Kubernetes cluster (ready)
├── Could restore database here
└── DNS points to healthy region
```

### 5. Automated Health Checks
Prometheus + Grafana (ADR-005) provides:
- Continuous monitoring
- Automatic alerts
- Quick incident response

## Consequences

### Positive
- ✅ Achieves 99.9% availability (RTO < 1 hour, RPO < 1 day)
- ✅ Automated recovery (no manual intervention)
- ✅ Multi-region resilience
- ✅ Data protection (backups, replication)
- ✅ Tested, proven architecture
- ✅ Industry standard

### Negative
- ❌ More complex than single-instance
- ❌ Higher infrastructure costs (2-3x)
- ❌ Operational overhead (monitoring, testing)
- ❌ Multi-region adds latency
- ❌ Eventual consistency issues in multi-region
- ❌ Requires regular testing/drills

## Alternative Strategies

### 1. Single Instance (No HA)
- **Pros**: Simple, cheap
- **Cons**: Any failure = downtime, data loss risk
- **Why not**: Unacceptable for critical service

### 2. Hot-Hot Multi-Region
- **Pros**: Zero downtime, instant failover
- **Cons**: Very expensive, complexity (distributed transactions)
- **Why not**: Overkill for our RTO/RPO requirements

### 3. Manual Failover
- **Pros**: Simpler automation
- **Cons**: Slow (manual intervention required), error-prone
- **Why not**: Need automated failover

## Related ADRs

- **Depends on**: ADR-004 (Kubernetes provides application HA)
- **Depends on**: ADR-002 (PostgreSQL for data durability)
- **Depends on**: ADR-009 (S3 backups for DR)
- **Related to**: ADR-005 (Monitoring detects failures)
- **Related to**: ADR-011 (Security during failover)

## Implementation Details

### Application HA (Kubernetes)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: training-service
spec:
  replicas: 3  # Multiple instances
  selector:
    matchLabels:
      app: training-service
  template:
    metadata:
      labels:
        app: training-service
    spec:
      containers:
      - name: training
        image: gym/training-service:latest
        livenessProbe:           # Is it alive?
          httpGet:
            path: /health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:          # Is it ready for traffic?
          httpGet:
            path: /ready
            port: 8082
          initialDelaySeconds: 5
          periodSeconds: 5
```

### Database HA (PostgreSQL Streaming Replication)

```bash
# Primary PostgreSQL
Primary (primary.gym.com)
  ├── Data directory: /var/lib/postgresql/data
  ├── wal_level: replica
  ├── max_wal_senders: 3
  └── Sends WAL to replica

# Standby PostgreSQL (replica)
Standby (standby.gym.com)
  ├── Restored from primary
  ├── recovery.conf
  ├── primary_conninfo = 'host=primary.gym.com'
  └── Applies received WAL
```

### Multi-Region Architecture

```
AWS Region 1 (Primary)
├── Kubernetes cluster (3 nodes)
├── PostgreSQL Primary
├── RDS backup copy
└── Route53 health checks: HEALTHY

AWS Region 2 (Standby)
├── Kubernetes cluster (3 nodes, standby)
├── PostgreSQL (empty, ready to restore)
└── Route53 health checks: check Region 1

Internet
  └── Route53 (DNS failover)
      ├── If Region 1 healthy → route there
      └── If Region 1 down → route to Region 2
```

### Failover Procedure

**Scenario: Region 1 Primary Fails**

Automated steps:
1. Health check detects primary region down
2. Route53 switches DNS to Region 2
3. Region 2 Kubernetes cluster becomes active
4. Region 2 database restored from S3 backup (most recent)
5. Application connections start flowing to Region 2
6. Users reconnect, resume activity

Timeline:
- Detection: ~30 seconds (health check interval)
- DNS propagation: ~5 minutes
- Database restore: ~30 minutes
- Application ready: ~5 minutes
- **Total RTO: ~40 minutes**

### Testing Strategy

**Monthly HA Drill:**
1. Intentionally kill pods → verify auto-restart
2. Simulate database failover → test replica promotion
3. Full region failover test → verify multi-region failover
4. Backup restoration test → verify S3 recovery
5. Document findings, update runbooks

## Monitoring & Alerting

Critical metrics:
- Service availability per region
- Database replication lag
- Backup success/failure
- Failover readiness
- Recovery time (RTO) measurement

Alerts:
- Replication lag > 5 minutes
- Backup failed
- Health check failures
- Service unavailability > 5 minutes

## Future Considerations

- Achieve 99.99% (4 nines) with cross-region active-active
- Add database encryption at rest
- Add disaster recovery playbook automation
- Consider chaos engineering (regular failure testing)
- Monitor and improve RTO/RPO over time
