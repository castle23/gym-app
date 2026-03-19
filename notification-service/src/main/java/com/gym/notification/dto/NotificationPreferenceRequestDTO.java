package com.gym.notification.dto;

import com.gym.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceRequestDTO {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @Builder.Default
    private Boolean isEnabled = true;

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;
}
