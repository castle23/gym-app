# Security Framework

## Overview

Gym Platform implements comprehensive security using **Spring Security 6.x** with **JWT (JSON Web Tokens)** for authentication and **Role-Based Access Control (RBAC)** for authorization. This document covers authentication mechanisms, JWT implementation, and authorization strategies.

**Security Stack:**
- Spring Security 6.x
- JWT (JJWT library)
- BCrypt password hashing
- CORS security
- HTTPS/TLS enforcement

## Authentication Flow

### Authentication Flow

```
Client              API Gateway              Auth Service
  │                      │                       │
  ├─ POST /auth/login ──>│                       │
  │                      ├─ forward to auth ────>│
  │                      │                       ├─ validate credentials
  │                      │                       ├─ generate JWT
  │                      │<─ AuthResponse ────────┤
  │<─ AuthResponse ──────┤                       │
  │  { token, refreshToken, userId, email }       │
  │                      │                       │
  ├─ GET /training/... ──>│                      │
  │  Authorization: Bearer <jwt>                  │
  │                      ├─ validate JWT          │
  │                      ├─ inject X-User-Id      │
  │                      ├─ inject X-User-Roles   │
  │                      ├─ forward ────────────>Training Service
  │<─ response ──────────┤<──────────────────────┤
```

### JWT Token Structure

```
Payload (Claims):
{
  "sub": "123",        // User ID (Long, as string)
  "roles": "ROLE_USER", // comma-separated: ROLE_USER, ROLE_PROFESSIONAL, ROLE_ADMIN
  "iat": 1516239022,
  "exp": 1516325422
}
```

## Spring Security Configuration

### gym-common Auto-configuration

Security is centralized in `gym-common` via `GymSecurityAutoConfiguration`. Services do **not** define their own `SecurityConfig` — the auto-configuration is applied automatically.

```java
// GymSecurityAutoConfiguration (active on @Profile("!test"))
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
    .authorizeHttpRequests(auth -> {
        auth.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll(); // actuator
        auth.requestMatchers(publicPaths).permitAll();                    // from gym.security.public-paths
        auth.requestMatchers(ALWAYS_PUBLIC_PATHS).permitAll();            // swagger
        auth.anyRequest().authenticated();
    });
```

Public paths per service configured in `application.yml`:

```yaml
gym:
  security:
    public-paths:
      - /api/v1/exercises/system
      - /api/v1/exercises/discipline/**
```

### Auth Service — Custom SecurityConfig

Auth service overrides the common config (CSRF enabled for browser clients):

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GymSecurityProperties props) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/", "/**"))
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll();
                auth.requestMatchers(props.getPublicPaths().toArray(new String[0])).permitAll();
                auth.requestMatchers(ALWAYS_PUBLIC_PATHS).permitAll();
                auth.anyRequest().authenticated();
            });
        return http.build();
    }
}
```

## JWT Provider

### Token Generation & Validation

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    @Value("${jwt.secret:your-super-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")  // 1 hour in seconds
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800}")  // 7 days in seconds
    private long refreshTokenExpirationMs;

    private final UserRepository userRepository;

    /**
     * Generate JWT access token
     */
    public String generateToken(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return Jwts.builder()
            .subject(user.getId().toString())  // Long userId as string
            .claim("username", user.getUsername())
            .claim("email", user.getEmail())
            .claim("roles", List.of(user.getRole()))  // e.g. ["ROLE_USER"]
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs * 1000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs * 1000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    /**
     * Extract user ID from token
     */
    public Long getUserIdFromToken(String token) {
        String subject = Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
        return Long.parseLong(subject);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return (List<String>) Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("roles");
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
```

## JWT Filter (API Gateway only)

JWT validation happens **exclusively in the API Gateway** (`JwtAuthFilter`). The gateway injects `X-User-Id` and `X-User-Roles` headers into every forwarded request. Downstream services do **not** run a JWT filter — they read the injected headers via `GymRoleInterceptor` from `gym-common`.

```java
// API Gateway: JwtAuthFilter validates token and injects:
//   X-User-Id: 123
//   X-User-Roles: ROLE_USER

// Downstream services: GymRoleInterceptor (from gym-common)
// reads headers → stores in UserContextHolder
```

## Authentication Controller

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
```

**AuthResponse fields:** `token`, `refreshToken`, `userId`, `email`, `message`

**RegisterResponse fields:** `userId`, `email`, `message` (no tokens)

## Role-Based Access Control (RBAC)

### User Roles

```java
// Roles: ROLE_USER, ROLE_PROFESSIONAL, ROLE_ADMIN
// Stored as a string field on the User entity
```

### Authorization via gym-common

Services use `@RequiresRole` from `gym-common` and read identity from `UserContextHolder`:

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping
    @RequiresRole({"ROLE_ADMIN"})
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        String currentUserId = UserContextHolder.getUserId();
        Set<String> roles = UserContextHolder.getRoles();
        if (!id.toString().equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
            throw new UnauthorizedException("Access denied");
        }
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/{userId}/programs")
    @RequiresRole({"ROLE_PROFESSIONAL", "ROLE_ADMIN"})
    public ResponseEntity<ProgramDTO> createProgram(
            @PathVariable Long userId,
            @RequestBody CreateProgramRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(programService.create(userId, request));
    }
}
```

## Password Security

### Password Encoding

```java
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // 12 rounds of hashing
    }
}
```

### Password Validation

```java
@Component
public class PasswordValidator {

    /**
     * Validate password strength
     */
    public ValidationResult validatePasswordStrength(String password) {

        List<String> errors = new ArrayList<>();

        if (password.length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain uppercase letters");
        }

        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain lowercase letters");
        }

        if (!password.matches(".*\\d.*")) {
            errors.add("Password must contain digits");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            errors.add("Password must contain special characters");
        }

        return new ValidationResult(!errors.isEmpty(), errors);
    }

    @Data
    public static class ValidationResult {
        private boolean hasErrors;
        private List<String> errors;

        public ValidationResult(boolean hasErrors, List<String> errors) {
            this.hasErrors = hasErrors;
            this.errors = errors;
        }
    }
}
```

## HTTPS/TLS Configuration

> **Note**: TLS is not configured at the application layer in the current Docker Compose deployment. TLS termination would be handled by a reverse proxy in front of the services.

## Security Best Practices

1. **Always use HTTPS in production**
2. **Rotate JWT secrets regularly**
3. **Use strong password requirements**
4. **Implement rate limiting on login attempts**
5. **Log authentication events**
6. **Use CORS properly** - restrict origins
7. **Validate and sanitize all inputs**
8. **Use parameterized queries** to prevent SQL injection
9. **Keep dependencies updated**
10. **Use environment variables** for sensitive data

## Key References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc7519)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security RBAC](https://spring.io/blog/2022/02/21/spring-security-without-the-servlet-api)
- See also: [docs/security/](../security/)
