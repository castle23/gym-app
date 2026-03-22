# API Documentation

This section contains API documentation, endpoint references, and integration guides.

## Contents

- **01-api-overview.md** - API overview and general information
- **02-swagger-usage-guide.md** - Using Swagger UI for API documentation
- **03-authentication.md** - Authentication and authorization (RBAC)
- **04-endpoints-reference.md** - API endpoint reference
- **05-error-handling.md** - Error codes and handling
- **06-integration-guide.md** - Integration examples and best practices

## Subdirectories

- **services/** - Service-specific API documentation
- **examples/** - Code examples and sample requests
- **faqs/** - Common API questions

## API Verification

✅ **Swagger UI**: Available at `http://localhost:[port]/swagger-ui.html`
✅ **API Status**: All 80 endpoints verified and documented
✅ **Schema Validation**: All endpoints include @Schema annotations

See `swagger-verification.md` and `rbac-verification.md` for verification results.

## Quick Start: Using the API

### 1. Authentication
First, authenticate to get a JWT token:
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password"
}
```

See **03-authentication.md** for detailed authentication information.

### 2. Making Requests
Include the JWT token in the Authorization header:
```
Authorization: Bearer <JWT_TOKEN>
```

### 3. Service Endpoints

| Service | Base URL | Port |
|---------|----------|------|
| Auth Service | `/api/v1/auth` | 8081 |
| Training Service | `/api/v1/training` | 8082 |
| Tracking Service | `/api/v1/tracking` | 8083 |
| Notification Service | `/api/v1/notification` | 8084 |

### 4. Full Endpoint Reference
See **04-endpoints-reference.md** for complete endpoint listing.

## Accessing Swagger UI

Each service exposes Swagger UI:
- Auth: `http://localhost:8081/swagger-ui.html`
- Training: `http://localhost:8082/swagger-ui.html`
- Tracking: `http://localhost:8083/swagger-ui.html`
- Notification: `http://localhost:8084/swagger-ui.html`

See **02-swagger-usage-guide.md** for detailed Swagger usage.

## Testing the API

### Using Postman
1. Import test collections from [tests/](../../tests/)
2. Import environment configuration
3. Run requests from Postman

See [Testing Guide](../../tests/03-postman-testing-guide.md) for details.

### Using curl
```bash
# Authentication
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"password"}'

# Authenticated request
curl http://localhost:8082/api/v1/training/programs \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Error Handling

API errors follow standard HTTP status codes and return error details in JSON format.

See **05-error-handling.md** for:
- Error codes
- Error response format
- Handling strategies

## Integration Examples

See **examples/** directory for:
- Python examples
- Java/Spring Boot examples
- cURL examples

See **06-integration-guide.md** for integration best practices.

## Service-Specific Documentation

Each service has detailed documentation:
- See **services/** directory for service-specific endpoints
- Each service folder contains endpoint documentation

## RBAC (Role-Based Access Control)

The API implements role-based access control. See:
- **03-authentication.md** - Role definitions
- `rbac-verification.md` - RBAC testing results

## Rate Limiting

API requests may be rate-limited. See **01-api-overview.md** for:
- Rate limit thresholds
- Rate limit headers
- Retry strategies

## API Versioning

The API uses URL versioning: `/api/v1/...`

Future versions will be at: `/api/v2/...`, etc.

See **01-api-overview.md** for versioning strategy.

## Pagination

Large result sets are paginated. See **04-endpoints-reference.md** for:
- Pagination parameters
- Response structure
- Pagination examples

## For More Information

- **Architecture**: See [Architecture Documentation](../arquitectura/)
- **Development**: See [Development Documentation](../development/)
- **Testing**: See [Testing Guide](../../tests/)
- **Troubleshooting**: See [Troubleshooting Guide](../troubleshooting/)
