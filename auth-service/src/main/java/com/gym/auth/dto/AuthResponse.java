package com.gym.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "AuthResponse",
    description = "Authentication response with JWT tokens and user information",
    example = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"userId\": \"550e8400-e29b-41d4-a716-446655440000\", \"email\": \"john.doe@example.com\", \"message\": \"Authentication successful\", \"success\": true}"
)
public class AuthResponse {
    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE2NzQ4Mjk2MDB9.signature")
    private String token;

    @Schema(description = "JWT refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...")
    private String refreshToken;

    @Schema(description = "Authenticated user's unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @Schema(description = "Authenticated user's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Response message with details about the operation", example = "Authentication successful")
    private String message;

    @Schema(description = "Whether the operation was successful", example = "true")
    private Boolean success;
}
