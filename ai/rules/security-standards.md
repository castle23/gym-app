# Security Standards

> Security rules for the Gym Platform API. Defense in depth — every layer assumes the previous one failed.

## Security Architecture (5 Layers)

1. **Perimeter** — TLS 1.2+ termination, firewall rules, rate limiting.
2. **Authentication** — JWT token validation at the API Gateway.
3. **Authorization** — RBAC enforced at the service level.
4. **Data Protection** — Encryption at rest and in transit, parameterized queries.
5. **Audit** — Logging of all security-relevant events.

## Network Boundaries

6. Traffic flow: `Internet → API Gateway (8080) → Internal Docker Network → Services (8081-8084) → PostgreSQL (5432)`.
7. Only the API Gateway is exposed to the internet. Services are internal-only.
8. PostgreSQL is accessible only from the Docker network, never from the internet.

## JWT Authentication

9. JWT is **mandatory** on all protected endpoints. Token expiration: **1 hour**.
10. The API Gateway validates the JWT and injects two headers into downstream requests:
    - `X-User-Id` — The authenticated user's ID.
    - `X-User-Roles` — Comma-separated roles (e.g., `ROLE_USER,ROLE_ADMIN`).
11. Services **read user identity from `X-User-Id` header only**. Never trust user IDs from request bodies or path params for authorization decisions.
12. `JWT_SECRET` must be minimum **32 characters**. Store in environment variables, never in code or config files.

## Public Endpoints (No Auth Required)

13. The following endpoints are public:
    - `POST /auth/register`, `POST /auth/login`, `POST /auth/verify`
    - `GET /training/exercises` (system exercises only)
    - `GET /training/templates` (system templates only)
    - Swagger paths: `/swagger-ui/**`, `/v3/api-docs/**`
14. All other endpoints require a valid JWT.

## RBAC (Role-Based Access Control)

15. Three roles: `ROLE_ADMIN`, `ROLE_PROFESSIONAL`, `ROLE_USER`.
16. Role hierarchy: Admin > Professional > User.
17. Enforce roles at the controller level with `@PreAuthorize` or in `SecurityConfig`.
18. Users can only access their own data unless they have `ROLE_ADMIN`.
    > See also: `../rules/testing-standards.md` rules 27-29 for security testing.

## Password Security

19. **BCrypt** is the only acceptable password hashing algorithm. Never use MD5, SHA-1, or SHA-256 for passwords.
20. Never log, return, or expose passwords in any form — not even hashed.
21. Enforce minimum password complexity at registration (min 8 chars, mixed case, number, special char).

## Secrets Management

22. **Never hardcode secrets** in source code, configuration files, or Docker images.
23. Use `.env` files for local development. Use cloud secrets managers for production.
24. `.env` files must be in `.gitignore`. Provide `.env.example` with placeholder values.
25. Rotate secrets on any suspected compromise.
    > See also: `../rules/coding-standards.md` rule 30.

## Input Validation

26. Use `@Valid` on all `@RequestBody` parameters in controllers.
27. Validate DTOs with Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`).
28. Sanitize all user input before persistence — no raw HTML or script content.

## SQL & Data Protection

29. **Parameterized queries only.** Never concatenate user input into SQL strings.
    ```java
    // CORRECT — Spring Data JPA / named parameters
    @Query("SELECT u FROM User u WHERE u.email = :email")

    // NEVER — SQL injection vulnerability
    @Query("SELECT u FROM User u WHERE u.email = '" + email + "'")
    ```
30. Encrypt sensitive data at rest (PII, health data) where regulations require it.
31. Use HTTPS (TLS 1.2+) for all communication. No exceptions.

## Audit Logging

32. Log all security events at **INFO** or **WARN** level:
    - Successful and failed login attempts.
    - Permission denied / authorization failures.
    - Sensitive data access (profile views, data exports).
    - Admin actions (role changes, user management).
33. Include `userId`, `action`, `resource`, `outcome`, and `timestamp` in audit log entries.
34. Never include secrets, tokens, or passwords in audit logs.
    > See also: `../rules/coding-standards.md` rules 18-23 for logging levels.
