# AI Security Review Prompt

## Context

You are conducting a security audit of the **Gym Platform API**, a Java 17+ /
Spring Boot 3.x microservices system with the following security stack:

| Component           | Technology / Approach                     |
|---------------------|-------------------------------------------|
| Authentication      | JWT (issued by Auth service, port 8081)   |
| Password hashing    | bcrypt via Spring Security                |
| Authorization       | RBAC — ROLE_ADMIN, ROLE_PROFESSIONAL, ROLE_USER |
| API Gateway         | Routes requests, forwards auth headers    |
| User context        | X-User-Id and X-User-Role headers (set by gateway) |
| Input validation    | Bean Validation (`@Valid`, `@NotBlank`, etc.) |
| API documentation   | springdoc-openapi (Swagger UI)            |

---

## Instructions

### OWASP Top 10 — Project-Specific Checklist

#### 1. Injection
- [ ] All database queries use Spring Data JPA parameterized methods or `@Query` with
      named parameters — **never** string concatenation.
- [ ] `@Valid` annotation present on all `@RequestBody` parameters.
- [ ] Custom validators for business rules (email format, ID patterns).

#### 2. Broken Authentication
- [ ] JWT tokens have a defined expiration (`exp` claim) — max 24h access, 7d refresh.
- [ ] Token validation checks signature, expiration, and issuer.
- [ ] Failed login attempts are rate-limited or trigger account lockout.
- [ ] Refresh token rotation implemented (old tokens invalidated on use).

#### 3. Sensitive Data Exposure
- [ ] Passwords hashed with bcrypt (cost factor >= 10).
- [ ] No secrets, tokens, or credentials in source code or version control.
- [ ] Sensitive fields excluded from API responses (password, internal IDs).
- [ ] Logs do not contain PII, tokens, or passwords.

#### 4. XML External Entities (XXE)
- [x] **Not applicable** — API accepts JSON only. XML parsing is disabled.
      Verify: no `@Consumes(MediaType.APPLICATION_XML)` exists.

#### 5. Broken Access Control
- [ ] Every endpoint has explicit RBAC via `@PreAuthorize` or security config.
- [ ] X-User-Id header verified against JWT subject — users cannot impersonate others.
- [ ] Resource ownership checked: users can only access their own data unless admin.
- [ ] Admin-only endpoints (`/admin/**`) restricted at gateway and service level.

#### 6. Security Misconfiguration
- [ ] Swagger UI disabled or restricted in production. Actuator: only `/health` public.
- [ ] No stack traces in error responses. HTTPS enforced. Debug mode off in prod.

#### 7. Cross-Site Scripting (XSS)
- [x] **Low risk** — API-only, no HTML rendering. Verify no view-returning controllers.
- [ ] JSON responses set `Content-Type: application/json`.

#### 8. Insecure Deserialization
- [ ] Jackson configured to reject unknown properties (`FAIL_ON_UNKNOWN_PROPERTIES`).
- [ ] No `@JsonTypeInfo` with `Id.CLASS` (prevents polymorphic deserialization attacks).
- [ ] Request DTOs use specific types — no `Object` or `Map<String, Object>` fields.

#### 9. Using Components with Known Vulnerabilities
- [ ] Dependencies scanned with OWASP Dependency-Check or Snyk.
- [ ] No dependencies with known critical CVEs.
- [ ] Spring Boot BOM version is current (within latest patch release).
- [ ] Base Docker images use specific tags, not `latest`.

#### 10. Insufficient Logging & Monitoring
- [ ] Authentication events logged: login success, login failure, token refresh, logout.
- [ ] Authorization failures logged with user ID, endpoint, and required role.
- [ ] Logs shipped to centralized system (ELK, Loki, or CloudWatch).
- [ ] Alerts configured for: repeated auth failures, unusual traffic patterns.

---

### Additional Security Checklist

**JWT Configuration:**
- [ ] Secret key is >= 256 bits, stored in environment variable.
- [ ] Algorithm explicitly set (e.g., HS256 or RS256) — no `none` algorithm accepted.
- [ ] Token contains only necessary claims (sub, roles, exp, iat).

**CORS:** Explicit allowed origins (no wildcard `*` in prod), restricted methods.
**Rate Limiting:** Login: 5 attempts/min/IP. General API limits. Use `429 + Retry-After`.
**Secrets:** Env vars or secrets manager only. `.env` in `.gitignore`. No inline secrets.

---

## Expected Output Format

```markdown
## Security Review: [Scope — e.g., "Auth Service v2.1"]

### Findings

| # | Severity | Category          | Finding                     | Recommendation          |
|---|----------|-------------------|-----------------------------|-------------------------|
| 1 | CRITICAL | Broken Auth       | JWT has no expiration set   | Add `exp` claim, max 24h|
| 2 | HIGH     | Access Control    | Missing @PreAuthorize on... | Add role check          |
| 3 | MEDIUM   | Misconfiguration  | Swagger UI exposed in prod  | Disable via profile     |

### Summary
- **Critical/High**: must fix before deployment / within current sprint
- **Medium/Low**: schedule for next sprint / regular maintenance

### Positive Observations & Recommended Next Steps
- [What is done well] → [Top priority actions]
```

---

## References

- [Security Standards](../rules/security-standards.md)
- [Security Documentation](../../docs/security/)
