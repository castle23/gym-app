# Database Overview

## Overview

The Gym Platform uses PostgreSQL 14+ as the primary relational database for storing user accounts, workout data, training plans, and tracking information. This guide provides comprehensive information about database setup, architecture, configuration, connection management, and best practices for the Gym Platform microservices.

PostgreSQL provides ACID compliance, JSON support, full-text search, and advanced features required by the Gym Platform. The database is deployed in a highly available configuration with replication, automated backups, and disaster recovery capabilities.

## Table of Contents

- [Database Architecture](#database-architecture)
- [PostgreSQL Setup](#postgresql-setup)
- [Connection Pooling](#connection-pooling)
- [Database Instances](#database-instances)
- [Configuration & Optimization](#configuration--optimization)
- [Security](#security)
- [Monitoring & Alerting](#monitoring--alerting)
- [Disaster Recovery](#disaster-recovery)
- [Best Practices](#best-practices)

---

## Database Architecture

### High Availability Setup

The Gym Platform uses a PostgreSQL primary-replica architecture with automatic failover:

```
Primary Database (Primary)
├── Synchronous Replication
├── Read-only Replicas (3x)
├── Backup Standby
└── WAL Archive
```

**Architecture Diagram:**

```
                    ┌─────────────────────────────┐
                    │   Application Servers       │
                    │  (Auth, Training, Tracking) │
                    └────────────┬────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
            Read-Write (Primary)       Read-Only (Replicas)
            ┌──────────────────┐      ┌──────────────────┐
            │ gym-db-primary   │      │  gym-db-replica1 │
            │ Port: 5432       │◄─────│  Port: 5433      │
            │ (Active)         │      │  (Hot Standby)   │
            └──────┬───────────┘      └──────────────────┘
                   │                  
                   │ WAL Replication
                   │
        ┌──────────┴───────────┐
        │                      │
    ┌───────────┐         ┌─────────────┐
    │ Replica 2 │         │ Replica 3   │
    │ Port 5433 │         │ Port 5433   │
    └───────────┘         └─────────────┘

    ┌──────────────────────────────────┐
    │  WAL Archive (S3/Backup Storage) │
    │  Point-in-time Recovery          │
    └──────────────────────────────────┘
```

### Replication Strategy

**Synchronous Replication:**
- Primary waits for replica acknowledgment before committing
- Guarantees no data loss on failover
- Slight latency impact (~50-100ms for critical writes)

**Asynchronous Read Replicas:**
- Used for read operations and reporting
- Eventually consistent
- Zero latency impact on primary

### Failover Mechanism

Automatic failover with Patroni or pg_auto_failover:

```yaml
failover-configuration:
  failover-type: automatic
  failover-timeout: 30-seconds
  leader-election: consensus-based
  
  replica-priority:
    1: gym-db-replica1 (primary candidate)
    2: gym-db-replica2 (secondary candidate)
    3: gym-db-replica3 (tertiary candidate)
  
  health-check:
    interval: 10-seconds
    timeout: 5-seconds
    retries: 3
```

---

## PostgreSQL Setup

### Installation

**Docker Installation (Development):**

```dockerfile
FROM postgres:14-alpine

ENV POSTGRES_USER=gym
ENV POSTGRES_PASSWORD=${DB_PASSWORD}
ENV POSTGRES_DB=gym_platform

# Install extensions
RUN apk add --no-cache postgresql-contrib

# Copy initialization scripts
COPY init-db.sql /docker-entrypoint-initdb.d/

# Configuration
COPY postgresql.conf /etc/postgresql/
COPY pg_hba.conf /etc/postgresql/

EXPOSE 5432
```

**Linux Installation (Production):**

```bash
#!/bin/bash

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y postgresql-14 postgresql-contrib-14 postgresql-14-repack

# Start service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installation
sudo -u postgres psql --version
```

### Initial Configuration

**postgresql.conf:**

```ini
# PostgreSQL Configuration for Gym Platform

# Connection Settings
listen_addresses = '*'
port = 5432
max_connections = 500

# Memory Settings
shared_buffers = 4GB           # 25% of available RAM
effective_cache_size = 12GB    # 75% of available RAM
maintenance_work_mem = 1GB
work_mem = 16MB

# Replication Settings
wal_level = replica
max_wal_senders = 5
wal_keep_size = 1GB
hot_standby = on
hot_standby_feedback = on

# WAL Settings
wal_buffers = 16MB
min_wal_size = 1GB
max_wal_size = 4GB
checkpoint_completion_target = 0.9

# Logging
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%a.log'
log_truncate_on_rotation = on
log_rotation_age = 1d
log_rotation_size = 0
log_min_duration_statement = 1000  # Log queries > 1 second
log_connections = on
log_disconnections = on
log_statement = 'mod'

# Query Planning
random_page_cost = 1.1
effective_io_concurrency = 200

# Locks
deadlock_timeout = 1s
```

**pg_hba.conf (Authentication):**

```conf
# Gym Platform PostgreSQL Authentication

# Local connections
local   all             all                                     trust

# Localhost connections
host    all             all             127.0.0.1/32           md5
host    all             all             ::1/128                md5

# Replication connections
host    replication     gym_replicator  10.0.0.0/8             md5

# Application connections (private network)
host    gym_platform    gym_app         10.0.0.0/8             md5

# SSL connections
hostssl gym_platform    gym_app         10.0.0.0/8             md5
```

### Database Initialization

**init-db.sql:**

```sql
-- Create application user
CREATE USER gym_app WITH PASSWORD 'secure_password_here';
CREATE USER gym_replicator WITH REPLICATION PASSWORD 'replication_password_here';

-- Create database
CREATE DATABASE gym_platform OWNER gym_app;

-- Connect to database
\c gym_platform

-- Create schemas
CREATE SCHEMA auth AUTHORIZATION gym_app;
CREATE SCHEMA training AUTHORIZATION gym_app;
CREATE SCHEMA tracking AUTHORIZATION gym_app;
CREATE SCHEMA common AUTHORIZATION gym_app;

-- Grant permissions
GRANT CONNECT ON DATABASE gym_platform TO gym_app;
GRANT USAGE ON SCHEMA auth, training, tracking, common TO gym_app;
GRANT CREATE ON SCHEMA auth, training, tracking, common TO gym_app;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "hstore";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create base tables (schemas defined in 02-schema-design.md)
-- See schema-design documentation for full DDL
```

---

## Connection Pooling

### PgBouncer Configuration

Connection pooling with PgBouncer reduces database connection overhead:

```ini
# /etc/pgbouncer/pgbouncer.ini

[databases]
gym_platform = host=gym-db-primary port=5432 dbname=gym_platform

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 25
min_pool_size = 10
reserve_pool_size = 5
reserve_pool_timeout = 3

# Connection timeout
connect_timeout = 15
server_lifetime = 3600
server_idle_in_transaction_session_timeout = 60

# Logging
log_connections = 1
log_disconnections = 1
log_file = /var/log/pgbouncer.log
```

### Spring Boot Connection Pool

**application.yml:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://pgbouncer:6432/gym_platform
    username: ${DB_USERNAME:gym_app}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      auto-commit: true
      leak-detection-threshold: 60000
      connection-test-query: "SELECT 1"
```

**Java Configuration:**

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DATABASE_URL"));
        config.setUsername(System.getenv("DB_USERNAME"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
}
```

---

## Database Instances

### Instance Specifications

| Instance | Role | CPU | RAM | Storage | Purpose |
|----------|------|-----|-----|---------|---------|
| gym-db-primary | Primary | 8 cores | 32GB | 500GB SSD | Production writes |
| gym-db-replica1 | Replica | 8 cores | 32GB | 500GB SSD | Hot standby + reads |
| gym-db-replica2 | Replica | 4 cores | 16GB | 500GB SSD | Reporting queries |
| gym-db-replica3 | Replica | 4 cores | 16GB | 500GB SSD | Analytics queries |
| gym-db-backup | Backup | 4 cores | 16GB | 1TB SSD | WAL archive, PITR |

### Instance Configuration

**Primary Database:**

```sql
-- Primary-specific settings
ALTER SYSTEM SET wal_level = replica;
ALTER SYSTEM SET max_wal_senders = 5;
ALTER SYSTEM SET wal_keep_size = '1GB';
ALTER SYSTEM SET synchronous_commit = remote_write;

SELECT pg_ctl_reload_config();
```

**Replica Configuration:**

```sql
-- Replica-specific settings
ALTER SYSTEM SET hot_standby = on;
ALTER SYSTEM SET hot_standby_feedback = on;
ALTER SYSTEM SET max_standby_streaming_delay = '5min';

SELECT pg_ctl_reload_config();
```

---

## Configuration & Optimization

### Performance Tuning

**Memory Configuration:**

```sql
-- Check current settings
SHOW shared_buffers;
SHOW effective_cache_size;
SHOW work_mem;

-- Optimal settings for 32GB RAM
ALTER SYSTEM SET shared_buffers = '8GB';
ALTER SYSTEM SET effective_cache_size = '24GB';
ALTER SYSTEM SET maintenance_work_mem = '2GB';
ALTER SYSTEM SET work_mem = '32MB';
```

**WAL Configuration:**

```sql
-- Optimize write-ahead log
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET min_wal_size = '1GB';
ALTER SYSTEM SET max_wal_size = '4GB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
```

**Query Planning:**

```sql
-- Optimize query planner
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;
ALTER SYSTEM SET jit = on;
ALTER SYSTEM SET jit_above_cost = 100000;
```

### Monitoring Performance

```sql
-- Long-running queries
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes';

-- Cache hit ratio
SELECT sum(heap_blks_hit)/(sum(heap_blks_hit)+sum(heap_blks_read)) AS cache_ratio
FROM pg_statio_user_tables;

-- Index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Table bloat
SELECT schemaname, tablename, 
       round(100.0 * (pg_relation_size(schemaname||'.'||tablename) - 
             pg_relation_size(schemaname||'.'||tablename, 'main')) / 
             pg_relation_size(schemaname||'.'||tablename), 2) AS bloat_ratio
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema');
```

---

## Security

### Access Control

**Role-based Access:**

```sql
-- Create roles
CREATE ROLE gym_app_read NOINHERIT;
CREATE ROLE gym_app_write NOINHERIT;
CREATE ROLE gym_app_admin NOINHERIT;

-- Grant to users
GRANT gym_app_read TO gym_app;
GRANT gym_app_write TO gym_app;

-- Grant schema permissions
GRANT USAGE ON SCHEMA auth, training, tracking TO gym_app_read;
GRANT USAGE ON SCHEMA auth, training, tracking TO gym_app_write;

-- Grant table permissions
GRANT SELECT ON ALL TABLES IN SCHEMA auth TO gym_app_read;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth TO gym_app_write;
```

### Encryption

**SSL/TLS Configuration:**

```bash
# Generate self-signed certificate (development)
openssl req -x509 -newkey rsa:4096 -keyout server.key -out server.crt -days 365

# Generate certificate from CA (production)
openssl req -new -key server.key -out server.csr
# Submit CSR to CA for signing

# Configure PostgreSQL
sudo cp server.crt /etc/postgresql/14/main/
sudo cp server.key /etc/postgresql/14/main/
sudo chown postgres:postgres /etc/postgresql/14/main/server.*
sudo chmod 600 /etc/postgresql/14/main/server.key
```

**postgresql.conf:**

```ini
ssl = on
ssl_cert_file = '/etc/postgresql/14/main/server.crt'
ssl_key_file = '/etc/postgresql/14/main/server.key'
```

### Password Security

```sql
-- Enforce strong passwords
CREATE OR REPLACE FUNCTION enforce_password_strength() RETURNS void AS $$
BEGIN
  EXECUTE 'ALTER ROLE ' || quote_identifier(session_user) || 
    ' ENCRYPTED PASSWORD ''' || 
    crypt('password', gen_salt('bf')) || ''';'
END;
$$ LANGUAGE plpgsql;

-- Audit logging
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  user_name TEXT NOT NULL,
  action TEXT NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  details JSONB
);

CREATE OR REPLACE FUNCTION audit_log_trigger()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO audit_log (user_name, action, details)
  VALUES (SESSION_USER, TG_OP, row_to_json(NEW));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## Monitoring & Alerting

### Key Metrics

```sql
-- Database size
SELECT pg_size_pretty(pg_database_size('gym_platform')) AS db_size;

-- Connection count
SELECT count(*) FROM pg_stat_activity;

-- Transaction wraparound
SELECT datname, age(datfrozenxid) FROM pg_database;

-- Replication lag
SELECT slot_name, restart_lsn, confirmed_flush_lsn FROM pg_replication_slots;
```

### Prometheus Metrics

**postgres_exporter:**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'postgresql'
    static_configs:
      - targets: ['localhost:9187']
        labels:
          instance: 'gym-db-primary'
```

**Key Alerts:**

```yaml
groups:
  - name: postgresql_alerts
    rules:
      - alert: PostgresqlDown
        expr: pg_up == 0
        for: 1m
        
      - alert: PostgresqlReplicationLag
        expr: pg_replication_lag_seconds > 10
        for: 5m
        
      - alert: PostgresqlCacheHitRatio
        expr: pg_cache_hit_ratio < 0.99
        for: 10m
        
      - alert: PostgresqlConnectionLimit
        expr: pg_stat_activity_count > 450
        for: 5m
```

---

## Disaster Recovery

### Backup Strategy

```yaml
backup-configuration:
  type: WAL + Full Backups
  
  full-backups:
    frequency: daily
    time: 02:00 UTC
    retention: 30 days
    destination: AWS S3
  
  wal-archives:
    enabled: yes
    destination: AWS S3
    compression: gzip
    retention: 7 days
  
  point-in-time-recovery:
    enabled: yes
    retention: 7 days
```

### Recovery Procedure

```bash
#!/bin/bash

# 1. Stop database
sudo systemctl stop postgresql

# 2. List available backups
aws s3 ls s3://gym-backups/postgresql/

# 3. Download backup
aws s3 cp s3://gym-backups/postgresql/full-backup-2024-03-20.tar.gz .
tar -xzf full-backup-2024-03-20.tar.gz -C /var/lib/postgresql/14/main

# 4. Create recovery configuration
cat > /var/lib/postgresql/14/main/recovery.conf <<EOF
restore_command = 'aws s3 cp s3://gym-backups/postgresql/wal/%f %p'
recovery_target_timeline = 'latest'
EOF

# 5. Start database
sudo systemctl start postgresql

# 6. Monitor recovery
tail -f /var/log/postgresql/postgresql-*.log
```

---

## Best Practices

### Development

- Use separate database for each developer
- Use fixtures/seeds for test data
- Never use production data in development
- Enable slow query logging (> 1 second)

### Production

- Monitor query performance continuously
- Perform regular VACUUM and ANALYZE
- Archive old data periodically
- Test backup/recovery procedures monthly
- Keep PostgreSQL updated with security patches
- Document all customizations
- Monitor replication lag
- Maintain adequate disk space

---

## Related Documentation

- [Schema Design](02-schema-design.md) - Database schema and DDL
- [Backup & Recovery](03-backup-recovery.md) - Backup procedures
- [Performance Tuning](04-performance-tuning.md) - Query optimization
- [Migration Guide](05-migration-guide.md) - Schema migrations
- [Maintenance](06-maintenance-procedures.md) - Routine maintenance
- [PostgreSQL Documentation](../stack/02-database-postgresql.md) - Stack documentation

## References

- [PostgreSQL Official Documentation](https://www.postgresql.org/docs/14/)
- [PostgreSQL Configuration](https://www.postgresql.org/docs/14/config-setting.html)
- [PostgreSQL Replication](https://www.postgresql.org/docs/14/warm-standby.html)
- [High Availability with PostgreSQL](https://www.postgresql.org/docs/14/warm-standby.html)
- [PostgreSQL Security](https://www.postgresql.org/docs/14/sql-security.html)
