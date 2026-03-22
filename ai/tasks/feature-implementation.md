# Task: Feature Implementation

## Prerequisites
- [ ] Clear requirement or user story defined
- [ ] Target service identified (Auth, Training, Tracking, or Notification)
- [ ] No blocking ADR decisions pending
- [ ] Clean git state

## Workflow

### 1. Create Branch
```bash
git checkout -b feat/[short-description]
```

### 2. Define DTOs
- Create request DTO: `[Entity]RequestDto`
- Create response DTO: `[Entity]ResponseDto`
- Add `@Schema(description = "...", example = "...")` on every field
- Add validation annotations: `@NotNull`, `@NotBlank`, `@Size`, `@Email`, etc.
- Location: `com.gym.[service].dto`

### 3. Create/Update Entity
- `@Entity`, `@Table(name = "...", schema = "[service]_schema")`
- Add fields, relationships (`@ManyToOne`, `@OneToMany`, etc.)
- Add Flyway migration: `V[N]__description.sql` in `src/main/resources/db/migration/`
- Location: `com.gym.[service].entity`

### 4. Implement Repository
- Extend `JpaRepository<Entity, Long>`
- Add custom query methods if needed (`@Query` or derived queries)
- Location: `com.gym.[service].repository`

### 5. Implement Service
- `@Service`, `@RequiredArgsConstructor`
- `@Transactional` on write methods
- `@Transactional(readOnly = true)` on read methods
- Throw custom exceptions for business rule violations
- Location: `com.gym.[service].service`

### 6. Implement Controller
- `@RestController`, `@RequestMapping("/api/v1/[resource]")`
- `@Tag(name = "[Resource]")` for Swagger grouping
- `@Operation(summary = "...")` on each method
- `@ApiResponse(responseCode = "...", description = "...")` for each response
- `@PreAuthorize("hasRole('...')")` for protected endpoints
- Return `ResponseEntity<>` with correct status codes
- Location: `com.gym.[service].controller`

### 7. Write Unit Tests
```bash
mvn test -pl [service] -Dtest=[ServiceClass]Test
```
- Use `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- Cover happy path, error paths, edge cases
- See `ai/skills/test-generation.md` for templates

### 8. Write Controller Tests
```bash
mvn test -pl [service] -Dtest=[Controller]Test
```
- Use `@WebMvcTest([Controller].class)`
- `@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})`
- Test request validation, response structure, status codes
- MockMvc paths: NO context-path prefix

### 9. Write Integration Tests
```bash
mvn test -pl [service] -Dtest=[Controller]IT
```
- `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- End-to-end with real DB (test profile)

### 10. Verify Coverage
```bash
mvn jacoco:report -pl [service]
```
- Must be >= 85% on new code

### 11. Swagger Verification
- Start service and check `http://localhost:[port]/swagger-ui.html`
- Verify endpoint appears with correct description, parameters, responses

### 12. Commit
```bash
git add -A
git commit -m "feat(scope): description of new feature"
```

## Package Structure
```
com.gym.[service]/
├── controller/    @RestController classes
├── dto/           Request/Response DTOs with @Schema
├── entity/        @Entity JPA classes
├── exception/     Custom exception classes
├── repository/    JpaRepository interfaces
└── service/       @Service business logic
```

## Completion Checklist
- [ ] Endpoint documented with `@Operation` and `@ApiResponse`
- [ ] DTOs have `@Schema` on all fields
- [ ] Security annotations (`@PreAuthorize`) on protected endpoints
- [ ] Input validation (`@Valid`, constraint annotations)
- [ ] Custom error handling (no generic 500s)
- [ ] Unit tests passing (service layer)
- [ ] Controller tests passing (WebMvcTest)
- [ ] Integration tests passing (SpringBootTest)
- [ ] Coverage >= 85%
- [ ] Flyway migration included if schema changed
- [ ] Commit follows `feat(scope): description` format

## References
- `ai/skills/test-generation.md`
- `ai/skills/architecture-design.md`
- `ai/rules/coding-standards.md`
- `ai/rules/testing-standards.md`
- `ai/rules/documentation-standards.md`
