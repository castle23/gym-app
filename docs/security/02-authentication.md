# Authentication

## Overview

This guide covers user authentication mechanisms in the Gym Platform: JWT token implementation, password security, credential management, and session handling. Authentication is the foundation of security - if this fails, all other controls are bypassed.

> **Note**: This document describes the implemented JWT/password authentication. Sections on OAuth2 and MFA describe future/aspirational features not currently implemented.

**Authentication Methods:**
- JWT (JSON Web Tokens) - validated at API Gateway only
- Password-based login with bcrypt hashing
- Optional (not implemented): OAuth2, MFA

---

## Table of Contents

1. [JWT Implementation](#jwt-implementation)
2. [Password Security](#password-security)
3. [Session Management](#session-management)
4. [OAuth2 Integration](#oauth2-integration)
5. [MFA Implementation](#mfa-implementation)
6. [Credential Management](#credential-management)

---

## JWT Implementation

### JWT Structure

```
Header.Payload.Signature

Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "123",           // userId as string (Long)
  "roles": "ROLE_USER",   // ROLE_USER, ROLE_PROFESSIONAL, ROLE_ADMIN (comma-separated string)
  "exp": 1680456000,
  "iat": 1680369600
}

Signature: HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

### JWT Configuration

JWT is generated and validated in the auth-service (`JwtService`) and validated at the API Gateway (`JwtAuthFilter`). Downstream services do not validate JWT — they trust the `X-User-Id` and `X-User-Roles` headers injected by the gateway.

```yaml
# auth-service application.yml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  expiration-ms: 86400000       # 24h
  refresh-expiration-ms: 604800000  # 7d
```

### Login Endpoint

The auth service context-path is `/auth`, so the login endpoint is accessible at `/auth/login` via the API Gateway.

```
POST /auth/login
Body: { "email": "user@example.com", "password": "SecurePassword123!" }

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "email": "user@example.com",
  "message": "Login successful"
}
```

### Token Validation Filter (API Gateway only)

JWT validation happens exclusively in the API Gateway (`JwtAuthFilter`). Downstream services do **not** validate JWT — they read the `X-User-Id` and `X-User-Roles` headers injected by the gateway.

```java
// In API Gateway: JwtAuthFilter validates token and injects headers
// X-User-Id: 123
// X-User-Roles: ROLE_USER

// In downstream services: GymRoleInterceptor reads injected headers
// No JWT validation occurs in training-service, tracking-service, etc.
```

---

## Password Security

### Password Requirements

```properties
# application.properties
password.min.length=12
password.require.uppercase=true
password.require.lowercase=true
password.require.digits=true
password.require.special.chars=true
password.expiration.days=90  # Require change every 90 days
```

### Password Validator

```java
@Component
public class PasswordValidator {
    
    public void validate(String password) throws PasswordValidationException {
        if (password.length() < 12) {
            throw new PasswordValidationException("Password must be at least 12 characters");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new PasswordValidationException("Password must contain uppercase letters");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new PasswordValidationException("Password must contain lowercase letters");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new PasswordValidationException("Password must contain digits");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new PasswordValidationException("Password must contain special characters");
        }
    }
}
```

### Bcrypt Password Encoding

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Bcrypt with strength 12 (takes ~100ms to hash - balances security and performance)
        return new BCryptPasswordEncoder(12);
    }
}

@Service
public class UserService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public void registerUser(UserRegistrationRequest request) {
        // Validate password strength
        passwordValidator.validate(request.getPassword());
        
        // Hash password with bcrypt
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(encodedPassword);  // Store hash, not plaintext
        user.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }
}
```

### Password Reset Flow

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Generate reset token (valid for 15 minutes)
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        
        // Send reset link via email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getResetToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));
        
        // Verify token not expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Reset token expired");
        }
        
        // Validate new password
        passwordValidator.validate(request.getNewPassword());
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
}
```

---

## Session Management

### Session Configuration

```properties
# application.properties
server.servlet.session.timeout=30m  # 30 minutes of inactivity
spring.session.store-type=none  # Stateless JWT (no session storage)
```

### Logout Implementation

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // For stateless JWT, logout is handled client-side (delete token)
        // Optionally, can add token to blacklist for server-side validation
        tokenBlacklist.add(token);
        
        log.info("User logged out");
        return ResponseEntity.ok().build();
    }
}

@Component
public class TokenBlacklist {
    
    private final Set<String> blacklistedTokens = new ConcurrentHashSet<>();
    
    public void add(String token) {
        blacklistedTokens.add(token);
    }
    
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
    
    // Cleanup expired tokens (run periodically)
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
    public void cleanupExpiredTokens() {
        // Remove tokens that have expired
    }
}
```

---

## OAuth2 Integration

> **Note**: OAuth2 integration is not currently implemented. The section below describes a potential future implementation.

### OAuth2 with Google

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${OAUTH2_GOOGLE_CLIENT_ID}
            client-secret: ${OAUTH2_GOOGLE_CLIENT_SECRET}
            scope: profile,email
            redirect-uri: http://localhost:8080/login/oauth2/code/google
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://www.googleapis.com/oauth2/v4/token
            user-info-uri: https://www.googleapis.com/oauth2/v1/userinfo
            user-name-attribute: sub
```

### OAuth2 User Service

```java
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // Check if user exists in our database
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            // Create new user from OAuth2 data
            user = new User();
            user.setUsername(oAuth2User.getAttribute("name"));
            user.setEmail(email);
            user.setAuthProvider("GOOGLE");
            user.setAuthProviderUserId(oAuth2User.getAttribute("sub"));
            user.setProfilePictureUrl(oAuth2User.getAttribute("picture"));
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        
        return oAuth2User;
    }
}
```

---

## MFA Implementation

> **Note**: MFA is not currently implemented. The section below describes a potential future implementation.

### Time-based One-Time Password (TOTP)

```java
@Service
public class MfaService {
    
    private final TimeBasedOneTimePasswordProvider totpProvider = new TimeBasedOneTimePasswordProvider();
    
    // Generate QR code for authenticator app
    public String generateMfaSecret(Long userId) {
        String secret = Base32.random();
        String issuer = "Gym Platform";
        String label = userId + "@gym-platform.com";
        
        String qrCodeUrl = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            issuer, label, secret, issuer
        );
        
        return qrCodeUrl;
    }
    
    // Verify TOTP code
    public boolean verifyTotp(String secret, String code) {
        try {
            long timeWindow = System.currentTimeMillis() / 30000;
            
            // Check current and ±1 time window (30 second validity)
            for (int i = -1; i <= 1; i++) {
                long time = timeWindow + i;
                String expectedCode = totpProvider.getCode(secret, time);
                
                if (expectedCode.equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }
}

@RestController
@RequestMapping("/api/auth")
public class MfaController {
    
    @PostMapping("/mfa/enable")
    public ResponseEntity<MfaSetupResponse> enableMfa(@CurrentUser User user) {
        String qrCodeUrl = mfaService.generateMfaSecret(user.getId());
        return ResponseEntity.ok(new MfaSetupResponse(qrCodeUrl));
    }
    
    @PostMapping("/mfa/verify")
    public ResponseEntity<Void> verifyMfa(
            @CurrentUser User user,
            @RequestParam String code) {
        
        if (mfaService.verifyTotp(user.getMfaSecret(), code)) {
            user.setMfaEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
```

---

## Credential Management

### Secret Rotation

```properties
# application.properties
secret.rotation.enabled=true
secret.rotation.interval.days=90  # Rotate every 90 days
jwt.secret.rotation.interval.days=30  # Rotate JWT secret monthly
```

### Environment-based Secrets

```yaml
# docker-compose.yml
services:
  gym-auth:
    environment:
      JWT_SECRET: ${JWT_SECRET}  # From .env or environment
      OAUTH2_GOOGLE_CLIENT_ID: ${OAUTH2_GOOGLE_CLIENT_ID}
      OAUTH2_GOOGLE_CLIENT_SECRET: ${OAUTH2_GOOGLE_CLIENT_SECRET}

# .env (NEVER commit to git)
JWT_SECRET=your-super-secret-key-here-min-32-chars
OAUTH2_GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
OAUTH2_GOOGLE_CLIENT_SECRET=GOCSPX-xxxxx
```

### Audit Logging for Authentication

```java
@Component
public class AuthenticationAuditListener {
    
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();
        
        auditLog.log(new AuditEvent(
            AuditEventType.AUTHENTICATION_SUCCESS,
            username,
            ipAddress,
            LocalDateTime.now()
        ));
    }
    
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();
        
        auditLog.log(new AuditEvent(
            AuditEventType.AUTHENTICATION_FAILED,
            username,
            ipAddress,
            LocalDateTime.now()
        ));
    }
}
```

---

## Related Documentation

- [01-security-overview.md](01-security-overview.md) - Security architecture
- [03-authorization-rbac.md](03-authorization-rbac.md) - RBAC and access control
- [04-data-security.md](04-data-security.md) - Data encryption and protection
- docs/troubleshooting/06-security-troubleshooting.md - Authentication issues
- docs/stack/04-security-framework.md - Spring Security configuration
