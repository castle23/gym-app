package com.gym.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String email;
    private Set<String> roles;
    private String traceId;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... roleNames) {
        if (roles == null) return false;
        for (String role : roleNames) {
            if (roles.contains(role)) return true;
        }
        return false;
    }

    public void clear() {
        userId = null;
        email = null;
        roles = null;
        traceId = null;
    }
}
