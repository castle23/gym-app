package com.gym.notification.dto;

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
    name = "PushTokenResponseDTO",
    description = "Push token registration details",
    example = "{\"id\": 1, \"userId\": 123, \"token\": \"****...xxxxxxxxxxxxxx\", \"deviceType\": \"iOS\", \"isActive\": true, \"lastUsedAt\": \"2026-03-21T10:30:00\", \"createdAt\": \"2026-03-21T09:00:00\"}"
)
public class PushTokenResponseDTO {
    @Schema(description = "Unique push token identifier", example = "1")
    private Long id;

    @Schema(description = "User ID who owns the token", example = "123")
    private Long userId;

    @Schema(description = "Masked push token (for security)", example = "****...xxxxxxxxxxxxxx")
    private String token;

    @Schema(description = "Device type/platform", example = "iOS")
    private String deviceType;

    @Schema(description = "Whether token is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Last timestamp when token was used", example = "2026-03-21T10:30:00")
    private LocalDateTime lastUsedAt;

    @Schema(description = "Token registration timestamp", example = "2026-03-21T09:00:00")
    private LocalDateTime createdAt;

    /**
     * Get masked token for security - shows only last 20 characters preceded by asterisks
     * @return masked token in format "****...last20chars"
     */
    public String getToken() {
        if (token == null || token.isEmpty()) {
            return "";
        }
        if (token.length() <= 20) {
            return "****..." + token;
        }
        return "****..." + token.substring(token.length() - 20);
    }

    /**
     * Set full token - used internally during object construction
     * @param token the full token to set
     */
    public void setToken(String token) {
        this.token = token;
    }
}
