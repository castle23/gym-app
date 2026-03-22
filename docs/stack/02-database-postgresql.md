# PostgreSQL Database

## Overview

Gym Platform uses **PostgreSQL 13+** as the primary relational database. This document covers PostgreSQL configuration, setup, connection pooling, optimization strategies, and best practices for all microservices.

**Current Version:** PostgreSQL 13.x (production)

## Architecture

### Database Structure

```
Gym Platform PostgreSQL (single instance: gym_db)
├── auth_schema      (Auth Service)
├── training_schema  (Training Service)
├── tracking_schema  (Tracking Service)
└── notification_schema (Notification Service)
```

### Service Isolation Strategy

All services share a single PostgreSQL instance (`gym_db`) with separate schemas per service. A single user `gym_admin` is used for all services. Schema isolation is enforced via `hibernate.default_schema` in each service's `application.yml`. Cross-service queries are not performed — services communicate only via the API Gateway.

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

**Docker Compose (actual configuration):**
```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: gym-postgres
    environment:
      POSTGRES_USER: gym_admin
      POSTGRES_PASSWORD: ${DB_PASSWORD:-gym_password}
      POSTGRES_DB: gym_db
    volumes:
      - gym_postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gym_admin -d gym_db"]
      interval: 10s
      timeout: 5s
      retries: 5
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

**Connection configuration (application.yml):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/gym_db
    username: gym_admin
    password: ${DB_PASSWORD:gym_password}
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

**Example Auth Schema (Hibernate-managed, BIGSERIAL PKs):**
```sql
-- Schema created by Hibernate ddl-auto=update
-- auth_schema.users (representative structure)
CREATE TABLE auth_schema.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON auth_schema.users(username);
CREATE INDEX idx_users_email ON auth_schema.users(email);
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

**Index Strategy (example for auth_schema):**
```sql
CREATE INDEX idx_users_role ON auth_schema.users(role);
CREATE INDEX idx_users_created_at ON auth_schema.users(created_at);
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
# Backup single database (gym_db contains all schemas)
docker exec gym-postgres pg_dump -U gym_admin gym_db | gzip > gym_db_$(date +%Y%m%d_%H%M%S).sql.gz
```

**Scheduled backups (cron):**
```bash
0 2 * * * /usr/local/bin/backup-gym-db.sh

#!/bin/bash
BACKUP_DIR="/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker exec gym-postgres pg_dump -U gym_admin gym_db | gzip > $BACKUP_DIR/gym_db_${TIMESTAMP}.sql.gz
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

### Recovery

**Restore from dump:**
```bash
gunzip < gym_db_backup.sql.gz | docker exec -i gym-postgres psql -U gym_admin gym_db
```

## Replication (High Availability)

> **Note**: Replication is not currently configured. The platform runs a single PostgreSQL instance via Docker Compose.

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
# docker-compose.yml addition (not currently configured)
postgres-exporter:
  image: prometheuscommunity/postgres-exporter:latest
  environment:
    DATA_SOURCE_NAME: "postgresql://gym_admin:gym_password@postgres:5432/gym_db?sslmode=disable"
  ports:
    - "9187:9187"
```

## Security Best Practices

### User Management

```sql
-- Single user gym_admin has access to all schemas
GRANT CONNECT ON DATABASE gym_db TO gym_admin;
GRANT USAGE ON SCHEMA auth_schema, training_schema, tracking_schema, notification_schema TO gym_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth_schema TO gym_admin;
-- (repeat for other schemas)
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
