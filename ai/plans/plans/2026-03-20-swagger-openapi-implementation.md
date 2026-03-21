# Swagger/OpenAPI Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add comprehensive Swagger/OpenAPI documentation to all 6 microservices with interactive API explorers, security scheme definitions, and endpoint annotations.

**Architecture:** Each microservice gets:
1. springdoc-openapi dependency (Maven)
2. OpenAPI configuration bean that defines service metadata and security schemes
3. Swagger annotations on controllers (@Operation, @ApiResponse, @Tag, @SecurityRequirement)
4. Auto-generated Swagger UI at `/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`

**Tech Stack:** 
- springdoc-openapi 2.1.0 (WebMVC for microservices, WebFlux for API Gateway)
- OpenAPI 3.0 specification
- JWT Bearer token security scheme

---

## Task C1: Add Springdoc-OpenAPI Dependencies to All Services

**Files:**
- Modify: `auth-service/pom.xml`
- Modify: `training-service/pom.xml`
- Modify: `tracking-service/pom.xml`
- Modify: `notification-service/pom.xml`
- Modify: `api-gateway/pom.xml`
- Modify: `common/pom.xml`

### Step C1.1: Add Springdoc-OpenAPI WebMVC to Auth Service

- [ ] Edit `auth-service/pom.xml` - Add dependency after line 71 (before closing `</dependencies>`)

```xml
        <!-- Springdoc OpenAPI for Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.1.0</version>
        </dependency>
```

- [ ] Run: `mvn dependency:resolve -pl auth-service`
- [ ] Expected: SUCCESS (no dependency errors)

### Step C1.2: Add Springdoc-OpenAPI WebMVC to Training Service

- [ ] Edit `training-service/pom.xml` - Add same dependency

- [ ] Run: `mvn dependency:resolve -pl training-service`
- [ ] Expected: SUCCESS

### Step C1.3: Add Springdoc-OpenAPI WebMVC to Tracking Service

- [ ] Edit `tracking-service/pom.xml` - Add same dependency

- [ ] Run: `mvn dependency:resolve -pl tracking-service`
- [ ] Expected: SUCCESS

### Step C1.4: Add Springdoc-OpenAPI WebMVC to Notification Service

- [ ] Edit `notification-service/pom.xml` - Add same dependency

- [ ] Run: `mvn dependency:resolve -pl notification-service`
- [ ] Expected: SUCCESS

### Step C1.5: Add Springdoc-OpenAPI WebFlux to API Gateway

- [ ] Edit `api-gateway/pom.xml` - Add WEBFLUX variant (not WEBMVC):

```xml
        <!-- Springdoc OpenAPI WebFlux for Gateway -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
            <version>2.1.0</version>
        </dependency>
```

- [ ] Run: `mvn dependency:resolve -pl api-gateway`
- [ ] Expected: SUCCESS

### Step C1.6: Commit Dependencies

```bash
git add auth-service/pom.xml training-service/pom.xml tracking-service/pom.xml notification-service/pom.xml api-gateway/pom.xml
git commit -m "feat: Add springdoc-openapi dependencies to all services"
```

---

## Task C2: Create OpenAPI Configuration for Auth Service

**Files:**
- Create: `auth-service/src/main/java/com/gym/auth/config/OpenApiConfig.java`

### Step C2.1: Create OpenApiConfig.java for Auth Service

- [ ] Create file at `auth-service/src/main/java/com/gym/auth/config/OpenApiConfig.java`

```java
package com.gym.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("JWT-based authentication and authorization service for Gym Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gym Platform Team")
                                .email("support@gym-platform.com")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

- [ ] Verify compilation: `mvn compile -pl auth-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add auth-service/src/main/java/com/gym/auth/config/OpenApiConfig.java
git commit -m "feat: Add OpenAPI configuration to auth service"
```

---

## Task C3: Create OpenAPI Configuration for Training Service

**Files:**
- Create: `training-service/src/main/java/com/gym/training/config/OpenApiConfig.java`

### Step C3.1: Create OpenApiConfig.java for Training Service

- [ ] Create file at `training-service/src/main/java/com/gym/training/config/OpenApiConfig.java`

```java
package com.gym.training.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trainingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Training Service API")
                        .description("Exercise, routine, and training program management service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gym Platform Team")
                                .email("support@gym-platform.com")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

- [ ] Verify compilation: `mvn compile -pl training-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add training-service/src/main/java/com/gym/training/config/OpenApiConfig.java
git commit -m "feat: Add OpenAPI configuration to training service"
```

---

## Task C4: Create OpenAPI Configuration for Tracking Service

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/config/OpenApiConfig.java`

### Step C4.1: Create OpenApiConfig.java for Tracking Service

- [ ] Create file at `tracking-service/src/main/java/com/gym/tracking/config/OpenApiConfig.java`

```java
package com.gym.tracking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trackingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tracking Service API")
                        .description("Diet tracking, measurements, objectives, and progress tracking service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gym Platform Team")
                                .email("support@gym-platform.com")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

- [ ] Verify compilation: `mvn compile -pl tracking-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add tracking-service/src/main/java/com/gym/tracking/config/OpenApiConfig.java
git commit -m "feat: Add OpenAPI configuration to tracking service"
```

---

## Task C5: Create OpenAPI Configuration for Notification Service

**Files:**
- Create: `notification-service/src/main/java/com/gym/notification/config/OpenApiConfig.java`

### Step C5.1: Create OpenApiConfig.java for Notification Service

- [ ] Create file at `notification-service/src/main/java/com/gym/notification/config/OpenApiConfig.java`

```java
package com.gym.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("Push notifications and user notification management service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gym Platform Team")
                                .email("support@gym-platform.com")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

- [ ] Verify compilation: `mvn compile -pl notification-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add notification-service/src/main/java/com/gym/notification/config/OpenApiConfig.java
git commit -m "feat: Add OpenAPI configuration to notification service"
```

---

## Task C6: Annotate Auth Service Controllers

**Files:**
- Modify: `auth-service/src/main/java/com/gym/auth/controller/AuthController.java`

### Step C6.1: Update AuthController with Swagger Annotations

- [ ] Open `auth-service/src/main/java/com/gym/auth/controller/AuthController.java`

- [ ] Add imports at top (after existing imports):

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
```

- [ ] Replace class declaration to add `@Tag`:

```java
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication, registration, and JWT token management")
public class AuthController {
```

- [ ] Update `/register` endpoint:

```java
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided credentials and sends verification email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate email")
    })
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
```

- [ ] Update `/login` endpoint:

```java
    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user with email and password, returns JWT access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, tokens returned"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or account not verified")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
```

- [ ] Update `/verify` endpoint:

```java
    @PostMapping("/verify")
    @Operation(
            summary = "Verify email address",
            description = "Verifies user email using the verification code sent to their email address"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    })
    public ResponseEntity<AuthResponse> verify(@RequestBody VerifyEmailRequest request) {
        log.info("Verification attempt for email: {}", request.getEmail());
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }
```

- [ ] Update `/refresh` endpoint:

```java
    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL', 'ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Refresh access token",
            description = "Uses refresh token to obtain a new access token (requires authentication)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        TokenRefreshResponse response = authService.refreshToken(request);
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
```

- [ ] Update `/profile` endpoint:

```java
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL', 'ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Get user profile",
            description = "Retrieves the authenticated user's profile information (requires authentication)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - token missing or invalid")
    })
    public ResponseEntity<AuthResponse> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "unknown";
        
        log.info("Profile request for user: {}", userId);
        return ResponseEntity.ok(AuthResponse.builder()
                .userId(userId)
                .message("Profile retrieved")
                .success(true)
                .build());
    }
```

- [ ] Update `/health` endpoint:

```java
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifies that the Auth Service is running and healthy")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
```

- [ ] Verify compilation: `mvn compile -pl auth-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add auth-service/src/main/java/com/gym/auth/controller/AuthController.java
git commit -m "docs: Add OpenAPI/Swagger annotations to auth controller"
```

---

## Task C7: Annotate Training Service Controllers

**Files:**
- Modify: `training-service/src/main/java/com/gym/training/controller/ExerciseController.java` (if exists)
- Or identify all training controllers and annotate them

### Step C7.1: Identify Training Service Controllers

- [ ] Run: `find training-service/src/main/java -name "*Controller.java" -type f`
- [ ] Expected: List of controller files (e.g., ExerciseController, ExerciseSessionController, RoutineTemplateController, UserRoutineController)

- [ ] Note: The exact controllers depend on your implementation. For this task, we'll add @Tag to all controllers and @Operation/@ApiResponse to all public endpoints.

### Step C7.2: Add Swagger Annotations to All Training Controllers

For each controller in training-service:

- [ ] Add imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
```

- [ ] Add `@Tag` to class (e.g., for ExerciseController):

```java
@Tag(name = "Exercises", description = "Exercise CRUD operations and system exercise management")
public class ExerciseController {
```

- [ ] For each public endpoint, add @Operation and @ApiResponses:

```java
@GetMapping
@Operation(summary = "List all exercises", description = "Retrieves all exercises with filtering and pagination")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Exercises retrieved successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
})
public ResponseEntity<?> getExercises(...) { ... }

@PostMapping
@SecurityRequirement(name = "bearer-jwt")
@Operation(summary = "Create new exercise", description = "Creates a new exercise (requires PROFESSIONAL or ADMIN role)")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Exercise created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid exercise data"),
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
})
public ResponseEntity<?> createExercise(...) { ... }

@GetMapping("/{id}")
@Operation(summary = "Get exercise by ID", description = "Retrieves a single exercise by its ID")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Exercise retrieved successfully"),
    @ApiResponse(responseCode = "404", description = "Exercise not found")
})
public ResponseEntity<?> getExerciseById(...) { ... }

@PutMapping("/{id}")
@SecurityRequirement(name = "bearer-jwt")
@Operation(summary = "Update exercise", description = "Updates an existing exercise (requires PROFESSIONAL or ADMIN role)")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Exercise updated successfully"),
    @ApiResponse(responseCode = "404", description = "Exercise not found"),
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
})
public ResponseEntity<?> updateExercise(...) { ... }

@DeleteMapping("/{id}")
@SecurityRequirement(name = "bearer-jwt")
@Operation(summary = "Delete exercise", description = "Deletes an exercise (requires ADMIN role)")
@ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Exercise deleted successfully"),
    @ApiResponse(responseCode = "404", description = "Exercise not found"),
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
})
public ResponseEntity<?> deleteExercise(...) { ... }
```

- [ ] Repeat for all other training controllers (ExerciseSession, RoutineTemplate, UserRoutine, etc.)

- [ ] Verify compilation: `mvn compile -pl training-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add training-service/src/main/java/com/gym/training/controller/
git commit -m "docs: Add OpenAPI/Swagger annotations to training service controllers"
```

---

## Task C8: Annotate Tracking Service Controllers

**Files:**
- Modify: All controller files in `tracking-service/src/main/java/com/gym/tracking/controller/`

### Step C8.1: Identify Tracking Service Controllers

- [ ] Run: `find tracking-service/src/main/java -name "*Controller.java" -type f`
- [ ] Expected: List of controller files (DietComponent, DietLog, Measurement, Objective, Plan, Recommendation, TrainingComponent, etc.)

### Step C8.2: Add Swagger Annotations to All Tracking Controllers

- [ ] Add same imports as Step C7.1

- [ ] For each controller, add `@Tag` with appropriate description (e.g., for MeasurementController):

```java
@Tag(name = "Measurements", description = "User body measurement tracking and history")
public class MeasurementController {
```

- [ ] For each endpoint, add @Operation and @ApiResponses following the pattern from C7.2

- [ ] Verify compilation: `mvn compile -pl tracking-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add tracking-service/src/main/java/com/gym/tracking/controller/
git commit -m "docs: Add OpenAPI/Swagger annotations to tracking service controllers"
```

---

## Task C9: Annotate Notification Service Controllers

**Files:**
- Modify: All controller files in `notification-service/src/main/java/com/gym/notification/controller/`

### Step C9.1: Identify Notification Service Controllers

- [ ] Run: `find notification-service/src/main/java -name "*Controller.java" -type f`
- [ ] Expected: List of controller files (NotificationController, PushTokenController, etc.)

### Step C9.2: Add Swagger Annotations to All Notification Controllers

- [ ] Add same imports as Step C7.1

- [ ] For each controller, add `@Tag` (e.g., for NotificationController):

```java
@Tag(name = "Notifications", description = "User notifications and notification management")
public class NotificationController {
```

- [ ] For each endpoint, add @Operation and @ApiResponses following the pattern from C7.2

- [ ] Verify compilation: `mvn compile -pl notification-service`
- [ ] Expected: BUILD SUCCESS

- [ ] Commit:

```bash
git add notification-service/src/main/java/com/gym/notification/controller/
git commit -m "docs: Add OpenAPI/Swagger annotations to notification service controllers"
```

---

## Task C10: Create Swagger Usage Guide

**Files:**
- Create: `docs/SWAGGER_USAGE_GUIDE.md`

### Step C10.1: Create Usage Guide

- [ ] Create file at `docs/SWAGGER_USAGE_GUIDE.md`

```markdown
# Swagger/OpenAPI Documentation Guide

## Overview

All Gym Platform microservices provide interactive API documentation through Swagger UI, automatically generated from OpenAPI 3.0 specifications.

---

## Accessing Swagger UI

Each microservice exposes its API documentation at:

| Service | URL | Purpose |
|---------|-----|---------|
| **Auth Service** | http://localhost:8081/swagger-ui.html | Authentication and JWT management |
| **Training Service** | http://localhost:8082/swagger-ui.html | Exercises, routines, and training programs |
| **Tracking Service** | http://localhost:8083/swagger-ui.html | Diet, measurements, objectives, progress |
| **Notification Service** | http://localhost:8084/swagger-ui.html | Push notifications and alerts |
| **API Gateway** | http://localhost:8080/swagger-ui.html | Unified API documentation |

---

## Features

### Try It Out

Every endpoint includes a **"Try it out"** button:

1. Click the endpoint to expand it
2. Click **"Try it out"** button
3. Fill in request parameters (path, query, body)
4. Click **"Execute"**
5. View the exact request sent and response received

### Authentication

For endpoints requiring JWT tokens:

1. **Get a token:**
   - Use Auth Service `/login` endpoint to get a JWT token
   
2. **Authorize in Swagger UI:**
   - Click the green **"Authorize"** button (top right)
   - Enter: `Bearer <your-jwt-token>`
   - Click **"Authorize"**
   
3. **Make authenticated requests:**
   - All subsequent requests automatically include the token
   - Token persists until you click **"Logout"**

### Schema Documentation

Every request and response includes detailed schema information:

- **Data types**: string, integer, boolean, array, object, etc.
- **Required fields**: marked with `*`
- **Optional fields**: optional
- **Format**: email, date-time, UUID, etc.
- **Examples**: sample values for each field
- **Constraints**: min/max length, patterns, etc.

---

## OpenAPI Specification

### Raw Specification

The raw OpenAPI 3.0 JSON specification is available at:

- **Individual service**: `http://localhost:<port>/v3/api-docs`
  - Auth: http://localhost:8081/v3/api-docs
  - Training: http://localhost:8082/v3/api-docs
  - Tracking: http://localhost:8083/v3/api-docs
  - Notification: http://localhost:8084/v3/api-docs
  
- **API Gateway aggregated**: http://localhost:8080/v3/api-docs

### Spec Format

The specification is in JSON format following OpenAPI 3.0 standard:

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "Auth Service API",
    "description": "...",
    "version": "1.0.0"
  },
  "paths": { ... },
  "components": {
    "schemas": { ... },
    "securitySchemes": { ... }
  }
}
```

---

## Integration with Tools

### Postman Import

Import Swagger specs into Postman for advanced testing:

1. Open Postman
2. Click **"Import"** → **"Link"**
3. Paste the OpenAPI spec URL: `http://localhost:8081/v3/api-docs`
4. Click **"Import"**
5. Postman auto-generates requests for all endpoints

### IntelliJ IDEA Integration

IntelliJ provides built-in OpenAPI support:

1. Install "OpenAPI (Swagger) Editor" plugin
2. Open `http://localhost:8081/v3/api-docs` in browser
3. Copy the JSON
4. Paste into a new `.json` file in IntelliJ
5. Right-click → "Generate REST Client code"

### cURL

Use cURL with OpenAPI specs to test endpoints:

```bash
# Get OpenAPI spec
curl http://localhost:8081/v3/api-docs | jq .

# Test endpoint directly
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}'
```

---

## Security

### Authentication Headers

All protected endpoints require:

```
Authorization: Bearer <JWT-token>
X-Trace-Id: <trace-id>
```

### Role-Based Access Control (RBAC)

Different endpoints require different user roles:

| Role | Description | Access |
|------|-------------|--------|
| `USER` | Regular user | Basic user operations |
| `PROFESSIONAL` | Professional trainer | Training program creation, advanced features |
| `ADMIN` | Administrator | Full system access, user management |

Each endpoint's documentation specifies required roles.

### Bearer Token Format

JWT tokens are signed and include:
- `iss` (issuer): "gym-auth-service"
- `sub` (subject): user ID
- `aud` (audience): "gym-api"
- `roles`: array of user roles
- `exp` (expiration): token expiration timestamp

Example token decoded:
```json
{
  "iss": "gym-auth-service",
  "sub": "user-123",
  "aud": "gym-api",
  "roles": ["USER", "PROFESSIONAL"],
  "exp": 1703001600
}
```

---

## Common Response Codes

| Code | Meaning | When |
|------|---------|------|
| **200** | OK | Request succeeded, data returned |
| **201** | Created | Resource created successfully |
| **204** | No Content | Request succeeded, no response body |
| **400** | Bad Request | Invalid input data or parameters |
| **401** | Unauthorized | Authentication required or failed |
| **403** | Forbidden | Authenticated but insufficient permissions |
| **404** | Not Found | Resource does not exist |
| **409** | Conflict | Resource conflict (e.g., duplicate) |
| **422** | Unprocessable Entity | Validation failed on input |
| **500** | Internal Server Error | Server error occurred |

---

## Examples

### Example 1: User Registration

**Endpoint:** `POST /auth/register`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "userType": "USER"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "userId": "user-123",
  "message": "User registered successfully. Please verify your email.",
  "token": null,
  "refreshToken": null
}
```

### Example 2: User Login

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "userId": "user-123",
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Example 3: Create Exercise (Protected Endpoint)

**Endpoint:** `POST /training/exercises`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-Trace-Id: trace-123
```

**Request:**
```json
{
  "name": "Bench Press",
  "description": "Chest and triceps exercise",
  "targetMuscles": ["chest", "triceps", "shoulders"],
  "difficulty": "intermediate"
}
```

**Response (201 Created):**
```json
{
  "id": "exercise-456",
  "name": "Bench Press",
  "description": "Chest and triceps exercise",
  "targetMuscles": ["chest", "triceps", "shoulders"],
  "difficulty": "intermediate",
  "createdAt": "2026-03-20T10:30:00Z"
}
```

---

## Troubleshooting

### Swagger UI Not Loading

1. **Check service is running:**
   ```bash
   curl http://localhost:8081/health
   ```

2. **Verify Swagger dependency:**
   - Check `pom.xml` includes `springdoc-openapi-starter-webmvc-ui`

3. **Clear browser cache:**
   - Hard refresh: `Ctrl+Shift+R` (Windows) or `Cmd+Shift+R` (Mac)

### 401 Unauthorized in Swagger

1. **Token expired:** Get a new token from `/auth/login`
2. **Invalid format:** Ensure token is prefixed with `Bearer `
3. **Wrong URL:** Verify you're testing against correct service URL

### 403 Forbidden

**Insufficient permissions:**
- Verify your user has required role (USER, PROFESSIONAL, ADMIN)
- Check endpoint documentation for required roles
- Login with different user account if needed

---

## Best Practices

1. **Always test in Swagger first** before writing client code
2. **Use "Try it out" feature** to validate request/response format
3. **Copy exact error responses** to understand validation requirements
4. **Save successful requests** from Swagger history
5. **Use OpenAPI spec** for code generation in client libraries
6. **Bookmark service URLs** for quick access during development

---

**Version:** 1.0.0  
**Last Updated:** 2026-03-20  
**Maintained By:** Gym Platform Development Team
```

- [ ] Verify file created: `ls -la docs/SWAGGER_USAGE_GUIDE.md`

- [ ] Commit:

```bash
git add docs/SWAGGER_USAGE_GUIDE.md
git commit -m "docs: Add comprehensive Swagger/OpenAPI usage guide"
```

---

## Task C11: Full Build and Verification

**Files:**
- No new files, verification only

### Step C11.1: Clean Build All Services

- [ ] Run: `mvn clean package -DskipTests`
- [ ] Expected: All 7 services BUILD SUCCESS (including api-gateway)

### Step C11.2: Run Unit Tests

- [ ] Run: `mvn clean test -DskipITs`
- [ ] Expected: 95%+ tests PASS (same as before, no regression)

### Step C11.3: Verify Swagger UI Accessible (Without Docker)

After ensuring services compile:

- [ ] Start auth service: `mvn spring-boot:run -pl auth-service`
- [ ] Wait 10 seconds for startup
- [ ] In browser, visit: http://localhost:8080/swagger-ui.html
- [ ] Expected: Swagger UI loads with Auth Service endpoints documented

- [ ] Stop auth service: `Ctrl+C`

### Step C11.4: Final Verification Commit

- [ ] Run: `git log --oneline -10`
- [ ] Verify all Swagger/OpenAPI commits present

- [ ] Final commit message:

```bash
git commit --allow-empty -m "docs: Part C - Swagger/OpenAPI implementation complete

- Added springdoc-openapi dependencies to all 6 services
- Created OpenAPI configuration beans for each service
- Added @Operation, @ApiResponse, @Tag annotations to all controllers
- Created comprehensive Swagger usage guide
- All services compile successfully
- Swagger UI available at /swagger-ui.html on each service
- OpenAPI spec available at /v3/api-docs on each service"
```

---

## Summary

### Deliverables

✅ **Dependencies Added**
- springdoc-openapi-starter-webmvc-ui on: auth, training, tracking, notification services
- springdoc-openapi-starter-webflux-ui on: api-gateway

✅ **OpenAPI Configurations**
- OpenApiConfig.java created for: auth, training, tracking, notification services
- Each defines service metadata, version, contact info, security schemes

✅ **Controller Annotations**
- @Tag on all controllers
- @Operation on all public endpoints
- @ApiResponse on all endpoints with HTTP status codes
- @SecurityRequirement on protected endpoints

✅ **Documentation**
- SWAGGER_USAGE_GUIDE.md with comprehensive examples and troubleshooting

✅ **Verification**
- All services compile successfully
- Swagger UI auto-generated at /swagger-ui.html
- OpenAPI specs available at /v3/api-docs
- No test regressions

### Result

All Gym Platform microservices now have interactive API documentation accessible via Swagger UI, making it easier for developers and API consumers to:
- Explore available endpoints
- Understand request/response formats
- Test endpoints directly
- Integrate with tools like Postman and IDE extensions
