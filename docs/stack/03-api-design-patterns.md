# API Design Patterns

## Overview

Gym Platform implements industry-standard design patterns for REST API development. This document covers the architectural patterns (MVC, Repository, Dependency Injection, Service layer) used across all microservices to ensure consistency, maintainability, and testability.

## Layered Architecture (MVC Pattern)

### Architecture Layers

```
┌─────────────────────────────────────────┐
│         REST Controllers                │  ← HTTP Request/Response
├─────────────────────────────────────────┤
│       Service Layer (Business Logic)    │  ← Core Logic
├─────────────────────────────────────────┤
│         Repository Layer                │  ← Data Access
├─────────────────────────────────────────┤
│         JPA/ORM Entities                │  ← Database Model
├─────────────────────────────────────────┤
│           PostgreSQL                    │  ← Persistent Storage
└─────────────────────────────────────────┘
```

### Layer Responsibilities

#### Controller Layer
- HTTP request/response handling
- Input validation (DTOs)
- Response formatting
- HTTP status codes

#### Service Layer
- Business logic implementation
- Transaction management
- Business rule validation
- Cross-service coordination

#### Repository Layer
- CRUD operations
- Query building
- Database access
- Query optimization

#### Entity Layer
- Database table mapping
- Column definitions
- Relationships
- JPA annotations

## Controller Pattern

### REST Controller Example

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequiresRole({"ROLE_ADMIN"})
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PostMapping
    @RequiresRole({"ROLE_ADMIN"})
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        String currentUserId = UserContextHolder.getUserId();
        Set<String> roles = UserContextHolder.getRoles();
        if (!id.toString().equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
            throw new UnauthorizedException("Cannot access other users' data");
        }
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        String currentUserId = UserContextHolder.getUserId();
        Set<String> roles = UserContextHolder.getRoles();
        if (!id.toString().equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
            throw new UnauthorizedException("Cannot update other users' data");
        }
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresRole({"ROLE_ADMIN"})
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Request/Response DTOs

```java
@Data
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    private String password;
}

@Data
@Builder
public class UserDTO {

    private Long id;  // BIGSERIAL PK
    private String username;
    private String email;
    private String role;  // ROLE_USER, ROLE_PROFESSIONAL, ROLE_ADMIN
    private LocalDateTime createdAt;
}
```

## Service Pattern

### Service Implementation

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidDataException("Username already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role("ROLE_USER")
            .build();

        return toDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
```

## Repository Pattern

### Spring Data JPA Repository

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByRole(String role, Pageable pageable);
}
```

### Custom Repository Implementation

```java
@Component
@RequiredArgsConstructor
public class UserRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public Page<User> findByComplexCriteria(UserSearchCriteria criteria, Pageable pageable) {

        String sql = """
            SELECT id, username, email, role, is_active, created_at, updated_at
            FROM users
            WHERE 1=1
            """;

        List<Object> params = new ArrayList<>();

        if (StringUtils.hasText(criteria.getUsername())) {
            sql += " AND username ILIKE ?";
            params.add("%" + criteria.getUsername() + "%");
        }

        if (criteria.getRole() != null) {
            sql += " AND role = ?";
            params.add(criteria.getRole());
        }

        if (criteria.getIsActive() != null) {
            sql += " AND is_active = ?";
            params.add(criteria.getIsActive());
        }

        sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        List<User> users = jdbcTemplate.query(sql, params.toArray(), 
            new UserRowMapper());

        return new PageImpl<>(users, pageable, users.size());
    }
}
```

## Dependency Injection Pattern

### Constructor Injection (Recommended)

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // All dependencies immutable and clearly visible
}
```

### Field Injection (Discouraged)

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Testing is harder, dependencies not obvious
}
```

### Setter Injection (Flexible)

```java
@Service
public class UserService {

    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

## Entity Mapper Pattern

### MapStruct Mapper

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    List<UserDTO> toDTOList(List<User> users);

    User toEntity(CreateUserRequest request);

    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    @Mapping(source = "userId", target = "id")
    @Mapping(source = "userName", target = "username")
    User fromExternalDTO(ExternalUserDTO dto);
}
```

### Manual Mapper

```java
@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().toString())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    public User toEntity(CreateUserRequest request) {
        return User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .role(UserRole.valueOf(request.getRole()))
            .build();
    }
}
```

## Exception Handling Pattern

### Custom Exceptions

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(message)
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

## Transaction Pattern

### Transaction Management

```java
@Service
public class UserService {

    /**
     * Read-only transaction for queries
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    /**
     * Write transaction with rollback on exception
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(CreateUserRequest request) {
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .build();

        return userRepository.save(user);
    }

    /**
     * Propagation: requires new transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditUserAction(UUID userId, String action) {
        auditService.log(userId, action);
    }
}
```

## Validation Pattern

### Bean Validation

```java
public record UserRegistrationRequest(

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain uppercase, lowercase, and digits")
    String password
) {}
```

### Custom Validator

```java
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !userRepository.existsByEmail(value);
    }
}

@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface UniqueEmail {
    String message() default "Email is already registered";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

## Pagination Pattern

### Pagination Response

```java
@Data
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;

    public PageResponse(List<T> content, int page, int pageSize, 
                        long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = page == 0;
        this.isLast = page == totalPages - 1;
        this.hasNext = !isLast;
        this.hasPrevious = !isFirst;
    }
}
```

## Key References

- [Spring Framework Documentation](https://spring.io/projects/spring-framework)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Clean Code: A Handbook of Agile Software Craftsmanship](https://www.oreilly.com/library/view/clean-code/9780136083238/)
- See also: [docs/arquitectura/02-microservices-architecture.md](../arquitectura/02-microservices-architecture.md)
- See also: [docs/development/01-getting-started.md](../development/01-getting-started.md)
