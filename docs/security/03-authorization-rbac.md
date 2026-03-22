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
| **ROLE_ADMIN** | System administrator | All permissions |
| **ROLE_PROFESSIONAL** | Fitness professional/trainer | Professional-level operations |
| **ROLE_USER** | Regular member | Own data access, standard operations |

### Role Assignment

Roles are assigned at registration and stored in the `auth_schema`. The API Gateway reads roles from the JWT and injects them as the `X-User-Roles` header. Services read this header via `GymRoleInterceptor` and enforce access control using `@RequiresRole`.

### Role Configuration

```java
// Roles are stored as strings in the User entity
// Values: ROLE_USER, ROLE_PROFESSIONAL, ROLE_ADMIN
public enum Role {
    ROLE_USER,
    ROLE_PROFESSIONAL,
    ROLE_ADMIN
}
```

## Authorization Implementation

### How Authorization Works

Services do **not** use Spring Security or validate JWT. Authorization is enforced via the `gym-common` library:

1. API Gateway validates JWT and injects `X-User-Id` and `X-User-Roles` headers
2. `GymRoleInterceptor` (from `gym-common`) reads these headers and stores them in `GymSecurityContext`
3. `@RequiresRole` annotation on controller methods enforces role checks

```java
// From gym-common: annotation for role-based access control
@RequiresRole({"ROLE_ADMIN"})
@GetMapping("/admin/users")
public ResponseEntity<List<UserDTO>> getAllUsers() {
    return ResponseEntity.ok(userService.getAllUsers());
}

// Access current user in service layer
Long userId = GymSecurityContext.getCurrentUserId();  // from X-User-Id header
List<String> roles = GymSecurityContext.getCurrentRoles();  // from X-User-Roles header
```

### Resource Ownership Verification

```java
@GetMapping("/{userId}")
public ResponseEntity<UserDTO> getUserProfile(@PathVariable Long userId) {
    Long currentUserId = GymSecurityContext.getCurrentUserId();
    List<String> roles = GymSecurityContext.getCurrentRoles();
    
    // User can access own data; admin can access any
    if (!userId.equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
        throw new UnauthorizedException("Cannot access other users' profiles");
    }
    
    return ResponseEntity.ok(userService.getUserProfile(userId));
}
```

---

## Fine-grained Authorization

### Service-Level Authorization

```java
@Service
public class UserService {
    
    public UserDTO getUserProfile(Long userId) {
        Long currentUserId = GymSecurityContext.getCurrentUserId();
        List<String> roles = GymSecurityContext.getCurrentRoles();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check authorization
        if (!userId.equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
            throw new UnauthorizedException("Cannot access other users' profiles");
        }
        
        // Filter sensitive data based on role
        UserDTO dto = new UserDTO(user);
        if (!roles.contains("ROLE_ADMIN") && !userId.equals(currentUserId)) {
            dto.setEmail(null);
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
@RequiresRole({"ROLE_USER"})  // Any logged-in user
public List<User> getUsers() {
    return userRepository.findAll();
}

// After: Least privilege
@RequiresRole({"ROLE_ADMIN"})  // Only admins
public List<User> getUsers() {
    return userRepository.findAll();
}
```

### 2. Never Trust Client-Side Authorization

```java
// Before: Trusts client (WRONG!)
@GetMapping("/users/{userId}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getUser(userId));
}

// After: Server-side authorization check using injected headers
@GetMapping("/users/{userId}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
    Long currentUserId = GymSecurityContext.getCurrentUserId();
    List<String> roles = GymSecurityContext.getCurrentRoles();
    
    if (!userId.equals(currentUserId) && !roles.contains("ROLE_ADMIN")) {
        throw new UnauthorizedException("Cannot access other users' data");
    }
    
    return ResponseEntity.ok(userService.getUser(userId));
}
```

### 3. Use Immutable Roles from Gateway Headers

```java
// Roles come from X-User-Roles header injected by API Gateway
// The gateway extracted them from the JWT signature — they cannot be forged
// by the client since the gateway validates the JWT signature first
List<String> roles = GymSecurityContext.getCurrentRoles();
```

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
