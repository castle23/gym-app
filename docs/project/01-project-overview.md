# Project Overview

## Vision

Microservices-based gym management platform with centralized authentication, multi-disciplinary training tracking, meal planning, and push notifications.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
│                  (Web, Mobile via Flutter)                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
        ┌────────────────────────────────────────────┐
        │         API Gateway (Spring Cloud)         │
        │  - TraceIdMdcFilter (order: -1)            │
        │  - JwtAuthFilter (order: 0)                │
        │  - Route mapping to 4 services             │
        └────────┬───────────┬───────────┬───────────┘
                 │           │           │
    ┌────────────▼─┐ ┌──────▼──────┐ ┌─▼──────────────┐ ┌──────────────────┐
    │ Auth Service │ │ Training    │ │ Tracking       │ │ Notification     │
    │ Port 8081    │ │ Service     │ │ Service        │ │ Service          │
    │              │ │ Port 8082   │ │ Port 8083      │ │ Port 8084        │
    │ - Register   │ │ - Exercises │ │ - Measurements │ │ - Notifications  │
    │ - Login      │ │ - Routines  │ │ - Plans        │ │ - Push Tokens    │
    │ - JWT Mgmt   │ │ - Sessions  │ │ - Diet Logs    │ │ - Preferences    │
    └──────────────┘ └─────────────┘ └────────────────┘ └──────────────────┘
         │                  │                │                    │
         └──────────────────┴────────────────┴────────────────────┘
                                        │
                          ┌─────────────▼──────────────┐
                          │   PostgreSQL (Single)      │
                          │  - auth_schema             │
                          │  - training_schema         │
                          │  - tracking_schema         │
                          │  - notification_schema     │
                          └────────────────────────────┘
```

## Key Design Decisions

**Authentication model** — Centralized in Auth Service. JWT tokens issued at login. API Gateway validates and injects `X-User-Id` / `X-User-Roles` headers. All downstream services trust gateway headers.

**Database strategy** — Single PostgreSQL instance with 4 separate schemas. Each service owns its schema via `hibernate.default_schema`. No cross-service DB queries; all communication via REST.

**Tracing** — `X-Trace-Id` generated at API Gateway, propagated through all requests. MDC logging per service.

**Professional/component relationship** — Professionals linked at component level (`TrainingComponent.professionalId`, `DietComponent.professionalId`), allowing multiple professionals per plan.

**Single plan constraint** — Each user has 1 active Plan with 0–1 TrainingComponent and 0–1 DietComponent. Recommendations are always tied to a component.

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.2.0 |
| Language | Java | 17+ |
| Database | PostgreSQL | 15 |
| Gateway | Spring Cloud Gateway | 4.0.0 |
| Security | Spring Security + JWT (jjwt) | 1.0.11 |
| ORM | Hibernate JPA | 3.1 |
| Testing | JUnit 5 + TestContainers | 5.9.0 |
| Build | Maven | 3.8.0+ |
| Containers | Docker & Docker Compose | Latest |
| Notifications | Firebase Cloud Messaging | Latest |
| Coverage | JaCoCo | 0.8.10 |

## Service Summary

| Service | Port | Context Path | Schema | Endpoints |
|---------|------|-------------|--------|-----------|
| API Gateway | 8080 | `/` | — | Routing only |
| Auth Service | 8081 | `/auth` | `auth_schema` | 6 |
| Training Service | 8082 | `/training` | `training_schema` | 25 |
| Tracking Service | 8083 | `/tracking` | `tracking_schema` | 39 |
| Notification Service | 8084 | `/notifications` | `notification_schema` | 10 |

## Test Coverage Target

Minimum 85% across all services:
- Line coverage: ≥ 85%
- Branch coverage: ≥ 80%
- Method coverage: ≥ 85%
