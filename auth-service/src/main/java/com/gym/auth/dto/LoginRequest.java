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
@Schema(
    name = "LoginRequest",
    description = "User login credentials for authentication",
    example = "{\"email\": \"john.doe@example.com\", \"password\": \"SecurePassword123!\"}"
)
public class LoginRequest {
    @Email
    @NotBlank
    @Schema(description = "User email address", example = "john.doe@example.com")
    private String email;

    @NotBlank
    @Schema(description = "User password (min 8 chars)", example = "SecurePassword123!")
    private String password;
}
