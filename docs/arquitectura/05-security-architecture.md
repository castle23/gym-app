# Security Architecture

## Overview

Gym Platform uses **Spring Security 6.x** with **JWT** for authentication and **RBAC** for authorization. Security is centralized in `gym-common` as a Spring Boot auto-configuration, consumed by all services.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                          │
│  JwtAuthFilter (GlobalFilter, WebFlux)                      │
│  - Validates JWT on every request                           │
│  - Injects X-User-Id / X-User-Roles headers downstream      │
│  - PUBLIC_PATHS bypass JWT check                            │
└──────────────────────────┬──────────────────────────────────┘
                           │ forwards with X-User-* headers
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │  Training   │  │  Tracking   │  │Notification │
   │  Service    │  │  Service    │  │  Service    │
   │             │  │             │  │             │
   │GymSecurity  │  │GymSecurity  │  │GymSecurity  │
   │AutoConfig   │  │AutoConfig   │  │AutoConfig   │
   └─────────────┘  └─────────────┘  └─────────────┘
          ▲
   ┌─────────────┐
   │Auth Service │
   │             │
   │Own Security │  ← Custom SecurityConfig (CSRF enabled)
   │Config       │
   └─────────────┘
```

## gym-common Security Auto-configuration

All services (except auth) use `GymSecurityAutoConfiguration` from `gym-common`:

- Active on `@Profile("!test")`
- `@ConditionalOnMissingBean(SecurityFilterChain)` — overridable per service
- Always permits: Swagger UI, OpenAPI docs, all Actuator endpoints (`EndpointRequest.toAnyEndpoint()`)
- Additional public paths via `gym.security.public-paths` in each service's `application.yml`
- Stateless sessions, CSRF disabled, HTTP Basic fallback

```yaml
# Example: training-service application.yml
gym:
  security:
    public-paths:
      - /api/v1/exercises/system
      - /api/v1/exercises/discipline/**
      - /api/v1/routine-templates/system
```

## Auth Service — Custom SecurityConfig

Auth service overrides the common config with its own `SecurityConfig` because it needs CSRF enabled (cookie-based CSRF token for browser clients):

- CSRF with `CookieCsrfTokenRepository` (ignored on `/**` for API calls)
- Same public paths pattern via `GymSecurityProperties`
- Actuator endpoints always public via `EndpointRequest.toAnyEndpoint()`

## JWT Flow

```
1. Client → POST /auth/login
2. Auth Service validates credentials, returns { token, refreshToken, userId, email }
3. Client → any request with Authorization: Bearer <token>
4. API Gateway JwtAuthFilter:
   a. Parses and validates JWT (HMAC-SHA256)
   b. Extracts userId + roles from claims
   c. Injects X-User-Id and X-User-Roles headers
   d. Forwards to downstream service
5. Downstream service:
   a. GymRoleInterceptor reads X-User-Id / X-User-Roles
   b. Populates UserContextHolder + MDC userId
   c. @RequiresRole annotations enforce RBAC
```

## JWT Configuration

`GymJwtAutoConfiguration` activates only in auth-service (`@ConditionalOnProperty("gym.jwt.issuer")`):

```yaml
# auth-service only
gym:
  jwt:
    issuer: gym-platform
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000       # 24h
  refresh-expiration-ms: 604800000  # 7d
```

All other services only need `jwt.secret` to verify tokens (no `GymJwtAutoConfiguration` activation).

## API Gateway Public Paths

The gateway `JwtAuthFilter` maintains a `PUBLIC_PATHS` list checked via `startsWith`:

```
/auth/register, /auth/login, /auth/verify
/auth/actuator, /training/actuator, /tracking/actuator, /notifications/actuator
/auth/swagger-ui, /auth/v3/api-docs
/training/swagger-ui, /training/v3/api-docs
/tracking/swagger-ui, /tracking/v3/api-docs
/notifications/swagger-ui, /notifications/v3/api-docs
```

## RBAC Roles

| Role | Description |
|------|-------------|
| `ROLE_USER` | Standard gym member |
| `ROLE_PROFESSIONAL` | Trainer / nutritionist |
| `ROLE_ADMIN` | Full platform access |

Enforced via `@PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL', 'ADMIN')")` at controller level.

## Test Security

`@WebMvcTest` slices do not load auto-configurations. All controller tests must explicitly import:

```java
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
```

`GymTestSecurityAutoConfiguration` is active on `@Profile("test")` and permits all requests.
