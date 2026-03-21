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

### User Login Process

```
Client                    Auth Service                  Token Service
  │                            │                              │
  ├─ POST /api/v1/auth/login──>│                              │
  │  (username, password)      │                              │
  │                            ├─ Validate credentials        │
  │                            ├─ Hash password check         │
  │                            │                              │
  │                            ├─ Generate JWT token ────────>│
  │                            │                              │
  │                            │<─ Token created ─────────────┤
  │                            │                              │
  │<─ 200 OK ─────────────────┤                              │
  │  { "token": "jwt..." }     │                              │
  │  { "refresh": "token..." } │                              │
```

### JWT Token Structure

```
Header.Payload.Signature

Header (Algorithm & Type):
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (Claims):
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",  // User ID
  "username": "john.doe",
  "email": "john@example.com",
  "role": "USER",
  "iat": 1516239022,      // Issued at
  "exp": 1516242622,      // Expiration (1 hour)
  "iss": "gym-auth-service"
}

Signature:
HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

## Spring Security Configuration

### Security Config Class

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Set session management to stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                .accessDeniedHandler(new JwtAccessDeniedHandler()))

            // Request filtering
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health"
                ).permitAll()

                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Supervisor endpoints
                .requestMatchers("/api/v1/supervisor/**").hasAnyRole("ADMIN", "SUPERVISOR")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Add JWT filter
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userDetailsService;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://app.gym.local"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
            .subject(user.getId().toString())
            .claim("username", user.getUsername())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().toString())
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
    public UUID getUserIdFromToken(String token) {
        String userId = Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();

        return UUID.fromString(userId);
    }

    /**
     * Extract role from token
     */
    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("role", String.class);
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

## JWT Filter

### Request Authentication Filter

```java
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractJwtFromRequest(request);

            if (token != null && jwtProvider.validateToken(token)) {
                String username = jwtProvider.getUsernameFromToken(token);
                String role = jwtProvider.getRoleFromToken(token);

                List<GrantedAuthority> authorities = Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        username, null, authorities);

                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User authenticated: {} with role: {}", username, role);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT from Authorization header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
```

## Authentication Controller

### Login & Token Management

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String username = authentication.getName();
            String token = jwtProvider.generateToken(username);
            String refreshToken = jwtProvider.generateRefreshToken(username);

            LoginResponse response = LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

            log.info("User logged in: {}", username);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed: invalid credentials for user: {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        try {
            String username = jwtProvider.getUsernameFromToken(request.getRefreshToken());

            if (!jwtProvider.validateToken(request.getRefreshToken())) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            String token = jwtProvider.generateToken(username);

            LoginResponse response = LoginResponse.builder()
                .token(token)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new UnauthorizedException("Could not refresh token");
        }
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.registerUser(request);

        RegisterResponse response = RegisterResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .message("User registered successfully")
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Logout (invalidate token)
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // Token invalidation typically handled by client or token blacklist
        SecurityContextHolder.clearContext();
        log.info("User logged out");
        return ResponseEntity.noContent().build();
    }
}
```

## Role-Based Access Control (RBAC)

### User Roles

```java
public enum UserRole {
    ADMIN(3),        // Full system access
    SUPERVISOR(2),   // Manage other users, view reports
    COACH(1),        // Manage assigned users, create programs
    USER(0);         // Basic access to own data

    private final int level;

    UserRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
```

### Authorization Annotations

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /**
     * Only ADMIN can access
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        // Implementation
    }

    /**
     * ADMIN or SUPERVISOR can access
     */
    @GetMapping("/{id}/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ReportDTO> getUserReport(@PathVariable UUID id) {
        // Implementation
    }

    /**
     * User can access only own data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') and #id == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        // Implementation
    }

    /**
     * Complex permission check
     */
    @PostMapping("/{userId}/programs")
    @PreAuthorize("""
        hasRole('COACH') and 
        (
            hasRole('ADMIN') or 
            @authorizationService.isCoachFor(#userId)
        )
    """)
    public ResponseEntity<ProgramDTO> createProgram(
            @PathVariable UUID userId,
            @RequestBody CreateProgramRequest request) {
        // Implementation
    }
}
```

### Custom Permission Service

```java
@Component
public class AuthorizationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoachUserMappingRepository coachUserMappingRepository;

    /**
     * Check if coach is assigned to user
     */
    public boolean isCoachFor(UUID userId, UUID coachId) {
        return coachUserMappingRepository
            .existsByUserIdAndCoachId(userId, coachId);
    }

    /**
     * Check if user has permission to resource
     */
    public boolean canAccessResource(UUID userId, UUID resourceId, String resourceType) {
        // Custom logic based on resource type
        return true;
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

### SSL Configuration (Production)

**application-prod.yml:**
```yaml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: gym-api
  http2:
    enabled: true
```

**Generate certificate:**
```bash
# Generate self-signed certificate (development)
keytool -genkeypair \
  -alias gym-api \
  -keyalg RSA \
  -keysize 2048 \
  -keystore keystore.p12 \
  -keypass changeit \
  -storepass changeit \
  -storetype PKCS12 \
  -validity 365 \
  -dname "CN=localhost,OU=Gym,O=Gym,L=City,ST=State,C=US"

# Convert to PEM format if needed
openssl pkcs12 -in keystore.p12 -out gym-api.pem -nodes
```

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
