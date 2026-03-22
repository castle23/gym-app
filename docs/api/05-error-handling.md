# Error Handling

## Overview

All services use a centralized `GymExceptionHandlerAutoConfiguration` (`@RestControllerAdvice`) from `gym-common` that maps exceptions to HTTP status codes consistently across all services.

## HTTP Status Codes

| Status | Meaning | When |
|--------|---------|------|
| 200 | OK | Successful GET / PUT / PATCH |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation failure, invalid input |
| 401 | Unauthorized | Missing or invalid JWT token (gateway) |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource does not exist |
| 500 | Internal Server Error | Unexpected server error |

## Exception Mapping

| Exception | HTTP Status | Usage |
|-----------|-------------|-------|
| `ResourceNotFoundException` | 404 Not Found | Entity not found by ID |
| `UnauthorizedException` | 403 Forbidden | Authenticated but not permitted (e.g. accessing another user's resource) |
| `AuthenticationException` | 401 Unauthorized | Invalid credentials, expired/invalid token |
| `InvalidDataException` | 400 Bad Request | Business rule violation, duplicate data |
| `MethodArgumentNotValidException` | 400 Bad Request | `@Valid` bean validation failure |
| `MissingServletRequestParameterException` | 400 Bad Request | Missing required query param |
| `MissingRequestHeaderException` | 400 Bad Request | Missing required header |
| `IllegalArgumentException` | 400 / 403 / 404 (by message content) | Legacy fallback |

> `UnauthorizedException` → **403 Forbidden** (authorized but not permitted).  
> `AuthenticationException` → **401 Unauthorized** (bad credentials, invalid token).  
> 401 from the API Gateway means JWT is missing or invalid before reaching the service.

All exception classes are in `com.gym.common.exception`.

## Error Response Format

```json
{
  "status": "NOT_FOUND",
  "message": "Resource not found with id: 123",
  "timestamp": "2024-03-21T10:30:00"
}
```

## Validation Errors (400)

When `@Valid` fails on a request body, the response includes field-level details:

```json
{
  "status": "BAD_REQUEST",
  "message": "Validation failed: email: must not be blank, password: size must be between 8 and 100",
  "timestamp": "2024-03-21T10:30:00"
}
```

## Common Error Scenarios

### Missing JWT (401)
```bash
curl http://localhost:8080/training/api/v1/exercises/my-exercises
# → 401 Unauthorized (from API Gateway)
```

### Insufficient Role (403)
```bash
# User with ROLE_USER accessing admin endpoint
# → 403 Forbidden (UnauthorizedException from service)
```

### Resource Not Found (404)
```bash
curl http://localhost:8080/training/api/v1/exercises/99999 \
  -H "Authorization: Bearer $TOKEN"
# → 404 Not Found (ResourceNotFoundException)
```

### Invalid Input (400)
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "not-an-email", "password": "123"}'
# → 400 Bad Request (MethodArgumentNotValidException)
```
