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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Get all users with pagination
     * @param page Zero-indexed page number
     * @param size Page size
     * @return Paginated user list
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userService.getAllUsers(pageable);

        List<UserDTO> content = users.getContent()
            .stream()
            .map(userMapper::toDTO)
            .collect(Collectors.toList());

        PageResponse<UserDTO> response = new PageResponse<>(
            content,
            users.getPageable().getPageNumber(),
            users.getPageable().getPageSize(),
            users.getTotalElements(),
            users.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Create new user
     * @param request User creation request
     * @return Created user with 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        User user = userService.createUser(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userMapper.toDTO(user));
    }

    /**
     * Get user by ID
     * @param id User UUID
     * @return User data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR') or #id == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {

        return userService.getUserById(id)
            .map(userMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Update user
     * @param id User UUID
     * @param request Update request
     * @return Updated user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        try {
            User user = userService.updateUser(id, request);
            return ResponseEntity.ok(userMapper.toDTO(user));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete user
     * @param id User UUID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {

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

    @NotNull(message = "Role is required")
    private String role;
}

@Data
@Builder
public class UserDTO {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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
    private final UserMapper userMapper;

    /**
     * Create new user with hashed password
     */
    @Transactional
    public User createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.valueOf(request.getRole()))
            .isActive(true)
            .build();

        User savedUser = userRepository.save(user);
        log.info("User created: {} ({})", savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Update user
     */
    @Transactional
    public User updateUser(UUID id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (StringUtils.hasText(request.getEmail()) 
            && !user.getEmail().equals(request.getEmail())
            && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userRepository.delete(user);
        log.info("User deleted: {} ({})", user.getUsername(), id);
    }
}
```

## Repository Pattern

### Spring Data JPA Repository

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByIsActiveTrue(Pageable pageable);

    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);

    @Query(value = """
        SELECT u.id, u.username, COUNT(t.id) as token_count
        FROM users u
        LEFT JOIN tokens t ON u.id = t.user_id
        WHERE u.created_at > NOW() - INTERVAL '30 days'
        GROUP BY u.id, u.username
        ORDER BY token_count DESC
        """, nativeQuery = true)
    List<Object[]> findRecentActiveUsers();
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
