package com.gym.common.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecurityUtils {
    
    public static Set<String> parseRoles(String rolesString) {
        if (rolesString == null || rolesString.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(rolesString.split(",")));
    }

    public static boolean hasRole(String requiredRole) {
        UserContext context = UserContextHolder.getContext();
        return context != null && context.hasRole(requiredRole);
    }

    public static boolean hasAnyRole(String... roles) {
        UserContext context = UserContextHolder.getContext();
        return context != null && context.hasAnyRole(roles);
    }

    public static String getCurrentUserId() {
        return UserContextHolder.getUserId();
    }

    public static Set<String> getCurrentRoles() {
        return UserContextHolder.getRoles();
    }

    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new RuntimeException("User does not have required role: " + role);
        }
    }

    public static void requireAnyRole(String... roles) {
        if (!hasAnyRole(roles)) {
            throw new RuntimeException("User does not have any of required roles");
        }
    }
}
