package com.gym.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PasswordResetConfirm", description = "Confirmation request for password reset")
public class PasswordResetConfirm {
    @NotBlank
    @Schema(description = "Reset token sent via email", example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

    @NotBlank
    @Schema(description = "New password (min 8 chars)", example = "NewSecurePassword123!")
    private String newPassword;
}
