# Deployment Runbook - Gym Platform API

Step-by-step procedures for deploying to production, monitoring, and recovery.

**Audience:** DevOps Engineers, Release Managers, On-Call Engineers  
**Last Updated:** March 21, 2026  
**Environment:** Kubernetes (AWS EKS) - Staging and Production

---

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Deployment Procedures](#deployment-procedures)
3. [Database Migrations](#database-migrations)
4. [Monitoring During Deployment](#monitoring-during-deployment)
5. [Verification Steps](#verification-steps)
6. [Rollback Procedures](#rollback-procedures)
7. [Recovery Procedures](#recovery-procedures)
8. [Troubleshooting](#troubleshooting)

---

## Pre-Deployment Checklist

Complete ALL items before proceeding:

```
☐ Code Review
  □ PR has minimum 2 approvals
  □ All CI/CD checks passing (tests, lint, build)
  □ No console.log() or debug code remaining
  □ No hardcoded secrets or credentials

☐ Testing
  □ Unit tests passing locally (mvn clean test)
  □ Integration tests passing (mvn verify)
  □ All tests passing in CI/CD pipeline
  □ Coverage targets met (>70%)

☐ Database
  □ Database migrations reviewed and tested
  □ Rollback procedure verified
  □ Data migration (if applicable) tested on staging
  □ Backup created pre-deployment

☐ Configuration
  □ Environment variables verified for target environment
  □ Secrets updated (if changed)
  □ Feature flags configured
  □ Log level appropriate (INFO for prod)

☐ Documentation
  □ Release notes prepared
  □ Known issues documented
  □ Rollback criteria defined

☐ Stakeholders
  □ Team lead approval obtained
  □ OnCall engineer assigned
  □ Monitoring dashboards prepared
  □ Communication plan ready
```

---

## Deployment Procedures

### Pre-Deployment Phase

**Step 1: Final Staging Validation** (~30 minutes)

```bash
# 1. Deploy to staging first
kubectl set image deployment/gym-staging \
  auth-service=gym/auth-service:v1.2.0 \
  training-service=gym/training-service:v1.2.0 \
  tracking-service=gym/tracking-service:v1.2.0 \
  notification-service=gym/notification-service:v1.2.0 \
  -n staging

# 2. Wait for rollout
kubectl rollout status deployment/gym-staging -n staging --timeout=5m

# 3. Run smoke tests
./scripts/operational/smoke-tests.sh staging

# 4. Verify metrics normal
# Check Grafana: http://grafana.internal/staging-health
# Error rate < 1%
# Latency p95 < 500ms
# Memory < 80%
```

**If staging tests FAIL:**
- Investigate error
- Roll back staging: `kubectl rollout undo deployment/gym-staging -n staging`
- Fix issue, commit, return to pre-deployment checklist

**Step 2: Production Window Announcement**

```
Announce to Slack #deployments channel:

:rocket: DEPLOYMENT IN PROGRESS
Service: Gym Platform API
Version: v1.2.0
Window: 14:00-15:00 UTC
Status: Starting pre-production checks
Estimated downtime: 0 min (rolling deployment)
```

---

### Deployment Phase

**Step 3: Rolling Deployment** (~15 minutes)

```bash
# Update Kubernetes manifests
# File: k8s/overlays/production/kustomization.yaml

apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

images:
- name: auth-service
  newTag: v1.2.0
- name: training-service
  newTag: v1.2.0
- name: tracking-service
  newTag: v1.2.0
- name: notification-service
  newTag: v1.2.0

# Apply changes
kubectl apply -k k8s/overlays/production/

# Watch rollout
kubectl rollout status deployment/gym-prod -n production --timeout=10m

# Monitor with:
watch -n 1 kubectl get pods -n production
```

**Rolling Deployment Timeline:**
```
Time  Event
00:00 First pod updated (1/3 ready)
01:30 First pod healthy, second pod starts
03:00 Second pod healthy, third pod starts
04:30 All pods updated and healthy
05:00 Deployment complete
```

**Step 4: Database Migrations** (~5-15 minutes, depends on migration)

```bash
# If database migrations needed:
# IMPORTANT: Do this BEFORE or IMMEDIATELY after app rollout

# 1. Run migrations in dry-run mode
kubectl exec -it db-migration-job -n production -- \
  flyway info

# Expected output: Shows pending migrations

# 2. Back up database
kubectl exec -it postgres-0 -n production -- \
  pg_dump -U gym_user gym_platform | \
  gzip > /backups/gym_platform_$(date +%Y%m%d_%H%M%S).sql.gz

# 3. Run actual migration
kubectl exec -it db-migration-job -n production -- \
  flyway migrate

# Expected output:
# V20260321_001__add_user_preferences.sql validated
# V20260321_001__add_user_preferences.sql executed
# SUCCESS

# 4. Verify migration success
kubectl exec -it postgres-0 -n production -- \
  psql -U gym_user -d gym_platform -c "\dt"

# Should show new table/columns
```

---

### Post-Deployment Phase

**Step 5: Immediate Health Checks** (~5 minutes)

```bash
# 1. Check pod status
kubectl get pods -n production

# Expected: All pods in "Running" state with "1/1" Ready
# NAME                               READY   STATUS    RESTARTS
# auth-service-7d8f9c2b4-xyz12       1/1     Running   0
# training-service-5c3a8b1f2-abc34   1/1     Running   0
# tracking-service-9e2f7a4c8-def56   1/1     Running   0
# notification-service-3b1e6f2a9-ghi78 1/1   Running   0

# 2. Check service endpoints
for service in auth training tracking notification; do
  curl -s http://${service}-service.production.svc.cluster.local:8080/health | jq .
done

# Expected output: { "status": "ok" } for each

# 3. Check logs for errors
kubectl logs -f deployment/gym-prod -n production --all-containers=true \
  --tail=50 | grep -i "error\|exception"

# Should see minimal errors (startup warnings OK)
```

**Step 6: Smoke Tests** (~5 minutes)

```bash
# Run automated smoke tests (happy path only)
./scripts/operational/smoke-tests.sh production

# Expected: All smoke tests pass
# ✓ Create user
# ✓ Create workout
# ✓ Log weight
# ✓ Get notifications
# ...
# 8 passing (2.3s)
```

**Manual smoke tests** (if automated tests limited):
```bash
# 1. Create a test user
curl -X POST https://api.gym.com/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "smoketest@example.com",
    "password": "Test123!",
    "firstName": "Smoke",
    "lastName": "Test"
  }'

# 2. Login and get token
TOKEN=$(curl -X POST https://api.gym.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoketest@example.com","password":"Test123!"}' \
  | jq -r '.token')

# 3. Create workout
curl -X POST https://api.gym.com/training/workouts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Test Workout"}'

# 4. Log weight
curl -X POST https://api.gym.com/tracking/weight-logs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"weight_kg":80.5}'

# All should return 2xx responses
```

---

## Database Migrations

### Migration Planning

**Before every deployment:**

1. **Check for pending migrations**
   ```bash
   kubectl exec -it db-migration-job -n production -- flyway info
   ```

2. **Categorize migrations:**
   - **Safe:** Adding nullable columns, creating new tables
   - **Risky:** Dropping columns, changing constraints
   - **Dangerous:** Large table alterations (may lock tables)

3. **Risk Level Determination:**
   ```
   Safe → Deploy during business hours
   Risky → Schedule maintenance window
   Dangerous → Requires DBA review + specific window
   ```

### Migration Execution

**Standard Migration (< 1 min):**
```bash
# Run during normal business hours
kubectl exec db-migration-job -- flyway migrate
```

**Long-Running Migration (> 5 min):**
```bash
# 1. Announce maintenance window
echo "Deployment: Migration in progress, may affect performance" | \
  curl -X POST https://api.slack.com/notify

# 2. Reduce traffic (optional) - update load balancer
kubectl patch service api-gateway -p '{"spec":{"type":"LoadBalancer","loadBalancerSourceRanges":["10.0.0.0/8"]}}'

# 3. Run migration with monitoring
time kubectl exec db-migration-job -- flyway migrate

# 4. Monitor database
watch -n 1 'psql -c "SELECT count(*) FROM pg_stat_activity;"'

# 5. Restore full traffic after migration
kubectl patch service api-gateway -p '{"spec":{"type":"LoadBalancer","loadBalancerSourceRanges":[]}}'
```

### Migration Rollback

**If migration fails:**

```bash
# 1. Identify issue
kubectl logs db-migration-job

# 2. Stop current migration (if still running)
kubectl delete job db-migration-job -n production

# 3. Restore from backup
BACKUP_FILE="/backups/gym_platform_$(date +%Y%m%d_%H%M%S).sql.gz"
gunzip < $BACKUP_FILE | \
  psql -U gym_user -d gym_platform

# 4. Revert application deployment
kubectl rollout undo deployment/gym-prod -n production

# 5. Verify restoration
curl https://api.gym.com/health
./scripts/operational/smoke-tests.sh production
```

---

## Monitoring During Deployment

### Key Metrics to Watch

**Dashboard:** Grafana → Production Health

```
Metric                    Target      Alert Threshold
Error Rate               < 1%        > 5%
Latency (p95)            < 500ms     > 1000ms
Latency (p99)            < 1000ms    > 2000ms
CPU Usage                < 70%       > 85%
Memory Usage             < 75%       > 90%
Database Connections     < 80        > 90
Pod Restarts             0           > 0
```

### Monitoring Commands

```bash
# Watch pod status in real-time
watch -n 2 'kubectl get pods -n production --sort-by=.metadata.creationTimestamp'

# Monitor logs for errors
kubectl logs -f deployment/gym-prod -n production \
  --all-containers=true \
  | grep -E "ERROR|Exception|panic"

# Check resource usage
kubectl top pods -n production --containers

# Monitor database
psql -U gym_user -d gym_platform -c "SELECT * FROM pg_stat_database WHERE datname='gym_platform';"

# Check Prometheus metrics
curl http://prometheus:9090/api/v1/query?query='rate(http_requests_total[5m])'
```

### Alert Response

**If error rate spikes > 5%:**
1. Check pod logs for errors
2. Check database connectivity
3. Check recent code changes
4. **If unfixable:** Proceed to [Rollback](#rollback-procedures)

**If latency increases > 2x baseline:**
1. Check database query performance
2. Check resource availability
3. Check for lock contention
4. **If unfixable:** Proceed to [Rollback](#rollback-procedures)

---

## Verification Steps

### Immediate Verification (5 min post-deploy)

```bash
# 1. Pod running and ready
DESIRED=$(kubectl get deployment/gym-prod -n production -o jsonpath='{.spec.replicas}')
READY=$(kubectl get deployment/gym-prod -n production -o jsonpath='{.status.readyReplicas}')
[ "$DESIRED" = "$READY" ] && echo "✓ All pods ready" || echo "✗ Pods not ready: $READY/$DESIRED"

# 2. Service endpoints healthy
kubectl get endpoints gym-prod -n production

# 3. Error rate normal
curl -s http://prometheus:9090/api/v1/query?query='rate(http_requests_total{status=~"5.."}[5m])' | jq .

# 4. No restart loop
kubectl get pods -n production | grep -v "0       0" | wc -l

# 5. Database healthy
psql -U gym_user -d gym_platform -c "SELECT version();"
```

### Extended Verification (30 min post-deploy)

```bash
# Check metrics trends over 30 minutes
# Error rate stable?
# Latency stable?
# Memory usage stable?
# No unusual patterns?

# In Grafana, look for:
# - Smooth error rate line (not spikes)
# - Latency line in normal range
# - Memory not continuously increasing
# - CPU usage fluctuating normally
```

### User-Facing Verification

```bash
# Can users login?
# Can create workouts?
# Can log food/weight?
# Notifications sent?

# Ask QA team or early users to verify:
"Deployment complete - please verify basic workflows"
```

---

## Rollback Procedures

### When to Rollback

**Rollback immediately if:**
- Error rate > 5% sustained (> 2 min)
- Service unavailable (no response)
- Data corruption detected
- Critical bug affecting core functionality
- Database migration failed

**Rollback after investigation if:**
- Latency > 2x baseline
- Memory leak detected
- Specific feature broken

### Rollback Steps

**Step 1: Announce Rollback**
```
Slack notification:
:warning: ROLLBACK IN PROGRESS
Reason: [Brief explanation]
Target version: v1.1.5 (previous stable)
ETA: 5-10 minutes
```

**Step 2: Stop Current Deployment**
```bash
# Get current deployment status
kubectl rollout history deployment/gym-prod -n production

# Output:
# REVISION  CHANGE-CAUSE
# 2         Deployment v1.1.5
# 3         Deployment v1.2.0 (current)

# Rollback to previous version
kubectl rollout undo deployment/gym-prod -n production

# Watch rollout
kubectl rollout status deployment/gym-prod -n production --timeout=5m
```

**Step 3: Verify Rollback**
```bash
# Confirm old version running
kubectl get deployment/gym-prod -n production -o jsonpath='{.spec.template.spec.containers[0].image}'

# Expected: gym/gym-service:v1.1.5 (old version)

# Run smoke tests
./scripts/operational/smoke-tests.sh production

# Check metrics
# Error rate should return to normal
# Latency should return to baseline
```

**Step 4: Post-Rollback Communication**
```
Slack:
:heavy_check_mark: ROLLBACK COMPLETE
Rolled back to v1.1.5
Root cause: [Brief explanation]
Next steps: Investigation + fix + redeployment tomorrow
Thank you for your patience!
```

---

## Recovery Procedures

### Data Loss Recovery

**Scenario: Data corruption detected in production**

```bash
# 1. Assess scope of corruption
# How many users affected?
# What data is corrupted?
# When did it happen?

# 2. Stop the bleeding
# Scale down affected services
kubectl scale deployment auth-service --replicas=0 -n production

# 3. Restore from backup
BACKUP_FILE="/backups/gym_platform_20260321_140000.sql.gz"
gunzip < $BACKUP_FILE | \
  psql -U gym_user -d gym_platform

# 4. Verify data integrity
psql -U gym_user -d gym_platform -c "SELECT COUNT(*) FROM users;"
psql -U gym_user -d gym_platform -c "SELECT COUNT(*) FROM workouts;"

# 5. Scale services back
kubectl scale deployment auth-service --replicas=3 -n production

# 6. Notify affected users
"Data corruption recovered. Your data restored to [timestamp]."
```

### Multi-Region Failover

**Scenario: Entire region fails (fire, AWS outage, etc.)**

```bash
# 1. Detect primary region failure
# Monitoring detects health checks failing
# All pods in region-1 unreachable

# 2. Initiate failover to region-2
# AWS Route53 automatically switches DNS

# 3. Verify failover
# curl https://api.gym.com/health → region-2 response
# Error rate should increase briefly then normalize

# 4. Monitor region-2
# Ensure capacity sufficient
# Check error rates
# Validate all services running

# 5. Communicate status
"Primary region failed. Switched to DR region. Functionality maintained. Investigating region-1."

# 6. Recovery
# Once region-1 recovered:
# - Sync data from region-2 back to region-1
# - Fail back (or keep on region-2 temporarily)
# - Post-mortem analysis
```

### Service-Specific Recovery

**If specific service crashes:**

```bash
# Example: Training service crashes

# 1. Check service status
kubectl describe deployment training-service -n production

# 2. Check pod logs
kubectl logs -p <crashed-pod-name> -n production

# 3. Increase replicas to bypass crashed pods
kubectl scale deployment training-service --replicas=5 -n production

# 4. Identify root cause from logs
# Memory leak? Infinite loop? Deadlock?

# 5. Fix and redeploy
git checkout <commit-hash>
# ...fix issue...
docker build -t gym/training-service:v1.2.1 .
docker push gym/training-service:v1.2.1
# Update deployment
kubectl set image deployment/training-service \
  training-service=gym/training-service:v1.2.1

# 6. Monitor recovery
kubectl logs -f deployment/training-service
```

---

## Troubleshooting

### Pods Not Starting

**Symptom:** Pods stuck in "Pending" or "CrashLoopBackOff"

**Diagnosis:**
```bash
# Check pod status
kubectl describe pod <pod-name> -n production

# Check logs
kubectl logs <pod-name> -n production

# Check resource availability
kubectl describe nodes

# Check probes (health checks)
kubectl get pod <pod-name> -n production -o yaml | grep -A 5 "livenessProbe\|readinessProbe"
```

**Common Causes & Solutions:**

1. **Insufficient resources**
   ```bash
   # Increase node capacity
   # or reduce pod resource requests
   kubectl set resources deployment gym-prod -n production \
     --requests=cpu=500m,memory=512Mi \
     --limits=cpu=1000m,memory=1Gi
   ```

2. **Image not found**
   ```bash
   # Check image exists in registry
   docker image inspect gym/auth-service:v1.2.0
   
   # Check registry credentials
   kubectl get secrets -n production
   ```

3. **Liveness probe failing**
   ```bash
   # Increase probe timeout
   kubectl edit deployment gym-prod -n production
   # Increase initialDelaySeconds: 30 → 60
   ```

### High Latency

**Symptom:** API responses slow (> 2 seconds)

**Diagnosis:**
```bash
# Check database connections
psql -U gym_user -d gym_platform -c "SELECT count(*) FROM pg_stat_activity;"

# Check slow queries
psql -U gym_user -d gym_platform -c "SELECT query, calls, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 5;"

# Check connection pooling
kubectl exec db-bouncer -n production -- \
  psql -U pgbouncer -d pgbouncer -c "SHOW POOLS;"
```

**Solutions:**
- Increase PgBouncer pool size (ADR-008)
- Add indexes on frequently queried columns
- Optimize slow queries
- Cache frequently accessed data (ADR-012)

### Database Connection Errors

**Symptom:** "Connection refused" or "Too many connections"

**Diagnosis:**
```bash
# Check connection limit
psql -U gym_user -d gym_platform -c "SHOW max_connections;"

# Check active connections
psql -U gym_user -d gym_platform -c "SELECT count(*) FROM pg_stat_activity;"

# Check for idle connections
psql -U gym_user -d gym_platform -c "SELECT * FROM pg_stat_activity WHERE state = 'idle';"
```

**Solutions:**
- Increase max_connections in PostgreSQL
- Reduce connection timeout
- Kill idle connections
- Restart PgBouncer to reset connections

---

## Post-Deployment Checklist

After successful deployment:

```
☐ All smoke tests passed
☐ Error rate < 1%
☐ Latency p95 < 500ms
☐ No pod restarts
☐ Database healthy
☐ Users can login and use app
☐ Release notes sent to team
☐ Monitoring dashboard updated
☐ On-call engineer notified of changes
☐ Knowledge base updated with any new procedures
```

---

## Escalation Path

**If deployment goes wrong:**

1. **Alert Level 1** (minor issue)
   - Error rate 1-3%
   - Latency slightly elevated
   - **Action:** Monitor, likely recovers

2. **Alert Level 2** (moderate issue)
   - Error rate 3-5%
   - Some users affected
   - **Action:** Investigate + potentially rollback

3. **Alert Level 3** (critical issue)
   - Error rate > 5%
   - Service unavailable
   - **Action:** IMMEDIATE ROLLBACK, investigate later

**Escalation Chain:**
- Level 1: On-call engineer
- Level 2: On-call + team lead
- Level 3: On-call + team lead + VP Engineering

---

**Last Updated:** March 21, 2026  
**Maintained by:** DevOps Team  
**Next Review:** June 21, 2026

See Also:
- [ADR-004: Docker & Kubernetes Deployment](../adr/ADR-004-docker-kubernetes-deployment.md)
- [ADR-010: Disaster Recovery & HA Strategy](../adr/ADR-010-disaster-recovery-ha.md)
- [ADR-009: S3 Backups](../adr/ADR-009-s3-cloud-storage-backups.md)
