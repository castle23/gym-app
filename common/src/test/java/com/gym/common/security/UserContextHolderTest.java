package com.gym.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class UserContextHolderTest {
    
    @BeforeEach
    void setUp() {
        UserContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void testSetAndGetContext() {
        UserContext context = UserContext.builder()
                .userId("123")
                .email("user@example.com")
                .roles(Set.of("USER", "ADMIN"))
                .build();
        
        UserContextHolder.setContext(context);
        
        assertNotNull(UserContextHolder.getContext());
        assertEquals("123", UserContextHolder.getUserId());
    }

    @Test
    void testHasRole() {
        UserContext context = UserContext.builder()
                .userId("123")
                .roles(Set.of("USER"))
                .build();
        
        UserContextHolder.setContext(context);
        
        assertTrue(SecurityUtils.hasRole("USER"));
        assertFalse(SecurityUtils.hasRole("ADMIN"));
    }

    @Test
    void testHasAnyRole() {
        UserContext context = UserContext.builder()
                .userId("123")
                .roles(Set.of("USER", "PROFESSIONAL"))
                .build();
        
        UserContextHolder.setContext(context);
        
        assertTrue(SecurityUtils.hasAnyRole("ADMIN", "USER"));
        assertFalse(SecurityUtils.hasAnyRole("SUPER_ADMIN"));
    }

    @Test
    void testClearContext() {
        UserContext context = UserContext.builder()
                .userId("123")
                .roles(Set.of("USER"))
                .build();
        
        UserContextHolder.setContext(context);
        UserContextHolder.clear();
        
        assertNull(UserContextHolder.getContext());
    }
}
