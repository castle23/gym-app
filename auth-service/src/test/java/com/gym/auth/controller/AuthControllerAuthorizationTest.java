package com.gym.auth.controller;

import com.gym.auth.dto.AuthResponse;
import com.gym.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JavaMailSender javaMailSender;

    private final AuthResponse stubResponse = AuthResponse.builder()
            .userId("1").email("test@example.com").message("ok").build();

    @Test
    void testGetProfileWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetProfileWithUserRole() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProfileWithAdminRole() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk());
    }

    @Test
    void testRegisterEndpointIsPublic() throws Exception {
        when(authService.register(any())).thenReturn(stubResponse);
        mockMvc.perform(post("/register")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"Pass123!\",\"userType\":\"USER\"}"))
                .andExpect(status().isCreated());
    }
}
