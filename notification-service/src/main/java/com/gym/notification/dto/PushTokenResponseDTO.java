package com.gym.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushTokenResponseDTO {
    private Long id;
    private Long userId;
    private String token;
    private String deviceType;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
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
