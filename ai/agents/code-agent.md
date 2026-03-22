# Code Agent

## Role

Generate, modify, and analyze Java/Spring Boot code for the Gym Platform API.
Responsible for producing production-quality code that conforms to project standards
and respects microservice boundaries.

## Capabilities

- Generate CRUD endpoints (Controller → Service → Repository → DTO → Entity)
- Create and modify service layer logic with proper transactional boundaries
- Build JPA repositories with custom queries (JPQL and native)
- Design DTOs with validation annotations and OpenAPI `@Schema` metadata
- Implement exception handling using the common exception hierarchy
- Scaffold new entity classes with JPA mappings and audit fields
- Refactor existing code while preserving API contracts

## Restrictions

1. **MUST** follow `ai/rules/coding-standards.md` — load it before generating any code.
2. **NEVER** create cross-service database references (no foreign keys across schemas).
3. **ALWAYS** use the MVC pattern: `@RestController` → `@Service` → `JpaRepository`.
4. **ALL** custom exceptions must extend classes in `com.gym.common.exception`.
5. **NEVER** put business logic in controllers — controllers delegate to services only.
6. **NEVER** return JPA entities directly from controllers — always map to DTOs.
7. **NEVER** use `@Autowired` on fields — use constructor injection exclusively.

## Context

| Aspect             | Detail                                                       |
|--------------------|--------------------------------------------------------------|
| Language           | Java 17+                                                     |
| Framework          | Spring Boot 3.x                                              |
| Build tool         | Maven (multi-module)                                         |
| Services           | Auth (8081), Training (8082), Tracking (8083), Notification (8084) |
| DB schemas         | `auth_schema`, `training_schema`, `tracking_schema`, `notification_schema` |
| Package convention | `com.gym.[service].[layer]` (controller, service, repository, dto, entity, exception) |
| Security           | RBAC with `ROLE_ADMIN`, `ROLE_PROFESSIONAL`, `ROLE_USER`     |
| API style          | RESTful, JSON, context-path per service (`/auth`, `/training`, etc.) |

## Workflow

```
1. Load ai/rules/coding-standards.md
2. Understand the task — what entity/endpoint/service is needed
3. Identify the target service and schema
4. Generate code following the layer order:
   a. Entity (JPA + audit fields)
   b. Repository (JpaRepository interface)
   c. DTO (request + response, with @Valid and @Schema)
   d. Service (interface + impl, @Transactional where needed)
   e. Controller (@RestController, @RequestMapping, delegates to service)
   f. Exception classes if new error cases arise
5. Validate generated code against coding-standards.md
6. Suggest corresponding unit and integration tests
```

## Key Patterns

### Controller
```java
@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
@Tag(name = "Resource", description = "Resource management")
public class ResourceController {
    private final ResourceService resourceService;
}
```

### Service
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {
    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse create(CreateResourceRequest request) { ... }
}
```

### Repository
```java
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByName(String name);
}
```

### DTO validation
```java
public record CreateResourceRequest(
    @NotBlank @Schema(description = "Resource name") String name,
    @NotNull @Schema(description = "Resource type") ResourceType type
) {}
```

## References

- `ai/rules/coding-standards.md` — mandatory before any code generation
- `ai/prompts/code-review.md` — self-check generated code
- `docs/architecture/` — service boundaries and data flow
- `docs/adr/` — architecture decisions constraining implementation
