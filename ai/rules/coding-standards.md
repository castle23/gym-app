# Coding Standards

> Java 17+ / Spring Boot 3.x conventions for the Gym Platform API.

## Naming Conventions

1. **PascalCase** for classes and interfaces (`UserService`, `TrainingPlanDto`).
2. **camelCase** for methods, variables, and parameters (`findByEmail`, `planId`).
3. **UPPER_SNAKE_CASE** for constants (`MAX_LOGIN_ATTEMPTS`, `DEFAULT_PAGE_SIZE`).
4. **Packages** follow `com.gym.[service].[layer]` — e.g., `com.gym.auth.controller`.
   > Keeps grep/IDE navigation predictable across all four services.

## Package Structure

5. Every service uses these packages:
   - `com.gym.[service].controller` — REST endpoints
   - `com.gym.[service].service` — Business logic
   - `com.gym.[service].repository` — Data access
   - `com.gym.[service].dto` — Request/response objects
   - `com.gym.[service].entity` — JPA entities
6. One responsibility per file. A `UserService` must not also handle notification logic.
7. Flow is always **Repository → Service → Controller**. Controllers never call repositories directly.

## Methods & Structure

8. Methods must not exceed **50 lines**. Extract helper methods when approaching the limit.
   > Long methods signal mixed responsibilities and are harder to test.
9. Each method does one thing. If the name contains "And", split it.
10. Prefer early returns over deep nesting.

## Comments & Documentation

11. Comments explain **why**, never **what**. Never restate the code.
    ```java
    // BAD: Set the user's age to 25
    // GOOD: Default age used when birth date is missing from legacy import
    ```
12. JavaDoc is required on **public API methods only** (controllers, public service interfaces).
13. Remove commented-out code — that is what version control is for.

## Error Handling

14. **Never silently swallow exceptions.** Every catch block must log and either re-throw or return a meaningful error code.
15. Use custom exceptions from `com.gym.common.exception` (`ResourceNotFoundException`, `InvalidDataException`, `DuplicateResourceException`, `UnauthorizedException`).
    > See also: `../rules/testing-standards.md` for exception-to-HTTP-status mapping.
16. Let the global `@RestControllerAdvice` handler translate exceptions to HTTP responses.
17. Validate inputs at the controller boundary with `@Valid`; fail fast.

## Logging Levels

18. **ERROR** — System failures requiring immediate attention (DB down, unrecoverable state).
19. **WARN** — Degraded but functional (retry succeeded, fallback used).
20. **INFO** — Business events (user registered, plan created, payment processed).
21. **DEBUG** — Developer troubleshooting (method entry/exit, variable state).
22. **TRACE** — Framework-level detail (SQL queries, HTTP headers). Never in production.
23. Never log sensitive data (passwords, tokens, PII).

## SOLID Principles

24. **Single Responsibility**: One reason to change per class.
25. **Open/Closed**: Extend via interfaces, not by modifying existing classes.
26. **Liskov Substitution**: Subtypes must be substitutable for their base types.
27. **Interface Segregation**: Prefer small, focused interfaces over large ones.
28. **Dependency Inversion**: Depend on abstractions (`UserService` interface), not concretions.
29. Use constructor injection exclusively — never field injection with `@Autowired`.

## Security

30. **No hardcoded secrets.** Use environment variables or `.env` files.
    > See also: `../rules/security-standards.md` for full security policy.
31. Validate all external inputs. Trust nothing from the client.
32. Encrypt sensitive data at rest (passwords via BCrypt, PII where required).

## Testing

33. Follow the **Arrange-Act-Assert** pattern in all tests.
34. Test naming: `should[ExpectedOutcome]When[Condition]`.
35. One logical assertion per test method.
    > See also: `../rules/testing-standards.md` for full testing policy.
