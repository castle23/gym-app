# ADR-009: S3/Cloud Storage for Backups

## Status
Accepted

## Date
2026-03-21

## Context

The Gym Platform stores critical user data:
- User accounts and profiles
- Workout history
- Diet and tracking data
- Personal metrics

Data loss risks:
1. **Hardware Failure**: Disk failure, data corruption
2. **Accidental Deletion**: Operations mistake, bug deletes data
3. **Ransomware**: Malicious encryption of data
4. **Disaster**: Data center fire, natural disaster
5. **Compliance**: Data retention requirements, audit trails

Backup strategy requirements:
1. **Durability**: Multiple copies in different locations
2. **Recoverability**: Can restore point-in-time
3. **Scalability**: Grows with data volume
4. **Cost-Effective**: Shouldn't be too expensive
5. **Automation**: Hands-off, reliable backups

## Decision

We chose **AWS S3 (or equivalent cloud storage) for backup destination**:

1. **Automated PostgreSQL Backups**: Daily full backup + continuous WAL archiving
2. **S3 Storage**: Backups stored in S3 with versioning
3. **Multi-Region**: Backups replicated to different AWS region
4. **Lifecycle Policy**: Old backups auto-deleted after retention period
5. **Encryption**: Backups encrypted at rest and in transit

## Rationale

### 1. Durability
S3 provides:
- 99.999999999% (11 nines) durability
- Automatic replication across data centers
- Redundancy built-in
- Separate from primary database (different geography)

### 2. Scalability
S3 can handle:
- Unlimited storage capacity
- Grows as database grows
- No capacity planning needed
- Scales to petabytes if needed

### 3. Cost Effective
- Pay only for storage used
- Standard S3: ~$0.023/GB/month
- Cheaper than on-premise backup infrastructure
- Can use Glacier for long-term archive (cheaper)

### 4. Recovery Options
- Full database restore (1-2 hours)
- Point-in-time recovery (PITR) (up to backup retention)
- Restore to test environment
- Single table restore (if supported)

### 5. Compliance & Audit
- S3 access logging
- Backup encryption audit trail
- Retention policies enforced
- Can be cross-account for extra security

### 6. Ease of Operations
- Automated, hands-off
- No backup tapes to manage
- No backup infrastructure to run
- Easy rotation and cleanup

## Consequences

### Positive
- ✅ High durability (11 nines)
- ✅ Geographically distributed
- ✅ Unlimited scalability
- ✅ Cost effective
- ✅ Easy point-in-time recovery
- ✅ Automated operations

### Negative
- ❌ Recovery time isn't instant (1-2+ hours)
- ❌ AWS dependency (vendor lock-in)
- ❌ Network bandwidth costs (data transfer out)
- ❌ RPO (Recovery Point Objective) = backup interval (not real-time)
- ❌ Requires IAM permissions, encryption key management
- ❌ Backup size grows linearly with data

## Alternatives Considered

### 1. On-Premise Backup (NAS, Tape)
- **Pros**: Full control, no AWS dependency
- **Cons**: Capital costs, operational overhead, disaster doesn't protect from fire
- **Why not**: More expensive, more operational burden

### 2. Database Replication (Streaming Replication)
- **Pros**: Continuous availability, low RPO
- **Cons**: Real-time copy, not a backup, takes resources
- **Why not**: Complements backups but isn't a replacement

### 3. Google Cloud Storage or Azure Blob Storage
- **Pros**: Similar to S3, possibly cheaper
- **Cons**: AWS already in use (ADR-004)
- **Why not**: S3 most mature, integrate with AWS

### 4. PostgreSQL Point-in-Time Recovery (PITR) Only
- **Pros**: Works without separate backups
- **Cons**: Requires WAL files, complex to manage
- **Why not**: Need full backups too for major recovery

## Related ADRs

- **Depends on**: ADR-002 (PostgreSQL database)
- **Related to**: ADR-004 (Backup automation in Kubernetes)
- **Related to**: ADR-010 (Part of disaster recovery strategy)
- **Related to**: ADR-011 (Backup encryption)

## Implementation Details

### Backup Strategy

**Full Backups:**
- Frequency: Daily (00:00 UTC)
- Retention: 30 days
- Time: ~15-30 minutes (depends on DB size)
- Size: Grows with data volume

**WAL Archiving (Continuous):**
- Frequency: Continuous (as data changes)
- Retention: 7 days
- Enables: Point-in-time recovery

**Example:**
```
Day 1 00:00 → Full backup to S3
Day 1 00:30 → WAL archiving enabled
Day 2 00:00 → Full backup to S3
Day 2 00:30 → WAL archiving enabled
...
Day 31 00:00 → Day 1's backup auto-deleted (30-day retention)
```

### Backup Configuration

```yaml
# PostgreSQL backup in Kubernetes
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
spec:
  schedule: "0 0 * * *"  # Daily at midnight
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:13
            command:
            - /bin/sh
            - -c
            - |
              pg_dump -h postgres -U gym_user -d gym_platform | \
              gzip | \
              aws s3 cp - s3://gym-backups/postgres/backup-$(date +%Y%m%d).sql.gz
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: password
            - name: AWS_ACCESS_KEY_ID
              valueFrom:
                secretKeyRef:
                  name: aws-credentials
                  key: access_key
            - name: AWS_SECRET_ACCESS_KEY
              valueFrom:
                secretKeyRef:
                  name: aws-credentials
                  key: secret_key
```

### S3 Configuration

```json
{
  "bucket": "gym-backups",
  "versioning": "Enabled",
  "serverSideEncryption": "AES256",
  "lifecycle": {
    "rules": [
      {
        "id": "delete-old-backups",
        "status": "Enabled",
        "prefix": "postgres/",
        "expiration": {
          "days": 30
        }
      },
      {
        "id": "archive-to-glacier",
        "status": "Enabled",
        "prefix": "postgres/",
        "transitions": [
          {
            "days": 7,
            "storageClass": "GLACIER"
          }
        ]
      }
    ]
  },
  "publicAccessBlockConfiguration": {
    "blockPublicAcls": true,
    "blockPublicPolicy": true,
    "ignorePublicAcls": true,
    "restrictPublicBuckets": true
  }
}
```

### Recovery Procedure

**Quick Recovery (Latest Full Backup):**
1. Create new PostgreSQL instance (Kubernetes pod)
2. Download latest backup from S3
3. Restore: `gunzip backup.sql.gz | psql`
4. Update connection strings
5. Test recovery
6. Done (~30 minutes)

**Point-in-Time Recovery:**
1. Restore from full backup
2. Apply WAL files to specific timestamp
3. Verify data integrity
4. Switch over
5. Done (~1-2 hours)

### Monitoring & Alerting

Alert when:
- Backup job fails (backup-error > 0)
- Backup takes too long (backup_duration > 1 hour)
- S3 upload fails
- No backup in last 25 hours
- S3 bucket access denied
- Backup size suddenly increases (possible corruption)

## Testing Backups

**Monthly Recovery Drill:**
1. Restore backup to test environment
2. Verify data integrity
3. Test application connections
4. Document any issues
5. Sign off

## Future Considerations

- Add incremental backups (faster, less storage)
- Add backup encryption with KMS (key management)
- Add cross-region replication for extra disaster recovery
- Consider backup validation tools (backup testing)
- Add monitoring dashboard for backup health
