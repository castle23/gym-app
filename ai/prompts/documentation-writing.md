# AI Documentation Writing Prompt

## Context

You are generating documentation for the **Gym Platform API**, a Java 17+ / Spring Boot 3.x
microservices platform with 4 services (Auth:8081, Training:8082, Tracking:8083,
Notification:8084). Documentation is powered by **springdoc-openapi** (Swagger UI) and
maintained in **bilingual format** (English primary, Spanish translations where specified).

---

## Instructions

### Swagger / OpenAPI Annotations

#### DTOs — `@Schema` on every field:
```java
@Schema(description = "User's email address", example = "john@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED)
private String email;
```
- Always include `description` and `example`. Use `requiredMode` for required fields.
- Use `@Schema(hidden = true)` for internal fields. Enum fields: list allowed values.

#### Controllers — `@Operation` and `@ApiResponse` on every endpoint:
```java
@Operation(summary = "Create training plan",
    description = "Creates a new plan. Requires ROLE_PROFESSIONAL or ROLE_ADMIN.")
@ApiResponse(responseCode = "201", description = "Plan created successfully")
@ApiResponse(responseCode = "400", description = "Invalid input data")
```
- Summary: short verb phrase. Description: include required roles and side effects.
- Document all possible response codes (success + error).

### Bilingual Convention
- Code annotations: English only. Markdown docs: English primary, Spanish via
  `<!-- lang: es -->` blocks or parallel `/es/` directory.

---

## Documentation Templates

### Template 1: API Endpoint Documentation
```markdown
## `METHOD /api/v1/resource`
**Service**: [name] | **Auth**: [role(s)] | **Since**: v[version]
### Description
[What this endpoint does and when to use it.]
### Request
| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| id        | path     | UUID | yes      | Resource ID |
#### Body Example
\```json
{ "field": "value" }
\```
### Response `200 OK`
\```json
{ "id": "uuid", "field": "value" }
\```
### Errors
| Code | Condition |
|------|-----------|
| 400  | Validation failed | 401 | Invalid token | 403 | Wrong role | 404 | Not found |
```

### Template 2: Architecture Decision Record (ADR)
```markdown
# ADR-NNN: [Title]
- **Status**: Proposed | Accepted | Deprecated | Superseded
- **Date**: YYYY-MM-DD
- **Context**: [Problem or situation driving this decision]
- **Decision**: [What we decided]
- **Rationale**: [Why this option was chosen]
- **Consequences**: Positive: [...] | Negative: [...]
- **Alternatives Considered**: [Other options evaluated]
- **Mitigations**: [How we address negative consequences]
- **Related ADRs**: [Links to related decisions]
```

### Template 3: README Section
```markdown
## [Feature Name]
[1-2 sentence overview.]
### Quick Start
\```bash
[minimal commands to get running]
\```
### Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `ENV_VAR` | `value` | What it controls |
```

### Template 4: Troubleshooting Guide
```markdown
## Problem: [Short description]
**Symptoms**: [What the user observes] | **Cause**: [Root cause]
**Solution**: [Step-by-step fix] | **Prevention**: [How to avoid recurrence]
```

---

## Expected Output Format

- Markdown ready to commit — no TODOs, no placeholders.
- Proper heading hierarchy, code blocks with language IDs, aligned tables.
- Links use relative paths within the repository.

---

## References

- [Documentation Standards](../rules/documentation-standards.md)
