# Database Overview

## Current Setup

Single PostgreSQL 15 instance running in Docker, shared by all four microservices via separate schemas.

```
gym_db (PostgreSQL 15, port 5432)
├── auth_schema        ← auth-service
├── training_schema    ← training-service
├── tracking_schema    ← tracking-service
└── notification_schema ← notification-service
```

Each service connects with its own `spring.datasource.url` pointing to `gym_db` with `?currentSchema=<schema>`. DDL is managed by Hibernate (`ddl-auto: update` in dev, `validate` in prod).

## Connection Configuration

```yaml
# Example: auth-service application.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/gym_db?currentSchema=auth_schema
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.default_schema: auth_schema
```

## Docker Setup

```yaml
# docker-compose.yml (relevant section)
postgres:
  image: postgres:15
  environment:
    POSTGRES_DB: gym_db
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
  volumes:
    - ./dba/initialization/schemas:/docker-entrypoint-initdb.d
    - postgres_data:/var/lib/postgresql/data
  ports:
    - "5432:5432"
```

Initialization scripts in `dba/initialization/schemas/` create the four schemas on first startup.

## Connection Pooling

HikariCP (Spring Boot default) is used per service. No external connection pooler (PgBouncer) is configured in the current setup.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

## Schema Isolation

Services only access their own schema. There are no cross-schema foreign keys — services are linked only by `userId` (a Long passed via the `X-User-Id` header injected by the API Gateway).

## Related Documentation

- [Schema Design](02-schema-design.md) — DDL and table structure
- [Architecture: Database Schema](../arquitectura/03-database-schema.md) — Entity-level reference
- [Backup & Recovery](03-backup-recovery.md) — Backup procedures
- [Performance Tuning](04-performance-tuning.md) — Query optimization
- [Migration Guide](05-migration-guide.md) — Schema migrations
