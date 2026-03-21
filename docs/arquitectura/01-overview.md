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
│              API Gateway (Optional Layer)                    │
│            Port: 8080 (for future implementation)            │
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

- **Service-to-Service**: RESTful HTTP/HTTPS calls
- **Database Access**: JDBC/JPA via Hibernate ORM
- **Authentication**: JWT tokens via HTTP headers
- **Message Queue**: Internal event-based (future: RabbitMQ/Kafka)

## Microservices at a Glance

### 1. Auth Service (Port 8081)

**Responsibility**: User authentication, authorization, and access control

**Capabilities**:
- User registration and login
- JWT token generation and validation
- Role-based access control (RBAC)
- Permission management
- Session management
- OAuth 2.0 support (extensible)

**Key Endpoints**:
- `POST /api/v1/auth/login` - Authenticate user
- `POST /api/v1/auth/register` - Create new user
- `POST /api/v1/auth/refresh` - Refresh JWT token
- `GET /api/v1/auth/verify` - Verify token validity
- `GET /api/v1/auth/users` - List users (admin only)

**Database Schema**: `auth_schema`
- users, roles, permissions, user_roles, role_permissions

### 2. Training Service (Port 8082)

**Responsibility**: Training program and exercise management

**Capabilities**:
- Create and manage training programs
- Define exercises with form descriptions
- Build workout templates
- Schedule training sessions
- Track exercise progressions
- Generate training recommendations

**Key Endpoints**:
- `GET /api/v1/training/programs` - List training programs
- `POST /api/v1/training/programs` - Create program
- `GET /api/v1/training/exercises` - List exercises
- `GET /api/v1/training/workouts` - List workouts
- `POST /api/v1/training/workouts` - Create workout

**Database Schema**: `training_schema`
- programs, exercises, workouts, sets, exercise_progressions, training_plans

### 3. Tracking Service (Port 8083)

**Responsibility**: Performance tracking, metrics, and analytics

**Capabilities**:
- Record workout completions
- Track performance metrics
- Analyze progress over time
- Generate performance reports
- Identify trends and patterns
- Provide analytics and insights

**Key Endpoints**:
- `POST /api/v1/tracking/workouts/log` - Log completed workout
- `GET /api/v1/tracking/metrics` - Get performance metrics
- `GET /api/v1/tracking/progress` - View progress analytics
- `GET /api/v1/tracking/reports` - Generate reports
- `GET /api/v1/tracking/charts` - Get chart data

**Database Schema**: `tracking_schema`
- workout_logs, performance_metrics, progress_analytics, reports, achievements

### 4. Notification Service (Port 8084)

**Responsibility**: User notifications and messaging

**Capabilities**:
- Send notifications via multiple channels (email, SMS, push)
- Manage notification preferences
- Queue and retry failed notifications
- Track notification history
- Template-based notifications
- Event-driven notifications

**Key Endpoints**:
- `POST /api/v1/notification/send` - Send notification
- `GET /api/v1/notification/preferences` - Get user preferences
- `PUT /api/v1/notification/preferences` - Update preferences
- `GET /api/v1/notification/history` - View notification history
- `GET /api/v1/notification/templates` - List templates

**Database Schema**: `notification_schema`
- notifications, notification_queue, notification_history, templates, user_preferences

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
| Auth Service | 8081 | :8081/health | :8081/swagger-ui.html |
| Training Service | 8082 | :8082/health | :8082/swagger-ui.html |
| Tracking Service | 8083 | :8083/health | :8083/swagger-ui.html |
| Notification Service | 8084 | :8084/health | :8084/swagger-ui.html |
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
- Roles: ADMIN, MANAGER, USER, TRAINER
- Permissions: Granular control at endpoint level
- Easily extensible for new use cases

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
