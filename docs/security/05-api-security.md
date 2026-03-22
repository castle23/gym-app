# API Security

## Overview

API security is the foundational layer protecting the Gym Platform's microservices from unauthorized access, injection attacks, brute force attempts, and data exfiltration. This guide covers authentication mechanisms, rate limiting, input validation, CORS configuration, and request/response security for REST APIs built with Spring Boot 3.x.

> **Note**: This document describes API security patterns and aspirational features. Rate limiting with Redis, request signing, API versioning, and OAuth2 resource server are not currently implemented. The JWT authentication section reflects the actual API Gateway implementation.

The Gym Platform implements defense-in-depth at the API layer:
- **Authentication:** JWT tokens validated at the API Gateway only
- **Input Validation:** Bean Validation (Jakarta Validation) annotations
- **CORS:** Cross-origin resource sharing policies
- **Output Encoding:** Automatic JSON serialization with XSS prevention

## Table of Contents

- [API Authentication](#api-authentication)
- [Rate Limiting and Throttling](#rate-limiting-and-throttling)
- [Input Validation](#input-validation)
- [Output Encoding](#output-encoding)
- [CORS Configuration](#cors-configuration)
- [Request Signing](#request-signing)
- [API Versioning](#api-versioning)
- [Security Headers](#security-headers)
- [Error Handling](#error-handling)
- [Audit Logging](#audit-logging)
- [Best Practices](#best-practices)

---

## API Authentication

### JWT Authentication Flow

The Gym Platform uses JWT (JSON Web Tokens) for stateless API authentication across all microservices.

#### Token Structure

JWT tokens consist of three parts separated by dots: `header.payload.signature`

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload (Claims):**
```json
{
  "sub": "user123",
  "username": "john.doe@gym.com",
  "roles": ["ROLE_USER", "ROLE_TRAINER"],
  "scope": "read write",
  "iat": 1707910800,
  "exp": 1707914400,
  "iss": "gym-auth-service",
  "aud": "gym-api"
}
```

**Signature:**
```
HMAC-SHA256(
  base64(header) + "." + base64(payload),
  secret_key
)
```

#### JWT Token Generation

In the Auth Service (`auth-service/src/main/java/com/gym/auth/security/JwtProvider.java`):

```java
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.access:86400000}") // 24 hours
    private long accessTokenExpiration;

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Jwts.builder()
            .setSubject(userDetails.getUsername())  // userId as string
            .claim("roles", getAuthoritiesFromAuthentication(authentication))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
```

#### JWT Filter (API Gateway only)

JWT validation happens exclusively in the API Gateway. Downstream services receive `X-User-Id` and `X-User-Roles` headers — they do **not** run a JWT filter.

```java
// API Gateway: JwtAuthFilter validates token and injects headers
// X-User-Id: 123
// X-User-Roles: ROLE_USER

// Downstream services: GymRoleInterceptor reads injected headers
// No JWT validation in training-service, tracking-service, etc.
```

#### Configuration

**application.yml:**
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours in milliseconds
```

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            .authorizeRequests()
                .antMatchers("/api/v1/auth/**").permitAll()
                .antMatchers("/api/v1/health/**").permitAll()
                .anyRequest().authenticated();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
}
```

### OAuth2 Integration

> **Note**: OAuth2 integration is not currently implemented.

### API Key Authentication

> **Note**: API key authentication for service-to-service communication is not currently implemented. There is no service-to-service communication in this platform.

---

## Rate Limiting and Throttling

> **Note**: Rate limiting with Redis/Bucket4j is not currently implemented. The patterns below describe a target implementation.

### Token Bucket Algorithm

The Gym Platform implements rate limiting using a token bucket algorithm with Redis for distributed rate limiting across microservices.

#### Redis-Based Rate Limiter

**Dependency:**
```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>7.10.0</version>
</dependency>
```

**Implementation:**
```java
@Component
public class RateLimiterService {

    @Autowired
    private RedisTemplate<String, Bucket> redisTemplate;

    private static final int TOKENS_PER_MINUTE = 100;

    public Bucket resolveBucket(String key) {
        // Try to get existing bucket from Redis
        Bucket bucket = redisTemplate.opsForValue().get(key);
        
        if (bucket == null) {
            // Create new bucket if doesn't exist
            Bucket newBucket = Bucket.builder()
                .addLimit(Limit.smoothRatePerMinute(TOKENS_PER_MINUTE))
                .build();
            
            redisTemplate.opsForValue().set(key, newBucket, Duration.ofHours(1));
            bucket = newBucket;
        }
        
        return bucket;
    }

    public boolean allowRequest(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.tryConsume(1);
    }

    public long getRemainingTokens(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.estimateAbilityToConsume(1).getRoundedTokensToWait();
    }
}
```

#### Rate Limiting Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    String key() default "";
    int tokensPerMinute() default 100;
}

@Aspect
@Component
public class RateLimitingAspect {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Around("@annotation(rateLimited)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited)
            throws Throwable {
        
        String key = generateKey(joinPoint, rateLimited.key());
        
        if (!rateLimiterService.allowRequest(key)) {
            throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
        }
        
        return joinPoint.proceed();
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // Generate unique key based on user, endpoint, custom expression
        HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes()).getRequest();
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return "rate-limit:" + userId + ":" + request.getRequestURI();
    }
}
```

#### Controller Usage

```java
@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    @PostMapping
    @RateLimited(tokensPerMinute = 50)
    public ResponseEntity<WorkoutDto> createWorkout(@RequestBody CreateWorkoutRequest request) {
        // Implementation
    }

    @GetMapping
    @RateLimited(tokensPerMinute = 200)
    public ResponseEntity<List<WorkoutDto>> getWorkouts() {
        // Implementation
    }
}
```

#### Rate Limit Response Headers

```java
@Component
public class RateLimitHeaderFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = extractRateLimitKey(request);
        long remaining = rateLimiterService.getRemainingTokens(key);

        response.setHeader("X-RateLimit-Limit", "100");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(
            Instant.now().plusSeconds(60).getEpochSecond()));

        filterChain.doFilter(request, response);
    }

    private String extractRateLimitKey(HttpServletRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return "rate-limit:" + userId + ":" + request.getRequestURI();
    }
}
```

### Tiered Rate Limiting

Different endpoints and user roles have different rate limits:

**Configuration:**
```yaml
rate-limiting:
  tiers:
    anonymous:
      requests-per-minute: 10
      burst-size: 20
    authenticated:
      requests-per-minute: 100
      burst-size: 150
    premium:
      requests-per-minute: 500
      burst-size: 1000
    service-account:
      requests-per-minute: 2000
      burst-size: 5000
  
  endpoints:
    /api/v1/auth/login:
      requests-per-minute: 5
      burst-size: 10
    /api/v1/workouts:
      requests-per-minute: 50
      burst-size: 100
    /api/v1/tracking:
      requests-per-minute: 200
      burst-size: 300
```

**Implementation:**
```java
@Component
public class TieredRateLimiter {

    @Autowired
    private RateLimiterService rateLimiterService;

    public boolean isAllowed(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String tier = determineUserTier(auth);
        String endpoint = request.getRequestURI();
        String key = tier + ":" + endpoint;
        
        return rateLimiterService.allowRequest(key);
    }

    private String determineUserTier(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        
        // Roles come from X-User-Roles header via GymSecurityContext
        List<String> roles = GymSecurityContext.getCurrentRoles();
        if (roles.contains("ROLE_ADMIN")) {
            return "service-account";
        } else if (roles.contains("ROLE_PROFESSIONAL")) {
            return "premium";
        }
        
        return "authenticated";
    }
}
```

---

## Input Validation

### Bean Validation Annotations

Use Jakarta Validation (formerly javax.validation) for declarative input validation.

**Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Request DTO with Validation

```java
public class CreateWorkoutRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Start time is required")
    @PastOrPresent(message = "Start time cannot be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @Min(0)
    @Max(1000)
    private Integer duration;

    @Email(message = "Invalid email format")
    private String notificationEmail;

    @Pattern(regexp = "^(STRENGTH|CARDIO|FLEXIBILITY|SPORTS)$",
             message = "Invalid workout type")
    private String workoutType;

    @NotEmpty(message = "At least one exercise is required")
    @Valid
    private List<ExerciseDto> exercises;

    @ValidDateRange(message = "End time must be after start time")
    public boolean isDateRangeValid() {
        return endTime.isAfter(startTime);
    }

    // Getters and setters
}
```

#### Custom Validator

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "Invalid date range";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, CreateWorkoutRequest> {

    @Override
    public boolean isValid(CreateWorkoutRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }
        return request.getEndTime().isAfter(request.getStartTime());
    }
}
```

#### Global Validation Handler

```java
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
```

### Input Sanitization

Prevent injection attacks by sanitizing user input:

```java
@Component
public class InputSanitizer {

    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>\"'%;()&+]");

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Remove dangerous characters
        return DANGEROUS_CHARS.matcher(input).replaceAll("");
    }

    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        // Use OWASP Java HTML Sanitizer
        Policy policy = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS);
        return policy.sanitize(input);
    }
}
```

**Usage:**
```java
@Component
public class UserRepository {

    @Autowired
    private InputSanitizer sanitizer;

    public User findByUsername(String username) {
        String sanitized = sanitizer.sanitize(username);
        // Database query with sanitized input
    }
}
```

---

## Output Encoding

### XSS Prevention

Prevent cross-site scripting by properly encoding output:

```java
@Configuration
public class JsonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure Jackson for safe serialization
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        
        return mapper;
    }
}
```

**Response DTO:**
```java
public class UserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    @JsonSerialize(using = XssPreventingSerializer.class)
    private String username;

    @JsonProperty("email")
    private String email;

    // Getters and setters
}

public class XssPreventingSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value != null) {
            String escaped = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
            gen.writeString(escaped);
        }
    }
}
```

---

## CORS Configuration

### CORS Policy Definition

Cross-Origin Resource Sharing (CORS) controls which origins can access the API.

**Configuration:**
```java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "https://gym-web.example.com",
                        "https://gym-mobile.example.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600)
                    .exposedHeaders("X-Total-Count", "X-RateLimit-Remaining");

                registry.addMapping("/api/public/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET", "OPTIONS")
                    .maxAge(3600);
            }
        };
    }
}
```

**Application Configuration:**
```yaml
cors:
  allowed-origins:
    - https://gym-web.example.com
    - https://gym-mobile.example.com
  allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
  allowed-headers: '*'
  allow-credentials: true
  max-age: 3600
  exposed-headers: X-Total-Count,X-RateLimit-Remaining
```

### CORS Filter

```java
@Component
public class CorsFilter extends OncePerRequestFilter {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String origin = request.getHeader("Origin");
        
        if (isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Expose-Headers",
                "X-Total-Count, X-RateLimit-Remaining");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isOriginAllowed(String origin) {
        return allowedOrigins.contains(origin);
    }
}
```

---

## Request Signing

> **Note**: Request signing is not currently implemented.

## API Versioning

> **Note**: The Gym Platform does not use API versioning (`/api/v1/`). Endpoints are accessed via context-paths: `/auth/...`, `/training/...`, `/tracking/...`, `/notifications/...`.

## Security Headers

### Response Security Headers

Configure security headers in the response:

```java
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Referrer policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
        
        // Permissions policy
        response.setHeader("Permissions-Policy",
            "geolocation=(), microphone=(), camera=()");

        filterChain.doFilter(request, response);
    }
}
```

---

## Error Handling

### Secure Error Responses

Never leak sensitive information in error messages:

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {
        
        // Log detailed error for debugging
        log.error("Authentication failed: {}", ex.getMessage());
        
        // Return generic error to client
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.UNAUTHORIZED.value())
            .message("Invalid credentials")
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex) {
        
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .message("Access denied")
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        
        log.error("Unexpected error: ", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

---

## Audit Logging

### API Call Auditing

Log all API calls with request/response details for security audits:

```java
@Component
@Aspect
public class ApiAuditingAspect {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Pointcut("@annotation(com.gym.common.annotation.Auditable)")
    public void auditableMethod() {}

    @Around("auditableMethod()")
    public Object auditApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        
        HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes()).getRequest();
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long startTime = System.currentTimeMillis();

        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            AuditEvent event = AuditEvent.builder()
                .username(username)
                .action(joinPoint.getSignature().getName())
                .method(request.getMethod())
                .url(request.getRequestURI())
                .ipAddress(getClientIp(request))
                .status(exception == null ? "SUCCESS" : "FAILED")
                .errorMessage(exception != null ? exception.getMessage() : null)
                .duration(duration)
                .timestamp(LocalDateTime.now())
                .build();

            auditEventRepository.save(event);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

**Annotation:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action() default "";
}
```

**Usage:**
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @PostMapping
    @Auditable(action = "CREATE_USER")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        // Implementation
    }

    @DeleteMapping("/{id}")
    @Auditable(action = "DELETE_USER")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // Implementation
    }
}
```

---

## Best Practices

### 1. Defense in Depth

- **Multiple layers:** Combine authentication, authorization, rate limiting, and input validation
- **Principle of least privilege:** Users should only have permissions they need
- **Fail securely:** Default to deny, explicit allow

### 2. Secure by Default

- **No default credentials:** Always require configuration
- **Secure defaults:** HTTPS, short-lived tokens, strong encryption
- **Secure libraries:** Use well-maintained security libraries (Spring Security, OWASP)

### 3. Input/Output Security

- **Never trust user input:** Always validate and sanitize
- **Parametrized queries:** Use prepared statements to prevent SQL injection
- **Output encoding:** Prevent XSS attacks by encoding output

### 4. Token Management

- **Short-lived tokens:** Access tokens expire in 15-30 minutes
- **Refresh tokens:** Longer-lived, can be revoked, secure channel
- **Token rotation:** Regularly rotate tokens
- **Secure storage:** Never log or store tokens in plain text

### 5. API Monitoring

```java
@Component
@Aspect
public class ApiMonitoringAspect {

    @Autowired
    private MeterRegistry meterRegistry;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void apiEndpoint() {}

    @Around("apiEndpoint()")
    public Object monitorApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String endpoint = joinPoint.getSignature().getName();
        long startTime = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            meterRegistry.timer("api.request", "endpoint", endpoint, "status", "success")
                .record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            return result;
        } catch (Exception e) {
            meterRegistry.timer("api.request", "endpoint", endpoint, "status", "error")
                .record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            throw e;
        }
    }
}
```

### 6. Documentation

- **Swagger/OpenAPI:** Document all endpoints with security requirements
- **Security annotations:** Mark endpoints as requiring authentication/authorization
- **API contracts:** Version your APIs, document breaking changes

### 7. Testing

```java
@SpringBootTest
public class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUnauthorizedAccessDenied() throws Exception {
        mockMvc.perform(get("/workouts"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAuthorizedAccessAllowed() throws Exception {
        mockMvc.perform(get("/workouts")
            .header("X-User-Id", "1")
            .header("X-User-Roles", "ROLE_USER"))
            .andExpect(status().isOk());
    }

    @Test
    public void testRateLimitingEnforced() throws Exception {
        // Simulate multiple requests exceeding rate limit
        for (int i = 0; i < 110; i++) {
            mockMvc.perform(get("/api/v1/workouts"))
                .andExpect(i < 100 ? status().isOk() : status().isTooManyRequests());
        }
    }

    @Test
    public void testInvalidInputRejected() throws Exception {
        mockMvc.perform(post("/api/v1/workouts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"\", \"userId\": -1}"))
            .andExpect(status().isBadRequest());
    }
}
```

---

## Related Documentation

- [Security Overview](01-security-overview.md) - Security architecture and threat model
- [Authentication](02-authentication.md) - JWT and credential management
- [Authorization & RBAC](03-authorization-rbac.md) - Role-based access control
- [Data Security](04-data-security.md) - Encryption and data protection
- [Incident Response](07-incident-response.md) - Breach procedures
- [API Design Patterns](../stack/03-api-design-patterns.md) - RESTful API design
- [Spring Security Framework](../stack/04-security-framework.md) - Framework configuration

## References

- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Jakarta Validation Documentation](https://jakarta.ee/specifications/validation/)
- [RFC 6749 - OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
