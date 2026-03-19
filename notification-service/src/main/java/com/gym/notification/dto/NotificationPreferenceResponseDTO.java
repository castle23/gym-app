package com.gym.notification.dto;

import com.gym.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceResponseDTO {
    private Long id;
    private Long userId;
    private NotificationType notificationType;
    private Boolean isEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private LocalDateTime createdAt;
}
