# Monitoring and Alerting

## Overview

Comprehensive guide to monitoring PostgreSQL production systems, building dashboards, creating effective alerts, collecting metrics, and implementing proactive health monitoring for the Gym Platform. Covers Prometheus integration, Grafana dashboards, alert rules, metric collection, troubleshooting alerts, and incident response workflows for DBAs.

## Table of Contents

- [Monitoring Architecture](#monitoring-architecture)
- [Metrics Collection](#metrics-collection)
- [Prometheus Configuration](#prometheus-configuration)
- [Grafana Dashboards](#grafana-dashboards)
- [Key Performance Metrics](#key-performance-metrics)
- [Alert Rules](#alert-rules)
- [Log Aggregation](#log-aggregation)
- [Health Checks](#health-checks)
- [Incident Response](#incident-response)
- [Troubleshooting Monitoring](#troubleshooting-monitoring)

---

## Monitoring Architecture

### Monitoring Stack

```
┌─────────────────────────────────────────────────┐
│          PostgreSQL Instances (Prod)            │
├─────────────────────────────────────────────────┤
│  - Primary                                       │
│  - Replica 1, 2, 3                              │
└────────────┬──────────────────────────────────┘
             │ Metrics Export
             │
       ┌─────▼──────┐
       │ pg_exporter │
       │ Port: 9187  │
       └─────┬──────┘
             │
             │ Scrape interval: 15s
             │
       ┌─────▼───────────┐
       │  Prometheus     │
       │  - Scraping     │
       │  - Storage      │
       │  - Alerting     │
       └─────┬───────────┘
             │
        ┌────┴──────┐
        │            │
    ┌───▼────┐  ┌───▼─────────┐
    │ Grafana │  │ AlertManager │
    │Dashboards  │ - Slack      │
    │         │  │ - PagerDuty  │
    └─────────┘  └──────────────┘
```

### Multi-Cluster Monitoring

```
Cluster A (Production)     Cluster B (Production)
     │                            │
     └────────┬────────────────────┘
              │
         ┌────▼─────┐
         │Prometheus │ Remote Storage
         │  Federate │────────────────────┐
         └────┬─────┘                      │
              │                       ┌────▼──────────┐
              │                       │ TimescaleDB   │
              │                       │ Long-term     │
              │                       │ retention     │
              │                       └───────────────┘
         ┌────▼──────┐
         │  Grafana   │
         │  Central   │
         │ Dashboard  │
         └────────────┘
```

---

## Metrics Collection

### PostgreSQL Exporter Setup

```bash
#!/bin/bash
# setup-pg-exporter.sh - Install postgres_exporter

VERSION="0.11.1"
EXPORTER_HOME="/opt/postgres_exporter"
EXPORTER_USER="postgres_exporter"

# Create user
useradd --system --home /var/lib/postgres_exporter \
  --shell /bin/false postgres_exporter

# Download exporter
mkdir -p $EXPORTER_HOME
cd $EXPORTER_HOME
wget https://github.com/prometheus-community/postgres_exporter/releases/download/v${VERSION}/postgres_exporter-${VERSION}.linux-amd64.tar.gz
tar xzf postgres_exporter-${VERSION}.linux-amd64.tar.gz

# Create configuration
cat > /etc/postgres_exporter/postgres_exporter.env << EOF
DATA_SOURCE_NAME="postgresql://postgres_exporter:password@localhost:5432/postgres?sslmode=disable"
PG_EXPORTER_EXTEND_QUERY_PATH="/etc/postgres_exporter/queries.yaml"
PG_EXPORTER_METRIC_PREFIX="pg"
EOF

# Create systemd service
cat > /etc/systemd/system/postgres_exporter.service << EOF
[Unit]
Description=Prometheus PostgreSQL Exporter
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$EXPORTER_USER
EnvironmentFile=/etc/postgres_exporter/postgres_exporter.env
ExecStart=$EXPORTER_HOME/postgres_exporter --web.listen-address=":9187"
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# Enable and start
systemctl daemon-reload
systemctl enable postgres_exporter
systemctl start postgres_exporter

# Verify
curl http://localhost:9187/metrics | head -20
```

### Custom Query Metrics

```yaml
# /etc/postgres_exporter/queries.yaml - Custom metrics

pg_gym_workouts_hourly:
  query: |
    SELECT 
      DATE_TRUNC('hour', created_at) as hour,
      COUNT(*) as count
    FROM workouts
    WHERE created_at > NOW() - INTERVAL '1 day'
    GROUP BY DATE_TRUNC('hour', created_at)
  metrics:
    - {name: workouts_hourly_count, help: "Hourly workout count", type: gauge, valueCol: count}

pg_gym_user_growth:
  query: |
    SELECT 
      DATE(created_at) as date,
      COUNT(*) as total_users
    FROM users
    WHERE created_at > NOW() - INTERVAL '30 days'
    GROUP BY DATE(created_at)
  metrics:
    - {name: total_users_daily, help: "Cumulative user count", type: gauge, valueCol: total_users}

pg_gym_cache_hit_ratio:
  query: |
    SELECT 
      datname as db,
      ROUND(100.0 * heap_blks_hit / (heap_blks_hit + heap_blks_read), 2) as hit_ratio
    FROM pg_stat_database
    WHERE heap_blks_read + heap_blks_hit > 0
  metrics:
    - {name: cache_hit_ratio, help: "Cache hit ratio", type: gauge, valueCol: hit_ratio, labels: [db]}
```

---

## Prometheus Configuration

### Prometheus Config

```yaml
# /etc/prometheus/prometheus.yml

global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'gym-platform'
    environment: 'production'

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

# Load alert rules
rule_files:
  - '/etc/prometheus/rules/*.yml'

scrape_configs:
  # PostgreSQL Primary
  - job_name: 'postgres-primary'
    static_configs:
      - targets: ['10.0.1.10:9187']
        labels:
          instance: 'primary'
          environment: 'production'
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance_ip
      - target_label: __scheme__
        replacement: 'http'

  # PostgreSQL Replicas
  - job_name: 'postgres-replicas'
    static_configs:
      - targets: ['10.0.1.11:9187', '10.0.1.12:9187', '10.0.1.13:9187']
        labels:
          role: 'replica'
          environment: 'production'

  # Prometheus self-monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Node exporter (system metrics)
  - job_name: 'node'
    static_configs:
      - targets: 
          - '10.0.1.10:9100'
          - '10.0.1.11:9100'
          - '10.0.1.12:9100'

  # Federation from secondary cluster
  - job_name: 'prometheus-federation'
    honor_labels: true
    scrape_interval: 30s
    static_configs:
      - targets: ['10.0.2.10:9090/metrics']
```

### Prometheus Storage

```bash
# Setup Prometheus data retention and storage

# In /etc/prometheus/prometheus.yml:
# Retention: 30 days
# Storage: ~50GB
# Scrape interval: 15s

# Configure storage
storage:
  tsdb:
    retention:
      time: 30d
    max_block_duration: 24h
    min_block_duration: 2h
    wal_compression: true

# Estimate storage:
# Metrics per instance: ~500
# Instances: 6 (1 primary + 3 replicas + 2 secondaries)
# Retention: 30 days
# Size per metric-day: ~1KB
# Total: 500 * 6 * 30 * 1KB = 90GB

# Setup external storage for long-term
# Prometheus Remote Storage: TimescaleDB or similar
remote_write:
  - url: "http://timescaledb:9201/write"
    write_relabel_configs:
      - source_labels: [__name__]
        regex: 'pg_.*|node_.*'
        action: keep

remote_read:
  - url: "http://timescaledb:9201/read"
    read_recent: true
```

---

## Grafana Dashboards

### Dashboard: PostgreSQL Overview

```sql
-- Key metrics queries for dashboard

-- 1. Connection Status
SELECT 
  'Active Connections' as metric,
  COUNT(*) as value
FROM pg_stat_activity
WHERE state = 'active'
UNION ALL
SELECT 'Idle Connections', COUNT(*) 
FROM pg_stat_activity WHERE state = 'idle'
UNION ALL
SELECT 'Idle in Transaction', COUNT()
FROM pg_stat_activity WHERE state = 'idle in transaction';

-- 2. Transaction Rate
SELECT 
  DATE_TRUNC('minute', now()) as time,
  SUM(xact_commit) as commits,
  SUM(xact_rollback) as rollbacks
FROM pg_stat_database
GROUP BY DATE_TRUNC('minute', now());

-- 3. Cache Hit Ratio
SELECT 
  ROUND(100.0 * heap_blks_hit / (heap_blks_hit + heap_blks_read), 2) as cache_hit_ratio
FROM pg_stat_database
WHERE datname = current_database();

-- 4. Table Access Patterns
SELECT 
  schemaname,
  tablename,
  seq_scan,
  idx_scan,
  CASE WHEN seq_scan + idx_scan = 0 THEN 0
       ELSE ROUND(100.0 * idx_scan / (seq_scan + idx_scan), 2)
  END as index_hit_ratio
FROM pg_stat_user_tables
WHERE schemaname != 'information_schema'
ORDER BY seq_scan + idx_scan DESC
LIMIT 10;

-- 5. Slow Queries
SELECT 
  query,
  calls,
  total_time,
  mean_time,
  max_time
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;
```

### Dashboard: Health and Performance

```sql
-- 1. WAL Generation Rate
SELECT 
  pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') / (1024*1024) as wal_size_mb;

-- 2. Replication Lag
SELECT 
  slot_name,
  active,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / (1024*1024)) as retained_mb,
  (pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn) / (1024*1024)) as lag_mb
FROM pg_replication_slots;

-- 3. Memory Usage
SELECT 
  name,
  setting,
  (setting::bigint * CASE unit
    WHEN 'B' THEN 1
    WHEN 'kB' THEN 1024
    WHEN 'MB' THEN 1024*1024
    WHEN 'GB' THEN 1024*1024*1024
  END) / (1024*1024) as size_mb
FROM pg_settings
WHERE name IN ('shared_buffers', 'effective_cache_size', 'work_mem');

-- 4. Checkpoint Activity
SELECT 
  'Checkpoints Timed' as metric,
  checkpoints_timed as value
FROM pg_stat_bgwriter
UNION ALL
SELECT 'Checkpoints Requested', checkpoints_req FROM pg_stat_bgwriter;

-- 5. Autovacuum Activity
SELECT 
  schemaname,
  tablename,
  last_autovacuum,
  autovacuum_count,
  n_dead_tup
FROM pg_stat_user_tables
WHERE autovacuum_count > 0
ORDER BY autovacuum_count DESC
LIMIT 5;
```

---

## Key Performance Metrics

### Critical Metrics to Monitor

```
Database Health:
  ✓ Connection count (total, active, idle)
  ✓ Transaction rate (commits, rollbacks)
  ✓ Cache hit ratio (> 99%)
  ✓ Database size trend
  ✓ Replication lag (< 1MB)

Query Performance:
  ✓ Query execution time (p50, p95, p99)
  ✓ Slow query count
  ✓ Lock wait time
  ✓ Index usage vs seq scans

Memory:
  ✓ Shared buffer hit ratio
  ✓ Memory usage trend
  ✓ Work memory usage per query

I/O:
  ✓ Checkpoint frequency
  ✓ WAL write rate
  ✓ Disk I/O utilization
  ✓ IOPS per operation type

Replication:
  ✓ Standby count and status
  ✓ LSN positions (primary vs replica)
  ✓ Replication slot health
  ✓ WAL archiving lag
```

### Alert Thresholds

```yaml
# Alert thresholds for production

CPU:
  WARNING: > 70%
  CRITICAL: > 90%

Memory:
  WARNING: > 80%
  CRITICAL: > 95%

Disk:
  WARNING: > 70%
  CRITICAL: > 85%

Connections:
  WARNING: > 80% of max
  CRITICAL: > 95% of max

Cache Hit Ratio:
  WARNING: < 98%
  CRITICAL: < 95%

Replication Lag:
  WARNING: > 10MB
  CRITICAL: > 100MB

Query Time:
  WARNING: > 1000ms
  CRITICAL: > 5000ms
```

---

## Alert Rules

### Prometheus Alert Rules

```yaml
# /etc/prometheus/rules/postgres.yml

groups:
  - name: postgresql_alerts
    interval: 30s
    rules:
      # Connection pool exhaustion
      - alert: PostgreSQLMaxConnectionsApproaching
        expr: pg_setting_max_connections - pg_stat_activity_count{state="active"} < 20
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL max connections approaching on {{ $labels.instance }}"
          description: "Only {{ $value }} connections available"

      # High cache miss rate
      - alert: PostgreSQLCacheMissRate
        expr: rate(pg_stat_database_heap_blks_read[5m]) / (rate(pg_stat_database_heap_blks_read[5m]) + rate(pg_stat_database_heap_blks_hit[5m])) > 0.05
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High cache miss rate on {{ $labels.instance }}"
          description: "Cache miss rate: {{ $value | humanizePercentage }}"

      # Replication lag critical
      - alert: PostgreSQLReplicationLagCritical
        expr: pg_replication_lag_bytes > 104857600  # 100MB
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL replication lag critical on {{ $labels.instance }}"
          description: "Lag: {{ $value | humanize }}B on {{ $labels.slot_name }}"

      # Slow queries
      - alert: PostgreSQLSlowQuery
        expr: pg_stat_statements_mean_time > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow query detected on {{ $labels.instance }}"
          description: "Query time: {{ $value | humanize }}ms"

      # Table bloat warning
      - alert: PostgreSQLTableBloat
        expr: (pg_stat_user_tables_n_dead_tup / (pg_stat_user_tables_n_live_tup + pg_stat_user_tables_n_dead_tup)) > 0.2
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Table bloat detected on {{ $labels.relname }}"
          description: "Dead tuples: {{ $value | humanizePercentage }}"

      # Transaction wraparound risk
      - alert: PostgreSQLTransactionWraparoundRisk
        expr: (2147483647 - pg_database_datfrozenxid) / 1000000 < 100
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Transaction wraparound imminent on {{ $labels.datname }}"
          description: "Transactions until wraparound: {{ $value | humanize }}M"

      # Replication not active
      - alert: PostgreSQLReplicationNotActive
        expr: pg_replication_is_replica == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL replication not active on {{ $labels.instance }}"
          description: "Replica is not connected or not actively replicating"
```

---

## Log Aggregation

### PostgreSQL Logging Configuration

```ini
# postgresql.conf - Comprehensive logging

log_connections = on
log_disconnections = on
log_min_duration_statement = 500  # Log queries > 500ms
log_statement = 'all'
log_duration = on

log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_checkpoints = on
log_lock_waits = on
log_autovacuum_min_duration = 0

# Send logs to syslog
logging_collector = off
syslog_facility = LOCAL0
syslog_ident = postgres
syslog_sequence_numbers = on
syslog_split_messages = on
```

### ELK Stack Integration

```bash
#!/bin/bash
# filebeat-postgres.yml - Ship logs to Elasticsearch

filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/postgresql/*.log
  multiline.pattern: '^\d{4}-\d{2}-\d{2}'
  multiline.negate: true
  multiline.match: after
  fields:
    log_type: postgresql
    environment: production

output.elasticsearch:
  hosts: ["elasticsearch.gym.local:9200"]
  index: "postgres-%{+yyyy.MM.dd}"
  
setup.kibana:
  host: "kibana.gym.local:5601"

processors:
  - add_host_metadata: ~
  - add_docker_metadata: ~
  - dissect:
      tokenizer: "%{timestamp} [%{pid}]: [%{loglevel}] user=%{user},db=%{database},app=%{app},client=%{client} %{message}"
      field: message
      target_prefix: postgres
```

---

## Health Checks

### Automated Health Checks

```bash
#!/bin/bash
# health-check.sh - Automated database health check

HEALTH_CHECK_LOG="/var/log/postgresql/health-check.log"
ALERT_EMAIL="dba@gym.local"

check_database_availability() {
  if psql -U postgres -d postgres -c "SELECT 1" >/dev/null 2>&1; then
    echo "[$(date)] ✓ Database available" >> "$HEALTH_CHECK_LOG"
    return 0
  else
    echo "[$(date)] ✗ Database unavailable" >> "$HEALTH_CHECK_LOG"
    send_alert "Database availability check FAILED"
    return 1
  fi
}

check_replication_health() {
  LAG=$(psql -U postgres -t -c "SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn) / (1024*1024) FROM pg_replication_slots LIMIT 1")
  
  if (( $(echo "$LAG > 100" | bc -l) )); then
    echo "[$(date)] ✗ Replication lag critical: ${LAG}MB" >> "$HEALTH_CHECK_LOG"
    send_alert "Replication lag exceeds 100MB: ${LAG}MB"
    return 1
  else
    echo "[$(date)] ✓ Replication lag OK: ${LAG}MB" >> "$HEALTH_CHECK_LOG"
    return 0
  fi
}

check_cache_hit_ratio() {
  RATIO=$(psql -U postgres -t -c "SELECT ROUND(100.0 * heap_blks_hit / (heap_blks_hit + heap_blks_read), 2) FROM pg_stat_database WHERE datname = current_database()")
  
  if (( $(echo "$RATIO < 95" | bc -l) )); then
    echo "[$(date)] ✗ Cache hit ratio low: ${RATIO}%" >> "$HEALTH_CHECK_LOG"
    return 1
  else
    echo "[$(date)] ✓ Cache hit ratio OK: ${RATIO}%" >> "$HEALTH_CHECK_LOG"
    return 0
  fi
}

check_bloat() {
  BLOAT=$(psql -U postgres -t -c "SELECT COUNT(*) FROM pg_stat_user_tables WHERE (n_dead_tup::float / (n_live_tup + n_dead_tup)) > 0.3")
  
  if [ "$BLOAT" -gt 5 ]; then
    echo "[$(date)] ✗ Table bloat detected: $BLOAT tables" >> "$HEALTH_CHECK_LOG"
    return 1
  else
    echo "[$(date)] ✓ Table bloat OK" >> "$HEALTH_CHECK_LOG"
    return 0
  fi
}

send_alert() {
  echo "$1" | mail -s "[ALERT] PostgreSQL Health Check" "$ALERT_EMAIL"
}

# Run all checks
check_database_availability && check_replication_health && check_cache_hit_ratio && check_bloat
```

---

## Incident Response

### Alert Response Procedures

```bash
#!/bin/bash
# incident-response.sh - Automated incident response

INCIDENT_LOG="/var/log/postgresql/incidents.log"
SLACK_WEBHOOK="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
PAGERDUTY_KEY="xxx"

log_incident() {
  local severity=$1
  local message=$2
  local time=$(date '+%Y-%m-%d %H:%M:%S')
  
  echo "[$time] [$severity] $message" >> "$INCIDENT_LOG"
}

notify_slack() {
  local message=$1
  local severity=$2
  
  local color="warning"
  [ "$severity" = "CRITICAL" ] && color="danger"
  [ "$severity" = "RESOLVED" ] && color="good"
  
  curl -X POST -H 'Content-type: application/json' \
    --data "{
      \"attachments\": [{
        \"color\": \"$color\",
        \"title\": \"PostgreSQL Alert\",
        \"text\": \"$message\",
        \"ts\": $(date +%s)
      }]
    }" \
    "$SLACK_WEBHOOK"
}

trigger_pagerduty() {
  local incident=$1
  local severity=$2
  
  curl -X POST "https://events.pagerduty.com/v2/enqueue" \
    -H 'Content-Type: application/json' \
    -d "{
      \"routing_key\": \"$PAGERDUTY_KEY\",
      \"event_action\": \"trigger\",
      \"payload\": {
        \"summary\": \"$incident\",
        \"severity\": \"$(echo $severity | tr '[:upper:]' '[:lower:]')\",
        \"source\": \"postgres-monitoring\"
      }
    }"
}

# Response to critical replication lag
handle_replication_lag_critical() {
  log_incident "CRITICAL" "Replication lag exceeded 100MB"
  notify_slack "Replication lag CRITICAL - immediate action required" "CRITICAL"
  
  # Attempt recovery
  psql -U postgres -d postgres << SQL
  -- Check replica status
  SELECT slot_name, active, (pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn) / (1024*1024)) as lag_mb FROM pg_replication_slots;
  
  -- Attempt to push WAL
  SELECT pg_wal_flush();
SQL
  
  trigger_pagerduty "PostgreSQL replication lag critical" "critical"
}

# Response to high connection count
handle_high_connections() {
  log_incident "WARNING" "Connection count approaching limit"
  notify_slack "PostgreSQL connections approaching limit" "WARNING"
  
  # Kill idle connections
  psql -U postgres -d postgres << SQL
  SELECT pg_terminate_backend(pid) 
  FROM pg_stat_activity 
  WHERE state = 'idle' 
    AND query_start < NOW() - INTERVAL '10 minutes';
SQL
}
```

---

## Troubleshooting Monitoring

### Common Monitoring Issues

**Issue: Exporter connection refused**

```bash
# Check exporter is running
ps aux | grep postgres_exporter

# Check port is listening
netstat -tlnp | grep 9187

# Verify connection string
cat /etc/postgres_exporter/postgres_exporter.env

# Test connection
psql -h localhost -U postgres_exporter -d postgres
```

**Issue: Gaps in metrics**

```bash
# Check Prometheus scrape targets
curl http://localhost:9090/api/v1/targets

# Check exporter metrics endpoint
curl http://localhost:9187/metrics | wc -l

# View scrape errors
grep "scrape_duration_seconds_created" /prometheus/data/wal/00000000000000000000
```

**Issue: High memory usage by Prometheus**

```bash
# Reduce retention period
--storage.tsdb.retention.time=7d

# Disable expensive metrics
global:
  external_labels:
    __scrape_interval_adjust: "true"

# Use downsampling for old data
# Implement recording rules
```

---

## Related Documentation

- [04-performance-tuning.md](04-performance-tuning.md) - Tune based on metrics
- [05-maintenance-procedures.md](05-maintenance-procedures.md) - Maintain database health
- [08-troubleshooting.md](08-troubleshooting.md) - General troubleshooting

