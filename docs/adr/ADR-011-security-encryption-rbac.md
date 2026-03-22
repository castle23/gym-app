# ADR-011: Security, Encryption & RBAC

## Status
Accepted

## Date
2026-03-21

## Context

The Gym Platform handles sensitive user data:
- Personal information (email, name, health metrics)
- Password hashes and authentication tokens
- Workout and diet tracking data
- Potentially payment information (future)

Security requirements:
1. **Data Confidentiality**: Data encrypted at rest and in transit
2. **Authentication**: Users verified (ADR-003 JWT)
3. **Authorization**: Users only access their own data
4. **Integrity**: Data can't be tampered with
5. **Audit**: Track who accessed what data
6. **Compliance**: GDPR, privacy laws

Threat model:
- Man-in-the-middle attacks (intercept traffic)
- Compromised servers (disk stolen)
- SQL injection (data exfiltration)
- Unauthorized access (user A sees user B's data)
- Insider threats (employee steals data)

## Decision

We implemented **defense-in-depth security** with multiple layers:

1. **Network Security**: HTTPS/TLS everywhere
2. **Encryption at Rest**: Database and backup encryption
3. **Authentication**: JWT (ADR-003)
4. **Authorization**: Role-Based Access Control (RBAC)
5. **Data Masking**: Sensitive fields masked in logs
6. **Audit Logging**: Track all access
7. **Secrets Management**: Encrypted keys, rotation

## Rationale

### 1. HTTPS/TLS (Transport Encryption)
Ensures data encrypted in transit:
- API Gateway enforces HTTPS
- All inter-service communication encrypted
- Certificates from trusted CA (Let's Encrypt)
- TLS 1.2+ only (disable old, weak versions)

### 2. Database Encryption (Rest Encryption)
PostgreSQL data at rest:
- `pgcrypto` extension for column-level encryption
- Sensitive columns encrypted: passwords, SSN, health data
- Encryption key in separate secrets vault
- AWS RDS: Enable encryption at rest (EBS encryption)

### 3. Authentication (JWT)
See ADR-003 for details:
- Users authenticate once (provide credentials)
- Receive JWT token with 24-hour expiration
- JWT validated at API Gateway
- Can't forge without signing key

### 4. Authorization (RBAC)
Users have roles with permissions:

```
User Roles:
├── ROLE_ADMIN       (full access, manage all users)
├── ROLE_PROFESSIONAL (access to professional endpoints)
└── ROLE_USER        (access to own data only)
```

Roles are stored in the `users` table and embedded in the JWT as a comma-separated string (`roles` claim). The API Gateway injects them as `X-User-Roles` header; services enforce access via `@RequiresRole` annotation and `GymRoleInterceptor`.

### 5. Data Masking
In logs and error messages:
- Password hashes never logged
- Email addresses masked: `u***@example.com`
- Health metrics masked in error messages
- PII (Personally Identifiable Information) never in logs

### 6. Audit Logging
Track access:
- Who accessed what data
- When (timestamp)
- From where (IP address)
- Action (read, write, delete)
- Outcome (success, failure)

Example:
```
2026-03-21 10:15:23 user-789 read /profile/me SUCCESS ip-10.0.1.5
2026-03-21 10:15:45 user-789 write /workouts POST SUCCESS ip-10.0.1.5
2026-03-21 10:16:01 user-456 read /profile/user-789 DENIED ip-10.0.2.3
```

### 7. Secrets Management
Encryption keys protected:
- Stored in Kubernetes Secrets (encrypted at rest)
- Or use HashiCorp Vault for key management
- Rotated regularly (every 90 days)
- Never in source code or logs
- Only loaded at runtime

## Consequences

### Positive
- ✅ Data confidentiality (encrypted)
- ✅ Strong authentication (JWT)
- ✅ Fine-grained authorization (RBAC)
- ✅ Audit trail (compliance)
- ✅ Industry standard security
- ✅ User data protected

### Negative
- ❌ Performance overhead (encryption/decryption)
- ❌ Operational complexity (key management)
- ❌ More infrastructure (secrets manager)
- ❌ Learning curve (security concepts)
- ❌ False sense of security (if not implemented correctly)

## Alternatives Considered

### 1. Minimal Security
- **Pros**: Simple, low overhead
- **Cons**: Data breach risk, user data exposed
- **Why not**: Unacceptable risk

### 2. Only Encryption in Transit
- **Pros**: Simpler, less overhead
- **Cons**: If database compromised, all data exposed
- **Why not**: Need defense-in-depth

### 3. All Encrypted All The Time
- **Pros**: Maximum security
- **Cons**: Extreme performance impact, unusable
- **Why not**: Encrypt sensitive columns, not everything

## Related ADRs

- **Depends on**: ADR-003 (JWT authentication)
- **Depends on**: ADR-004 (Kubernetes Secrets)
- **Related to**: ADR-002 (Database security)
- **Related to**: ADR-005 (Monitor security events)

## Implementation Details

### HTTPS/TLS Setup

```yaml
# Kubernetes Ingress with TLS
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: gym-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.gym.com
    secretName: gym-tls-cert
  rules:
  - host: api.gym.com
    http:
      paths:
      - path: /
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
```

### Database Column Encryption

```sql
-- Create extension
CREATE EXTENSION pgcrypto;

-- Create encryption key function
CREATE OR REPLACE FUNCTION encrypt_password(plaintext text) RETURNS bytea AS $$
BEGIN
  RETURN pgp_sym_encrypt(plaintext, current_setting('app.encryption_key'));
END;
$$ LANGUAGE plpgsql;

-- Create user table with encrypted password
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  password_hash BYTEA NOT NULL,  -- Encrypted
  health_data JSONB NOT NULL,    -- Encrypted (optional)
  created_at TIMESTAMP
);

-- Encrypt when storing
INSERT INTO users (id, email, password_hash) 
VALUES (
  gen_random_uuid(),
  'user@example.com',
  encrypt_password('mypassword123')
);
```

### RBAC Configuration

```java
// gym-common auto-configures security for all services
// GymSecurityAutoConfiguration permits actuator + swagger, requires auth for the rest
// Services enforce role-level access with @RequiresRole annotation:

@RequiresRole("ROLE_ADMIN")
@GetMapping("/admin/users")
public ResponseEntity<?> listUsers() { ... }

// GymRoleInterceptor reads X-User-Roles header injected by the API Gateway
// and populates UserContextHolder for the current request
```

### Secrets Management

```yaml
# Kubernetes Secret (encrypted at rest)
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  jwt-secret: "your-super-secret-key-change-this"
  db-password: "postgres-password"
  encryption-key: "database-encryption-key"
  aws-access-key: "AKIA..."
  aws-secret-key: "..."

---
# Pod using secrets
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: gym/app:latest
    env:
    - name: JWT_SECRET
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: jwt-secret
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: db-password
```

### Audit Logging

```java
// Aspect to log all API access
@Aspect
@Component
public class AuditAspect {
    
    @Around("@annotation(audit)")
    public Object logAudit(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String action = audit.action();
        String resource = pjp.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = pjp.proceed();
            logger.info("AUDIT: user={} action={} resource={} result=SUCCESS time={}ms",
                user, action, resource, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            logger.warn("AUDIT: user={} action={} resource={} result=FAILURE error={}",
                user, action, resource, e.getMessage());
            throw e;
        }
    }
}
```

### Data Masking in Logs

```java
// Mask sensitive data before logging
public static String maskSensitiveData(String data) {
    return data
        .replaceAll("(?<=.{2}).(?=.{2}@)", "*")  // Email: u***@example.com
        .replaceAll("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b", "****-****-****-****")  // Cards
        .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");  // Passwords
}
```

## Security Checklist

- [ ] All traffic HTTPS/TLS
- [ ] Database encryption enabled
- [ ] JWT validation on all endpoints
- [ ] RBAC rules defined and tested
- [ ] Sensitive data masked in logs
- [ ] Audit logging implemented
- [ ] Secrets encrypted and rotated
- [ ] SQL injection prevented (parameterized queries)
- [ ] CSRF protection enabled
- [ ] XSS protection enabled
- [ ] Security headers set (HSTS, CSP, etc.)
- [ ] Regular security audits scheduled
- [ ] Penetration testing performed

## Future Considerations

- Implement OAuth 2.0/OpenID Connect for federated auth
- Add multi-factor authentication (MFA)
- Add API rate limiting (already at gateway)
- Implement end-to-end encryption (user app to database)
- Regular penetration testing
- Security training for team
- Implement backup encryption with customer-managed keys
