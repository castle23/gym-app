# ADR-003: JWT for Service Authentication

## Status
Accepted

## Date
2026-03-21

## Context

With microservices architecture (ADR-001), services need to authenticate with each other. The challenge:

1. **Service-to-Service Calls**: Training Service needs to verify requests from API Gateway
2. **User Tokens**: Users need tokens to authenticate API requests
3. **Stateless**: Services shouldn't need to query a central auth database for every request
4. **Scalability**: Solution must work across multiple instances
5. **Security**: Need to prevent token forgery

Traditional approaches:
- Session cookies (doesn't work across services)
- Shared database lookups (every request hits DB)
- OAuth 2.0 (complex, heavyweight)
- Simple tokens (vulnerable to forgery)

## Decision

We chose **JSON Web Tokens (JWT)** for all authentication:

1. **User Auth**: Users get JWT after login
2. **Service Auth**: Services use JWT to call other services
3. **Token Format**: Signed JWT with expiration
4. **Validation**: Services verify JWT signature locally (no central lookup)

## Rationale

### 1. Stateless Authentication
JWTs contain all information needed to validate them. Services don't need to query a database:

```
User: "Here's my JWT"
Service: Validates signature → Trusts the token
(No database lookup needed)
```

### 2. Scalability
JWTs enable:
- Horizontal scaling (any instance can validate)
- No session affinity needed
- Reduced database load

### 3. Standard & Widely Adopted
- RFC 7519 standard
- Supported by all major frameworks
- Easy client integration
- Well-documented

### 4. Works Across Services
A JWT from Auth Service is valid for:
- Training Service
- Tracking Service
- Notification Service
- API Gateway

### 5. Cryptographic Security
JWTs are:
- Signed with private key
- Can't be forged without the key
- Tamper-evident (signature breaks if modified)

## Consequences

### Positive
- ✅ Stateless (no session database needed)
- ✅ Scalable to many services
- ✅ No database hits for validation
- ✅ Works across different services/domains
- ✅ Can include custom claims
- ✅ Easy to implement

### Negative
- ❌ Token revocation is harder (expired tokens can't be revoked early)
- ❌ Token refresh requires new login or refresh token
- ❌ Larger than simple session cookies
- ❌ More complex than basic auth
- ❌ Secrets must be protected carefully

## Alternatives Considered

### 1. Session Cookies + Central Store
- **Pros**: Simple, standard web pattern
- **Cons**: Needs central session database, server affinity, doesn't work well across services
- **Why not**: Doesn't scale well with microservices

### 2. OAuth 2.0
- **Pros**: Industry standard for delegation
- **Cons**: Heavyweight, more complex than needed, overkill for internal services
- **Why not**: JWT sufficient for our use case

### 3. Mutual TLS (mTLS)
- **Pros**: Strong service-to-service authentication
- **Cons**: Complex cert management, more infrastructure overhead
- **Why not**: Good for service mesh, but JWT simpler for our needs

### 4. API Keys
- **Pros**: Simple
- **Cons**: Can't be revoked easily, no expiration, less secure
- **Why not**: JWTs provide better security and lifecycle management

## Related ADRs

- **Depends on**: ADR-001 (Needed for microservices)
- **Depends on**: ADR-004 (JWTs sent over HTTPS in Docker)
- **Related to**: ADR-011 (Key management and encryption)

## Implementation Details

### JWT Structure

```
Header.Payload.Signature

Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (access token):
{
  "sub": "123",          // userId
  "roles": "ROLE_USER",  // comma-separated roles
  "iss": "gym-platform",
  "iat": 1516239022,
  "exp": 1516242622
}

Payload (refresh token):
{
  "sub": "123",
  "iss": "gym-platform",
  "iat": 1516239022,
  "exp": 1517448622      // longer expiration
}

Signature: HMAC-SHA256(header.payload, secret)
```

### Gateway Header Injection

The API Gateway validates the JWT and injects user context as headers before forwarding to services:

```
X-User-Id: 123
X-User-Roles: ROLE_USER
```

Services read these headers via `GymRoleInterceptor` — they do **not** validate JWTs themselves and do **not** call each other directly.

### Best Practices

1. **Short Expiration**: 1-24 hours (not indefinite)
2. **Refresh Tokens**: Separate longer-lived tokens for renewal
3. **Secure Storage**: In HTTP-only cookies or secure storage (never localStorage)
4. **HTTPS Only**: Always transmit over encrypted channels
5. **Secret Rotation**: Regularly rotate signing keys
6. **Claims Validation**: Always verify signature and expiration

### Revocation Strategy

For early revocation (before expiration):
- Maintain a small blacklist cache (Redis)
- Include JTI (JWT ID) in token
- On logout, add token to blacklist
- Blacklist expires after token expiration

## Future Considerations

- Consider OAuth 2.0 if external API partners need access
- Consider additional JWT libraries/standards as security evolves
- Monitor for vulnerabilities in JWT implementation
