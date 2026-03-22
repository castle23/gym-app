# Lessons Learned

## What Went Well

### Architecture & Design
1. **Microservices from Day One**: Clean separation of concerns made each service independently developable and testable.
2. **Shared Common Module**: `com.gym.common` for exceptions, security config, and utilities prevented code duplication across services.
3. **Consistent MVC Pattern**: Controller → Service → Repository pattern across all services made the codebase predictable and easy to navigate.

### Development Process
4. **Comprehensive Testing Early**: Investing in unit + controller + integration tests from Phase 2 caught issues before they reached production.
5. **Documentation Priority**: Writing docs alongside code (Phase 3) resulted in 31,000+ words of operational guides -- invaluable for onboarding and ops.
6. **Docker from Day One**: Containerization made setup trivial (`docker-compose up -d`) and eliminated "works on my machine" issues.
7. **Swagger Integration**: Auto-generated API docs via springdoc reduced manual documentation burden and stayed in sync with code.

### Quality Metrics Achieved
- 80+ endpoints fully documented with @Schema annotations
- ~80%+ test coverage across all services (target: 85%)
- All 7 modules compiling and running
- 104 documentation files across 12 categories
- Zero hardcoded secrets in codebase

## Challenges and Solutions

### Challenge 1: JWT Token Configuration
- **Problem**: Token refresh complexity and ensuring JWT_SECRET consistency across all services.
- **Solution**: Centralized JWT_SECRET in `.env`, implemented refresh endpoint with proper validation.
- **Lesson**: Always use environment variables for shared secrets; never configure per-service.

### Challenge 2: Cross-Service Communication
- **Problem**: Service dependencies and cascading error handling.
- **Solution**: All communication goes through API Gateway. Timeout and retry logic with circuit breaker readiness.
- **Lesson**: Services should never call each other directly. Gateway handles routing and cross-cutting concerns.

### Challenge 3: Database Schema Management
- **Problem**: Multiple schemas in a single PostgreSQL instance required careful initialization ordering.
- **Solution**: Initialization scripts (`init-schemas.sql`) with proper schema creation order and Hibernate auto-DDL.
- **Lesson**: Always script schema creation; don't rely solely on Hibernate for production schemas.

### Challenge 4: Test Configuration Complexity
- **Problem**: Controller tests required specific imports (`GymTestSecurityAutoConfiguration`, `GymExceptionHandlerAutoConfiguration`) that weren't obvious.
- **Solution**: Documented the mandatory imports in testing standards. Created test base classes.
- **Lesson**: Non-obvious test requirements MUST be documented prominently. New developers will waste hours without this.

### Challenge 5: MockMvc Context Path Gotcha
- **Problem**: MockMvc does not include the service context-path, causing 404 errors in tests.
- **Solution**: Documented that test URLs should be `/profile` not `/auth/profile`.
- **Lesson**: Framework-specific gotchas should be in a prominent "CRITICAL" section of testing docs.

### Challenge 6: Exception-to-HTTP Mapping
- **Problem**: `UnauthorizedException` was returning 401 instead of the intended 403 (authorization, not authentication).
- **Solution**: Corrected the exception handler mapping. Documented the full exception → status code table.
- **Lesson**: Name exceptions carefully. "Unauthorized" is ambiguous -- consider "ForbiddenException" for 403.

## Phase-Specific Lessons

### Phase 1 (API Annotations)
- @Schema annotations on DTOs pay off immediately in Swagger UI quality.
- Annotating 80 endpoints is tedious but essential -- do it as you build, not retroactively.

### Phase 2 (Testing)
- Setting up Postman collections for integration testing is valuable but requires running services.
- H2 in-memory DB is sufficient for most tests but doesn't catch PostgreSQL-specific issues.

### Phase 3 (Documentation)
- 31,000+ words of docs seems excessive until you need to onboard someone or debug production.
- Runbooks with copy-pasteable commands save hours during incidents.

### Phase 4-5 (Training & Tracking Services)
- The Repository → Service → Controller pattern, once established, makes adding new entities fast and predictable.
- Consistent DTO naming (XRequestDTO / XResponseDTO) helps both developers and AI tools understand the codebase.

### Phase 6 (Notification Service)
- Firebase integration adds external dependency complexity -- mock it thoroughly in tests.
- Push token management (register, deactivate, cleanup) is more complex than expected.

### Phase 7 (Integration & Deployment)
- Docker Compose prod profile should always have `restart: always` and no exposed DB port.
- Health check endpoints are essential -- verify them as part of every deployment.

## Recommendations for Future

1. **Event-Driven Architecture**: Add RabbitMQ/Kafka for async notifications and audit events (ADR-006 approved).
2. **Redis Caching**: Implement Redis for 50x performance improvement on read-heavy endpoints (ADR-012 approved).
3. **Database-per-Service**: Migrate from shared PostgreSQL to separate instances when scaling demands it.
4. **Distributed Tracing**: Implement Spring Cloud Sleuth / Micrometer for cross-service request tracing.
5. **Kubernetes**: Move from Docker Compose to K8s for auto-scaling and HA (ADR-004 planned).
6. **CI/CD Pipeline**: Set up GitHub Actions for automated build → test → deploy.
7. **Mobile App Integration**: The API is ready; consider React Native or Flutter client.

## Best Practices Established

1. Always include tests with code changes (85% minimum).
2. Document API changes in Swagger annotations.
3. Use consistent naming conventions across all services.
4. Implement proper error handling with custom exceptions.
5. Keep logs informative but not verbose (use correct log levels).
6. Backup database before any deployment that changes schemas.
7. Run full test suite before committing.
8. Never hardcode secrets -- always use .env / environment variables.

---

**Last Updated**: 2026-03-22
