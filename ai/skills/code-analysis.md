# Skill: Code Analysis

## Description
Analyze Java code quality and generate a structured report with actionable findings categorized by severity.

## Prerequisites
- Access to target source files in the service module
- Familiarity with `ai/rules/coding-standards.md`
- Java 17+ / Spring Boot 3.x codebase

## Steps

1. **Identify Target File/Module**
   - Determine scope: single class, package, or entire service
   - Locate files under `[service]/src/main/java/com/gym/[service]/`

2. **Check Naming Conventions**
   - Classes: PascalCase, suffixed by role (`*Service`, `*Controller`, `*Repository`, `*Dto`, `*Exception`)
   - Methods: camelCase, verb-first (`getUserById`, `createTrainingSession`)
   - Constants: UPPER_SNAKE_CASE
   - Packages: lowercase, singular (`entity`, `dto`, `service`, `controller`, `repository`)

3. **Check Error Handling**
   - Custom exceptions extend `RuntimeException` or project base exceptions
   - No empty catch blocks or silent swallows
   - `@ExceptionHandler` or global handler covers all custom exceptions
   - Proper HTTP status codes mapped to exceptions

4. **Check Logging Levels**
   - ERROR: unrecoverable failures, external service failures
   - WARN: recoverable issues, fallback paths taken
   - INFO: business events (user registered, training created)
   - DEBUG: method entry/exit, variable state (never in production)
   - No `System.out.println` or `e.printStackTrace()`

5. **Check SOLID Adherence**
   - **S**: Each class has a single responsibility
   - **O**: Behavior extended via new classes, not modifying existing ones
   - **L**: Subtypes substitutable for base types
   - **I**: No fat interfaces; clients depend only on methods they use
   - **D**: Depend on abstractions (`@Service` injects interfaces or abstractions)

6. **Check Method Size**
   - Methods should be < 50 lines
   - Cyclomatic complexity per method should be <= 10
   - Flag deeply nested logic (> 3 levels)

7. **Check Security**
   - No hardcoded secrets, passwords, or API keys
   - Input validation via `@Valid`, `@NotNull`, `@Size`, etc.
   - `@PreAuthorize` or role checks on protected endpoints
   - No SQL concatenation (use parameterized queries / Spring Data)

8. **Generate Report**
   - Group issues by severity
   - Include file path, line number (if identifiable), and recommendation

## Metrics to Evaluate
| Metric                      | Target       |
|-----------------------------|--------------|
| Cyclomatic complexity       | <= 10/method |
| SOLID adherence             | All 5 met    |
| Test coverage               | >= 85%       |
| Error handling completeness | 100% paths   |
| Method length               | < 50 lines   |

## Output Format
```
## Code Analysis Report: [ClassName / Module]
Date: YYYY-MM-DD

### CRITICAL
- [FILE:LINE] Description — Recommendation

### HIGH
- [FILE:LINE] Description — Recommendation

### MEDIUM
- [FILE:LINE] Description — Recommendation

### LOW
- [FILE:LINE] Description — Recommendation

### Summary
- Total issues: N (C: x, H: x, M: x, L: x)
- SOLID score: X/5
- Estimated complexity: low/medium/high
```

## Severity Definitions
| Severity | Meaning                                       |
|----------|-----------------------------------------------|
| CRITICAL | Security vulnerability, data loss risk         |
| HIGH     | Bug-prone pattern, missing error handling      |
| MEDIUM   | Maintainability concern, naming violation      |
| LOW      | Style issue, minor improvement opportunity     |

## References
- `ai/rules/coding-standards.md`
- `ai/rules/project-overview.md`
- `ai/rules/documentation-standards.md`
