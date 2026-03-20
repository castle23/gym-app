package com.gym.training.config;

import com.gym.common.security.UserContext;
import com.gym.common.security.UserContextHolder;
import com.gym.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

@Slf4j
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_ROLES = "X-User-Roles";
    private static final String X_TRACE_ID = "X-Trace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String userId = request.getHeader(X_USER_ID);
            String rolesString = request.getHeader(X_USER_ROLES);
            String traceId = request.getHeader(X_TRACE_ID);

            if (userId != null) {
                Set<String> roles = SecurityUtils.parseRoles(rolesString);
                
                UserContext context = UserContext.builder()
                        .userId(userId)
                        .roles(roles)
                        .traceId(traceId)
                        .build();
                
                UserContextHolder.setContext(context);
                
                log.debug("User context set for user: {} with roles: {}", userId, roles);
            }

            return true;
        } catch (Exception e) {
            log.error("Error setting user context", e);
            return true; // Continue anyway
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
