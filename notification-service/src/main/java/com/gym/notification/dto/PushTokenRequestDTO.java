package com.gym.notification.dto;

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
public class PushTokenRequestDTO {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Token is required")
    @Size(min = 10, max = 1000, message = "Token must be between 10 and 1000 characters")
    private String token;

    @NotBlank(message = "Device type is required")
    @Size(min = 1, max = 50, message = "Device type must be between 1 and 50 characters")
    private String deviceType;
}
