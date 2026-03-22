package com.gym.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PasswordResetRequest", description = "Request to initiate password reset")
public class PasswordResetRequest {
    @Email
    @NotBlank
    @Schema(description = "User email address", example = "john.doe@example.com")
    private String email;
}
