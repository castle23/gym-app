package com.gym.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.auth.dto.AuthResponse;
import com.gym.auth.dto.PasswordResetConfirm;
import com.gym.auth.dto.PasswordResetRequest;
import com.gym.auth.service.AuthService;
import com.gym.common.config.GymExceptionHandlerAutoConfiguration;
import com.gym.common.config.GymTestSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. POST /password/request-reset
    @Test
    void testRequestPasswordReset_Success() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("user@example.com");
        AuthResponse response = AuthResponse.builder().message("Reset link sent").build();

        when(authService.requestPasswordReset(any(PasswordResetRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/password/request-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reset link sent"));
    }

    @Test
    void testRequestPasswordReset_UserNotFound() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("unknown@example.com");

        when(authService.requestPasswordReset(any(PasswordResetRequest.class)))
                .thenThrow(new IllegalArgumentException("User not found"));

        mockMvc.perform(post("/password/request-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()); // Expecting 404 based on controller API response definition
    }

    // 2. POST /password/reset
    @Test
    void testConfirmPasswordReset_Success() throws Exception {
        PasswordResetConfirm request = new PasswordResetConfirm("token", "newPassword");
        AuthResponse response = AuthResponse.builder().message("Password reset successful").build();

        when(authService.confirmPasswordReset(any(PasswordResetConfirm.class)))
                .thenReturn(response);

        mockMvc.perform(post("/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    void testConfirmPasswordReset_InvalidToken() throws Exception {
        PasswordResetConfirm request = new PasswordResetConfirm("invalid-token", "newPassword");

        when(authService.confirmPasswordReset(any(PasswordResetConfirm.class)))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        mockMvc.perform(post("/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConfirmPasswordReset_ExpiredToken() throws Exception {
        PasswordResetConfirm request = new PasswordResetConfirm("expired-token", "newPassword");

        when(authService.confirmPasswordReset(any(PasswordResetConfirm.class)))
                .thenThrow(new IllegalArgumentException("Expired token"));

        mockMvc.perform(post("/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
