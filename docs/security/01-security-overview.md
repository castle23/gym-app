# Security Overview

## Executive Summary

The Gym Platform implements a comprehensive, defense-in-depth security architecture protecting user data, system integrity, and compliance requirements. This document outlines the security model, threat landscape, and architectural principles guiding all security decisions across Auth, Training, Tracking, and Notification microservices.

**Security Posture:**
- **Authentication**: JWT-based with bcrypt password hashing
- **Authorization**: Role-based access control (RBAC) with fine-grained permissions
- **Data Protection**: Encrypted at rest (PostgreSQL) and in transit (TLS 1.2+)
- **Audit**: Comprehensive logging of security-relevant events
- **Compliance**: GDPR-ready with data retention policies

---

## Table of Contents

1. [Security Architecture](#security-architecture)
2. [Threat Model](#threat-model)
3. [Security Principles](#security-principles)
4. [Defense in Depth](#defense-in-depth)
5. [Security Boundaries](#security-boundaries)
6. [Risk Assessment](#risk-assessment)
7. [Security Governance](#security-governance)

---

## Security Architecture

### Layered Security Model

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: Perimeter (Network)                                │
│ - TLS/HTTPS everywhere                                      │
│ - Firewall rules, Security Groups                           │
│ - DDoS protection (optional)                                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: Application Authentication                         │
│ - JWT token validation                                      │
│ - API key verification                                      │
│ - Session management                                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: Authorization & Access Control                     │
│ - RBAC checks                                               │
│ - Permission validation                                     │
│ - Resource ownership verification                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 4: Data Protection                                    │
│ - Field-level encryption                                    │
│ - Redaction of sensitive data                               │
│ - Database constraints and triggers                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 5: Audit & Monitoring                                 │
│ - Security event logging                                    │
│ - Anomaly detection                                         │
│ - Incident response procedures                              │
└─────────────────────────────────────────────────────────────┘
```

### Component Security Responsibilities

**Auth Service:**
- User registration and login
- JWT token generation and validation
- Password reset and recovery
- Multi-factor authentication (future)

**Training Service:**
- Training data access control
- Session-level authorization
- Exercise data protection
- Performance tracking isolation

**Tracking Service:**
- Workout metrics protection
- User statistics isolation
- Progress tracking encryption
- Historical data retention

**Notification Service:**
- Secure message delivery
- User preference privacy
- Channel authentication
- Rate limiting

---

## Threat Model

### Assets to Protect

1. **User Credentials**
   - Passwords (must be hashed)
   - API keys (must be encrypted)
   - Secrets (must be rotated)

2. **User Data**
   - Personal information (PII)
   - Training history
   - Performance metrics
   - Health information

3. **System Infrastructure**
   - Database integrity
   - Service availability
   - Configuration data
   - Logs and audit trails

### Attack Vectors

#### 1. Authentication Bypass

**Threat**: Attacker gains unauthorized access by:
- Cracking weak passwords
- Exploiting JWT validation weaknesses
- Reusing expired tokens
- Brute-forcing login endpoints

**Mitigation**:
- Enforce strong password requirements (12+ chars, complexity)
- Implement rate limiting on login attempts (5 attempts per 15 min)
- Use secure JWT signing (HS256+)
- Set reasonable token expiration (24 hours for web, 1 hour for sensitive)
- Implement token refresh mechanism

#### 2. Privilege Escalation

**Threat**: User with limited permissions gains higher privileges:
- Modifying JWT claims client-side
- Exploiting authorization logic flaws
- Direct object reference (modifying userId in requests)
- Horizontal privilege escalation (accessing other users' data)

**Mitigation**:
- Never trust client-submitted claims
- Validate authorization server-side always
- Use immutable claims signed by server
- Implement proper access checks on all resources
- Use user IDs from JWT, not request parameters

#### 3. Data Breaches

**Threat**: Unauthorized access to sensitive data:
- SQL injection extracting user data
- Unencrypted data in transit
- Database backups exposed
- Logs containing sensitive info

**Mitigation**:
- Use parameterized queries (eliminate SQL injection)
- TLS 1.2+ for all data in transit
- Encrypt database backups
- Redact sensitive data from logs
- Implement field-level encryption for PII

#### 4. Denial of Service (DoS)

**Threat**: System becomes unavailable:
- Login endpoint bombarding
- Large payload uploads
- Resource exhaustion attacks
- API rate limit abuse

**Mitigation**:
- Implement rate limiting per IP/user
- Set request size limits (100KB body limit)
- Implement timeouts (30 sec max request time)
- Use circuit breakers for external services
- Monitor resource usage with alerts

#### 5. Man-in-the-Middle (MITM)

**Threat**: Attacker intercepts communication:
- Downgrade to HTTP
- Invalid SSL certificate acceptance
- DNS hijacking
- Network traffic sniffing

**Mitigation**:
- Enforce TLS 1.2+ only
- Use valid certificates from trusted CAs
- Implement HSTS headers
- Validate certificate chains
- Use certificate pinning for critical services

#### 6. Insider Threats

**Threat**: Malicious or compromised internal users:
- Excessive data access
- Privilege abuse
- Configuration tampering
- Credential theft

**Mitigation**:
- Principle of least privilege
- Comprehensive audit logging
- Segregation of duties
- Regular access reviews
- Multi-factor authentication for admins

---

## Security Principles

### 1. Principle of Least Privilege

Users and services have minimum permissions needed:

```java
// Before: Too permissive
@PreAuthorize("hasRole('USER')")  // All logged-in users
public ResponseEntity<List<User>> getAllUsers() {
    return ResponseEntity.ok(userRepository.findAll());
}

// After: Least privilege
@PreAuthorize("hasRole('ADMIN')")  // Only admins
public ResponseEntity<List<User>> getAllUsers() {
    return ResponseEntity.ok(userRepository.findAll());
}

// User can only access own data
@PreAuthorize("#userId == authentication.principal.id")
public ResponseEntity<UserProfile> getProfile(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getProfile(userId));
}
```

### 2. Defense in Depth

Multiple layers of security so single failure doesn't compromise system:

- **Network**: Firewall rules
- **Application**: Input validation, output encoding
- **Data**: Encryption, access control
- **Operations**: Monitoring, incident response

### 3. Fail Secure

System defaults to denying access when uncertain:

```java
// Before: Fails open (assumes permission if check missing)
public void deleteUser(Long userId) {
    userRepository.deleteById(userId);
}

// After: Fails secure (requires explicit permission)
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public void deleteUser(@PathVariable Long userId) {
    userRepository.deleteById(userId);
}
```

### 4. Never Trust User Input

All user input is potentially malicious:

```java
// Before: Trusts user input
String query = "SELECT * FROM users WHERE username = '" + request.getUsername() + "'";
List<User> users = em.createNativeQuery(query).getResultList();

// After: Parameterized query (safe from injection)
@Query("SELECT u FROM User u WHERE u.username = ?1")
List<User> findByUsername(String username);
```

### 5. Separation of Duties

Critical operations require multiple approvals:

- Password reset requires email verification
- Admin actions logged and auditable
- Financial transactions require dual authorization
- Sensitive data access requires multi-factor auth

### 6. Secure by Default

Default configuration is secure:

```properties
# application.properties - Secure defaults
server.ssl.enabled=true
server.port=8443
spring.jpa.hibernate.ddl-auto=validate  # No auto-create
spring.datasource.hikari.maximum-pool-size=10
logging.level.org=WARN  # Don't log everything
```

---

## Defense in Depth

### Strategy: Multiple Independent Security Controls

No single point of failure:

| Layer | Control | Example |
|-------|---------|---------|
| Perimeter | Network isolation | Private subnets, security groups |
| Transport | Encryption in transit | TLS 1.2+, HTTPS only |
| Authentication | Credential verification | JWT + bcrypt hashing |
| Authorization | Access control | RBAC with fine-grained permissions |
| Data | Encryption at rest | Database encryption, PII masking |
| Audit | Event logging | Security events, failed attempts |
| Operations | Monitoring & Response | Alerts, incident procedures |

### Example: Protecting User Password Reset

```
Layer 1: Network
  ↓
Layer 2: HTTPS/TLS
  ↓
Layer 3: Rate limiting (5 requests/hour)
  ↓
Layer 4: Verify email ownership
  ↓
Layer 5: Token validation (10 min expiry)
  ↓
Layer 6: Password complexity check
  ↓
Layer 7: Audit log entry
  ↓
Layer 8: User notification email
```

---

## Security Boundaries

### Service Boundaries

```
┌────────────────────────────────────────────────────────┐
│                  INTERNET (Untrusted)                  │
└────────────────────────────────────────────────────────┘
                            ↑
                    (TLS + API Gateway)
                            ↓
┌────────────────────────────────────────────────────────┐
│        Auth Service  │  Training  │  Tracking  │        │
│      (JWT issuer)    │  Tracking  │ Notification        │
│                                                        │
│  (Internal Network - Trusted)                          │
└────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────┐
│  PostgreSQL (Encrypted, no external access)            │
│  RabbitMQ (Internal messaging only)                    │
└────────────────────────────────────────────────────────┘
```

### Cross-Service Communication

- Services authenticate with JWT
- Service-to-service calls use internal network
- No service should trust another service implicitly
- All inter-service communication logged

---

## Risk Assessment

### Risk Matrix

```
          Low Impact    Medium Impact    High Impact
High Prob  MEDIUM       HIGH            CRITICAL
Med Prob   LOW          MEDIUM          HIGH
Low Prob   LOW          LOW             MEDIUM
```

### Critical Risks (CRITICAL Priority)

1. **Unencrypted data in transit**
   - Impact: High (data exposure)
   - Probability: Low (TLS enforced)
   - Mitigation: Mandatory HTTPS, TLS 1.2+

2. **SQL Injection vulnerability**
   - Impact: High (all data at risk)
   - Probability: Medium (requires code review)
   - Mitigation: Parameterized queries, ORM usage

3. **Authentication bypass**
   - Impact: High (system compromise)
   - Probability: Medium (cryptographic complexity)
   - Mitigation: Security review, penetration testing

### High Risks (HIGH Priority)

1. **Weak password policies**
   - Impact: Medium (individual account compromise)
   - Probability: High (user convenience vs security)
   - Mitigation: Enforce complexity, rate limiting

2. **Unvalidated redirects**
   - Impact: Medium (phishing attacks)
   - Probability: High (easy to miss in code review)
   - Mitigation: Whitelist allowed redirects

3. **Cross-Site Scripting (XSS)**
   - Impact: Medium (session hijacking)
   - Probability: Medium (depends on input handling)
   - Mitigation: Output encoding, Content Security Policy

### Medium Risks (MEDIUM Priority)

1. **Verbose error messages**
   - Impact: Low (information disclosure)
   - Probability: High (default behavior)
   - Mitigation: Generic error messages to users

2. **Missing audit logs**
   - Impact: Medium (cannot investigate incidents)
   - Probability: Low (logs implemented)
   - Mitigation: Comprehensive audit logging

---

## Security Governance

### Security Development Lifecycle (SDL)

```
┌─────────────────────────────────────────────────────────┐
│ 1. Design Phase                                         │
│    - Threat modeling                                    │
│    - Security requirements definition                   │
│    - Cryptography selection                             │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ 2. Development Phase                                    │
│    - Secure coding practices                            │
│    - Input validation                                   │
│    - Error handling                                     │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ 3. Testing Phase                                        │
│    - Security testing                                   │
│    - Penetration testing                                │
│    - DAST (Dynamic Analysis)                            │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ 4. Deployment Phase                                     │
│    - Security verification                              │
│    - Configuration hardening                            │
│    - Access control setup                               │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ 5. Operations Phase                                     │
│    - Security monitoring                                │
│    - Incident response                                  │
│    - Continuous improvement                             │
└─────────────────────────────────────────────────────────┘
```

### Security Roles & Responsibilities

| Role | Responsibility |
|------|-----------------|
| **Security Architect** | Design security architecture, review designs, threat modeling |
| **Developers** | Write secure code, security testing, code review participation |
| **DevOps/SRE** | Infrastructure security, secret management, monitoring |
| **Security Operations** | Incident response, breach investigation, forensics |
| **Security Lead** | Strategy, policy, governance, compliance |

### Security Review Process

1. **Code Review** (Every PR)
   - Check for injection vulnerabilities
   - Verify authentication/authorization
   - Validate input/output handling
   - Review cryptography usage

2. **Security Testing** (Pre-release)
   - Penetration testing by external team
   - Vulnerability scanning
   - Cryptographic analysis
   - Authentication/authorization testing

3. **Compliance Review** (Quarterly)
   - GDPR compliance check
   - Data retention policy verification
   - Access control audit
   - Audit log review

---

## Security Checklist

### Development Phase
- [ ] OWASP Top 10 addressed
- [ ] Input validation on all user inputs
- [ ] Output encoding for HTML/JSON
- [ ] Parameterized queries used
- [ ] No hardcoded secrets
- [ ] Error handling without information disclosure
- [ ] Authentication required for sensitive operations
- [ ] Authorization checks in place
- [ ] Sensitive data not logged
- [ ] Rate limiting on critical endpoints

### Pre-Deployment
- [ ] HTTPS/TLS enforced
- [ ] All secrets externalized
- [ ] Security headers configured (HSTS, CSP, etc.)
- [ ] CORS properly configured
- [ ] Audit logging enabled
- [ ] Default credentials changed
- [ ] Database backups encrypted
- [ ] SSL certificate valid and not self-signed
- [ ] Security headers present
- [ ] Firewall rules configured

### Production Operations
- [ ] Daily security event review
- [ ] Weekly vulnerability scans
- [ ] Monthly penetration testing
- [ ] Quarterly compliance review
- [ ] Incident response plan tested
- [ ] Backup restoration tested quarterly
- [ ] Access control validated monthly
- [ ] Security training completed by all team members

---

## Related Documentation

- [02-authentication.md](02-authentication.md) - JWT implementation, password security
- [03-authorization-rbac.md](03-authorization-rbac.md) - RBAC and access control
- [04-data-security.md](04-data-security.md) - Encryption and data protection
- [05-api-security.md](05-api-security.md) - API security measures
- [06-compliance.md](06-compliance.md) - GDPR and regulatory compliance
- [07-incident-response.md](07-incident-response.md) - Breach response procedures
- [08-vulnerability-management.md](08-vulnerability-management.md) - Vulnerability handling
- docs/troubleshooting/06-security-troubleshooting.md - Security issue diagnosis
- docs/stack/04-security-framework.md - Spring Security implementation
