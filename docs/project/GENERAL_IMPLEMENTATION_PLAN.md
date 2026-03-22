# Gym Platform Microservices - General Implementation Plan

> **Status:** Ready for execution
> **Target Tech Stack:** Spring Boot 3.2.0, PostgreSQL, Docker, JUnit 5, JWT (jjwt), Firebase
> **Test Coverage Target:** 85%+

## Project Vision

Build a complete **microservices-based gym management platform** with:
- Centralized authentication and JWT-based authorization via API Gateway
- Distributed tracing via Trace ID injection across all services
- Multi-disciplinary exercise tracking with professional guidance
- User measurement tracking and meal planning
- Push notifications via Firebase
- Flutter mobile frontend (future phase)

---

## Architecture Overview

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
        │  - Route mapping to 5 services             │
        └────────┬───────────┬───────────┬───────────┘
                 │           │           │
    ┌────────────▼─┐ ┌──────▼──────┐ ┌─▼──────────────┐
    │ Auth Service │ │ Training    │ │ Tracking       │
    │              │ │ Service     │ │ Service        │
    │ (Port 8081)  │ │ (Port 8082) │ │ (Port 8083)    │
    │              │ │             │ │                │
    │ - Register   │ │ - Exercises │ │ - Measurements │
    │ - Login      │ │ - Routines  │ │ - Plans        │
    │ - Verify     │ │ - Sessions  │ │ - Objectives   │
    │ - JWT Mgmt   │ │             │ │ - Diet Logs    │
    └──────────────┘ └─────────────┘ └────────────────┘
         │                  │                │
         └──────────────────┼────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │   PostgreSQL (Single)      │
              │  - auth_schema             │
              │  - training_schema         │
              │  - tracking_schema         │
              │  - notification_schema     │
              └────────────────────────────┘
```

### Key Design Decisions

1. **Authentication Model:**
   - Centralized in Auth Service
   - JWT tokens issued at login
   - API Gateway validates and injects headers (X-User-Id, X-User-Roles)
   - All downstream services trust gateway headers

2. **Database Strategy:**
   - Single PostgreSQL instance (for simplicity and cost efficiency)
   - 4 separate schemas (auth_schema, training_schema, tracking_schema, notification_schema)
   - Each service has its own Hibernate configuration with `spring.jpa.properties.hibernate.default_schema`
   - No cross-service database queries; all communication via REST

3. **Tracing & Observability:**
   - X-Trace-Id: Generated at API Gateway, propagated through all requests
   - X-Span-Id: Generated per service, identifies individual operation
   - MDC logging: Each service logs with trace context

4. **Professional Component Relationship:**
   - Professionals linked at **component level** (not plan level)
   - TrainingComponent.professionalId and DietComponent.professionalId
   - Allows multiple professionals per plan (one for training, one for diet)

5. **Single Plan Constraint:**
   - Each user can have 1 active Plan
   - A Plan can have: 0 or 1 TrainingComponent + 0 or 1 DietComponent
   - Recommendations tied to specific components, never standalone

---

## Phases Overview

### Phase 1: ✅ COMPLETE - Foundation
- [x] Git repository initialization
- [x] Parent Maven project with dependency management
- [x] Docker Compose setup with PostgreSQL + 5 services
- [x] Database schemas initialization
- [x] .gitignore, README, .env files

**Commits:** 656c14b

---

### Phase 2: ✅ COMPLETE - API Gateway
- [x] Spring Cloud Gateway setup
- [x] TraceIdMdcFilter (generates/propagates trace IDs)
- [x] JwtAuthFilter (validates tokens, injects user headers)
- [x] Route mapping to all 5 services
- [x] Configuration for local and Docker environments

**Commits:** 3467aac

---

### Phase 3: ✅ COMPLETE - Auth Service
- [x] Entities: User, Verification, ProfessionalRequest
- [x] Repositories: UserRepository, VerificationRepository, ProfessionalRequestRepository
- [x] Services: JwtService, EmailService, AuthService
- [x] Controller: AuthController with register/login/verify endpoints
- [x] Configuration: JwtConfig, SecurityConfig
- [x] Integration tests with TestContainers (85%+ coverage)

**Commits:** 9b1a72d

---

### Phase 4: ✅ COMPLETE - Training Service

**Sub-phases:**
- Phase 4a: Repositories, DTOs, Services (70% of work)
- Phase 4b: Controllers & Integration Tests (30% of work)

**Target:** Complete repositories, services, and controllers following Auth Service pattern + 85%+ test coverage

**Files to create:**
- `training-service/src/main/java/com/gym/training/repository/`
- `training-service/src/main/java/com/gym/training/dto/`
- `training-service/src/main/java/com/gym/training/service/`
- `training-service/src/main/java/com/gym/training/controller/`
- `training-service/src/test/java/com/gym/training/` (tests)

---

### Phase 5: ✅ COMPLETE - Tracking Service

**Sub-phases:**
- Phase 5a: Repositories, DTOs, Services (70% of work)
- Phase 5b: Controllers & Integration Tests (30% of work)

**Target:** Complete repositories, services, and controllers following Auth Service pattern + 85%+ test coverage

**Files to create:**
- `tracking-service/src/main/java/com/gym/tracking/repository/`
- `tracking-service/src/main/java/com/gym/tracking/dto/`
- `tracking-service/src/main/java/com/gym/tracking/service/`
- `tracking-service/src/main/java/com/gym/tracking/controller/`
- `tracking-service/src/test/java/com/gym/tracking/` (tests)

---

### Phase 6: ✅ COMPLETE - Notification Service

---

## Development Workflow

### For Each Service Implementation:

1. **Create/Fix Entities** (if needed)
   - Verify JPA annotations
   - Add @PrePersist/@PreUpdate for timestamps
   - Define relationships properly

2. **Create Repositories**
   - Extend Spring Data JPA repositories
   - Add custom query methods as needed

3. **Create DTOs**
   - Request DTOs for incoming data
   - Response DTOs for outgoing data
   - Use builders for complex objects

4. **Create Services** (Business Logic Layer)
   - Implement core business logic
   - Handle transactions (@Transactional)
   - Use repositories for data access

5. **Create Controllers** (REST API Layer)
   - Map HTTP requests to service methods
   - Handle validation and error responses
   - Return appropriate HTTP status codes

6. **Write Tests**
   - Unit tests for services (mocking repositories)
   - Integration tests with TestContainers (testing with real DB)
   - Controller tests (MockMvc)
   - Target: 85%+ coverage per service

7. **Commit**
   - One commit per significant feature
   - Clear commit messages

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.2.0 |
| Language | Java | 17+ |
| Database | PostgreSQL | 15 |
| Web | Spring Cloud Gateway | 4.0.0 |
| Security | Spring Security + JWT (jjwt) | 1.0.11 |
| ORM | Hibernate JPA | 3.1 |
| Testing | JUnit 5 + TestContainers | 5.9.0 |
| Build | Maven | 3.8.0+ |
| Containers | Docker & Docker Compose | Latest |
| Email | Spring Mail (SMTP) | Included |
| Notifications | Firebase Cloud Messaging | Latest |
| Code Quality | JaCoCo (Coverage reports) | 0.8.10 |

---

## Test Coverage Requirements

**Minimum 85% coverage** across:
- **Line coverage:** 85%+
- **Branch coverage:** 80%+
- **Method coverage:** 85%+

### Coverage Calculation:
```
Services: (total lines tested / total lines) >= 0.85
Controllers: (total methods tested / total methods) >= 0.85
Repositories: (custom query tests / custom queries) >= 1.0
```

### Tools:
- **JaCoCo** for coverage reports
- **Maven Surefire** for test execution
- **TestContainers** for integration tests with real database

---

## Git Workflow

### Branch Strategy:
- `main` - Production-ready code
- Feature branches per phase/service
- Commit frequently (after each major feature)
- Clear commit messages following pattern:
  - `feat: add [feature]`
  - `fix: resolve [issue]`
  - `test: add [test type]`
  - `chore: update [dependency]`

### Before Merging to Main:
- All tests pass (100%)
- Coverage >= 85%
- Code review (if applicable)
- Docker build succeeds

---

## Environment Configuration

### Local Development:
```bash
# Run PostgreSQL
docker-compose up postgres

# Run individual services
mvn spring-boot:run -pl training-service

# Run all services
docker-compose up
```

### Environment Variables (.env file):
```properties
DB_HOST=postgres
DB_PORT=5432
DB_USER=gym_admin
DB_PASSWORD=gym_password
JWT_SECRET=your-secret-key-here-min-256-bits
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
FIREBASE_CONFIG_PATH=/path/to/firebase-config.json
```

---

## Success Criteria

- [ ] All 5 services fully implemented with repositories, services, controllers
- [ ] 85%+ test coverage across all services
- [ ] All Docker images build successfully
- [ ] `docker-compose up` launches entire platform
- [ ] API Gateway routes all requests correctly
- [ ] JWT authentication works end-to-end
- [ ] Trace IDs propagate through all services
- [ ] Database schemas initialized and working
- [ ] Email verification flow works
- [ ] Firebase notifications send successfully
- [ ] All commits are in git history
- [ ] README documents how to run everything

---

## Next Steps for Execution

1. **Start Phase 4a** - Training Service Repositories & DTOs
   - Detailed plan: `PHASE_4A_TRAINING_SERVICE.md`

2. **Continue Phase 4b** - Training Service Controllers & Tests
   - Detailed plan: `PHASE_4B_TRAINING_CONTROLLERS.md`

3. **Start Phase 5a** - Tracking Service Repositories & DTOs
   - Detailed plan: `PHASE_5A_TRACKING_SERVICE.md`

4. **Continue Phase 5b** - Tracking Service Controllers & Tests
   - Detailed plan: `PHASE_5B_TRACKING_CONTROLLERS.md`

5. **Start Phase 6** - Notification Service Complete
   - Detailed plan: `PHASE_6_NOTIFICATION_SERVICE.md`

6. **Integration & Deployment** - Final testing and Docker validation
   - Detailed plan: `PHASE_7_INTEGRATION.md`

---

## Handoff Notes

- Auth Service is **fully functional** and can serve as the template for all remaining services
- All infrastructure (Gateway, DB, Docker) is in place and tested
- Test coverage should be tracked with JaCoCo reports after each phase
- Plan documents include exact code for each task
- Use @subagent-driven-development for parallel execution of independent tasks
