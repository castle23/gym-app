# Gym Platform API - System Overview

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Components](#system-components)
3. [Microservices at a Glance](#microservices-at-a-glance)
4. [Technology Stack](#technology-stack)
5. [System Deployment](#system-deployment)
6. [Key Design Decisions](#key-design-decisions)

## Executive Summary

The Gym Platform API is an enterprise-grade, microservices-based platform designed to manage comprehensive fitness and training operations. The system is built on Spring Boot 3.x with Java 17+, uses PostgreSQL for data persistence, and provides 80+ RESTful API endpoints across four independent microservices.

**Key Metrics**:
- **Build**: 7 modules, ~335 MB total JAR size
- **Services**: 4 microservices + API Gateway foundation
- **Endpoints**: 80+ documented and verified endpoints
- **Database**: PostgreSQL with 4 logical schemas
- **Deployment**: Docker containerized, production-ready
- **Status**: 100% complete, all 7 phases delivered

## System Components

### Core Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Clients                             │
│              (Web, Mobile, 3rd-party Systems)                │
└────────────────────┬────────────────────────────────────────┘
                     │
┌─────────────────────▼────────────────────────────────────────┐
│                    API Gateway                               │
│            Port: 8080 — JWT validation, header injection     │
└────────┬─────────────┬─────────────┬─────────────┬───────────┘
         │             │             │             │
┌────────▼─┐     ┌─────▼──┐   ┌──────▼──┐   ┌─────▼──────┐
│   Auth    │     │Training│   │Tracking │   │Notification│
│ Service   │     │Service │   │Service  │   │  Service   │
│ :8081     │     │ :8082  │   │  :8083  │   │   :8084    │
└────────┬──┘     └─────┬──┘   └──────┬──┘   └─────┬──────┘
         │              │             │             │
         └──────────────┴─────────────┴─────────────┘
                        │
         ┌──────────────▼──────────────┐
         │    PostgreSQL Database      │
         │    gym_db (Port: 5432)      │
         │                             │
         │  • auth_schema              │
         │  • training_schema          │
         │  • tracking_schema          │
         │  • notification_schema      │
         └─────────────────────────────┘
```

### Communication Patterns

- **Client → Gateway**: REST/HTTP, JWT in Authorization header
- **Gateway → Services**: REST/HTTP, X-User-Id + X-User-Roles headers injected
- **Database Access**: JDBC/JPA via Hibernate ORM
- **No inter-service calls**: Services do not call each other directly

## Microservices at a Glance

### 1. Auth Service (Port 8081)

**Responsibility**: User authentication, authorization, and access control

**Capabilities**:
- User registration and email verification
- Login with BCrypt password verification
- JWT token generation (access 24h + refresh 7d)
- Role-based access control (RBAC)
- Token refresh

**Key Endpoints**:
- `POST /auth/register` - Register new user
- `POST /auth/login` - Authenticate user
- `POST /auth/verify` - Verify email
- `POST /auth/refresh` - Refresh JWT token
- `GET /auth/profile` - Get authenticated user profile

**Database Schema**: `auth_schema`
- users, verifications

### 2. Training Service (Port 8082)

**Responsibility**: Training program and exercise management

**Capabilities**:
- Exercise catalog (system + user-created)
- Routine templates (system + user-created)
- User routine assignments
- Exercise session logging

**Key Endpoints**:
- `GET /training/api/v1/exercises/system` - List system exercises (public)
- `GET /training/api/v1/exercises/discipline/{id}` - Exercises by discipline (public)
- `GET /training/api/v1/exercises/my-exercises` - User exercises
- `GET /training/api/v1/routine-templates/system` - System templates (public)
- `GET /training/api/v1/user-routines` - User routines
- `POST /training/api/v1/exercise-sessions` - Log exercise session

**Database Schema**: `training_schema`
- exercises, routine_templates, user_routines, exercise_sessions

### 3. Tracking Service (Port 8083)

**Responsibility**: Performance tracking, metrics, and analytics

**Capabilities**:
- Body measurement tracking
- Measurement type management
- Fitness objectives
- Diet and training plans
- Diet log entries
- Diet and training components
- Recommendations

**Key Endpoints**:
- `GET /tracking/api/v1/measurements` - Get measurements
- `POST /tracking/api/v1/measurements` - Record measurement
- `GET /tracking/api/v1/objectives` - Get objectives
- `GET /tracking/api/v1/plans` - Get plans
- `GET /tracking/api/v1/diet-logs` - Get diet logs
- `GET /tracking/api/v1/recommendations/{id}` - Get recommendation

**Database Schema**: `tracking_schema`
- measurements, objectives, plans, diet_logs, diet_components, training_components, recommendations

### 4. Notification Service (Port 8084)

**Responsibility**: User notifications and messaging

**Capabilities**:
- In-app notifications (create, read, delete)
- Unread notification tracking
- Firebase Cloud Messaging (FCM) push notifications
- Push token registration and management
- Notification preferences per user

**Key Endpoints**:
- `GET /notifications/api/v1/notifications` - List notifications
- `GET /notifications/api/v1/notifications/unread` - Unread notifications
- `GET /notifications/api/v1/notifications/unread/count` - Unread count
- `POST /notifications/api/v1/notifications` - Create notification
- `PUT /notifications/api/v1/notifications/{id}/read` - Mark as read
- `POST /notifications/api/v1/push-tokens` - Register push token

**Database Schema**: `notification_schema`
- notifications, push_tokens, notification_preferences

## Technology Stack

### Backend Framework
- **Java**: 17 LTS
- **Spring Boot**: 3.x (latest stable)
- **Build Tool**: Maven 3.8+
- **Framework Modules**:
  - spring-boot-starter-web (REST API)
  - spring-boot-starter-data-jpa (Database ORM)
  - spring-boot-starter-security (Authentication)
  - spring-boot-starter-validation (Input validation)

### Database
- **Primary**: PostgreSQL 14+
- **Driver**: PostgreSQL JDBC Driver
- **ORM**: Hibernate/JPA
- **Connection Pool**: HikariCP (built-in with Spring Boot)

### API Documentation
- **Swagger/OpenAPI**: springdoc-openapi-ui
- **Annotations**: @Schema, @Operation, @Parameter
- **UI**: Swagger UI at `/swagger-ui.html`
- **Documentation**: `/v3/api-docs`

### Testing
- **Unit Tests**: JUnit 5
- **Mocking**: Mockito
- **Integration Tests**: Spring Boot Test
- **API Testing**: Postman (collections provided)
- **Coverage**: ~80%+ of service layer

### Security
- **Authentication**: Spring Security + JWT
- **Token Format**: JWT (JSON Web Tokens)
- **Password Hashing**: BCrypt
- **Authorization**: RBAC with custom filters
- **CORS**: Configured per service

### Containerization
- **Docker**: Multi-stage builds
- **Docker Compose**: Development and production configurations
- **Image Size**: ~70-80 MB per service
- **Health Checks**: Configured in docker-compose.yml

## System Deployment

### Development Environment
```bash
docker-compose up -d
```
Runs all services + PostgreSQL with hot reload support.

### Production Deployment
```bash
docker-compose -f docker-compose.prod.yml up -d
```
Production-ready configuration with:
- Resource limits
- Restart policies
- Health checks
- Log aggregation
- Environment-specific configs

### Service Port Mapping

| Service | Port | Health Check | Swagger UI |
|---------|------|--------------|-----------|
| API Gateway | 8080 | :8080/actuator/health | N/A |
| Auth Service | 8081 | :8081/auth/actuator/health | :8081/auth/swagger-ui/index.html |
| Training Service | 8082 | :8082/training/actuator/health | :8082/training/swagger-ui/index.html |
| Tracking Service | 8083 | :8083/tracking/actuator/health | :8083/tracking/swagger-ui/index.html |
| Notification Service | 8084 | :8084/notifications/actuator/health | :8084/notifications/swagger-ui/index.html |
| PostgreSQL | 5432 | N/A | N/A |

## Key Design Decisions

### 1. Microservices Architecture
**Why**: Enables independent scaling, deployment, and team ownership
- Each service has its own responsibility
- Services communicate via REST (extensible to event-based)
- Independent databases (future: database per service)

### 2. PostgreSQL with Shared Database
**Why**: Strong consistency, ACID transactions, multi-tenant support
- Currently 4 schemas in single database (gym_db)
- Can be separated to individual databases if needed
- JSONB support for flexible schema design

### 3. JWT-Based Authentication
**Why**: Stateless, scalable, standards-based authentication
- No server-side session storage required
- Can work with CDNs and load balancers
- Industry standard with broad client support

### 4. RBAC (Role-Based Access Control)
**Why**: Flexible, maintainable permission management
- Roles: `ROLE_USER`, `ROLE_PROFESSIONAL`, `ROLE_ADMIN`
- Enforced via `@RequiresRole` at controller level
- JWT claims carry roles, validated by API Gateway; injected as `X-User-Roles` header

### 5. Spring Boot 3.x with Java 17+
**Why**: Latest stable LTS version with modern features
- Project Loom support (virtual threads - future)
- Record types for data classes
- New string formatting APIs
- Updated security libraries

### 6. Docker Containerization
**Why**: Consistent environment, easy deployment
- Development = Production parity
- Container orchestration ready
- Easy to scale horizontally

### 7. OpenAPI/Swagger Documentation
**Why**: Automatic, always-in-sync API documentation
- Developers can explore API via UI
- Client SDK generation capability
- Automated integration testing possible

## System Characteristics

### Scalability
- Stateless services scale horizontally
- Read replicas possible on PostgreSQL
- Load balancer ready (API Gateway)
- Connection pooling optimized

### Reliability
- Health checks on all services
- Graceful shutdown handlers
- Retry logic on transient failures
- Error handling and logging

### Security
- JWT authentication required for most endpoints
- RBAC on sensitive endpoints
- Input validation on all controllers
- SQL injection prevention via parameterized queries
- CORS configured for specific origins

### Maintainability
- Clear separation of concerns
- Standard Spring Boot structure
- Comprehensive API documentation
- Extensive test coverage
- Standard coding patterns

## Related Documentation

- [Microservices Architecture Details](02-microservices-architecture.md)
- [Database Schema Design](03-database-schema.md)
- [API Design Principles](04-api-design.md)
- [Security Architecture](05-security-architecture.md)
- [Technology Stack Details](../stack/)
- [Deployment Guide](../deployment/)
