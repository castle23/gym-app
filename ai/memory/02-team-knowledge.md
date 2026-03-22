# Team Knowledge

## Core Expertise Areas

### Backend Development
- Java 17+ (records, sealed classes, pattern matching, text blocks)
- Spring Boot 3.x (auto-configuration, profiles, actuator)
- Spring Security (JWT filter chains, SecurityConfig, role-based access)
- Spring Data JPA (JpaRepository, @Query, Specifications, Pageable)
- RESTful API design (resource-oriented, proper HTTP methods/status codes)

### Database
- PostgreSQL 15 administration
- Multi-schema design (4 schemas in single instance)
- Query optimization (pg_stat_statements, EXPLAIN ANALYZE)
- Index strategy (B-tree, partial, composite, BRIN)
- Backup/recovery (pg_dump, WAL archiving)
- HikariCP connection pool tuning

### DevOps / Operations
- Docker containerization (multi-stage builds)
- Docker Compose orchestration (dev + prod profiles)
- Health checks (/actuator/health)
- Log aggregation (docker-compose logs, structured logging)
- Deployment runbooks and rollback procedures
- Monitoring readiness (Prometheus/Grafana planned)

### Testing
- JUnit 5 + Mockito (unit tests)
- @WebMvcTest slices (controller tests)
- @SpringBootTest (integration tests)
- H2 in-memory database for test profile
- JaCoCo coverage reporting (85% minimum target)
- AAA pattern (Arrange-Act-Assert)

### Security
- JWT token lifecycle (issue, validate, refresh, expire)
- BCrypt password hashing
- RBAC implementation (ROLE_ADMIN, ROLE_PROFESSIONAL, ROLE_USER)
- API Gateway header injection (X-User-Id, X-User-Roles)
- Input validation (@Valid, custom validators)
- OWASP awareness (injection, broken auth, access control)

## Design Patterns Used

| Pattern | Where | Example |
|---------|-------|---------|
| MVC | All services | Controller → Service → Repository |
| Repository | Data access | JpaRepository per entity |
| DTO | API contracts | RequestDTO / ResponseDTO per entity |
| Factory | Token creation | JWT token generation |
| Observer | Notifications | Event-driven notification triggers |
| Strategy | Validation | Different validation per user type |
| Builder | Complex objects | DTO construction |

## Common Troubleshooting Knowledge

| Issue | Diagnostic | Resolution |
|-------|-----------|------------|
| JWT 401 on valid token | Check JWT_SECRET matches across services | Ensure .env has same secret for all |
| DB connection pool exhausted | `docker-compose logs [svc] \| grep HikariPool` | Increase max-pool-size, check for leaks |
| Container not starting | `docker-compose logs [svc]` | Usually DB not ready; restart service |
| Port already in use | `lsof -i :[port]` or `netstat -ano` | Kill process or change port |
| Slow queries (>1s) | Enable pg_stat_statements | Add missing indexes, fix N+1 |
| UnauthorizedException returns 403 | By design (not 401) | See exception mapping in testing-standards |
| MockMvc test fails with 404 | URL includes context-path | Remove context-path from MockMvc URL |

## Key Conventions

1. **Package structure**: `com.gym.[service].[layer]` (controller, service, repository, dto, entity)
2. **Test naming**: `should[ExpectedOutcome]When[Condition]`
3. **Commit messages**: `<type>(<scope>): <subject>` (conventional commits)
4. **Branch naming**: `<type>/<ticket>-<short-description>`
5. **API paths**: `/{service}/api/v1/{resource}` (Training, Tracking, Notification)
6. **Exception classes**: All in `com.gym.common.exception`

## Learning Resources

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/15/)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [JWT Best Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- Project docs: `docs/development/01-getting-started.md`
