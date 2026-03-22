# Integration Testing Guide - Gym Platform API

Guide for writing and running integration tests across the Gym Platform microservices.

**Stack**: Java 17, Spring Boot 3, JUnit 5, Mockito, H2 (in-memory), Maven  
**No Redis, no message broker, no separate test databases** — single `gym_db` with 4 schemas.

---

## Test Types Overview

```
Unit Tests          → @ExtendWith(MockitoExtension.class)  — no Spring context, fast
Controller Tests    → @WebMvcTest                          — web layer only, MockMvc
Integration Tests   → @SpringBootTest + @AutoConfigureMockMvc — full context, real security
```

See [04-testing.md](04-testing.md) for the full reference on test configuration and imports.

---

## Integration Test Scenarios

Integration tests (`*AuthorizationTest`) load the full Spring context with real security filters.
They verify that authentication and authorization rules are enforced end-to-end.

### Pattern: Full Context with MockMvc

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainingControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExerciseService exerciseService;

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/exercises"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WhenValidToken() throws Exception {
        when(exerciseService.getAll(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/exercises")
                   .header("X-User-Id", "1")
                   .header("X-User-Roles", "ROLE_USER"))
               .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenInsufficientRole() throws Exception {
        mockMvc.perform(post("/exercises")
                   .header("X-User-Id", "1")
                   .header("X-User-Roles", "ROLE_USER")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{\"name\":\"Bench\",\"description\":\"Chest\",\"disciplineId\":1}"))
               .andExpect(status().isForbidden());
    }
}
```

> **Key point**: The gateway validates JWT and injects `X-User-Id` / `X-User-Roles` headers.
> In tests, simulate this by setting those headers directly — no JWT token needed.

### Scenario: Auth Service — Register & Login Flow

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterAndLoginSuccessfully() throws Exception {
        // Register
        mockMvc.perform(post("/register")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(Map.of(
                       "email", "test@example.com",
                       "password", "Test123!",
                       "firstName", "Test",
                       "lastName", "User",
                       "role", "ROLE_USER"
                   ))))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.userId").exists())
               .andExpect(jsonPath("$.email").value("test@example.com"));

        // Login
        mockMvc.perform(post("/login")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(Map.of(
                       "email", "test@example.com",
                       "password", "Test123!"
                   ))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").exists())
               .andExpect(jsonPath("$.refreshToken").exists())
               .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void shouldReturn401OnInvalidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(Map.of(
                       "email", "nobody@example.com",
                       "password", "WrongPassword!"
                   ))))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400OnDuplicateEmail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "email", "dup@example.com",
            "password", "Test123!",
            "firstName", "A", "lastName", "B",
            "role", "ROLE_USER"
        ));

        mockMvc.perform(post("/register")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(body))
               .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/register")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(body))
               .andExpect(status().isBadRequest());
    }
}
```

### Scenario: Role-Based Access Control

```java
@Test
void shouldAllowAdminToCreateExercise() throws Exception {
    when(exerciseService.create(any())).thenReturn(stubExercise());

    mockMvc.perform(post("/exercises")
               .header("X-User-Id", "1")
               .header("X-User-Roles", "ROLE_ADMIN")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"name\":\"Squat\",\"description\":\"Legs\",\"disciplineId\":1}"))
           .andExpect(status().isCreated());
}

@Test
void shouldDenyProfessionalFromDeletingExercise() throws Exception {
    mockMvc.perform(delete("/exercises/1")
               .header("X-User-Id", "2")
               .header("X-User-Roles", "ROLE_PROFESSIONAL"))
           .andExpect(status().isForbidden());
}
```

### Scenario: Validation Errors

```java
@Test
void shouldReturn400OnMissingRequiredFields() throws Exception {
    mockMvc.perform(post("/exercises")
               .header("X-User-Id", "1")
               .header("X-User-Roles", "ROLE_ADMIN")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))  // missing required fields
           .andExpect(status().isBadRequest());
}

@Test
void shouldReturn400OnMissingUserIdHeader() throws Exception {
    // X-User-Id header required for protected endpoints
    mockMvc.perform(get("/objectives")
               .header("X-User-Roles", "ROLE_USER"))
           .andExpect(status().isBadRequest());
}
```

---

## Test Configuration

### application.properties (test profile)

Each service has `src/test/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.default_schema=
```

Auth service uses H2 with a custom dialect. Training, tracking, and notification services also use H2.

### Required imports for `@WebMvcTest`

```java
@WebMvcTest(ExerciseController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
class ExerciseControllerTest {
    // ...
}
```

Both auto-configurations must be explicitly imported — `@WebMvcTest` does not scan `META-INF/spring/` autoconfiguration files.

---

## Running Tests

```bash
# All tests across all modules
mvn test

# Single service
mvn test -pl training-service

# Single test class
mvn test -pl auth-service -Dtest=AuthControllerAuthorizationTest

# Skip tests during build
mvn package -DskipTests
```

---

## Test Isolation

Each `@SpringBootTest` test uses H2 in-memory with `ddl-auto=create-drop`, so the schema is recreated fresh for each test run. For `@WebMvcTest`, the service layer is mocked with `@MockBean` — no database involved.

**No shared state between tests** — each test class gets a clean context.

---

## Common Pitfalls

**MockMvc does not include context-path**  
Use paths relative to context-path. For auth-service (context-path `/auth`):
```java
// Correct
mockMvc.perform(post("/login")...)

// Wrong — context-path is not part of MockMvc URL
mockMvc.perform(post("/auth/login")...)
```

**`@WebMvcTest` missing auto-configurations**  
Always `@Import` both `GymTestSecurityAutoConfiguration` and `GymExceptionHandlerAutoConfiguration`.
Without them, security is misconfigured and exceptions return 500 instead of the correct status.

**`@SpringBootTest` authorization tests need `@MockBean` for services**  
Full context loads real security but still needs service layer mocked to avoid DB calls:
```java
@MockBean
private ExerciseService exerciseService;  // prevents real DB queries
```
