package com.gym.auth.controller;

import com.gym.auth.dto.AuthResponse;
import com.gym.auth.dto.LoginRequest;
import com.gym.auth.dto.RegisterRequest;
import com.gym.auth.dto.RefreshTokenRequest;
import com.gym.auth.dto.TokenRefreshResponse;
import com.gym.auth.dto.VerifyEmailRequest;
import com.gym.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication, registration, and JWT token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided credentials and sends verification email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate email")
    })
    public ResponseEntity<AuthResponse> register(
        @RequestBody(
            description = "User registration details",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RegisterRequest.class),
                examples = @ExampleObject(
                    name = "Register User",
                    value = "{\"firstName\": \"Jane\", \"lastName\": \"Smith\", \"email\": \"jane.smith@example.com\", \"password\": \"SecurePassword123!\"}"
                )
            )
        )
        @org.springframework.web.bind.annotation.RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user with email and password, returns JWT access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, tokens returned", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or account not verified")
    })
    public ResponseEntity<AuthResponse> login(
        @RequestBody(
            description = "User login credentials",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginRequest.class),
                examples = @ExampleObject(
                    name = "Login User",
                    value = "{\"email\": \"john.doe@example.com\", \"password\": \"SecurePassword123!\"}"
                )
            )
        )
        @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify email address",
            description = "Verifies user email using the verification code sent to their email address"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    })
    public ResponseEntity<AuthResponse> verify(@RequestBody VerifyEmailRequest request) {
        log.info("Verification attempt for email: {}", request.getEmail());
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL', 'ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Refresh access token",
            description = "Uses refresh token to obtain a new access token (requires authentication)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        TokenRefreshResponse response = authService.refreshToken(request);
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL', 'ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Get user profile",
            description = "Retrieves the authenticated user's profile information (requires authentication)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - token missing or invalid")
    })
    public ResponseEntity<AuthResponse> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "unknown";
        
        log.info("Profile request for user: {}", userId);
        return ResponseEntity.ok(AuthResponse.builder()
                .userId(userId)
                .message("Profile retrieved")
                .success(true)
                .build());
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifies that the Auth Service is running and healthy")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
}
