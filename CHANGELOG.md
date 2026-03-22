# Changelog

All notable changes to Gym Platform API are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [1.0.0] - 2026-03-21

### Added

#### Core Platform
- Microservices architecture with 4 independent services:
  - **Auth Service** - User authentication and authorization
  - **Training Service** - Exercise and workout management
  - **Tracking Service** - Diet, weight, and metrics tracking
  - **Notification Service** - Multi-channel notifications

#### Authentication & Authorization
- JWT (JSON Web Tokens) authentication with HS256 algorithm
- Refresh token mechanism for persistent sessions
- Session tracking and logout functionality
- Role-based access control (RBAC) with three roles: admin, trainer, user
- Granular permissions system (read, write, delete per resource)

#### User Management
- User registration with email verification
- User profile management (name, age, contact info)
- Multi-role user support (users can be trainers or admins)
- Session management with concurrent login tracking

#### Training Features
- Exercise database with 100+ pre-built exercises
- Exercise categories by discipline (chest, back, legs, etc.)
- Workout session tracking and history
- Custom workout routine creation and scheduling
- Pre-built workout routines (5x5 Strength, PPL Split, etc.)
- Exercise tracking within workouts (sets, reps, weight, duration)
- Workout completion statistics and history

#### Tracking Features
- Daily weight tracking with trend analysis
- Dietary intake logging (calories, macros)
- Goal setting and progress tracking
- Progress checkpoints with percentage completion
- Activity metrics aggregation (total workouts, minutes)
- Dashboard metrics for user insights

#### Notifications
- Real-time notifications to users
- Multi-channel delivery (email, push, SMS)
- Notification preferences per user
- Welcome and achievement notifications
- Reminder notifications for upcoming workouts
- Notification templates for consistency

#### API Features
- RESTful API with comprehensive endpoint coverage
- Request validation and error handling
- Pagination support for list endpoints
- Sorting and filtering capabilities
- Request/response logging for debugging
- Rate limiting on auth endpoints
- CORS support for web clients

#### Infrastructure & Deployment
- Docker containerization for all services
- Docker Compose configuration for local development
- Kubernetes deployment manifests
- Rolling deployment strategy for zero-downtime updates
- Health check endpoints for all services
- Readiness and liveness probes

#### Data & Persistence
- PostgreSQL database with proper schema design
- 19 entities across all services
- Strategic database indexing for performance
- Connection pooling with PgBouncer
- Backup and disaster recovery procedures
- Database migrations management

#### Monitoring & Observability
- Prometheus metrics for all services
- Grafana dashboards for monitoring
- Application-level metrics (requests, latency, errors)
- Database performance metrics
- Log aggregation support
- Alerting rules for critical metrics

#### Security
- Password hashing with bcrypt
- Encrypted sensitive data at rest
- HTTPS/TLS for data in transit
- SQL injection prevention through parameterized queries
- XSS prevention through input validation
- CSRF protection on state-changing operations
- Rate limiting to prevent abuse
- Regular security audits and dependency updates

#### Testing & Quality Assurance
- Unit test framework (JUnit for Java)
- Integration test suite with 40+ scenarios
- Postman collection with 101+ API requests
- Test data seeds for reproducible testing
- Testing guide covering unit, integration, and E2E tests
- Load testing capability with k6
- Performance benchmarking

#### Documentation
- Comprehensive API documentation
- Architecture decision records (12 ADRs)
- Data dictionary with all entities and relationships
- Contributing guide for developers
- Code standards and style guide
- Integration testing guide
- Deployment runbook with procedures
- Advanced testing guide for QA

### Fixed

- Initial release

[Unreleased]: https://github.com/gym-platform/api/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/gym-platform/api/releases/tag/v1.0.0
