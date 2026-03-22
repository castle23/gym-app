package com.gym.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.auth.dto.AuthResponse;
import com.gym.auth.dto.PasswordResetConfirm;
import com.gym.auth.dto.PasswordResetRequest;
import com.gym.auth.dto.ProfessionalApprovalDto;
import com.gym.auth.dto.ProfessionalRegistrationDto;
import com.gym.auth.entity.User;
import com.gym.auth.repository.UserRepository;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @MockBean
    private UserRepository userRepository;

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


    @Test
    @WithMockUser(roles = "USER")
    void testRequestProfessionalStatus_Success() throws Exception {
        ProfessionalRegistrationDto dto = new ProfessionalRegistrationDto("Specialty", "License123");
        AuthResponse response = AuthResponse.builder().message("Registration request submitted").build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        when(authService.requestProfessionalStatus(any(User.class), any(ProfessionalRegistrationDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/professional/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration request submitted"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetRegistrationRequests_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/professional-requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testApproveRegistration_Success() throws Exception {
        Long requestId = 1L;
        ProfessionalApprovalDto dto = new ProfessionalApprovalDto(null, null);
        AuthResponse response = AuthResponse.builder().message("Registration approved").build();

        when(authService.approveRegistration(eq(requestId), any(ProfessionalApprovalDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/admin/professional-requests/{id}/approve", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration approved"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRejectRegistration_Success() throws Exception {
        Long requestId = 1L;
        ProfessionalApprovalDto dto = new ProfessionalApprovalDto(null, "Rejected reason");
        AuthResponse response = AuthResponse.builder().message("Registration rejected").build();

        when(authService.rejectRegistration(eq(requestId), any(ProfessionalApprovalDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/admin/professional-requests/{id}/reject", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration rejected"));
    }
}
