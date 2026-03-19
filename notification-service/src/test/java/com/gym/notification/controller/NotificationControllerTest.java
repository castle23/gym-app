package com.gym.notification.controller;

import com.gym.notification.dto.NotificationResponseDTO;
import com.gym.notification.exception.ResourceNotFoundException;
import com.gym.notification.exception.UnauthorizedException;
import com.gym.notification.service.NotificationService;
import com.gym.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final Long VALID_USER_ID = 1L;
    private static final Long VALID_NOTIFICATION_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void testGetAllNotifications_Success() throws Exception {
        // ARRANGE
        List<NotificationResponseDTO> notifications = Arrays.asList(
                NotificationResponseDTO.builder()
                        .id(1L)
                        .userId(VALID_USER_ID)
                        .title("Test Title")
                        .body("Test Body")
                        .type(NotificationType.WORKOUT_REMINDER)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .sentAt(LocalDateTime.now())
                        .build(),
                NotificationResponseDTO.builder()
                        .id(2L)
                        .userId(VALID_USER_ID)
                        .title("Test Title 2")
                        .body("Test Body 2")
                        .type(NotificationType.ACHIEVEMENT)
                        .isRead(true)
                        .createdAt(LocalDateTime.now())
                        .sentAt(LocalDateTime.now())
                        .build()
        );
        when(notificationService.getNotifications(VALID_USER_ID)).thenReturn(notifications);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/notifications")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Title"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Test Title 2"));
    }

    @Test
    void testGetAllNotifications_MissingHeader_BadRequest() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUnreadNotifications_Success() throws Exception {
        // ARRANGE
        List<NotificationResponseDTO> unreadNotifications = Arrays.asList(
                NotificationResponseDTO.builder()
                        .id(1L)
                        .userId(VALID_USER_ID)
                        .title("Unread 1")
                        .body("Unread Body 1")
                        .type(NotificationType.WORKOUT_REMINDER)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        when(notificationService.getUnreadNotifications(VALID_USER_ID)).thenReturn(unreadNotifications);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/notifications/unread")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    void testGetUnreadCount_Success() throws Exception {
        // ARRANGE
        when(notificationService.getUnreadCount(VALID_USER_ID)).thenReturn(3L);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/notifications/unread/count")
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void testSendNotification_Success() throws Exception {
        // ARRANGE
        NotificationResponseDTO responseDto = NotificationResponseDTO.builder()
                .id(1L)
                .userId(VALID_USER_ID)
                .title("New Notification")
                .body("Test Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.sendNotification(any())).thenReturn(responseDto);

        String requestBody = """
                {
                    "userId": 1,
                    "title": "New Notification",
                    "body": "Test Body",
                    "type": "WORKOUT_REMINDER"
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/notifications")
                .header("X-User-Id", VALID_USER_ID)
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("New Notification"));
    }

    @Test
    void testSendNotification_InvalidBody_BadRequest() throws Exception {
        // ARRANGE
        String invalidRequestBody = """
                {
                    "title": "Missing userId",
                    "body": "Test Body"
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/notifications")
                .header("X-User-Id", VALID_USER_ID)
                .contentType("application/json")
                .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMarkAsRead_Success() throws Exception {
        // ARRANGE
        NotificationResponseDTO responseDto = NotificationResponseDTO.builder()
                .id(VALID_NOTIFICATION_ID)
                .userId(VALID_USER_ID)
                .title("Test")
                .body("Test Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.markAsRead(VALID_NOTIFICATION_ID, VALID_USER_ID)).thenReturn(responseDto);

        // ACT & ASSERT
        mockMvc.perform(put("/api/v1/notifications/{id}/read", VALID_NOTIFICATION_ID)
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void testMarkAsRead_NotFound_Returns404() throws Exception {
        // ARRANGE
        when(notificationService.markAsRead(VALID_NOTIFICATION_ID, VALID_USER_ID))
                .thenThrow(new ResourceNotFoundException("Notification not found"));

        // ACT & ASSERT
        mockMvc.perform(put("/api/v1/notifications/{id}/read", VALID_NOTIFICATION_ID)
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNotification_Success() throws Exception {
        // ARRANGE
        doNothing().when(notificationService).deleteNotification(VALID_NOTIFICATION_ID, VALID_USER_ID);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/v1/notifications/{id}", VALID_NOTIFICATION_ID)
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteNotification_NotFound_Returns404() throws Exception {
        // ARRANGE
        doThrow(new ResourceNotFoundException("Notification not found"))
                .when(notificationService).deleteNotification(VALID_NOTIFICATION_ID, VALID_USER_ID);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/v1/notifications/{id}", VALID_NOTIFICATION_ID)
                .header("X-User-Id", VALID_USER_ID))
                .andExpect(status().isNotFound());
    }
}
