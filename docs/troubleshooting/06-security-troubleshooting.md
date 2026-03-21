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

3. **Verify JWT secret across services:**
```bash
# All services must share same JWT_SECRET
docker exec gym-auth env | grep JWT_SECRET
docker exec gym-training env | grep JWT_SECRET
docker exec gym-tracking env | grep JWT_SECRET
# Should all be identical
```

4. **Check service clock synchronization:**
```bash
# All services must have synchronized clocks
date && docker exec gym-auth date && docker exec postgres date
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
// Verify same secret is used everywhere
@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")  // 24 hours default
    private long jwtExpiration;
    
    @Bean
    public JwtProvider jwtProvider() {
        return new JwtProvider(jwtSecret, jwtExpiration);
    }
}
```

**Check environment variable:**
```bash
# Ensure JWT_SECRET is set
docker-compose config | grep JWT_SECRET

# If not set, update docker-compose.yml:
services:
  gym-auth:
    environment:
      - JWT_SECRET=${JWT_SECRET}  # From .env file
```

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

**Symptoms:**
```
LDAP connection refused
OAuth2 provider unreachable
"Failed to obtain access token"
```

**Diagnostic Steps:**

1. **Test LDAP connectivity:**
```bash
# Test LDAP connection
docker exec gym-auth ldapsearch -H ldap://ldap.example.com:389 \
  -D "cn=admin,dc=example,dc=com" \
  -w password \
  -b "dc=example,dc=com" \
  "uid=testuser"
```

2. **Check OAuth2 configuration:**
```bash
# Verify OAuth2 endpoints are accessible
curl https://oauth2.provider.com/.well-known/openid-configuration
```

3. **Verify credentials:**
```bash
# Check OAuth2 client ID and secret
docker exec gym-auth env | grep -i "oauth2\|ldap"
```

4. **Check SSL certificate for LDAP/OAuth2:**
```bash
openssl s_client -connect ldap.example.com:636
openssl s_client -connect oauth2.provider.com:443
```

**Resolution:**

**Configure LDAP correctly:**
```properties
# application.properties
spring.ldap.urls=ldap://ldap.example.com:389
spring.ldap.base=dc=example,dc=com
spring.ldap.username=cn=admin,dc=example,dc=com
spring.ldap.password=${LDAP_PASSWORD}

# Configure user search
spring.ldap.user.search.base=ou=users
spring.ldap.user.search.filter=uid={0}
```

**Configure OAuth2:**
```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          gym-oauth2:
            client-id: ${OAUTH2_CLIENT_ID}
            client-secret: ${OAUTH2_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: http://localhost:8080/login/oauth2/code/gym-oauth2
        provider:
          gym-oauth2:
            authorization-uri: https://oauth2.provider.com/oauth/authorize
            token-uri: https://oauth2.provider.com/oauth/token
            user-info-uri: https://oauth2.provider.com/oauth/user
            jwk-set-uri: https://oauth2.provider.com/.well-known/jwks.json
```

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
grep -r "@PreAuthorize\|@Secured\|@RolesAllowed" src/ --include="*.java"
```

2. **Verify method-level security is enabled:**
```bash
# Check for @EnableGlobalMethodSecurity
grep -r "@EnableGlobalMethodSecurity\|@EnableMethodSecurity" src/ --include="*.java"
```

3. **Test authorization boundaries:**
```bash
# Get user token
USER_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}' | jq -r '.token')

# Try to access admin endpoint
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8080/api/admin/users
# Should return 403 Forbidden
```

4. **Check role assignments:**
```bash
docker exec postgres psql -U gym_user -d gym_db -c \
  "SELECT u.username, r.name FROM users u JOIN user_roles ur ON u.id = ur.user_id JOIN roles r ON ur.role_id = r.id ORDER BY u.username;"
```

**Resolution:**

**Implement proper authorization annotations:**
```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    // Only ADMIN role can access
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
    
    // Multiple roles allowed
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable Long id) {
        userService.suspend(id);
        return ResponseEntity.ok().build();
    }
    
    // User can only access their own data
    @PreAuthorize("#userId == authentication.principal.id")
    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<List<Session>> getUserSessions(@PathVariable Long userId) {
        return ResponseEntity.ok(sessionRepository.findByUserId(userId));
    }
}
```

**Enable method-level security:**
```java
@Configuration
@EnableMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class SecurityConfig {
    // Configuration
}
```

**Verify tenant isolation (multi-tenant):**
```java
@Service
public class SessionService {
    
    public List<Session> getUserSessions(Long userId) {
        // Get current user's organization/tenant
        Long currentUserId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal().getId();
        
        // Verify user is in same organization
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        User targetUser = userRepository.findById(userId).orElseThrow();
        
        if (!currentUser.getOrganizationId().equals(targetUser.getOrganizationId())) {
            throw new UnauthorizedException("Cannot access other organization's data");
        }
        
        return sessionRepository.findByUserId(userId);
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
  gym-auth:
    environment:
      - JWT_SECRET=${JWT_SECRET}  # Loaded from .env
      - DB_PASSWORD=${DB_PASSWORD}
      - OAUTH2_CLIENT_SECRET=${OAUTH2_CLIENT_SECRET}
```

**.env file (NEVER commit to git):**
```bash
# .env - Add to .gitignore
JWT_SECRET=actual-secret-here
DB_PASSWORD=postgres-password-here
OAUTH2_CLIENT_SECRET=oauth-secret-here
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
oauth2.client.secret=${OAUTH2_CLIENT_SECRET}
```

**Use Spring Cloud Config (production):**
```yaml
# docker-compose.yml
services:
  config-server:
    image: spring-cloud-config-server
    ports:
      - "8888:8888"
    environment:
      - SPRING_CLOUD_CONFIG_SERVER_GIT_URI=https://secure-git-repo
      - SPRING_CLOUD_CONFIG_SERVER_GIT_USERNAME=${GIT_USER}
      - SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD=${GIT_TOKEN}

  gym-auth:
    environment:
      - SPRING_CONFIG_IMPORT=configserver:http://config-server:8888
```

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
docker logs gym-auth | grep "AUDIT: Authentication failed" | \
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
