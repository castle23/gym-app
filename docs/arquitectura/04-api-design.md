# API Design Principles

## Overview

All services follow consistent RESTful conventions. This document describes the patterns applied across the platform.

---

## URL Structure

Each service has a `server.servlet.context-path` that matches its gateway prefix:

| Service | Context Path | Example endpoint |
|---------|-------------|-----------------|
| Auth | `/auth` | `/auth/register` |
| Training | `/training` | `/training/api/v1/exercises/system` |
| Tracking | `/tracking` | `/tracking/api/v1/measurements` |
| Notification | `/notifications` | `/notifications/api/v1/push-tokens` |

Auth endpoints sit directly under the context path. All other services use `/api/v1/` as an additional prefix.

Via the API Gateway (port 8080), the full URL is the context path + endpoint:
```
http://localhost:8080/training/api/v1/exercises/system
http://localhost:8080/auth/login
```

---

## HTTP Methods

| Method | Usage | Success Status |
|--------|-------|---------------|
| GET | Retrieve resource(s) | 200 |
| POST | Create resource | 201 |
| PUT | Full update | 200 |
| PATCH | Partial update | 200 |
| DELETE | Remove resource | 204 |

---

## User Identity

Services do not receive JWT tokens directly. The API Gateway validates the JWT and injects:

```
X-User-Id: 123
X-User-Roles: ROLE_USER
```

Controllers read `X-User-Id` via `@RequestHeader("X-User-Id") Long userId` to scope queries to the authenticated user.

---

## Pagination

List endpoints that can return large result sets use Spring Data's `Pageable`:

```
GET /training/api/v1/exercises/system?page=0&size=20&sort=name,asc
```

| Parameter | Default | Notes |
|-----------|---------|-------|
| `page` | 0 | 0-indexed |
| `size` | 20 | Items per page |
| `sort` | varies | `field,asc` or `field,desc` |

Response format (`PageResponse<T>`):

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

Non-paginated list endpoints (measurements, objectives, plans, diet-logs) return a plain `List<T>`.

---

## Response Format

### Success

Services return the DTO directly — no wrapper object:

```json
{
  "id": 1,
  "name": "Squat",
  "description": "...",
  "type": "SYSTEM"
}
```

### Error

All errors go through `GymExceptionHandlerAutoConfiguration` and return `ErrorResponse`:

```json
{
  "status": "NOT_FOUND",
  "message": "Exercise not found with id: 99",
  "timestamp": "2024-03-21T10:30:00"
}
```

See [Error Handling](../api/05-error-handling.md) for the full exception → status mapping.

---

## Public vs Protected Endpoints

Endpoints that require no authentication are explicitly listed in:
1. `JwtAuthFilter.PUBLIC_PATHS` in the API Gateway (bypasses JWT validation)
2. `gym.security.public-paths` in each service's `application.yml` (bypasses Spring Security)

Currently public endpoints:

| Service | Public paths |
|---------|-------------|
| Auth | `/auth/register`, `/auth/login`, `/auth/verify` |
| Training | `/training/api/v1/exercises/system`, `/training/api/v1/exercises/discipline/**`, `/training/api/v1/routine-templates/system` |
| Tracking | `/tracking/api/v1/diet-components/**`, `/tracking/api/v1/training-components/**` |
| All | `/{service}/actuator/**`, `/{service}/swagger-ui/**`, `/{service}/v3/api-docs/**` |

---

## Validation

Request bodies are validated with `@Valid` + Jakarta Bean Validation annotations on DTOs. Validation failures return 400 with a message listing all failing fields:

```json
{
  "status": "BAD_REQUEST",
  "message": "Validation failed: email: must not be blank, password: size must be between 8 and 100",
  "timestamp": "2024-03-21T10:30:00"
}
```

---

## Versioning

API version is part of the URL path: `/api/v1/`. The auth service is unversioned (endpoints sit directly under the context path) since it is a single-purpose service unlikely to have breaking changes.

---

## API Documentation

Each service exposes Swagger UI and OpenAPI spec:

```
http://localhost:{port}/{context-path}/swagger-ui/index.html
http://localhost:{port}/{context-path}/v3/api-docs
```

Also accessible via gateway on port 8080 with the same paths.

---

## Related Documentation

- [Endpoints Reference](../api/04-endpoints-reference.md)
- [Error Handling](../api/05-error-handling.md)
- [Authentication](../api/03-authentication.md)
- [Security Architecture](05-security-architecture.md)
