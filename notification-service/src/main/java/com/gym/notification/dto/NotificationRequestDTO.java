package com.gym.notification.dto;

import com.gym.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "NotificationRequestDTO",
    description = "Request to create a new notification",
    example = "{\"userId\": 123, \"title\": \"Workout Reminder\", \"body\": \"Time for your scheduled chest day workout!\", \"type\": \"REMINDER\"}"
)
public class NotificationRequestDTO {
    @NotNull(message = "User ID is required")
    @Schema(description = "User ID to send notification to", example = "123")
    private Long userId;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @Schema(description = "Notification title", example = "Workout Reminder")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(min = 1, max = 2000, message = "Body must be between 1 and 2000 characters")
    @Schema(description = "Notification body/message", example = "Time for your scheduled chest day workout!")
    private String body;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Notification type", example = "REMINDER", allowableValues = {"REMINDER", "ALERT", "ACHIEVEMENT", "INFO", "WARNING"})
    private NotificationType type;
}
