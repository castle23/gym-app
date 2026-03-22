# API Authentication

## Overview

All services use JWT (JSON Web Tokens) for stateless authentication. The API Gateway validates tokens centrally and injects user context headers to downstream services.

## Authentication Flow

```
1. Client → POST /auth/login  { email, password }
2. Auth Service validates credentials (BCrypt password check)
3. Returns accessToken (24h) + refreshToken (7d)
4. Client sends: Authorization: Bearer <accessToken>
5. API Gateway (JwtAuthFilter):
   - Validates JWT signature (HMAC-SHA256)
   - Extracts userId + roles from claims
   - Injects X-User-Id and X-User-Roles headers
   - Forwards request to downstream service
6. Downstream service (GymRoleInterceptor):
   - Reads X-User-Id / X-User-Roles
   - Populates UserContextHolder
   - @PreAuthorize enforces RBAC
```

## Auth Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | No | Register new user |
| POST | `/auth/login` | No | Authenticate, get tokens |
| POST | `/auth/verify` | No | Verify email address |
| POST | `/auth/refresh` | Yes | Refresh access token |
| GET | `/auth/profile` | Yes | Get current user profile |

## Register

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Jane", "lastName": "Smith", "email": "jane@example.com", "password": "SecurePassword123!"}'
```

**Response (201)**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "success": true,
  "message": "User registered successfully"
}
```

## Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane@example.com", "password": "SecurePassword123!"}'
```

**Response (200)**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "success": true,
  "message": "Login successful"
}
```

## Refresh Token

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJhbGciOiJIUzI1NiJ9..."}'
```

**Response (200)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "success": true
}
```

## JWT Token Structure

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "roles": "ROLE_USER",
  "iat": 1647900000,
  "exp": 1647986400
}
```

- `sub` — userId (UUID)
- `roles` — comma-separated roles string
- Expiration: 24h access / 7d refresh

## Roles

| Role | Description |
|------|-------------|
| `ROLE_USER` | Standard gym member |
| `ROLE_PROFESSIONAL` | Trainer / nutritionist |
| `ROLE_ADMIN` | Full platform access |

## Public Paths (no JWT required)

These paths bypass JWT validation at the gateway:

| Service | Public Paths |
|---------|-------------|
| Auth | `/auth/register`, `/auth/login`, `/auth/verify` |
| Training | `/training/api/v1/exercises/system`, `/training/api/v1/exercises/discipline/**`, `/training/api/v1/routine-templates/system` |
| Tracking | `/tracking/api/v1/diet-components/**`, `/tracking/api/v1/training-components/**` |
| All | `/{service}/actuator/**`, `/{service}/swagger-ui/**`, `/{service}/v3/api-docs/**` |
