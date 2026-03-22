package com.gym.notification.controller;

import com.gym.common.config.GymExceptionHandlerAutoConfiguration;
import com.gym.notification.dto.PushTokenRequestDTO;
import com.gym.notification.dto.PushTokenResponseDTO;
import com.gym.common.exception.ResourceNotFoundException;
import com.gym.common.exception.UnauthorizedException;
import com.gym.notification.service.PushTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.gym.common.config.GymTestSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PushTokenController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
@ActiveProfiles("test")
class PushTokenControllerTest {

    private static final Long VALID_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PushTokenService pushTokenService;

    @Test
    void testRegisterToken_Success() throws Exception {
        // ARRANGE
        PushTokenResponseDTO responseDto = PushTokenResponseDTO.builder()
                .id(1L)
                .userId(VALID_USER_ID)
                .token("firebase_token_xyz")
                .deviceType("android")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();

        when(pushTokenService.registerToken(eq(VALID_USER_ID), anyString(), anyString()))
                .thenReturn(responseDto);

        String requestBody = """
                {
                    "userId": 1,
                    "token": "firebase_token_xyz",
                    "deviceType": "android"
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/push-tokens")
                .header("X-User-Id", VALID_USER_ID)
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.deviceType").value("android"));
    }

    @Test
    void testRegisterToken_InvalidBody_BadRequest() throws Exception {
        // ARRANGE
        String invalidRequestBody = """
                {
                    "token": "firebase_token_xyz"
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/push-tokens")
                .header("X-User-Id", VALID_USER_ID)
                .contentType("application/json")
                .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetTokens_Success() throws Exception {
        // ARRANGE
        List<PushTokenResponseDTO> tokens = Arrays.asList(
                PushTokenResponseDTO.builder()
                        .id(1L)
                        .userId(VALID_USER_ID)
                        .token("token1")
                        .deviceType("ios")
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build(),
                PushTokenResponseDTO.builder()
                        .id(2L)
                        .userId(VALID_USER_ID)
                        .token("token2")
                        .deviceType("android")
                        .isActive(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(pushTokenService.getTokensForUser(VALID_USER_ID)).thenReturn(tokens);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/push-tokens")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].deviceType").value("ios"))
                .andExpect(jsonPath("$[1].deviceType").value("android"));
    }

    @Test
    void testGetActiveTokens_Success() throws Exception {
        // ARRANGE
        List<PushTokenResponseDTO> activeTokens = Arrays.asList(
                PushTokenResponseDTO.builder()
                        .id(1L)
                        .userId(VALID_USER_ID)
                        .token("active_token1")
                        .deviceType("ios")
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(pushTokenService.getActiveTokens(VALID_USER_ID)).thenReturn(activeTokens);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/push-tokens/active")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void testGetActiveTokens_EmptyList() throws Exception {
        // ARRANGE
        when(pushTokenService.getActiveTokens(VALID_USER_ID)).thenReturn(new ArrayList<>());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/push-tokens/active")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testDeactivateToken_Success() throws Exception {
        // ARRANGE
        String token = "firebase_token_to_deactivate";
        doNothing().when(pushTokenService).deactivateToken(VALID_USER_ID, token);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/v1/push-tokens")
                .header("X-User-Id", VALID_USER_ID)
                .param("token", token))
                .andExpect(status().isNoContent());

        verify(pushTokenService, times(1)).deactivateToken(VALID_USER_ID, token);
    }

    @Test
    void testDeactivateToken_MissingToken_BadRequest() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(delete("/api/v1/push-tokens")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeactivateToken_Unauthorized() throws Exception {
        // ARRANGE
        String token = "firebase_token_xyz";
        doThrow(new UnauthorizedException("You do not have permission to deactivate this token"))
                .when(pushTokenService).deactivateToken(VALID_USER_ID, token);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/v1/push-tokens")
                .header("X-User-Id", VALID_USER_ID)
                .param("token", token))
                .andExpect(status().isForbidden());
    }
}
