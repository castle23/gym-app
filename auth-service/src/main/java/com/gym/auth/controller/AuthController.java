package com.gym.auth.controller;

import com.gym.auth.dto.*;
import com.gym.auth.entity.ProfessionalRegistrationRequest;
import com.gym.auth.entity.User;
import com.gym.auth.repository.UserRepository;
import com.gym.auth.service.AuthService;
import com.gym.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
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
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication, registration, and JWT token management")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
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
        return ResponseEntity.ok(authService.login(request));
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
        return ResponseEntity.ok(authService.verifyEmail(request));
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
        return ResponseEntity.ok(authService.refreshToken(request));
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
                .build());
    }

    @PostMapping("/password/request-reset")
    @Operation(summary = "Request password reset", description = "Sends a password reset link to user email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset link sent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.requestPasswordReset(request));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Confirm password reset", description = "Resets user password using the token received via email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<AuthResponse> confirmPasswordReset(@RequestBody PasswordResetConfirm request) {
        log.info("Password reset confirmation attempt");
        return ResponseEntity.ok(authService.confirmPasswordReset(request));
    }

    @PostMapping("/professional/request")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Request professional status", description = "Submits request to become a professional")
    public ResponseEntity<AuthResponse> requestProfessionalStatus(@RequestBody ProfessionalRegistrationDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(authService.requestProfessionalStatus(user, dto));
    }

    @GetMapping("/admin/professional-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Get professional requests", description = "List pending professional requests")
    public ResponseEntity<List<ProfessionalRegistrationRequest>> getRegistrationRequests() {
        return ResponseEntity.ok(authService.getRegistrationRequests());
    }

    @PostMapping("/admin/professional-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Approve professional request", description = "Approves request and promotes user")
    public ResponseEntity<AuthResponse> approveRegistration(@PathVariable Long id, @RequestBody ProfessionalApprovalDto dto) {
        return ResponseEntity.ok(authService.approveRegistration(id, dto));
    }

    @PostMapping("/admin/professional-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Reject professional request", description = "Rejects professional request")
    public ResponseEntity<AuthResponse> rejectRegistration(@PathVariable Long id, @RequestBody ProfessionalApprovalDto dto) {
        return ResponseEntity.ok(authService.rejectRegistration(id, dto));
    }
}
