package com.gym.common.config;

import com.gym.common.security.SecurityUtils;
import com.gym.common.security.UserContext;
import com.gym.common.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

public class GymRoleInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GymRoleInterceptor.class);
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_ROLES = "X-User-Roles";
    private static final String MDC_USER_ID = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String userId = request.getHeader(X_USER_ID);
            String rolesString = request.getHeader(X_USER_ROLES);
            String traceId = request.getHeader(GymMdcFilter.TRACE_ID_HEADER);

            if (userId != null) {
                Set<String> roles = SecurityUtils.parseRoles(rolesString);
                UserContextHolder.setContext(UserContext.builder()
                        .userId(userId)
                        .roles(roles)
                        .traceId(traceId)
                        .build());
                MDC.put(MDC_USER_ID, userId);
                log.debug("User context set for user: {} with roles: {}", userId, roles);
            }
        } catch (Exception e) {
            log.error("Error setting user context", e);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
        MDC.remove(MDC_USER_ID);
    }
}
