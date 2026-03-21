# API Security

## Overview

API security is the foundational layer protecting the Gym Platform's microservices from unauthorized access, injection attacks, brute force attempts, and data exfiltration. This guide covers authentication mechanisms, rate limiting, input validation, CORS configuration, and request/response security for REST APIs built with Spring Boot 3.x.

The Gym Platform implements defense-in-depth at the API layer:
- **Authentication:** JWT tokens with Spring Security filters
- **Rate Limiting:** Token bucket algorithm with Redis backing
- **Input Validation:** Bean Validation (Jakarta Validation) annotations
- **CORS:** Fine-grained cross-origin resource sharing policies
- **Request Signing:** HMAC-SHA256 for sensitive endpoints
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

    @Value("${jwt.expiration.access:900}") // 15 minutes
    private long accessTokenExpiration;

    @Value("${jwt.expiration.refresh:604800}") // 7 days
    private long refreshTokenExpiration;

    private static final String AUTHORITIES_KEY = "roles";

    /**
     * Generate JWT access token
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim(AUTHORITIES_KEY, getAuthoritiesFromAuthentication(authentication))
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plusSeconds(accessTokenExpiration)))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }

    /**
     * Generate JWT refresh token
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plusSeconds(refreshTokenExpiration)))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
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
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    /**
     * Get authorities from token
     */
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> getAuthoritiesFromToken(String token) {
        List<String> roles = (List<String>) Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .get(AUTHORITIES_KEY);
        
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    private List<String> getAuthoritiesFromAuthentication(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    }
}
```

#### JWT Filter

Implement a filter to validate tokens on each request (`auth-service/src/main/java/com/gym/auth/security/JwtAuthenticationFilter.java`):

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtProvider.validateToken(jwt)) {
                String username = jwtProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
```

#### Configuration

**application.yml:**
```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-base64-encoded-secret-key-minimum-43-characters}
  expiration:
    access: 900      # 15 minutes
    refresh: 604800  # 7 days
    # Access tokens: short-lived for security
    # Refresh tokens: long-lived, can be revoked
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

For third-party integrations, the Auth Service supports OAuth2 authorization code flow.

**Application Configuration:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-auth-provider.com
          jwk-set-uri: https://your-auth-provider.com/.well-known/jwks.json
```

**OAuth2 Resource Server Configuration:**
```java
@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
                .and()
            .oauth2ResourceServer()
                .jwt()
                    .jwtAuthenticationConverter(jwtAuthenticationConverter());
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

### API Key Authentication

For service-to-service communication, the Gym Platform supports API key authentication.

**Implementation:**
```java
@Component
public class ApiKeyValidator {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public boolean validateApiKey(String apiKey) {
        ApiKeyEntity keyEntity = apiKeyRepository.findByKeyAndActiveTrue(apiKey);
        if (keyEntity == null) {
            return false;
        }
        // Check rate limit, expiration, IP whitelist
        return !keyEntity.isExpired() && isIpWhitelisted(keyEntity);
    }

    private boolean isIpWhitelisted(ApiKeyEntity keyEntity) {
        // Implementation for IP whitelist validation
        return true;
    }
}

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyValidator apiKeyValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && apiKeyValidator.validateApiKey(apiKey)) {
            ApiKeyAuthenticationToken authToken = new ApiKeyAuthenticationToken(apiKey);
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## Rate Limiting and Throttling

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
@RequestMapping("/api/v1/workouts")
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
        
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_PREMIUM"))) {
            return "premium";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE"))) {
            return "service-account";
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

### HMAC-SHA256 Request Signing

For sensitive operations, require HMAC-SHA256 signatures:

**Implementation:**
```java
@Component
public class RequestSigningService {

    @Value("${api.signing.secret}")
    private String signingSecret;

    public String generateSignature(String method, String path, String body, long timestamp) {
        String message = method + "\n" + path + "\n" + body + "\n" + timestamp;
        return hmacSha256(message, signingSecret);
    }

    public boolean validateSignature(String method, String path, String body,
                                     long timestamp, String signature) {
        String expectedSignature = generateSignature(method, path, body, timestamp);
        return MessageDigest.isEqual(
            signature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 0,
                secret.getBytes(StandardCharsets.UTF_8).length, "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}

@Component
public class RequestSigningFilter extends OncePerRequestFilter {

    @Autowired
    private RequestSigningService signingService;

    private static final long MAX_TIMESTAMP_DIFF = 300; // 5 minutes

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Skip for public endpoints
        if (request.getRequestURI().startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader("X-Signature");
        String timestamp = request.getHeader("X-Timestamp");

        if (signature != null && timestamp != null) {
            try {
                long requestTimestamp = Long.parseLong(timestamp);
                long currentTimestamp = Instant.now().getEpochSecond();

                // Prevent replay attacks
                if (Math.abs(currentTimestamp - requestTimestamp) > MAX_TIMESTAMP_DIFF) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Request timestamp too old");
                    return;
                }

                // Validate signature
                String body = getRequestBody(request);
                boolean isValid = signingService.validateSignature(
                    request.getMethod(),
                    request.getRequestURI(),
                    body,
                    requestTimestamp,
                    signature);

                if (!isValid) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid signature");
                    return;
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Signature validation failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        // Implement request body reading without consuming the stream
        return "";
    }
}
```

---

## API Versioning

### URL-Based Versioning

The Gym Platform uses URL-based API versioning for backward compatibility:

```
/api/v1/workouts     # Current stable version
/api/v2/workouts     # New version with breaking changes
```

**Implementation:**
```java
@RestController
@RequestMapping("/api/v1")
public class WorkoutControllerV1 {

    @GetMapping("/workouts")
    public ResponseEntity<List<WorkoutDtoV1>> getWorkouts() {
        // V1 response format
    }
}

@RestController
@RequestMapping("/api/v2")
public class WorkoutControllerV2 {

    @GetMapping("/workouts")
    public ResponseEntity<List<WorkoutDtoV2>> getWorkouts() {
        // V2 response format with new fields
    }
}
```

### Version Deprecation

```java
@Component
public class VersionDeprecationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        if (requestPath.contains("/api/v1/")) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", "Sun, 01 Dec 2025 00:00:00 GMT");
            response.setHeader("Link", "</api/v2>; rel=\"successor-version\"");
        }

        filterChain.doFilter(request, response);
    }
}
```

---

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
        mockMvc.perform(get("/api/v1/workouts"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAuthorizedAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/workouts"))
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
