# Security Troubleshooting

## Overview

This guide addresses common security issues in the Gym Platform microservices: authentication failures, authorization problems, SSL/TLS certificate issues, and vulnerability remediation. Security issues require immediate attention as they directly impact data protection and compliance.

**Security Priorities:**
- Authentication: Users can only access system with valid credentials
- Authorization: Users can only access data they're permitted to see
- Data Confidentiality: Data encrypted in transit and at rest
- Data Integrity: Data not modified by unauthorized parties
- Audit: All security-relevant actions are logged

---

## Table of Contents

1. [Authentication Issues](#authentication-issues)
2. [Authorization Problems](#authorization-problems)
3. [SSL/TLS Certificate Issues](#ssltls-certificate-issues)
4. [Credential Management](#credential-management)
5. [Vulnerability Remediation](#vulnerability-remediation)
6. [Security Monitoring](#security-monitoring)

---

## Authentication Issues

### Issue: JWT Token Validation Failures

**Symptoms:**
```
401 Unauthorized: Token validation failed
"Invalid token signature" or "Token expired"
```

**Diagnostic Steps:**

1. **Decode and inspect token:**
```bash
# Extract token from Authorization header
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Decode without verification (use jwt.io or online tool)
echo $TOKEN | cut -d. -f1 | base64 -d | jq '.'
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.'
```

2. **Check token expiration:**
```bash
# Extract 'exp' claim and convert Unix timestamp
TOKEN_PAYLOAD=$(echo $TOKEN | cut -d. -f2 | base64 -d)
echo $TOKEN_PAYLOAD | jq '.exp'
date -d @$(jq '.exp' <<< $TOKEN_PAYLOAD)
```

3. **Verify JWT secret in gateway:**
```bash
docker exec api-gateway env | grep JWT_SECRET
# JWT validation only happens in the API Gateway
```

4. **Check service clock synchronization:**
```bash
date && docker exec api-gateway date && docker exec postgres date
# Differences >1 minute will cause token issues
```

5. **Enable JWT debug logging:**
```properties
# application.properties
logging.level.com.gym.security.jwt=DEBUG
```

**Resolution:**

**Fix token signature mismatch:**
```java
// Verify secret is configured in gateway
@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    @Bean
    public JwtProvider jwtProvider() {
        return new JwtProvider(jwtSecret, jwtExpiration);
    }
}
```

**Check environment variable in gateway:**
```bash
docker-compose config | grep JWT_SECRET

# If not set, update docker-compose.yml:
services:
  api-gateway:
    environment:
      - JWT_SECRET=${JWT_SECRET}  # From .env file
```

> **Note**: JWT validation only happens in the API Gateway. Downstream services trust `X-User-Id` and `X-User-Roles` headers.

**Allow clock skew for distributed systems:**
```java
public boolean validateToken(String token) {
    try {
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .setAllowedClockSkewSeconds(60)  // Allow 60 sec skew
            .build()
            .parseClaimsJws(token);
        return true;
    } catch (ExpiredJwtException e) {
        log.error("Token expired: {}", e.getExpiredDate());
        return false;
    }
}
```

---

### Issue: LDAP/OAuth2 Integration Failures

> **Note**: LDAP and OAuth2 are not part of the current platform implementation. The auth service uses email/password with JWT only.

---

## Authorization Problems

### Issue: Users Accessing Resources They Shouldn't

**Symptoms:**
```
User can access admin endpoints
Cross-tenant data visible
Privilege escalation possible
```

**Diagnostic Steps:**

1. **Find authorization annotations:**
```bash
grep -r "@RequiresRole" src/ --include="*.java"
```

2. **Test authorization boundaries:**
```bash
# Get user token
USER_TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass"}' | jq -r '.token')

# Try to access admin endpoint
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8080/admin/users
# Should return 403 Forbidden
```

3. **Check role assignments:**
```bash
docker exec postgres psql -U gym_admin -d gym_db -c \
  "SELECT id, email, roles FROM auth_schema.users ORDER BY email;"
```

**Resolution:**

**Use `@RequiresRole` annotation (correct pattern for this platform):**
```java
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    @RequiresRole("ROLE_ADMIN")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
    
    // Access own data using GymSecurityContext
    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> getUserSessions() {
        Long currentUserId = GymSecurityContext.getCurrentUserId();
        return ResponseEntity.ok(sessionRepository.findByUserId(currentUserId));
    }
}
```

---

## SSL/TLS Certificate Issues

### Issue: SSL Certificate Expired or Invalid

**Symptoms:**
```
curl: (60) SSL certificate problem: certificate has expired
PKIX path building failed
SSL: CERTIFICATE_VERIFY_FAILED
```

**Diagnostic Steps:**

1. **Check certificate validity:**
```bash
# Get certificate details
openssl s_client -connect localhost:8443 < /dev/null | openssl x509 -noout -text

# Check expiration specifically
openssl s_client -connect localhost:8443 < /dev/null | openssl x509 -noout -dates
```

2. **Verify certificate chain:**
```bash
openssl s_client -connect localhost:8443 -showcerts < /dev/null
```

3. **Check keystore:**
```bash
docker exec gym-auth keytool -list -v -keystore /app/keystore.p12 \
  -storepass ${KEYSTORE_PASSWORD} \
  -storetype PKCS12
```

4. **Validate certificate against domain:**
```bash
# Check Subject Alternative Names (SAN)
openssl s_client -connect localhost:8443 < /dev/null | openssl x509 -noout -text | grep -A1 "Subject Alternative Name"
```

**Resolution:**

**Generate self-signed certificate (development only):**
```bash
# Generate new certificate
keytool -genkey -alias gym-cert \
  -keyalg RSA -keysize 2048 \
  -keystore keystore.p12 \
  -storetype PKCS12 \
  -storepass password \
  -validity 365 \
  -dname "CN=localhost,O=Gym,C=US"

# Copy to container
docker cp keystore.p12 gym-auth:/app/keystore.p12
```

**Configure in Spring Boot:**
```properties
# application.properties
server.ssl.key-store=/app/keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=gym-cert
server.ssl.key-password=${KEYSTORE_PASSWORD}
server.ssl.enabled=true
server.port=8443
```

**Renew production certificate:**
```bash
# Request certificate from CA
# Copy certificate and key to server
# Update keystore
keytool -importcert -alias gym-cert-prod \
  -file certificate.crt \
  -keystore keystore.p12 \
  -storepass ${KEYSTORE_PASSWORD} \
  -storetype PKCS12

# Restart service
docker-compose restart gym-auth
```

---

## Credential Management

### Issue: Credentials Exposed in Logs or Version Control

**Symptoms:**
```
Secrets visible in container logs
Passwords in docker-compose.yml
API keys in source code
```

**Diagnostic Steps:**

1. **Search for exposed credentials:**
```bash
# In source code
grep -r "password\|secret\|token\|api.key" src/ --include="*.java" \
  | grep -v ".properties\|.yml\|configuration"

# In logs
docker logs gym-auth | grep -i "password\|secret"

# In environment
docker exec gym-auth env | grep -v "JAVA_\|SPRING_\|PATH"
```

2. **Check git history:**
```bash
# Look for credential patterns
git log --all --oneline | head -20
git diff HEAD~10 | grep -i "password\|secret"
```

3. **Scan files:**
```bash
# Using git secrets
git secrets --scan
git secrets --list
```

**Resolution:**

**Use environment variables for secrets:**
```yaml
# docker-compose.yml - NEVER include actual secrets
services:
  api-gateway:
    environment:
      - JWT_SECRET=${JWT_SECRET}
  postgres:
    environment:
      - POSTGRES_PASSWORD=${DB_PASSWORD}
```

**.env file (NEVER commit to git):**
```bash
# .env - Add to .gitignore
JWT_SECRET=actual-secret-here
DB_PASSWORD=postgres-password-here
```

**.gitignore:**
```
.env
.env.local
secrets/
*.key
*.pem
keystore.p12
```

**Externalize configuration:**
```properties
# application.properties - NEVER include secrets
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

**Use Spring Cloud Config (aspirational):**

> **Note**: Spring Cloud Config is not currently configured in this platform. Secrets are managed via `.env` file and Docker Compose environment variables.

---

## Vulnerability Remediation

### Issue: Known Vulnerabilities in Dependencies

**Symptoms:**
```
Vulnerability scan shows CVE-XXXX-XXXXX
Dependency version outdated
Security patch available
```

**Diagnostic Steps:**

1. **Identify vulnerable dependencies:**
```bash
# Using Maven
mvn dependency-check:check

# Using Gradle
./gradlew dependencyCheckAnalyze

# Using OWASP CLI
dependency-check --project "Gym Platform" --scan .
```

2. **Check specific dependency version:**
```bash
# Find all versions of Log4j in project
mvn dependency:tree | grep log4j
```

3. **Review CVE details:**
```bash
# Check NIST database
curl https://nvd.nist.gov/vuln/detail/CVE-XXXX-XXXXX
```

**Resolution:**

**Update vulnerable dependencies:**
```xml
<!-- pom.xml -->
<!-- Before: Vulnerable version -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.13.0</version>  <!-- Vulnerable -->
</dependency>

<!-- After: Patched version -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.0</version>  <!-- Patched -->
</dependency>
```

**Test after update:**
```bash
# Run tests
mvn clean test

# Run security scan
mvn dependency-check:check

# Build and verify
mvn clean package
```

**Create security patch process:**
1. Scan dependencies weekly
2. Create issue for vulnerabilities
3. Update pom.xml
4. Run tests in isolation
5. Build and test on staging
6. Deploy to production

---

## Security Monitoring

### Enable Security Audit Logging

**Audit successful and failed authentication:**
```java
@Component
public class SecurityAuditListener {
    
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("AUDIT: Authentication successful for user: {}", username);
        // Could also write to audit table
    }
    
    @EventListener
    public void onFailure(AuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        log.warn("AUDIT: Authentication failed for user: {}", username);
    }
}
```

**Log authorization denials:**
```java
@Component
public class AccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        log.warn("AUDIT: Access denied for user: {} on resource: {}",
            username, request.getRequestURI());
    }
}
```

### Monitor Security Metrics

**Track failed login attempts:**
```bash
# Count failed logins in last hour
docker logs auth-service | grep "AUDIT: Authentication failed" | \
  awk -F'user: ' '{print $2}' | sort | uniq -c
```

**Alert on suspicious activity:**
```properties
# Configure alerts in monitoring
- Multiple failed logins from same IP
- Privilege escalation attempts
- Unusual API call patterns
```

---

## Security Checklist

- [ ] All services use HTTPS (8443)
- [ ] JWT secrets identical across services
- [ ] Passwords hashed (BCrypt minimum)
- [ ] Authorization annotations on protected endpoints
- [ ] No secrets in logs
- [ ] No secrets in version control
- [ ] SSL certificates valid and not expired
- [ ] Dependencies scanned for vulnerabilities
- [ ] Audit logging enabled
- [ ] Sensitive data encrypted at rest (if applicable)

---

## Related Documentation

- [02-debugging-techniques.md](02-debugging-techniques.md) - Debug security issues
- [03-common-issues.md](03-common-issues.md) - Common security problems
- [04-diagnostic-procedures.md](04-diagnostic-procedures.md) - Security diagnostics
- docs/stack/04-security-framework.md - Security architecture
- docs/security/ - Comprehensive security documentation
