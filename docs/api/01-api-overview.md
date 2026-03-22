# API Overview

## Base Information

### API Version
- **Current Version**: v1
- **Base URL**: http://localhost:[port]/api/v1
- **Protocol**: HTTP (Development) / HTTPS (Production)
- **Format**: JSON
- **Character Encoding**: UTF-8

### Service Endpoints

| Service | Port | Base URL | Health Check |
|---------|------|----------|--------------|
| Auth Service | 8081 | http://localhost:8081/auth | http://localhost:8081/auth/actuator/health |
| Training Service | 8082 | http://localhost:8082/training | http://localhost:8082/training/actuator/health |
| Tracking Service | 8083 | http://localhost:8083/tracking | http://localhost:8083/tracking/actuator/health |
| Notification Service | 8084 | http://localhost:8084/notifications | http://localhost:8084/notifications/actuator/health |

Via API Gateway (port 8080): prefix each URL with `http://localhost:8080`.

## Authentication

### JWT Token-Based Authentication

All API endpoints (except `/auth/login` and `/auth/register`) require a valid JWT token in the `Authorization` header.

```http
Authorization: Bearer <JWT_TOKEN>
```

### Getting a Token

**Endpoint**: `POST /auth/login`

```json
{
  "email": "user@example.com",
  "password": "your-password"
}
```

**Response**:
```json
{
  "userId": "<uuid>",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "message": "Login successful"
}
```

### Token Expiration & Refresh

Access tokens expire after 24h, refresh tokens after 7d.

**Endpoint**: `POST /auth/refresh`

```json
{
  "token": "your-refresh-token"
}
```

## Request Format

### HTTP Methods

- `GET` - Retrieve resources
- `POST` - Create new resources
- `PUT` - Update existing resources (full replacement)
- `PATCH` - Partially update resources
- `DELETE` - Remove resources

### Headers

```http
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
Accept-Encoding: gzip, deflate
```

### Request Body Example

```json
{
  "name": "Beginner Strength",
  "description": "12-week strength building program",
  "duration": 12,
  "targetAudience": "beginners"
}
```

## Response Format

All responses are in JSON format.

### Successful Response (2xx)

Services return the DTO directly — no wrapper object:

```json
{
  "id": 1,
  "name": "Beginner Strength",
  "description": "12-week strength building program"
}
```

### Error Response (4xx, 5xx)

```json
{
  "status": "NOT_FOUND",
  "message": "Resource not found with id: 123",
  "timestamp": "2024-03-21T10:30:00"
}
```

### Paginated Response

```json
{
  "content": [
    { "id": 1, "name": "Exercise 1" },
    { "id": 2, "name": "Exercise 2" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

## Pagination

### Query Parameters

- `page` - Page number (0-indexed, default: 0)
- `size` - Items per page (default: 20)
- `sort` - Sort field and direction: `field,asc` or `field,desc`

### Example

```http
GET /training/api/v1/exercises/system?page=0&size=10&sort=name,asc
```

## Filtering & Search

### Query Parameters

Services support filtering through query parameters:

```http
GET /api/v1/training/programs?status=active&duration=12
```

### Common Filter Operations

| Operator | Format | Example |
|----------|--------|---------|
| Equals | `field=value` | `status=active` |
| Contains | `field~value` | `name~strength` |
| Greater Than | `field>value` | `duration>8` |
| Less Than | `field<value` | `duration<16` |
| Range | `field[]=value1,value2` | `duration[]=8,12` |

## Rate Limiting

### Limits

- **Per User**: 100 requests per hour
- **Per IP**: 1000 requests per hour
- **Burst**: 10 requests per second

### Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1647900000
```

### Exceeding Limits

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Retry-After: 3600
```

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 3600 seconds",
  "status": 429
}
```

## CORS (Cross-Origin Resource Sharing)

### Allowed Origins

- Development: `http://localhost:3000`, `http://localhost:8080`
- Production: Your domain configuration

### Allowed Methods

- GET, POST, PUT, PATCH, DELETE, OPTIONS

### Allowed Headers

- Content-Type
- Authorization
- Accept
- Origin

## API Documentation

### Swagger UI

Each service provides interactive API documentation via Swagger UI:

```
http://localhost:8081/auth/swagger-ui/index.html        # Auth Service
http://localhost:8082/training/swagger-ui/index.html    # Training Service
http://localhost:8083/tracking/swagger-ui/index.html    # Tracking Service
http://localhost:8084/notifications/swagger-ui/index.html  # Notification Service
```

Also accessible via gateway:
```
http://localhost:8080/auth/swagger-ui/index.html
http://localhost:8080/training/swagger-ui/index.html
...
```

### OpenAPI Specification

Machine-readable API specification at:

```
http://localhost:8081/auth/v3/api-docs
http://localhost:8082/training/v3/api-docs
http://localhost:8083/tracking/v3/api-docs
http://localhost:8084/notifications/v3/api-docs
```

## Versioning Strategy

### URL Versioning

API version is part of the URL path: `/api/v1/...`

### Future Versions

- Backward compatibility maintained for current version
- New versions introduced when breaking changes needed
- Deprecation notice provided 6 months before removal

### Version Transition

```
Current:  /api/v1/training/programs
Future:   /api/v2/training/programs
```

## Common Use Cases

### Registering a New User

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName": "John", "lastName": "Doe", "email": "user@example.com", "password": "SecurePassword123!"}'

# 2. Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "SecurePassword123!"}'

# 3. Use token for authenticated requests
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
curl http://localhost:8080/training/api/v1/exercises/my-exercises \
  -H "Authorization: Bearer $TOKEN"
```

### Fetching Paginated Results

```bash
curl 'http://localhost:8080/training/api/v1/exercises/system?page=0&size=10&sort=name,asc' \
  -H "Authorization: Bearer $TOKEN"
```

## Related Documentation

- [Authentication Details](03-authentication.md)
- [Endpoints Reference](04-endpoints-reference.md)
- [Error Handling](05-error-handling.md)
- [Integration Guide](06-integration-guide.md)
- [Postman Testing Guide](../../tests/03-postman-testing-guide.md)
