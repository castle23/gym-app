# Project Context

## Project Overview

**Project Name**: Gym Platform API
**Status**: 100% Complete - Production Ready
**Type**: Enterprise Microservices Platform
**Domain**: Fitness & Training Management

### Vision

The Gym Platform API provides a comprehensive, enterprise-grade API for managing fitness operations including user authentication, training program management, performance tracking, and user notifications.

### Mission

Deliver a scalable, maintainable, and secure microservices architecture that enables fitness facilities to manage members, training programs, and track progress effectively.

## Key Facts

### Build Status
- **Languages**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Modules**: 7 (auth, training, tracking, notification, common, gateway-foundation, parent)
- **Build Size**: ~335 MB total JAR
- **Build Time**: ~2-3 minutes

### Deployment Status
- **Container Runtime**: Docker
- **Orchestration**: Docker Compose
- **Services**: 4 microservices
- **Database**: PostgreSQL
- **Running Ports**: 8081-8084 (services), 5432 (database)
- **Status**: ✅ All services healthy and running

### API Status
- **Total Endpoints**: 80+
- **Documentation**: Complete with Swagger/OpenAPI
- **Authentication**: JWT-based
- **Authorization**: RBAC (4 roles: ADMIN, MANAGER, USER, TRAINER)
- **API Version**: v1

### Project Phases Completed

| Phase | Focus | Status |
|-------|-------|--------|
| Phase 1 | API Endpoints & Schema Annotations | ✅ Complete |
| Phase 2 | Testing Infrastructure | ✅ Complete |
| Phase 3 | Production Documentation | ✅ Complete |
| Phase 4 | Training Service Implementation | ✅ Complete |
| Phase 5 | Tracking Service Implementation | ✅ Complete |
| Phase 6 | Notification Service & Tests | ✅ Complete |
| Phase 7 | Integration & Deployment | ✅ Complete |

## Architecture Overview

### Services

1. **Auth Service** (Port 8081)
   - User authentication and authorization
   - JWT token management
   - Role-based access control

2. **Training Service** (Port 8082)
   - Training program management
   - Exercise library
   - Workout creation and scheduling

3. **Tracking Service** (Port 8083)
   - Performance metrics tracking
   - Progress analytics
   - Achievement tracking

4. **Notification Service** (Port 8084)
   - Multi-channel notifications
   - Template management
   - Event-driven notifications

### Database

- **Type**: PostgreSQL
- **Port**: 5432
- **Schemas**: 4 (auth_schema, training_schema, tracking_schema, notification_schema)
- **Tables**: ~20+ across all schemas

### Technology Stack

- **Backend**: Java 17+ with Spring Boot 3.x
- **Database**: PostgreSQL 14+
- **ORM**: Hibernate/JPA
- **Security**: Spring Security + JWT
- **API Docs**: Swagger/OpenAPI (springdoc)
- **Testing**: JUnit 5, Mockito
- **Containerization**: Docker, Docker Compose

## Key Achievements

### Code Quality
- ✅ 80+ API endpoints
- ✅ All endpoints documented with @Schema annotations
- ✅ ~80%+ test coverage
- ✅ Consistent code structure
- ✅ SOLID principles followed

### Documentation
- ✅ 31,000+ words of documentation
- ✅ Comprehensive API documentation
- ✅ Architectural documentation
- ✅ Operations runbooks
- ✅ Troubleshooting guides

### Security
- ✅ JWT authentication
- ✅ RBAC with 4 roles
- ✅ Input validation
- ✅ SQL injection prevention
- ✅ Password hashing (BCrypt)

### Testing
- ✅ Unit tests for all services
- ✅ Integration tests
- ✅ API tests via Postman
- ✅ RBAC verification
- ✅ Swagger UI verification

### Production Readiness
- ✅ Docker containerization
- ✅ Health checks implemented
- ✅ Graceful shutdown handling
- ✅ Error handling and logging
- ✅ Connection pooling optimized

## Current State

### What Works
- All 4 microservices running
- All 80+ endpoints accessible
- Authentication and authorization functional
- Database initialized with test data
- Swagger UI documentation available
- All tests passing
- Health checks operational

### What's Configured
- Docker Compose setup (dev + prod)
- PostgreSQL with 4 schemas
- JWT authentication
- RBAC system
- API versioning (/api/v1)
- OpenAPI/Swagger documentation
- Error handling
- Input validation

### What's Deployed
- Production-ready Docker images
- Docker Compose orchestration files
- Health check endpoints
- Monitoring-ready structure
- Logging infrastructure

## Project Structure

```
gym-platform-api/
├── auth-service/              # Authentication service
├── training-service/          # Training management
├── tracking-service/          # Performance tracking
├── notification-service/      # Notifications
├── common/                     # Shared utilities
├── docs/                       # Documentation hub
├── ai/                         # AI context
├── dba/                        # DBA resources
├── tests/                      # Test resources
├── scripts/                    # Operational scripts
├── docker-compose.yml          # Dev environment
├── docker-compose.prod.yml     # Prod environment
├── pom.xml                     # Maven parent POM
└── README.md                   # Project README
```

## Team Knowledge

### Key Concepts
- **Microservices Architecture**: Each service independent, RESTful communication
- **JWT Authentication**: Stateless auth with token-based access
- **RBAC**: Four-tier role system (ADMIN, MANAGER, USER, TRAINER)
- **Spring Boot**: Standard enterprise Java framework
- **PostgreSQL**: Multi-schema database design

### Common Patterns
- **Repository Pattern**: Data access abstraction
- **Dependency Injection**: Spring's @Autowired
- **MVC**: Controller → Service → Repository
- **Exception Handling**: Custom exceptions with proper HTTP status
- **Validation**: @Valid annotations for input validation

### Design Principles
- Single Responsibility: Each service has clear purpose
- Loose Coupling: Services independent, communicate via REST
- High Cohesion: Related functionality grouped together
- Stateless Services: No server-side session storage
- Security First: JWT tokens on all endpoints

## How Things Work Together

### User Registration Flow
```
1. User → POST /auth/register
2. Auth Service validates input
3. Creates user in auth_schema.users
4. Returns user confirmation
5. User can now login
```

### Training Program Workflow
```
1. Trainer → POST /training/programs (requires TRAINER role)
2. Training Service stores in training_schema.programs
3. Trainer → POST /training/exercises
4. Trainer → POST /training/workouts
5. Users → GET /training/programs (uses stored program)
```

### Progress Tracking Flow
```
1. User completes workout
2. User → POST /tracking/workouts/log
3. Tracking Service records in tracking_schema.workout_logs
4. Service calculates metrics (1RM, volume, etc)
5. User → GET /tracking/progress (sees analytics)
```

## Known Limitations & Future Enhancements

### Current Limitations
- Single database (can scale to database-per-service)
- Synchronous inter-service communication (extensible to events)
- No caching layer (Redis-ready)
- Manual scaling (Kubernetes-ready)
- Single environment (can add more as needed)

### Planned Enhancements
- Event-driven architecture (RabbitMQ/Kafka)
- Distributed caching (Redis)
- Kubernetes deployment
- Advanced analytics
- Mobile app integration
- Third-party OAuth integration

## Documentation Organization

### For Developers
- `docs/development/` - Setup and coding
- `docs/api/` - API reference
- `docs/stack/` - Technology stack
- `docs/arquitectura/` - System design

### For Operations
- `docs/deployment/` - Deployment procedures
- `docs/operations/` - Runbooks and monitoring
- `scripts/` - Operational scripts

### For Database
- `dba/` - DBA resources
- `docs/database/` - Database documentation
- `dba/initialization/` - Database setup

### For AI/Automation
- `ai/memory/` - Project knowledge
- `ai/rules/` - Standards
- `ai/prompts/` - Reusable prompts
- `ai/agents/` - Agent definitions

## Quick Reference

### Getting Started (Developer)
1. Clone: `git clone ...`
2. Build: `mvn clean install`
3. Run: `docker-compose up -d`
4. Access: `http://localhost:8081/swagger-ui.html`

### Making API Calls
1. Login: POST `/auth/login`
2. Get token from response
3. Use token: `Authorization: Bearer {token}`

### Viewing Logs
- All services: `docker-compose logs -f`
- Single service: `docker-compose logs -f auth-service`

### Database Access
- Connect: `psql -U postgres -d gym_db`
- Schemas: auth, training, tracking, notification

## Contact & Support

### Key Contacts
- Team Lead: [See team structure]
- Architecture: Review ADRs in ai/memory/
- Database: See dba/
- DevOps: See docs/operations/

### Documentation
- Start: docs/README.md
- Architecture: docs/arquitectura/01-overview.md
- API: docs/api/01-api-overview.md
- Development: docs/development/01-getting-started.md

## Maintenance Notes

- Update dependencies quarterly
- Review logs weekly
- Backup database daily
- Test disaster recovery monthly
- Review security quarterly
- Update documentation as needed

---

**Last Updated**: 2024-03-21
**Project Status**: Production Ready ✅
**Documentation Status**: Complete ✅
