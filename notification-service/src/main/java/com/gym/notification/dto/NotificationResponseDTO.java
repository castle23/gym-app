package com.gym.notification.dto;

import com.gym.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "NotificationResponseDTO",
    description = "Notification details",
    example = "{\"id\": 1, \"userId\": 123, \"title\": \"Workout Reminder\", \"body\": \"Time for your scheduled chest day workout!\", \"type\": \"REMINDER\", \"isRead\": false, \"createdAt\": \"2026-03-21T10:30:00\", \"sentAt\": \"2026-03-21T10:30:01\"}"
)
public class NotificationResponseDTO {
    @Schema(description = "Unique notification identifier", example = "1")
    private Long id;

    @Schema(description = "User ID who received the notification", example = "123")
    private Long userId;

    @Schema(description = "Notification title", example = "Workout Reminder")
    private String title;

    @Schema(description = "Notification body/message", example = "Time for your scheduled chest day workout!")
    private String body;

    @Schema(description = "Notification type", example = "REMINDER")
    private NotificationType type;

    @Schema(description = "Whether notification has been read", example = "false")
    private Boolean isRead;

    @Schema(description = "Creation timestamp", example = "2026-03-21T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when notification was sent", example = "2026-03-21T10:30:01")
    private LocalDateTime sentAt;
}
