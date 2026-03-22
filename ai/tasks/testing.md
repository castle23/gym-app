# Task: Testing

## Prerequisites
- [ ] Code to test exists and compiles
- [ ] Test type determined: unit, controller (WebMvcTest), or integration
- [ ] Test dependencies available (JUnit 5, Mockito, Spring Boot Test)

## Workflow

### 1. Identify Class to Test
- Locate the class under `[service]/src/main/java/com/gym/[service]/`
- Determine the class role: Service, Controller, Repository, Utility

### 2. Determine Test Type
| Class Type   | Test Type    | Annotation Stack                                       |
|--------------|--------------|--------------------------------------------------------|
| Service      | Unit         | `@ExtendWith(MockitoExtension.class)`                  |
| Controller   | Slice        | `@WebMvcTest` + `@Import` security/exception configs   |
| Repository   | Integration  | `@SpringBootTest` + `@ActiveProfiles("test")`          |
| End-to-end   | Integration  | `@SpringBootTest` + `@AutoConfigureMockMvc`            |

### 3. Apply Template
- Use the correct template from `ai/skills/test-generation.md`
- Ensure all required annotations are present
- For WebMvcTest: ALWAYS include:
  ```java
  @Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
  ```

### 4. Write Tests (AAA Pattern)
```java
@Test
void should_[ExpectedBehavior]_When_[Condition]() {
    // Arrange — set up test data and mocks
    // Act — call the method under test
    // Assert — verify the result
}
```
- Cover: happy path, validation errors, not-found cases, auth failures, edge cases
- One logical assertion per test method
- Use AssertJ for assertions

### 5. Run Tests
```bash
# Single test class
mvn test -pl [service] -Dtest=TestClassName

# Single test method
mvn test -pl [service] -Dtest=TestClassName#methodName

# All tests in service
mvn test -pl [service]
```

### 6. Check Coverage
```bash
mvn jacoco:report -pl [service]
```
- Open report: `[service]/target/site/jacoco/index.html`
- Target: >= 85% line coverage on the class under test

### 7. Fix Until 85%+
- Identify uncovered lines from JaCoCo report
- Add tests for uncovered branches and paths
- Re-run until coverage target is met

### 8. Commit
```bash
git add -A
git commit -m "test(scope): add tests for [ClassName]"
```

## Key Commands Quick Reference
| Action                    | Command                                          |
|---------------------------|--------------------------------------------------|
| Run all tests             | `mvn test`                                       |
| Run service tests         | `mvn test -pl auth-service`                      |
| Run single test class     | `mvn test -pl [service] -Dtest=UserServiceTest`  |
| Run single test method    | `mvn test -pl [service] -Dtest=UserServiceTest#should_ReturnUser` |
| Compile tests only        | `mvn test-compile -pl [service]`                 |
| Generate coverage report  | `mvn jacoco:report -pl [service]`                |
| Run with verbose output   | `mvn test -pl [service] -Dtest=X -X`             |

## Common Mistakes to Avoid
- Using `/auth-service/api/v1/...` in MockMvc (drop the context-path)
- Expecting 401 for `UnauthorizedException` (it's 403)
- Forgetting `@Import` on `@WebMvcTest` classes
- Mixing `@Mock` and `@MockBean` in the same test class
- Writing tests that depend on execution order
- Testing implementation details instead of behavior

## Completion Checklist
- [ ] All new tests pass
- [ ] No existing tests broken
- [ ] Coverage >= 85% on tested class
- [ ] Correct annotations for test type
- [ ] AAA pattern followed
- [ ] Descriptive test method names: `should_X_When_Y`
- [ ] No test interdependencies
- [ ] Commit follows `test(scope): add tests for X` format

## References
- `ai/skills/test-generation.md` — templates, gotchas, and patterns
- `ai/rules/testing-standards.md` — project testing standards
- `ai/rules/coding-standards.md` — code quality requirements
