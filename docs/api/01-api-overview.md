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
| Auth Service | 8081 | http://localhost:8081/api/v1 | http://localhost:8081/health |
| Training Service | 8082 | http://localhost:8082/api/v1 | http://localhost:8082/health |
| Tracking Service | 8083 | http://localhost:8083/api/v1 | http://localhost:8083/health |
| Notification Service | 8084 | http://localhost:8084/api/v1 | http://localhost:8084/health |

## Authentication

### JWT Token-Based Authentication

All API endpoints (except `/auth/login` and `/auth/register`) require a valid JWT token in the `Authorization` header.

```http
Authorization: Bearer <JWT_TOKEN>
```

### Getting a Token

**Endpoint**: `POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "your-password"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTI4NDYzODAwLCJleHAiOjE1Mjg0Njc0MDB9.signature",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"]
  }
}
```

### Token Expiration & Refresh

Tokens expire after 1 hour by default. To extend your session:

**Endpoint**: `POST /api/v1/auth/refresh`

```json
{
  "token": "your-current-token"
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

```json
{
  "data": {
    "id": 1,
    "name": "Beginner Strength",
    "description": "12-week strength building program",
    "duration": 12
  },
  "status": "success",
  "timestamp": "2024-03-21T10:30:00Z"
}
```

### Error Response (4xx, 5xx)

```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token",
  "status": 401,
  "timestamp": "2024-03-21T10:30:00Z",
  "path": "/api/v1/training/programs"
}
```

### Paginated Response

```json
{
  "data": [
    { "id": 1, "name": "Program 1" },
    { "id": 2, "name": "Program 2" }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalElements": 42,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  },
  "status": "success"
}
```

## Pagination

### Query Parameters

- `page` - Page number (1-indexed, default: 1)
- `pageSize` - Items per page (default: 20, max: 100)
- `sort` - Sort field and direction: `field:asc` or `field:desc`

### Example

```http
GET /api/v1/training/programs?page=2&pageSize=10&sort=name:asc
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
http://localhost:8081/swagger-ui.html    # Auth Service
http://localhost:8082/swagger-ui.html    # Training Service
http://localhost:8083/swagger-ui.html    # Tracking Service
http://localhost:8084/swagger-ui.html    # Notification Service
```

### OpenAPI Specification

Machine-readable API specification at:

```
http://localhost:8081/v3/api-docs        # Auth Service
http://localhost:8082/v3/api-docs        # Training Service
http://localhost:8083/v3/api-docs        # Tracking Service
http://localhost:8084/v3/api-docs        # Notification Service
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
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe"
  }'

# 2. Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'

# 3. Use token for authenticated requests
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
curl http://localhost:8082/api/v1/training/programs \
  -H "Authorization: Bearer $TOKEN"
```

### Fetching Paginated Results

```bash
curl 'http://localhost:8082/api/v1/training/programs?page=1&pageSize=10&sort=name:asc' \
  -H "Authorization: Bearer $TOKEN"
```

### Filtering Resources

```bash
curl 'http://localhost:8082/api/v1/training/programs?status=active&duration>8' \
  -H "Authorization: Bearer $TOKEN"
```

## Related Documentation

- [Authentication Details](03-authentication.md)
- [Endpoints Reference](04-endpoints-reference.md)
- [Error Handling](05-error-handling.md)
- [Integration Guide](06-integration-guide.md)
- [Postman Testing Guide](../../tests/03-postman-testing-guide.md)
