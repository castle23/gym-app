# ADR-001: Microservices Architecture

## Status
Accepted

## Date
2026-03-21

## Context

The Gym Platform was growing rapidly. As the team and feature set expanded, a monolithic architecture created friction:

1. **Scalability Issues**: Different services have vastly different load patterns. Auth spikes at login, training during peak workout hours, notifications are occasional. A monolith forces all services to scale together.

2. **Team Independence**: Multiple teams needed to work on different services independently without constant coordination on database schema changes.

3. **Deployment Friction**: A small bug in notifications shouldn't require rebuilding and redeploying the entire platform.

4. **Technology Lock-in**: Different services have different optimal technology choices, but a monolith forced everyone to use the same stack.

5. **Data Management**: Keeping all data in one database schema led to tight coupling between services.

## Decision

We decided to adopt a **microservices architecture** with four independent services:

1. **Auth Service** - User authentication, registration, token management  
2. **Training Service** - Exercises, routines, workout sessions, metrics
3. **Tracking Service** - Diet logs, weight tracking, progress metrics
4. **Notification Service** - User notifications, push tokens, alerts

Each service:
- Runs independently
- Has its own database  
- Communicates via REST API + event queues
- Can be deployed separately
- Can use different technologies if needed

## Rationale

### 1. Independent Scaling
Each service scales based on actual demand. During peak workout hours, only Training Service needs to scale. This saves resources and improves efficiency.

### 2. Deployment Agility
A new feature in Training Service can be deployed without touching Auth, Tracking, or Notifications. This enables:
- Faster time-to-market
- Easier rollbacks (just that service)
- Better availability (one service down ≠ full outage)

### 3. Team Autonomy
Teams can own entire services end-to-end:
- No coordination needed for database schema changes
- Faster decision making
- Clear ownership and accountability

### 4. Technology Flexibility
Each team can choose the best tool for their job:
- Auth might benefit from a lightweight Node.js framework
- Training could use Java for performance
- Notifications could use Go for concurrency
- (Currently all Java, but architecture allows flexibility)

### 5. Resilience
Failures are isolated. If Auth service goes down, users can't login (expected). If Training goes down, other services continue working.

## Consequences

### Positive
- ✅ Independent scaling improves resource efficiency
- ✅ Faster deployments and time-to-market
- ✅ Team autonomy and ownership
- ✅ Better failure isolation
- ✅ Technology flexibility
- ✅ Easier to onboard new services

### Negative
- ❌ Increased operational complexity (more services to manage)
- ❌ Distributed system debugging is harder
- ❌ Network latency between services
- ❌ Data consistency challenges (eventual consistency)
- ❌ Requires good monitoring & observability (ADR-005)
- ❌ More databases to manage

## Alternatives Considered

### 1. Monolithic Architecture (Status Quo)
- **Pros**: Simple deployment, easy debugging, ACID guarantees within single database
- **Cons**: Can't scale services independently, deployment friction, team coupling, limited technology options
- **Why not**: Hits scalability and team agility limits as project grows

### 2. Modular Monolith
- **Pros**: Single database, simpler operations, some independence
- **Cons**: Still monolithic deployment, limited scaling, eventual consistency issues
- **Why not**: Doesn't solve deployment friction or team independence problems

### 3. Monolith + Microservices Hybrid
- **Pros**: Gradual migration, simpler than full microservices
- **Cons**: Hybrid complexity, harder to maintain, half-measures
- **Why not**: We committed to full microservices pattern

## Related ADRs

- **Depends on**: ADR-003 (JWT for inter-service authentication)
- **Depends on**: ADR-006 (Event-driven architecture for async communication)
- **Related to**: ADR-005 (Monitoring to manage distributed systems)
- **Related to**: ADR-002 (Each service has own database)

## Mitigation Strategies

To manage the increased complexity:

1. **Standardized Communication**: All services use REST API + event queues (ADR-006)
2. **Strong Monitoring**: Prometheus + Grafana + centralized logging (ADR-005)
3. **Circuit Breakers**: Timeout slow services, fail gracefully
4. **API Versioning**: Allow services to evolve independently
5. **Clear Documentation**: Document all APIs and contracts between services
6. **Health Checks**: Each service exposes /health endpoint
7. **Distributed Tracing**: Track requests across services for debugging

