# Swagger/OpenAPI Documentation Guide

## Overview

All Gym Platform microservices provide interactive API documentation through Swagger UI, automatically generated from OpenAPI 3.0 specifications.

---

## Accessing Swagger UI

Each microservice exposes its API documentation at:

| Service | URL | Purpose |
|---------|-----|---------|
| **Auth Service** | http://localhost:8081/auth/swagger-ui/index.html | Authentication and JWT management |
| **Training Service** | http://localhost:8082/training/swagger-ui/index.html | Exercises, routines, and training programs |
| **Tracking Service** | http://localhost:8083/tracking/swagger-ui/index.html | Diet, measurements, objectives, progress |
| **Notification Service** | http://localhost:8084/notifications/swagger-ui/index.html | Push notifications and alerts |

Also accessible via API Gateway (port 8080) with the same paths.

---

## Features

### Try It Out

Every endpoint includes a **"Try it out"** button:

1. Click the endpoint to expand it
2. Click **"Try it out"** button
3. Fill in request parameters (path, query, body)
4. Click **"Execute"**
5. View the exact request sent and response received

### Authentication

For endpoints requiring JWT tokens:

1. **Get a token:**
   - Use Auth Service `/login` endpoint to get a JWT token
   
2. **Authorize in Swagger UI:**
   - Click the green **"Authorize"** button (top right)
   - Enter: `Bearer <your-jwt-token>`
   - Click **"Authorize"**
   
3. **Make authenticated requests:**
   - All subsequent requests automatically include the token
   - Token persists until you click **"Logout"**

### Schema Documentation

Every request and response includes detailed schema information:

- **Data types**: string, integer, boolean, array, object, etc.
- **Required fields**: marked with `*`
- **Optional fields**: optional
- **Format**: email, date-time, UUID, etc.
- **Examples**: sample values for each field
- **Constraints**: min/max length, patterns, etc.

---

## OpenAPI Specification

### Raw Specification

The raw OpenAPI 3.0 JSON specification is available at:

- **Individual service**: `http://localhost:<port>/<context-path>/v3/api-docs`
  - Auth: http://localhost:8081/auth/v3/api-docs
  - Training: http://localhost:8082/training/v3/api-docs
  - Tracking: http://localhost:8083/tracking/v3/api-docs
  - Notification: http://localhost:8084/notifications/v3/api-docs

### Spec Format

The specification is in JSON format following OpenAPI 3.0 standard:

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "Auth Service API",
    "description": "...",
    "version": "1.0.0"
  },
  "paths": { ... },
  "components": {
    "schemas": { ... },
    "securitySchemes": { ... }
  }
}
```

---

## Integration with Tools

### Postman Import

Import Swagger specs into Postman for advanced testing:

1. Open Postman
2. Click **"Import"** → **"Link"**
3. Paste the OpenAPI spec URL: `http://localhost:8081/v3/api-docs`
4. Click **"Import"**
5. Postman auto-generates requests for all endpoints

### IntelliJ IDEA Integration

IntelliJ provides built-in OpenAPI support:

1. Install "OpenAPI (Swagger) Editor" plugin
2. Open `http://localhost:8081/v3/api-docs` in browser
3. Copy the JSON
4. Paste into a new `.json` file in IntelliJ
5. Right-click → "Generate REST Client code"

### cURL

Use cURL with OpenAPI specs to test endpoints:

```bash
# Get OpenAPI spec
curl http://localhost:8081/v3/api-docs | jq .

# Test endpoint directly
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}'
```

---

## Security

### Authentication Headers

All protected endpoints require:

```
Authorization: Bearer <JWT-token>
X-Trace-Id: <trace-id>
```

### Role-Based Access Control (RBAC)

Different endpoints require different user roles:

| Role | Description | Access |
|------|-------------|--------|
| `ROLE_USER` | Regular user | Basic user operations |
| `ROLE_PROFESSIONAL` | Professional trainer | Training program creation, advanced features |
| `ROLE_ADMIN` | Administrator | Full system access, user management |

Each endpoint's documentation specifies required roles.

### Bearer Token Format

JWT tokens are signed with HMAC-SHA256 and include:
- `sub`: user ID (Long as string)
- `roles`: comma-separated roles string (e.g. `"ROLE_USER"` or `"ROLE_USER,ROLE_PROFESSIONAL"`)
- `iat`: issued-at timestamp
- `exp`: expiration timestamp (24h access / 7d refresh)

Example token decoded:
```json
{
  "sub": "123",
  "roles": "ROLE_USER",
  "iat": 1703001600,
  "exp": 1703088000
}
```

---

## Common Response Codes

| Code | Meaning | When |
|------|---------|------|
| **200** | OK | Request succeeded, data returned |
| **201** | Created | Resource created successfully |
| **204** | No Content | Request succeeded, no response body |
| **400** | Bad Request | Invalid input data or parameters |
| **401** | Unauthorized | Authentication required or failed |
| **403** | Forbidden | Authenticated but insufficient permissions |
| **404** | Not Found | Resource does not exist |
| **409** | Conflict | Resource conflict (e.g., duplicate) |
| **422** | Unprocessable Entity | Validation failed on input |
| **500** | Internal Server Error | Server error occurred |

---

## Examples

### Example 1: User Registration

**Endpoint:** `POST /auth/register`

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "role": "ROLE_USER"
}
```

**Response (201 Created):**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "message": "User registered successfully"
}
```

### Example 2: User Login

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful"
}
```

### Example 3: Create Exercise (Protected Endpoint)

**Endpoint:** `POST /training/api/v1/exercises`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request:**
```json
{
  "name": "Bench Press",
  "description": "Chest and triceps compound exercise",
  "disciplineId": 1
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Bench Press",
  "description": "Chest and triceps compound exercise",
  "type": "USER",
  "disciplineId": 1,
  "createdAt": "2026-03-20T10:30:00"
}
```

---

## Troubleshooting

### Swagger UI Not Loading

1. **Check service is running:**
   ```bash
   curl http://localhost:8081/auth/actuator/health
   ```

2. **Verify Swagger dependency:**
   - Check `pom.xml` includes `springdoc-openapi-starter-webmvc-ui`

3. **Clear browser cache:**
   - Hard refresh: `Ctrl+Shift+R` (Windows) or `Cmd+Shift+R` (Mac)

### 401 Unauthorized in Swagger

1. **Token expired:** Get a new token from `/auth/login`
2. **Invalid format:** Ensure token is prefixed with `Bearer `
3. **Wrong URL:** Verify you're testing against correct service URL

### 403 Forbidden

**Insufficient permissions:**
- Verify your user has required role (USER, PROFESSIONAL, ADMIN)
- Check endpoint documentation for required roles
- Login with different user account if needed

---

## Best Practices

1. **Always test in Swagger first** before writing client code
2. **Use "Try it out" feature** to validate request/response format
3. **Copy exact error responses** to understand validation requirements
4. **Save successful requests** from Swagger history
5. **Use OpenAPI spec** for code generation in client libraries
6. **Bookmark service URLs** for quick access during development

---

**Version:** 1.0.0  
**Last Updated:** 2026-03-20  
**Maintained By:** Gym Platform Development Team
