package com.gym.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RoleAuthorizationFilterTest {

    private RoleAuthorizationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RoleAuthorizationFilter();
        chain = mock(GatewayFilterChain.class);
    }

    @Test
    void testAdminRouteWithAdminRole() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/training/admin/all-exercises")
                .header("X-User-Roles", "ADMIN")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain);

        verify(chain).filter(exchange);
        assertNotEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testAdminRouteWithoutAdminRole() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/training/admin/all-exercises")
                .header("X-User-Roles", "USER")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain);

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(exchange);
    }

    @Test
    void testPublicRouteWithoutAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/training/exercises")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain);

        verify(chain).filter(exchange);
    }
}
