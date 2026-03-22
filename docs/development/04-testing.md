# Testing Guide

## Overview

Unit and integration testing strategy for Gym Platform microservices using JUnit 5, Mockito, and Spring Boot Test.

## Test Types

### Unit Tests (`*ServiceTest`, `*RepositoryTest`)

Plain JUnit 5 + Mockito, no Spring context. Fast, no DB required.

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldCreateNotification() {
        // given
        when(notificationRepository.save(any())).thenReturn(mockNotification());
        // when / then
        assertThat(notificationService.create(request)).isNotNull();
    }
}
```

### Controller Tests (`*ControllerTest`)

`@WebMvcTest` slice — loads only the web layer. Does **not** load auto-configurations from classpath.

**Required imports on every controller test:**

```java
@WebMvcTest(NotificationController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;
}
```

- `GymTestSecurityAutoConfiguration` — `@Profile("test")`, permits all requests
- `GymExceptionHandlerAutoConfiguration` — `@RestControllerAdvice`, maps exceptions to HTTP status codes

Both must be explicitly `@Import`ed because `@WebMvcTest` does not scan `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Integration Tests (`*AuthorizationTest`)

`@SpringBootTest` + `@AutoConfigureMockMvc` — loads full context with real security.

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/profile"))
               .andExpect(status().isUnauthorized());
    }
}
```

> **Note**: MockMvc does **not** include `context-path` in request URLs. Use paths relative to context-path (e.g. `/profile` not `/auth/profile`).

## Exception Mapping

`GymExceptionHandlerAutoConfiguration` maps exceptions to HTTP status codes:

| Exception | HTTP Status |
|-----------|-------------|
| `ResourceNotFoundException` | 404 Not Found |
| `UnauthorizedException` | 403 Forbidden |
| `InvalidDataException` | 400 Bad Request |
| `IllegalArgumentException` | 400 / 403 / 404 (by message content) |
| `MethodArgumentNotValidException` | 400 Bad Request |
| `MissingServletRequestParameterException` | 400 Bad Request |
| `MissingRequestHeaderException` | 400 Bad Request |

> `UnauthorizedException` maps to **403 Forbidden**, not 401 — it represents an authorization failure (authenticated but not permitted), not an authentication failure.

All exception classes live in `com.gym.common.exception`.

## Test Configuration

### application.properties (auth-service test)

`src/test/resources/application.properties` overrides datasource for tests:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

### H2 In-Memory Database

Training, tracking, and notification services use H2 for tests:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

## Running Tests

```bash
# All tests
mvn test

# Single service
mvn test -pl training-service

# Single test class
mvn test -pl notification-service -Dtest=NotificationControllerTest

# Skip tests
mvn package -DskipTests
```
