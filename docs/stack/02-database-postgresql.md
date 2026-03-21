# PostgreSQL Database

## Overview

Gym Platform uses **PostgreSQL 13+** as the primary relational database. This document covers PostgreSQL configuration, setup, connection pooling, optimization strategies, and best practices for all microservices.

**Current Version:** PostgreSQL 13.x (production)

## Architecture

### Database Structure

```
Gym Platform PostgreSQL
├── auth_db (Auth Service)
│   └── public schema (users, roles, tokens)
├── training_db (Training Service)
│   └── public schema (programs, exercises, workouts)
├── tracking_db (Tracking Service)
│   └── public schema (metrics, progress, analytics)
└── notification_db (Notification Service)
    └── public schema (notifications, subscriptions, templates)
```

### Service Isolation Strategy

Each microservice has:
- Dedicated database (logical isolation)
- Separate credentials
- Independent backup/restore procedures
- Scalable schema structure
- Cross-service queries via API only (no direct DB access)

## PostgreSQL Configuration

### Installation

**Ubuntu/Debian:**
```bash
# Update package manager
sudo apt-get update

# Install PostgreSQL
sudo apt-get install postgresql postgresql-contrib postgresql-13

# Start service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installation
psql --version
```

**Docker (Recommended):**
```bash
docker run --name gym-postgres \
  -e POSTGRES_PASSWORD=secure_password \
  -e PGDATA=/var/lib/postgresql/data/pgdata \
  -v gym_postgres_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  -d postgres:13-alpine
```

**Docker Compose (Full Stack):**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:13-alpine
    container_name: gym-postgres
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=en_US.UTF-8"
    volumes:
      - gym_postgres_data:/var/lib/postgresql/data
      - ./dba/initialization/schemas:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - gym-network

  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: gym-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@gym.local
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    networks:
      - gym-network

volumes:
  gym_postgres_data:

networks:
  gym-network:
    driver: bridge
```

### postgresql.conf Optimization

**Development Profile:**
```ini
# Connection settings
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB

# Query planning
random_page_cost = 1.1
effective_io_concurrency = 200

# Logging
log_min_duration_statement = 1000
log_connections = on
log_disconnections = on
log_statement = 'all'
```

**Production Profile:**
```ini
# Connection settings
max_connections = 500
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 16MB
maintenance_work_mem = 1GB

# Query planning
random_page_cost = 1.1
effective_io_concurrency = 200

# WAL & Replication
wal_level = replica
wal_buffers = 16MB
checkpoint_timeout = 15min

# Logging (minimal for performance)
log_min_duration_statement = 5000
log_statement = 'mod'
log_connections = off

# Performance
shared_preload_libraries = 'pg_stat_statements'
```

## Connection Management

### Connection Pooling with HikariCP

**Configuration (application.yml):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: auth_user
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
      auto-commit: true
```

### Java Configuration

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### Connection Monitoring

**Monitor active connections:**
```sql
SELECT datname, usename, application_name, state, count(*)
FROM pg_stat_activity
GROUP BY datname, usename, application_name, state
ORDER BY count(*) DESC;
```

**Terminate idle connections:**
```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'auth_db'
  AND state = 'idle'
  AND query_start < now() - INTERVAL '30 minutes';
```

## Database Initialization

### Schema Creation Scripts

**Location:** `dba/initialization/schemas/01-init-schemas.sql`

Each service database is created with:
- Dedicated schema (public)
- Service user with limited permissions
- UUID extensions
- Audit tables and triggers

**Example Auth Schema:**
```sql
-- Create database
CREATE DATABASE auth_db
    ENCODING 'UTF8'
    LOCALE 'en_US.UTF-8'
    TEMPLATE template0;

-- Connect to auth_db
\c auth_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Auth schema
CREATE SCHEMA IF NOT EXISTS auth;

-- Create user tables
CREATE TABLE auth.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Audit table
CREATE TABLE auth.audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_users_username ON auth.users(username);
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_audit_user_id ON auth.audit_log(user_id);
CREATE INDEX idx_audit_timestamp ON auth.audit_log(timestamp);
```

### Data Initialization

See [dba/initialization/README.md](../../dba/initialization/README.md) for data loading procedures.

## Optimization Strategies

### Query Optimization

**Use EXPLAIN to analyze queries:**
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT u.id, u.username, COUNT(t.id) as token_count
FROM auth.users u
LEFT JOIN auth.tokens t ON u.id = t.user_id
WHERE u.created_at > NOW() - INTERVAL '30 days'
GROUP BY u.id, u.username
ORDER BY token_count DESC;
```

**Index Strategy:**
```sql
-- Single column indices for frequent filters
CREATE INDEX idx_users_role ON auth.users(role);
CREATE INDEX idx_users_is_active ON auth.users(is_active);
CREATE INDEX idx_users_created_at ON auth.users(created_at);

-- Composite indices for common queries
CREATE INDEX idx_users_active_role 
    ON auth.users(is_active, role) 
    WHERE is_active = true;

-- Partial indices for specific conditions
CREATE INDEX idx_users_admin 
    ON auth.users(id) 
    WHERE role = 'ADMIN' AND is_active = true;
```

### Vacuum and Analyze

**Manual maintenance:**
```sql
-- Vacuum (reclaim storage)
VACUUM FULL auth.users;

-- Analyze (update statistics)
ANALYZE auth.users;

-- Combined
VACUUM ANALYZE auth.users;
```

**Automatic maintenance (postgresql.conf):**
```ini
autovacuum = on
autovacuum_naptime = 1min
autovacuum_vacuum_threshold = 50
autovacuum_analyze_threshold = 50
```

### Connection Tuning

**Monitor connection usage:**
```sql
SELECT datname, count(*) as connections
FROM pg_stat_activity
GROUP BY datname
ORDER BY connections DESC;
```

**Identify slow queries:**
```sql
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat%'
ORDER BY mean_time DESC
LIMIT 20;
```

## Backup and Recovery

### Backup Strategy

**Full backup:**
```bash
# Using pg_dump
pg_dump -U postgres -h localhost auth_db | gzip > auth_db_$(date +%Y%m%d_%H%M%S).sql.gz

# Using pg_dump with custom format (faster restore)
pg_dump -U postgres -h localhost -Fc auth_db > auth_db_backup.dump
```

**Scheduled backups (cron):**
```bash
# /etc/cron.d/postgres-backup
0 2 * * * postgres /usr/local/bin/backup-databases.sh

# backup-databases.sh
#!/bin/bash
BACKUP_DIR="/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

for db in auth_db training_db tracking_db notification_db; do
    pg_dump -U postgres -Fc $db | gzip > $BACKUP_DIR/${db}_${TIMESTAMP}.sql.gz
done

# Remove backups older than 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

### Recovery

**Restore from dump:**
```bash
# From SQL file
psql -U postgres auth_db < auth_db_backup.sql

# From custom format
pg_restore -U postgres -d auth_db auth_db_backup.dump
```

## Replication (High Availability)

### Primary-Replica Setup

**Primary server (postgresql.conf):**
```ini
wal_level = replica
max_wal_senders = 10
wal_keep_size = 1GB
hot_standby = on
```

**Create replication user:**
```sql
CREATE USER replication_user REPLICATION ENCRYPTED PASSWORD 'password';
```

**Replica initialization:**
```bash
pg_basebackup -h primary_host -U replication_user -D /var/lib/postgresql/data -v -P
```

## Monitoring and Alerting

### Key Metrics to Monitor

| Metric | Alert Threshold | Check Command |
|--------|-----------------|----------------|
| Connection count | > 80% of max | `SELECT count(*) FROM pg_stat_activity;` |
| Cache hit ratio | < 99% | `SELECT heap_blks_hit / (heap_blks_hit + heap_blks_read) FROM pg_statio_user_tables;` |
| Slow queries | > 1s | `SELECT query FROM pg_stat_statements WHERE mean_time > 1000;` |
| Table bloat | > 50% | `SELECT relname, n_live_tup, n_dead_tup FROM pg_stat_user_tables;` |
| Disk usage | > 80% | `SELECT pg_database_size(datname) FROM pg_database;` |

### PostgreSQL Exporter (Prometheus)

```yaml
# docker-compose.yml addition
postgres-exporter:
  image: prometheuscommunity/postgres-exporter:latest
  environment:
    DATA_SOURCE_NAME: "postgresql://postgres:password@postgres:5432/postgres?sslmode=disable"
  ports:
    - "9187:9187"
  networks:
    - gym-network
```

## Security Best Practices

### User Management

```sql
-- Create service user with minimal permissions
CREATE USER auth_service WITH ENCRYPTED PASSWORD 'secure_password';

-- Grant only necessary permissions
GRANT CONNECT ON DATABASE auth_db TO auth_service;
GRANT USAGE ON SCHEMA auth TO auth_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth TO auth_service;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA auth TO auth_service;
```

### Connection Security

**Use SSL in production:**
```bash
# Generate certificates
openssl req -new -x509 -days 365 -nodes -out server.crt -keyout server.key
chmod 600 server.key
chown postgres:postgres server.crt server.key
```

**postgresql.conf:**
```ini
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
```

**pg_hba.conf (with SSL required):**
```
# Host-based authentication
local   all             all                                     trust
hostssl all             all             127.0.0.1/32            md5
hostssl all             all             ::1/128                 md5
```

## Key References

- [PostgreSQL Official Documentation](https://www.postgresql.org/docs/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP/wiki/Configuration)
- See also: [dba/initialization/README.md](../../dba/initialization/README.md)
- See also: [docs/database/](../database/)
