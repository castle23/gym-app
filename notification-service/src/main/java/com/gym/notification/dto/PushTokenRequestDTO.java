package com.gym.notification.dto;

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
    name = "PushTokenRequestDTO",
    description = "Request to register a push notification token",
    example = "{\"userId\": 123, \"token\": \"ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]\", \"deviceType\": \"iOS\"}"
)
public class PushTokenRequestDTO {
    @NotNull(message = "User ID is required")
    @Schema(description = "User ID to register token for", example = "123")
    private Long userId;

    @NotBlank(message = "Token is required")
    @Size(min = 10, max = 1000, message = "Token must be between 10 and 1000 characters")
    @Schema(description = "Push notification token from device", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
    private String token;

    @NotBlank(message = "Device type is required")
    @Size(min = 1, max = 50, message = "Device type must be between 1 and 50 characters")
    @Schema(description = "Device type/platform", example = "iOS", allowableValues = {"iOS", "Android", "Web"})
    private String deviceType;
}
