# Architecture Decisions (ADRs)

## Decision 1: Microservices Architecture
**Status**: Implemented and in production
**Rationale**: Independent scaling, clear separation of concerns, team autonomy
**Consequences**: Inter-service communication overhead, distributed debugging complexity

## Decision 2: PostgreSQL with Shared Database
**Status**: Current implementation
**Rationale**: Strong ACID guarantees, multi-tenant support, simplified deployment
**Future**: Can evolve to database-per-service

## Decision 3: JWT Token-Based Authentication
**Status**: Implemented
**Rationale**: Stateless, scalable, no server-side session storage
**Consequences**: Token-based revocation requires cache

## Decision 4: Spring Boot 3.x with Java 17+
**Status**: Implemented
**Rationale**: Latest LTS versions, modern language features, long-term support
**Consequences**: Library ecosystem maturity, occasional dependency conflicts

## Decision 5: Docker Containerization
**Status**: Implemented
**Rationale**: Consistency across environments, easy deployment, orchestration-ready
**Consequences**: Docker knowledge required, learning curve for new developers

## Decision 6: Swagger/OpenAPI Documentation
**Status**: Implemented
**Rationale**: Automatic, always in sync, client SDK generation capability
**Consequences**: Additional annotations required in code

