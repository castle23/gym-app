# Testing Standards

> Testing rules for the Gym Platform API. Minimum **85% code coverage** across all services.

## Test Types

1. **Unit Tests** — `@ExtendWith(MockitoExtension.class)`. No Spring context. Fast. Test one class in isolation.
2. **Controller Tests** — `@WebMvcTest(ControllerClass.class)`. Test HTTP layer with mocked services.
3. **Integration Tests** — `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`. Full context with H2 database.

## Controller Test Setup (CRITICAL)

4. Controller tests **MUST** include both imports:
   ```java
   @WebMvcTest(ProfileController.class)
   @Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
   class ProfileControllerTest { ... }
   ```
   > Without `GymTestSecurityAutoConfiguration`, security filters block all requests.
   > Without `GymExceptionHandlerAutoConfiguration`, exceptions won't map to correct HTTP statuses.

5. **MockMvc does NOT include the context-path.** Use `/profile`, NOT `/auth/profile`.
   ```java
   // CORRECT
   mockMvc.perform(get("/profile").header("X-User-Id", "1"))
   // WRONG — will return 404
   mockMvc.perform(get("/auth/profile").header("X-User-Id", "1"))
   ```
   > This is the #1 cause of mysterious 404s in controller tests.

## Exception-to-Status Mapping

6. Exception classes live in `com.gym.common.exception`. Mappings:
   | Exception                    | HTTP Status |
   |------------------------------|-------------|
   | `AuthenticationException`    | 401         |
   | `ResourceNotFoundException`  | 404         |
   | `UnauthorizedException`      | **403**     |
   | `InvalidDataException`       | 400         |
   | `DuplicateResourceException` | 409         |

7. **GOTCHA**: `UnauthorizedException` maps to **403 Forbidden**, not 401. 401 is authentication failure; 403 is authorization failure (authenticated but insufficient permissions).

## Test Naming & Structure

8. Name pattern: `should[ExpectedOutcome]When[Condition]`.
   ```java
   void shouldReturn404WhenUserNotFound() { ... }
   void shouldCreatePlanWhenValidRequest() { ... }
   ```
9. Follow **Arrange-Act-Assert** (AAA):
   - **Arrange** — Set up test data and mocks.
   - **Act** — Call the method under test.
   - **Assert** — Verify the outcome.
10. One logical assertion per test. Multiple `assertThat` calls on the same object are acceptable.
11. Never test private methods directly. Test through public interfaces.

## Mocking Rules

12. Mock external dependencies only. Never mock the class under test.
13. Use `@Mock` for dependencies, `@InjectMocks` for the class under test (unit tests).
14. Use `@MockBean` for Spring-managed dependencies (controller/integration tests).
15. Verify interactions with `verify()` when side effects matter (e.g., service called repository).

## Test Data & Configuration

16. Use **H2 in-memory database** for test profile. Configure in `application-test.yml`.
17. Use `@Sql` or `@BeforeEach` to set up test data. Clean up after each test.
18. Use `TestEntityFactory` or builder patterns for creating test entities — avoid magic numbers.

## Running Tests

19. All services: `mvn clean test`
20. Single service: `mvn test -pl auth-service`
21. Single class: `mvn test -Dtest=ProfileControllerTest`
22. Single method: `mvn test -Dtest=ProfileControllerTest#shouldReturn404WhenUserNotFound`
23. **All tests must pass before any commit.**
    > See also: `../rules/git-workflow.md` for pre-commit requirements.

## Coverage

24. Minimum **85% line coverage** per service.
25. Focus coverage on service and controller layers. Entity/DTO coverage is lower priority.
26. Exclude generated code, configuration classes, and main application classes from coverage reports.

## Security Testing

27. Test protected endpoints **with and without** authentication headers.
28. Test role-based access: verify `ROLE_USER` cannot access `ROLE_ADMIN` endpoints.
29. Test that `X-User-Id` header is required where expected.
    > See also: `../rules/security-standards.md` for security layer details.
