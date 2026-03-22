# Skill: Test Generation

## Description
Generate tests for Java classes following project standards, with correct annotations and patterns per test type.

## Prerequisites
- Target class exists and compiles
- Familiarity with `ai/rules/testing-standards.md`
- Test dependencies available (JUnit 5, Mockito, Spring Boot Test)

## Steps

1. **Identify Class Type**
   - `*Service` → Unit test with mocks
   - `*Controller` → WebMvcTest (slice test)
   - `*Repository` → Integration test (or skip if only JpaRepository methods)
   - Utility / helper → Unit test, no mocks needed

2. **Select Test Template** (see Templates below)

3. **Generate Test Class**
   - Name: `[ClassName]Test` (unit) or `[ClassName]IT` (integration)
   - Package: mirrors source package under `src/test/java/`
   - Apply correct class-level annotations

4. **Write Test Methods**
   - Follow **AAA pattern**: Arrange → Act → Assert
   - Descriptive names: `should_ReturnUser_When_ValidIdProvided()`
   - One logical assertion per test (multiple `assertThat` calls on same object are fine)
   - Cover: happy path, edge cases, error paths, boundary values

5. **Add Assertions**
   - Use AssertJ: `assertThat(result).isEqualTo(expected)`
   - For exceptions: `assertThatThrownBy(() -> ...).isInstanceOf(X.class)`
   - For collections: `assertThat(list).hasSize(n).extracting("field").contains(...)`

6. **Verify Compilation**
   - Run: `mvn test-compile -pl [service]`
   - Fix any import or compilation issues before executing

## Templates

### Unit Test (Service)
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void should_ReturnUser_When_ValidIdProvided() {
        // Arrange
        var user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        var result = userService.getUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }
}
```

### Controller Test (WebMvcTest)
```java
@WebMvcTest(UserController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void should_ReturnOk_When_GetUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
```

### Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_CreateAndRetrieveUser() throws Exception {
        // Full end-to-end test with real DB
    }
}
```

## GOTCHAS — Read Before Writing Tests

1. **MockMvc paths have NO context-path.**
   Use `/api/v1/users`, NOT `/auth-service/api/v1/users`.

2. **`UnauthorizedException` maps to 403, not 401.**
   `assertExpect(status().isForbidden())` for access denied.

3. **WebMvcTest requires explicit imports.**
   Always add `@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})`.
   Without this, security auto-config or exception handling won't load.

4. **Use `@MockBean` in slice tests, `@Mock` in unit tests.**
   Mixing them causes context loading failures.

5. **Test profile config.** Integration tests need `application-test.yml` with test DB settings.

6. **ObjectMapper for request bodies.** Inject `@Autowired ObjectMapper` rather than manual JSON strings.

## Output
- Test file at `[service]/src/test/java/com/gym/[service]/.../*Test.java`
- All tests pass: `mvn test -pl [service] -Dtest=TestClassName`
- Coverage contribution toward >= 85% target

## References
- `ai/rules/testing-standards.md`
- `ai/rules/coding-standards.md`
- `ai/skills/code-analysis.md` (for verifying test quality)
