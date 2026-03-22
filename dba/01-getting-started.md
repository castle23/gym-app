# Getting Started with DBA Administration

## Overview

Comprehensive guide for Database Administrators (DBAs) getting started with the Gym Platform PostgreSQL infrastructure. This guide covers initial setup, access provisioning, essential tools, role and responsibility definition, and onboarding procedures for new DBAs joining the team. The Gym Platform uses PostgreSQL 14+ in a highly available primary-replica architecture deployed across cloud infrastructure.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Role & Responsibilities](#role--responsibilities)
- [Access & Authentication](#access--authentication)
- [Essential Tools](#essential-tools)
- [Key Concepts](#key-concepts)
- [Your First Week](#your-first-week)
- [Documentation & Resources](#documentation--resources)
- [Support & Escalation](#support--escalation)
- [Checklists](#checklists)

---

## Prerequisites

### Knowledge Requirements

**Must Have:**
- PostgreSQL fundamentals (tables, indexes, queries)
- SQL proficiency (DML, DDL, DCL)
- Linux command-line experience (bash, ssh, sed, awk)
- Basic networking (TCP/IP, DNS, firewall concepts)
- Version control (git basics)

**Should Have:**
- Kubernetes/Docker experience
- Cloud platform experience (AWS, GCP, or Azure)
- Monitoring and observability tools
- Backup and disaster recovery concepts
- Performance tuning fundamentals

**Nice to Have:**
- Replication and failover procedures
- Advanced PostgreSQL features
- High availability solutions
- Data migration experience

### System Access

Before your first day, ensure you have:

```
☐ VPN access
☐ SSH key pair generated
☐ Database server access
☐ Monitoring dashboard access (Grafana, Prometheus)
☐ Log aggregation access (ELK stack)
☐ Kubernetes cluster access
☐ Cloud platform console access
☐ On-call system setup (PagerDuty)
☐ Communication tools (Slack, etc.)
```

---

## Role & Responsibilities

### DBA Responsibilities Matrix

| Area | Responsibility | Priority | Owner |
|------|-----------------|----------|-------|
| **Database Availability** | Keep production database running | P0 | Primary DBA |
| **Performance** | Monitor and optimize query performance | P1 | Performance DBA |
| **Backups** | Ensure reliable backup procedures | P1 | Backup DBA |
| **Security** | Manage access control and encryption | P1 | Security DBA |
| **Maintenance** | Execute maintenance procedures | P2 | Operations DBA |
| **Capacity Planning** | Monitor growth and plan resources | P2 | Capacity DBA |
| **Documentation** | Maintain procedures and runbooks | P2 | All DBAs |
| **Training** | Educate team on best practices | P3 | Senior DBA |

### On-Call Rotation

**Schedule:**
- On-call period: 1 week per quarter (rotating)
- On-call hours: 24/7 during period
- Escalation: Page on-call DBA for P1/P2 issues
- Response time: < 5 minutes for P1, < 15 minutes for P2

**Responsibilities During On-Call:**
- Monitor alerts 24/7
- Respond to incidents
- Coordinate with engineering team
- Document actions taken
- Post-incident review

---

## Access & Authentication

### SSH Access Setup

**Generate SSH key pair (if needed):**

```bash
# Generate new key
ssh-keygen -t ed25519 -f ~/.ssh/gym_dba -C "your.email@example.com"

# Set correct permissions
chmod 600 ~/.ssh/gym_dba
chmod 644 ~/.ssh/gym_dba.pub

# Add to SSH config
cat >> ~/.ssh/config << EOF
Host gym-db-primary
    HostName 10.0.1.50
    User postgres
    IdentityFile ~/.ssh/gym_dba
    StrictHostKeyChecking accept-new
    
Host gym-db-replica1
    HostName 10.0.1.51
    User postgres
    IdentityFile ~/.ssh/gym_dba
    
Host gym-db-replica2
    HostName 10.0.1.52
    User postgres
    IdentityFile ~/.ssh/gym_dba
EOF
```

**Request access from IT/Ops team:**
- Provide public key (`~/.ssh/gym_dba.pub`)
- Specify which servers you need access to
- Request VPN access if remote

### Database Connection

**Setup psql profile:**

```bash
# Create .pgpass for password-less connections
cat > ~/.pgpass << EOF
gym-db-primary:5432:gym_db:dba_user:${DB_PASSWORD}
gym-db-replica1:5433:gym_db:dba_user:${DB_PASSWORD}
gym-db-replica2:5433:gym_db:dba_user:${DB_PASSWORD}
EOF

chmod 600 ~/.pgpass

# Test connection
psql -h gym-db-primary -U dba_user -d gym_db -c "SELECT version();"
```

**Create connection aliases:**

```bash
cat > ~/.psqlrc << EOF
-- Connection info
\set PROMPT1 '%n@%m:%> %~%x%# '
\set PROMPT2 '[more] %R=# '
\set PROMPT3 '>> '

-- Useful settings
\set HISTFILE ~/.psql_history
\set HISTCONTROL ignoredups
\set QUIET 1
\pset pager always
\set ECHO queries

-- Useful commands
\set server_version 'SELECT version();'
\set databases 'SELECT datname FROM pg_database ORDER BY datname;'
\set users 'SELECT usename, usecanlogin FROM pg_user ORDER BY usename;'
EOF
```

### Kubernetes Access

**Setup kubectl:**

```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Configure kubeconfig
mkdir -p ~/.kube
# Request kubeconfig from DevOps team
# Save to ~/.kube/config

# Verify access
kubectl cluster-info
kubectl get nodes
```

### Cloud Platform Access

**AWS Console:**

```bash
# Install AWS CLI
pip install awscli

# Configure credentials (use IAM role if possible)
aws configure

# Test access
aws s3 ls gym-platform-backups/
```

**GCP Console (if using GCP):**

```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash

# Initialize
gcloud init

# Test access
gcloud compute instances list
```

---

## Essential Tools

### PostgreSQL Client Tools

**psql - PostgreSQL interactive terminal**

```bash
# Basic connection
psql -h gym-db-primary -U dba_user -d gym_db

# Execute query and exit
psql -h gym-db-primary -U dba_user -d gym_db -c "SELECT * FROM auth.users LIMIT 5;"

# Execute from file
psql -h gym-db-primary -U dba_user -d gym_db -f backup.sql
```

**pg_dump - Database backup**

```bash
# Backup entire database
pg_dump -h gym-db-primary -U dba_user gym_db > backup.sql

# Backup schema only
pg_dump -h gym-db-primary -U dba_user -s gym_db > schema.sql

# Backup data only
pg_dump -h gym-db-primary -U dba_user -a gym_db > data.sql
```

**pg_restore - Restore from backup**

```bash
# Restore database
pg_restore -h gym-db-primary -U dba_user -d gym_db backup.dump
```

### Monitoring Tools

**Prometheus - Metrics collection**

```bash
# Access Prometheus dashboard
# URL: http://monitoring-server:9090

# Query examples
# CPU usage: node_cpu_seconds_total
# Memory: node_memory_MemAvailable_bytes
# PostgreSQL connections: pg_stat_activity_count
```

**Grafana - Visualization**

```bash
# Access Grafana
# URL: http://monitoring-server:3000

# Key dashboards for DBAs:
# - PostgreSQL Overview
# - Database Performance
# - Backup Status
# - Replication Status
```

**pgAdmin - Web-based PostgreSQL manager**

```bash
# Access pgAdmin
# URL: http://pgadmin-server:5050

# Add database connection:
# 1. Servers > Create > Server
# 2. Name: Gym DB Primary
# 3. Connection tab:
#    - Host: gym-db-primary
#    - Username: dba_user
#    - Password: [from .pgpass]
```

### System Administration Tools

**Essential Linux commands:**

```bash
# System info
uname -a                    # OS and kernel info
lsb_release -a             # Linux distribution
df -h                       # Disk space usage
free -h                     # Memory usage

# PostgreSQL processes
ps aux | grep postgres      # Find PostgreSQL processes
systemctl status postgresql # Service status
journalctl -u postgresql    # PostgreSQL logs

# Network tools
netstat -tlnp | grep 5432  # PostgreSQL port connections
ss -tlnp | grep 5432       # Modern version
dig gym-db-primary         # DNS resolution
ping gym-db-primary        # Network connectivity

# SSH and remoting
ssh -i ~/.ssh/gym_dba postgres@gym-db-primary
scp file.sql postgres@gym-db-primary:/tmp/
```

---

## Key Concepts

### High Availability Architecture

```
┌─────────────────────────────────────┐
│   Application Servers               │
│   (Auth, Training, Tracking)        │
└──────────────┬──────────────────────┘
               │
         ┌─────┴──────┐
         │            │
    ┌────▼────┐  ┌───▼────┐
    │ Primary │  │Replicas│
    │ (5432)  │  │ (5433) │
    │ Read/   │  │ Read   │
    │ Write   │  │ Only   │
    └────┬────┘  └───┬────┘
         │           │
    ┌────▼───────────▼────┐
    │   WAL Archive       │
    │   (S3 Backup)       │
    └─────────────────────┘
```

**Key Components:**
- **Primary Database:** Handles all writes, synchronously replicates to replicas
- **Read Replicas:** Scale read operations, provide HA candidates
- **WAL Archive:** Point-in-time recovery capability
- **Backup Storage:** Off-site backup for disaster recovery

### Replication Concepts

**Synchronous vs Asynchronous:**

```
Synchronous Replication (for critical data):
1. Primary receives transaction
2. Primary sends to replica
3. Waits for replica acknowledgment
4. Commits on primary
5. Lower throughput, zero data loss

Asynchronous Replication (for read scaling):
1. Primary receives transaction
2. Commits on primary immediately
3. Sends to replica asynchronously
4. Slightly higher throughput, possible data loss
```

### Failover Process

**Automated Failover with Patroni:**

```
1. Health Check Failure (Primary doesn't respond)
   │
2. Patroni Detects Failure
   │
3. Leader Election (Consensus among replicas)
   │
4. Best Replica Promoted
   │
5. Other Replicas Rejoin as Followers
   │
6. DNS/VIP Updated
   │
7. Applications Reconnect
```

---

## Your First Week

### Day 1: Orientation

**Morning:**
- [ ] Meet team and learn responsibilities
- [ ] Understand current infrastructure
- [ ] Review existing documentation
- [ ] Get system access set up

**Afternoon:**
- [ ] SSH into database servers
- [ ] Connect to databases with psql
- [ ] Explore database schema
- [ ] Review monitoring dashboards

**Checklist:**

```bash
# Verify access
ssh postgres@gym-db-primary "psql -c 'SELECT version();'"

# Check database status
psql -h gym-db-primary -c "SELECT datname FROM pg_database;"

# Review replicas
psql -h gym-db-primary -c "SELECT client_addr, state FROM pg_stat_replication;"

# Check backups
aws s3 ls s3://gym-platform-backups/full-backups/ --human-readable
```

### Day 2-3: Learning the System

**Study:**
- [ ] Database architecture documentation
- [ ] Backup procedures
- [ ] Monitoring setup
- [ ] Incident response procedures
- [ ] Team runbooks

**Hands-on Activities:**
- [ ] Create test database
- [ ] Practice backup and restore
- [ ] Monitor query performance
- [ ] Review slow query logs

### Day 4-5: First Tasks

**Easy Wins:**
- [ ] Help with routine maintenance
- [ ] Monitor database health
- [ ] Review documentation
- [ ] Participate in on-call shadow

**Knowledge Transfer:**
- [ ] Pair program with experienced DBA
- [ ] Understand current pain points
- [ ] Ask clarifying questions
- [ ] Document learnings

---

## Documentation & Resources

### Key Documentation

**In Gym Platform:**
- `docs/database/01-database-overview.md` - Architecture and setup
- `docs/database/02-schema-design.md` - Schema structure
- `docs/database/03-backup-recovery.md` - Backup procedures
- `docs/database/04-performance-tuning.md` - Performance optimization
- `docs/database/05-migration-guide.md` - Schema migrations
- `docs/database/06-maintenance-procedures.md` - Maintenance tasks

**External Resources:**
- [PostgreSQL Official Documentation](https://www.postgresql.org/docs/)
- [PostgreSQL Performance Wiki](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [Patroni Documentation](https://patroni.readthedocs.io/)

### Learning Path

**Week 1-2:**
- [ ] Review all documentation
- [ ] Understand architecture
- [ ] Learn backup procedures
- [ ] Study runbooks

**Week 3-4:**
- [ ] Perform maintenance tasks
- [ ] Handle minor incidents
- [ ] Review performance tuning
- [ ] Practice failover procedures

**Month 2-3:**
- [ ] Lead incident response
- [ ] Perform capacity planning
- [ ] Optimize critical queries
- [ ] Mentor new DBAs

---

## Support & Escalation

### Getting Help

**Internal Resources:**
- Senior DBA: Questions about procedures
- DevOps Team: Infrastructure and Kubernetes
- Application Teams: Database-related issues
- Slack Channel: #database-support

**External Resources:**
- PostgreSQL documentation
- Stack Overflow (#postgresql tag)
- PostgreSQL forums
- Professional PostgreSQL services

### Escalation Path

**For Issues:**

```
1. Try to resolve using documentation
2. Ask senior DBA for advice
3. Escalate to team lead if critical
4. Contact vendor support if needed (AWS, GCP, etc.)

Priority Levels:
P0: Complete outage - Immediate escalation
P1: Degraded service - Escalate within 5 minutes
P2: Minor issue - Escalate within 30 minutes
P3: Low impact - Handle during business hours
```

### After-Hours Support

**On-Call Procedures:**

```
1. Monitor PagerDuty alerts
2. Respond within 5 minutes
3. Page senior DBA if needed
4. Follow incident procedures
5. Document all actions
```

---

## Checklists

### First Day Checklist

- [ ] VPN and SSH access working
- [ ] Can connect to all database servers
- [ ] Monitoring dashboards accessible
- [ ] Kubernetes cluster accessible
- [ ] Added to on-call rotation (shadow mode)
- [ ] Introduced to team
- [ ] Have primary contact info
- [ ] Know escalation process
- [ ] Have local documentation copy

### First Week Checklist

- [ ] Read all core documentation
- [ ] Connect to all databases
- [ ] Understand backup procedures
- [ ] Reviewed last 3 months of incidents
- [ ] Practiced backup restore
- [ ] Monitored one full day of production
- [ ] Understood monitoring alerts
- [ ] Pair programmed with senior DBA
- [ ] Observed incident response
- [ ] Updated personal knowledge base

### First Month Checklist

- [ ] Handled routine maintenance
- [ ] Performed at least one backup restore
- [ ] Optimized one slow query
- [ ] Led one on-call shift (with backup)
- [ ] Reviewed capacity planning
- [ ] Updated documentation
- [ ] Attended team meeting
- [ ] Completed PostgreSQL online course
- [ ] Participated in disaster recovery drill
- [ ] Know who to contact for everything

---

## Common First-Week Questions

**Q: How do I know if the database is healthy?**

```bash
# Quick health check
psql -h gym-db-primary -c "
SELECT
    server_version,
    datname,
    pg_database_size(datname)
FROM pg_database
WHERE datname = 'gym_db';"
```

**Q: What should I monitor?**

Look at Grafana dashboards for:
- CPU and memory usage
- Query latency (p50, p95, p99)
- Connection count
- Replication lag
- Disk space usage

**Q: What's the most critical thing to know?**

Always have a recent backup and know how to restore it. Everything else follows from that.

**Q: How do I practice without affecting production?**

- Use staging environment for testing
- Restore backups to test database
- Run load tests during off-peak hours
- Ask senior DBA before trying new things

---

**Related Documentation:**
- [02-database-architecture.md](02-database-architecture.md) - Detailed architecture
- See [docs/database/](../../docs/database/) for comprehensive guides
- See [docs/operations/](../../docs/operations/) for operational procedures
- See [docs/troubleshooting/](../../docs/troubleshooting/) for incident procedures
