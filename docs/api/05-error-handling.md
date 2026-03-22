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

| Exception | HTTP Status |
|-----------|-------------|
| `ResourceNotFoundException` | 404 Not Found |
| `UnauthorizedException` | 403 Forbidden |
| `InvalidDataException` | 400 Bad Request |
| `MethodArgumentNotValidException` | 400 Bad Request |
| `MissingServletRequestParameterException` | 400 Bad Request |
| `MissingRequestHeaderException` | 400 Bad Request |
| `IllegalArgumentException` | 400 / 403 / 404 (by message content) |

> `UnauthorizedException` maps to **403 Forbidden**, not 401. It represents an authorization failure (authenticated but not permitted). 401 is returned by the API Gateway when JWT is missing or invalid.

All exception classes are in `com.gym.common.exception`.

## Error Response Format

```json
{
  "timestamp": "2024-03-21T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found with id: 123",
  "path": "/training/api/v1/exercises/123"
}
```

## Validation Errors (400)

When `@Valid` fails on a request body, the response includes field-level details:

```json
{
  "timestamp": "2024-03-21T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "must not be blank" },
    { "field": "password", "message": "size must be between 8 and 100" }
  ]
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
