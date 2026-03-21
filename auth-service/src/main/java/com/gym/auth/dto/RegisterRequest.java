package com.gym.auth.dto;

import com.gym.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "RegisterRequest",
    description = "User registration data for creating a new account",
    example = "{\"email\": \"jane.smith@example.com\", \"password\": \"SecurePassword123!\", \"firstName\": \"Jane\", \"lastName\": \"Smith\"}"
)
public class RegisterRequest {
    @NotBlank
    @Schema(description = "User first name", example = "Jane")
    private String firstName;

    @NotBlank
    @Schema(description = "User last name", example = "Smith")
    private String lastName;

    @Email
    @NotBlank
    @Schema(description = "User email address (must be unique)", example = "jane.smith@example.com")
    private String email;

    @NotBlank
    @Size(min = 8)
    @Schema(description = "User password (min 8 characters)", example = "SecurePassword123!")
    private String password;

    @Builder.Default
    @Schema(description = "User type", example = "USER")
    private User.UserType userType = User.UserType.USER;
}
