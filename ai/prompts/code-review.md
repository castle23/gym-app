# AI Code Review Prompt

## Context

You are reviewing code for the **Gym Platform API**, a Java 17+ / Spring Boot 3.x
microservices system with 4 services:

| Service        | Port | Responsibility              |
|----------------|------|-----------------------------|
| Auth           | 8081 | Authentication, users, RBAC |
| Training       | 8082 | Programs, exercises, plans  |
| Tracking       | 8083 | Progress, metrics, goals    |
| Notification   | 8084 | Alerts, emails, events      |

**Architecture**: MVC pattern — Controller → Service → Repository.
**Database**: PostgreSQL per service. **Auth**: JWT + bcrypt. **Roles**: ROLE_ADMIN,
ROLE_PROFESSIONAL, ROLE_USER.

---

## Instructions

Review the provided code against these checklist categories:

### 1. Naming Conventions
- Classes: PascalCase. Methods/variables: camelCase. Constants: UPPER_SNAKE_CASE.
- DTOs suffixed with `Request`/`Response`. Entities match DB table names.
- REST endpoints: plural nouns, kebab-case (`/api/v1/training-plans`).

### 2. Error Handling
- Use custom exceptions from `com.gym.common.exception` (e.g., `ResourceNotFoundException`,
  `BusinessRuleException`, `UnauthorizedException`).
- Never catch generic `Exception` unless re-throwing. Never swallow exceptions silently.
- All endpoints must return consistent error responses via `GlobalExceptionHandler`.

### 3. Logging
- Use SLF4J with `@Slf4j`. Correct levels: ERROR (failures), WARN (recoverable),
  INFO (business events), DEBUG (dev diagnostics).
- Never log sensitive data (passwords, tokens, PII).
- Include correlation context (userId, requestId) where available.

### 4. Security
- No hardcoded secrets, tokens, or passwords. Use environment variables or config server.
- All inputs validated with `@Valid` and Bean Validation annotations.
- RBAC enforced via `@PreAuthorize` or security config — verify role checks match requirements.
- SQL injection prevention: use Spring Data JPA parameterized queries only.

### 5. Tests
- Target: 85%+ line coverage. Unit tests for services, integration tests for controllers.
- Use `@WebMvcTest` for controller tests, `@DataJpaTest` for repositories.
- Mock external service calls. Assert both success and failure paths.

### 6. SOLID Principles
- Single Responsibility: one reason to change per class.
- Open/Closed: extend via interfaces, not modification.
- Dependency Inversion: inject interfaces, not concrete implementations.
- No God classes. Max ~300 lines per class, ~30 lines per method.

---

## Severity Levels

| Level      | Meaning                                       |
|------------|-----------------------------------------------|
| CRITICAL   | Security vulnerability or data loss risk       |
| HIGH       | Bug, broken logic, or missing error handling   |
| MEDIUM     | Code smell, maintainability, or performance    |
| LOW        | Style, naming, or minor improvement            |

---

## Expected Output Format

Return findings as a Markdown table:

```markdown
| File | Line | Severity | Issue | Suggestion |
|------|------|----------|-------|------------|
| UserService.java | 45 | HIGH | Generic exception caught | Use `ResourceNotFoundException` from common module |
| AuthController.java | 12 | CRITICAL | JWT secret hardcoded | Move to environment variable `JWT_SECRET` |
```

End with a **Summary** section: total issues per severity, overall assessment (PASS / PASS WITH
CONDITIONS / FAIL), and top 3 priorities to address.

---

## References

- [Coding Standards](../rules/coding-standards.md)
- [Security Standards](../rules/security-standards.md)
