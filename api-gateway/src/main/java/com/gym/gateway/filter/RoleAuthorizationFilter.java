package com.gym.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAuthorizationFilter implements GlobalFilter, Ordered {

    private static final String X_USER_ROLES = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Check if route requires admin role
        if (isAdminRoute(path)) {
            String roles = exchange.getRequest().getHeaders().getFirst(X_USER_ROLES);
            if (roles == null || !roles.contains("ADMIN")) {
                log.warn("Unauthorized admin access attempt to: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }
        
        return chain.filter(exchange);
    }

    private boolean isAdminRoute(String path) {
        return path.contains("/admin/");
    }

    @Override
    public int getOrder() {
        return -1; // Run after JwtAuthFilter (which has order 0)
    }
}
