# Authorization & RBAC

## Overview

Role-Based Access Control (RBAC) implementation in the Gym Platform: role definitions, permission models, access control policies, and fine-grained authorization. Authorization ensures authenticated users can only access resources they're permitted to use.

**Authorization Approach:**
- Role-Based Access Control (RBAC) - Primary method
- Permission-based fine-grained control
- Attribute-Based Access Control (ABAC) for complex scenarios
- Resource ownership verification

---

## Table of Contents

1. [Role Definitions](#role-definitions)
2. [Permission Model](#permission-model)
3. [Implementation](#implementation)
4. [Fine-grained Authorization](#fine-grained-authorization)
5. [Best Practices](#best-practices)

---

## Role Definitions

### Gym Platform Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| **ADMIN** | System administrator | All permissions |
| **MANAGER** | Gym manager | User management, session management, reports |
| **TRAINER** | Fitness trainer | Create sessions, manage assignments, view member progress |
| **USER** | Regular member | View own profile, join sessions, track progress |
| **GUEST** | Unauthenticated user | View public content only |

### Role Hierarchy

```
ADMIN
├── MANAGER
│   ├── TRAINER
│   │   └── USER
```

### Role Configuration

```java
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;  // ROLE_ADMIN, ROLE_TRAINER, etc.
    
    private String description;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "user_roles")
public class UserRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @Column(nullable = false)
    private LocalDateTime assignedAt;
}
```

---

## Permission Model

### Permission Types

```java
@Entity
@Table(name = "permissions")
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;  // FORMAT: resource:action
    
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### Permission Naming Convention

```
resource:action

Examples:
- user:create          # Create new users
- user:read            # View user details
- user:update          # Edit user information
- user:delete          # Remove users
- session:create       # Create training sessions
- session:manage       # Manage session assignments
- report:view          # Access reports
- admin:configure      # System configuration
```

### Permission Definition

```java
public class PermissionProvider {
    
    // User permissions
    public static final String USER_CREATE = "user:create";
    public static final String USER_READ = "user:read";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";
    
    // Session permissions
    public static final String SESSION_CREATE = "session:create";
    public static final String SESSION_READ = "session:read";
    public static final String SESSION_UPDATE = "session:update";
    public static final String SESSION_DELETE = "session:delete";
    
    // Reporting permissions
    public static final String REPORT_VIEW = "report:view";
    public static final String REPORT_EXPORT = "report:export";
    
    // Admin permissions
    public static final String ADMIN_CONFIG = "admin:configure";
    public static final String ADMIN_AUDIT = "admin:audit";
}
```

### Role-Permission Mapping

```sql
-- Role: ADMIN (all permissions)
INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', 'Administrator');

INSERT INTO permissions (name, description) VALUES
    ('user:create', 'Create new users'),
    ('user:read', 'View user details'),
    ('user:update', 'Edit user information'),
    ('user:delete', 'Remove users'),
    ('session:create', 'Create training sessions'),
    ('session:manage', 'Manage session assignments'),
    ('report:view', 'Access reports'),
    ('admin:configure', 'System configuration');

-- Assign all permissions to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN';

-- Role: TRAINER
INSERT INTO roles (name, description) VALUES ('ROLE_TRAINER', 'Fitness Trainer');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_TRAINER' AND p.name IN ('session:create', 'session:manage', 'report:view');

-- Role: USER (minimal permissions)
INSERT INTO roles (name, description) VALUES ('ROLE_USER', 'Regular User');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name IN ('user:read', 'session:read', 'report:view');
```

---

## Implementation

### Spring Security Configuration

```java
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/trainer/**").hasRole("TRAINER")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### Method-Level Authorization

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Only ADMIN can view all users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    // Admin or user can view own profile
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }
    
    // Only ADMIN can create users
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userService.createUser(request));
    }
    
    // User can update own profile, ADMIN can update any
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }
    
    // Only ADMIN can delete users
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    
    // TRAINER and ADMIN can create sessions
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<SessionDTO> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(sessionService.createSession(request));
    }
    
    // Session creator or ADMIN can update session
    @PreAuthorize("@sessionService.isSessionCreator(#sessionId, authentication.principal.id) or hasRole('ADMIN')")
    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionDTO> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSessionRequest request) {
        return ResponseEntity.ok(sessionService.updateSession(sessionId, request));
    }
    
    // Only ADMIN can delete sessions
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
```

### Custom Authorization Expressions

```java
@Component("sessionService")
public class SessionService {
    
    // Check if user is session creator
    public boolean isSessionCreator(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        
        return session.getTrainerId().equals(userId);
    }
    
    // Check if user can access session
    public boolean canAccessSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        
        // Trainer can access their own session
        if (session.getTrainerId().equals(userId)) {
            return true;
        }
        
        // User can access if they're a participant
        return session.getParticipants()
            .stream()
            .anyMatch(p -> p.getUser().getId().equals(userId));
    }
}
```

---

## Fine-grained Authorization

### Service-Level Authorization

```java
@Service
public class UserService {
    
    public UserDTO getUserProfile(Long userId, Long currentUserId, Set<String> roles) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Check authorization
        if (!userId.equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Cannot access other users' profiles");
        }
        
        // Filter sensitive data based on role
        UserDTO dto = new UserDTO(user);
        if (!roles.contains("ROLE_ADMIN") && !userId.equals(currentUserId)) {
            dto.setEmail(null);  // Hide email from other users
            dto.setPhoneNumber(null);  // Hide phone number
        }
        
        return dto;
    }
}
```

### Attribute-Based Access Control (ABAC)

```java
@Component
public class AccessDecisionService {
    
    public boolean canAccessResource(Long userId, Long resourceId, String resourceType) {
        // Get user
        User user = userRepository.findById(userId).orElseThrow();
        
        // Get resource and check ownership/access
        if ("session".equals(resourceType)) {
            Session session = sessionRepository.findById(resourceId).orElseThrow();
            
            // User is session creator
            if (session.getTrainerId().equals(userId)) {
                return true;
            }
            
            // User is participant
            if (session.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId))) {
                return true;
            }
            
            // Admin can always access
            return user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        }
        
        return false;
    }
}
```

### Audit Logging for Authorization

```java
@Component
public class AuthorizationAuditListener {
    
    @Before("@annotation(requiresAuth)")
    public void logAuthorizationCheck(JoinPoint joinPoint, RequiresAuthorization requiresAuth) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String resource = requiresAuth.resource();
        String action = requiresAuth.action();
        
        auditLog.log(new AuditEvent(
            AuditEventType.AUTHORIZATION_CHECK,
            user,
            resource,
            action,
            LocalDateTime.now()
        ));
    }
    
    @Before("@annotation(requiresAuth)")
    public void logAuthorizationFailure(JoinPoint joinPoint, AccessDeniedException ex) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        
        auditLog.log(new AuditEvent(
            AuditEventType.AUTHORIZATION_DENIED,
            user,
            ex.getMessage(),
            LocalDateTime.now()
        ));
    }
}
```

---

## Best Practices

### 1. Principle of Least Privilege

```java
// Before: Too permissive
@PreAuthorize("isAuthenticated()")  // Any logged-in user
public List<User> getUsers() {
    return userRepository.findAll();
}

// After: Least privilege
@PreAuthorize("hasRole('ADMIN')")  // Only admins
public List<User> getUsers() {
    return userRepository.findAll();
}
```

### 2. Never Trust Client-Side Authorization

```java
// Before: Trusts client (WRONG!)
@GetMapping("/users/{userId}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
    // Doesn't verify authorization - anyone can access any user!
    return ResponseEntity.ok(userService.getUser(userId));
}

// After: Server-side authorization check
@GetMapping("/users/{userId}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long userId, @CurrentUser User currentUser) {
    // Verify user owns this data
    if (!userId.equals(currentUser.getId()) && !currentUser.isAdmin()) {
        throw new AccessDeniedException("Cannot access other users' data");
    }
    
    return ResponseEntity.ok(userService.getUser(userId));
}
```

### 3. Use Immutable Roles in JWT

```java
// Immutable roles stored in JWT - can't be modified client-side
Claims claims = Jwts.parser()
    .setSigningKey(secret)
    .parseClaimsJws(token)
    .getBody();

List<String> roles = (List<String>) claims.get("roles");
// Even if client tries to modify JWT, signature won't match
```

### 4. Implement Role Hierarchy

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_MANAGER > ROLE_TRAINER > ROLE_USER");
        return hierarchy;
    }
    
    @Bean
    public DefaultWebSecurityExpressionHandler webSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}

// ADMIN automatically has all MANAGER permissions
@PreAuthorize("hasRole('MANAGER')")
public void managerAction() {
    // ADMIN can also call this method
}
```

### 5. Audit All Authorization Decisions

```java
@Aspect
@Component
public class AuthorizationAuditAspect {
    
    @Before("execution(* com.gym..*.*(..)) && @annotation(preAuthorize)")
    public void auditAuthorizationCheck(JoinPoint joinPoint) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String method = joinPoint.getSignature().getName();
        
        log.info("Authorization check: user={}, method={}", user, method);
    }
}
```

---

## Related Documentation

- [01-security-overview.md](01-security-overview.md) - Security architecture
- [02-authentication.md](02-authentication.md) - Authentication mechanisms
- [04-data-security.md](04-data-security.md) - Data protection
- [05-api-security.md](05-api-security.md) - API security
- docs/troubleshooting/06-security-troubleshooting.md - Authorization troubleshooting
- docs/stack/04-security-framework.md - Spring Security configuration
