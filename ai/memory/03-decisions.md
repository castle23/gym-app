# Architecture Decisions (ADRs)

Summary of all 12 Architecture Decision Records. Full details in `docs/adr/`.

---

## ADR-001: Microservices Architecture
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: 4 independent microservices (Auth, Training, Tracking, Notification), each with own schema, communicating via REST.
**Rationale**: Independent scaling, deployment agility, team autonomy, fault isolation.
**Consequences (+)**: Services scale independently, deploy without affecting others, technology flexibility.
**Consequences (-)**: Operational complexity, distributed debugging, network latency, eventual consistency.
**Mitigations**: Standardized communication, Prometheus/Grafana monitoring, circuit breakers, health checks.

## ADR-002: PostgreSQL as Primary Database
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Single PostgreSQL instance with 4 schemas (auth, training, tracking, notification).
**Rationale**: Strong ACID guarantees, multi-tenant support, simplified deployment.
**Future**: Can evolve to database-per-service when scaling demands it.

## ADR-003: JWT Token-Based Authentication
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Stateless JWT authentication with API Gateway validation.
**Rationale**: Scalable, no server-side session storage, works across microservices.
**Flow**: Login → JWT issued → Gateway validates → injects X-User-Id/X-User-Roles headers → service reads headers.
**Consequences**: Token revocation requires cache (Redis planned).

## ADR-004: Docker & Kubernetes Deployment
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Docker for containerization, Docker Compose for local/dev, Kubernetes-ready for production.
**Rationale**: Consistent environments, easy deployment, orchestration-ready.
**Current**: Docker Compose (dev + prod profiles). **Future**: Kubernetes with auto-scaling.

## ADR-005: Prometheus & Grafana Monitoring
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Prometheus for metrics collection, Grafana for visualization and alerting.
**Rationale**: Industry standard, integrates with Spring Boot Actuator, free and open-source.
**Metrics**: HTTP request rate/duration, JVM memory/GC, DB connection pool, custom business metrics.

## ADR-006: Event-Driven Architecture
**Status**: Accepted (Future) | **Date**: 2026-03-21
**Decision**: Introduce RabbitMQ or Kafka for async inter-service communication.
**Rationale**: Decouple services, handle spikes, enable eventual consistency patterns.
**Use cases**: Notification triggers, audit events, analytics data pipeline.
**Current**: Synchronous REST. **Future**: Events for non-critical paths.

## ADR-007: API Gateway Pattern
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Single API Gateway entry point (port 8080) with routing, centralized auth, rate limiting, logging.
**Routing**: /auth/* → 8081, /training/* → 8082, /tracking/* → 8083, /notifications/* → 8084.
**Cross-cutting**: JWT validation, X-Trace-Id generation, CORS, rate limiting (1000 req/min per API key).
**HA**: Dual gateway instances behind load balancer (planned).
**Alerts**: Response time >500ms, error rate >5%, CPU >80%.

## ADR-008: PgBouncer Connection Pooling
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: PgBouncer as connection pooling proxy for PostgreSQL.
**Rationale**: 80% memory reduction. PostgreSQL uses ~10MB per connection; 4000 connections = 40GB without pooling.
**Config**: Transaction pool mode, max_client_conn=1000, default_pool_size=25, max_db_connections=100.
**Deployment**: Sidecar or StatefulSet in Kubernetes.

## ADR-009: S3 Cloud Storage for Backups
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: AWS S3 for automated PostgreSQL backups.
**Schedule**: Daily full backup at 00:00 UTC (30-day retention) + continuous WAL archiving (7-day retention).
**Security**: AES256 server-side encryption, versioning enabled, all public access blocked.
**Recovery**: Quick restore ~30min from latest full, Point-in-Time recovery ~1-2hr.
**Drills**: Monthly recovery drill to verify backup integrity.

## ADR-010: Disaster Recovery & High Availability
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Multi-layered HA with defined RTO/RPO targets.
**Requirements**: 99.9% availability (8.6 hrs/yr downtime), RTO <= 1 hour, RPO <= 1 day.
**Layers**: K8s replicas (3 per service), PostgreSQL streaming replication, S3 backups, multi-region (planned).
**Failover**: Detection ~30s, DNS propagation ~5min, DB restore ~30min, total RTO ~40min.
**Drills**: Monthly HA drill (kill pods, simulate DB failover, backup restoration).

## ADR-011: Security, Encryption & RBAC
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: 7-layer defense-in-depth security model.
**Layers**: HTTPS/TLS 1.2+ → Encryption at Rest (pgcrypto) → JWT Auth → RBAC (3 roles) → Data Masking → Audit Logging (AOP) → Secrets Management (K8s Secrets / Vault).
**RBAC**: ROLE_ADMIN, ROLE_PROFESSIONAL, ROLE_USER. Roles stored in JWT claims, validated via GymRoleInterceptor reading X-User-Roles header.
**Audit**: AOP-based aspect capturing user, action, resource, result, timing.
**Checklist**: 13 security items (HTTPS, encryption, JWT, RBAC, masking, audit, SQL injection, CSRF, XSS, security headers).

## ADR-012: Caching Strategy (Redis)
**Status**: Accepted | **Date**: 2026-03-21
**Decision**: Redis for query caching, session tokens, computed results, rate limiting.
**Performance**: Redis ~1ms vs PostgreSQL ~50ms = 50x improvement, 80% reduction in DB load.
**Patterns**: Cache-Aside (lazy loading) for reads, Write-Through for updates, TTL-based expiration.
**TTLs**: Real-time 1-5min, semi-fresh 15-60min, long-lived 1-7 days.
**Key strategy**: `user:{id}`, `session:{id}`, `workouts:{id}:{page}`, `stats:{id}:{period}`, `ratelimit:{id}:{endpoint}`.
**Targets**: Hit rate >85%, memory <80%, eviction alerts >100/s.
**HA**: Redis Cluster with 3 masters + replicas, automatic failover.

---

**Full ADR documents**: See `docs/adr/ADR-001-*.md` through `ADR-012-*.md`
