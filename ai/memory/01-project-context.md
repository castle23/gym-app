# Project Context

## Project Overview

**Project Name**: Gym Platform API
**Status**: 100% Complete - Production Ready
**Type**: Enterprise Microservices Platform
**Domain**: Fitness & Training Management
**Date**: March 2026

### Vision

Comprehensive, enterprise-grade API for managing fitness operations including user authentication, training program management, performance tracking, and user notifications.

### Mission

Deliver a scalable, maintainable, and secure microservices architecture that enables fitness facilities to manage members, training programs, and track progress effectively.

## Architecture

### Services

| Service | Port | Endpoints | Context Path | Swagger UI |
|---------|------|-----------|--------------|------------|
| API Gateway | 8080 | Routing | / | N/A |
| Auth Service | 8081 | 6 | /auth | /auth/swagger-ui.html |
| Training Service | 8082 | 26 | /training | /training/swagger-ui.html |
| Tracking Service | 8083 | 39 | /tracking | /tracking/swagger-ui.html |
| Notification Service | 8084 | 10 | /notifications | /notifications/swagger-ui.html |

**Total Endpoints**: 81+
**API Version**: v1 (path prefix: `/api/v1/` for Training, Tracking, Notification)

### Technology Stack

- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build**: Maven
- **Database**: PostgreSQL 15 (single instance, 4 schemas)
- **ORM**: Hibernate/JPA
- **Security**: Spring Security + JWT (1hr expiration)
- **API Docs**: Swagger/OpenAPI (springdoc)
- **Testing**: JUnit 5, Mockito, H2 (test profile)
- **Containerization**: Docker, Docker Compose (dev + prod)

### Database Schemas

| Schema | Tables | Domain |
|--------|--------|--------|
| auth_schema | users, user_roles, verifications | Authentication, authorization |
| training_schema | disciplines, exercises, routine_templates, routine_template_exercises, user_routines, exercise_sessions | Workout programs |
| tracking_schema | measurement_types, measurement_values, objectives, plans, diet_components, training_components, diet_logs, recommendations | Progress tracking |
| notification_schema | notifications, push_tokens, notification_preferences | User notifications |

**Total entities**: 19+ tables across 4 schemas.
**Cross-service links**: By `userId` (Long) only -- no cross-schema foreign keys.

### RBAC Roles

| Role | Description |
|------|-------------|
| ROLE_ADMIN | Full system access |
| ROLE_PROFESSIONAL | Can create exercises, templates, recommendations |
| ROLE_USER | Standard member access |

### Security Flow

```
Client → API Gateway (8080) → JWT Validation → Inject X-User-Id + X-User-Roles headers → Route to Service
```

## Endpoints Quick Reference

### Auth Service (`/auth`)
- POST `/auth/register` (public) - Register user
- POST `/auth/login` (public) - Login, get JWT
- POST `/auth/verify` (public) - Verify email
- POST `/auth/refresh` (auth) - Refresh token
- POST `/auth/professional/request` (auth) - Request professional status
- GET `/auth/admin/professional-requests` (admin) - Get pending professional requests
- POST `/auth/admin/professional-requests/{id}/approve` (admin) - Approve professional request
- POST `/auth/admin/professional-requests/{id}/reject` (admin) - Reject professional request
- GET `/auth/profile` (auth) - User profile

### Training Service (`/training/api/v1/`)
- **Exercises**: 8 endpoints (CRUD + system/discipline/my-exercises + search)
  - `GET /exercises/search?name=&type=` (public) - Search by name (partial, case-insensitive) and/or type
- **Routine Templates**: 6 endpoints (CRUD + system/my-templates)
- **User Routines**: 7 endpoints (CRUD + active/assign/deactivate)
- **Exercise Sessions**: 6 endpoints (CRUD + by-routine/by-date)

### Tracking Service (`/tracking/api/v1/`)
- **Measurements**: 8 endpoints (CRUD + by-type + types management)
- **Objectives**: 5 endpoints (CRUD)
- **Plans**: 5 endpoints (CRUD)
- **Diet Logs**: 6 endpoints (CRUD + by-date)
- **Diet Components**: 5 endpoints (CRUD + by-plan)
- **Training Components**: 5 endpoints (CRUD + by-plan)
- **Recommendations**: 6 endpoints (CRUD + by-component)

### Notification Service (`/notifications/api/v1/`)
- **Notifications**: 6 endpoints (list, unread, count, create, mark-read, delete)
- **Push Tokens**: 4 endpoints (register, list, active, remove)

## Project Phases (All Complete)

| Phase | Focus | Status |
|-------|-------|--------|
| Phase 1 | API Endpoints & @Schema Annotations | Complete |
| Phase 2 | Testing Infrastructure | Complete |
| Phase 3 | Production Documentation (31,000+ words) | Complete |
| Phase 4 | Training Service Implementation | Complete |
| Phase 5 | Tracking Service Implementation | Complete |
| Phase 6 | Notification Service & Tests | Complete |
| Phase 7 | Integration & Deployment | Complete |

## Key Achievements

- 80+ API endpoints with full Swagger documentation
- ~80%+ test coverage across all services
- 31,000+ words of documentation across 104 files
- All 4 microservices containerized and running
- JWT + RBAC security fully implemented
- Health checks, graceful shutdown, connection pooling configured

## Key Patterns

- **MVC**: Controller → Service → Repository
- **Repository Pattern**: JpaRepository data access
- **Dependency Injection**: Spring @Autowired / constructor injection
- **Exception Handling**: Custom exceptions (com.gym.common.exception) → HTTP status codes
- **Validation**: @Valid annotations on request DTOs
- **Stateless**: No server-side sessions, JWT tokens

## Known Limitations & Planned Enhancements

| Current | Future |
|---------|--------|
| Single shared PostgreSQL | Database-per-service |
| Synchronous REST communication | Event-driven (RabbitMQ/Kafka) |
| No caching layer | Redis (ADR-012 approved) |
| Docker Compose | Kubernetes |
| Manual scaling | Auto-scaling with K8s |

## Quick Start

```bash
# Build
mvn clean install

# Run
docker-compose up -d

# Access Swagger UI
http://localhost:8081/auth/swagger-ui.html

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"Test123!"}'

# Use token
curl http://localhost:8080/training/api/v1/exercises/system \
  -H "Authorization: Bearer {token}"
```

## Documentation Map

| Audience | Location |
|----------|----------|
| Developers | `docs/development/`, `docs/api/`, `docs/stack/` |
| Operations | `docs/deployment/`, `docs/operations/` |
| DBA | `docs/database/`, `dba/` |
| Security | `docs/security/` |
| Architecture | `docs/arquitectura/`, `docs/adr/` |
| AI/Automation | `ai/` |

---

**Last Updated**: 2026-03-22
**Project Status**: Production Ready
**Documentation Status**: Complete
