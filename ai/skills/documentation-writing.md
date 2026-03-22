# Skill: Documentation Writing

## Description
Generate technical documentation from Java source code, producing Markdown files ready for commit to `docs/`.

## Prerequisites
- Access to Java source files in the target service
- Familiarity with `ai/rules/documentation-standards.md`
- Understanding of existing docs structure under `docs/`

## Steps

1. **Analyze Class/Module**
   - Read the class and its dependencies
   - Identify the domain context (Auth, Training, Tracking, Notification)
   - Note the service port (Auth:8081, Training:8082, Tracking:8083, Notification:8084)

2. **Identify Public API**
   - List all `public` methods on `@RestController` classes
   - Note HTTP method, path, parameters, and return type
   - Identify `@PreAuthorize` roles required

3. **Extract Annotations**
   - `@Operation(summary, description)` → endpoint summary
   - `@ApiResponse(responseCode, description)` → response docs
   - `@Schema(description, example)` → field-level docs on DTOs
   - `@Tag(name)` → grouping

4. **Generate Endpoint Documentation**
   - One section per endpoint with method, path, description, request/response examples
   - Include authentication requirements and roles
   - Document error responses

5. **Apply Markdown Format**
   - Use consistent heading hierarchy (H1 = title, H2 = sections, H3 = endpoints)
   - Code blocks with language tags (`java`, `json`, `bash`)
   - Tables for parameter and field descriptions

6. **Validate Completeness**
   - Every public endpoint is documented
   - Request and response examples are valid JSON
   - All required roles and permissions are listed
   - Cross-references to related docs use relative links

## Templates

### API Endpoint Documentation
```markdown
## [HTTP_METHOD] [path]
**Summary:** [from @Operation]
**Auth:** [role required or "Public"]

### Request
| Field | Type   | Required | Description        |
|-------|--------|----------|--------------------|
| name  | String | Yes      | [from @Schema]     |

### Response `[status code]`
```json
{ "example": "response" }
`` `

### Error Responses
| Code | Description              |
|------|--------------------------|
| 400  | Validation failed        |
| 403  | Insufficient permissions |
```

### Architecture Decision Record (ADR)
```markdown
# ADR-NNN: [Title]
**Date:** YYYY-MM-DD
**Status:** Proposed | Accepted | Deprecated

## Context
[Why this decision is needed]

## Decision
[What was decided]

## Consequences
### Positive
- [benefit]
### Negative
- [tradeoff]

## References
- [related ADRs or docs]
```

### README Section
```markdown
## [Section Title]

### Overview
[Brief description]

### Getting Started
[Quick start steps]

### Configuration
| Variable | Default | Description |
|----------|---------|-------------|
```

## Output
- Markdown file(s) placed in the correct `docs/` subdirectory:
  - ADRs → `docs/adr/`
  - API docs → `docs/api/`
  - Guides → `docs/development/`
  - Runbooks → `docs/operations/`
- Commit message: `docs(scope): description`

## References
- `ai/rules/documentation-standards.md`
- `ai/rules/project-overview.md`
- Existing docs in `docs/` for style consistency
