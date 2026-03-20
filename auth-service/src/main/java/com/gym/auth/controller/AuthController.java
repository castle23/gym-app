package com.gym.auth.controller;

import com.gym.auth.dto.AuthResponse;
import com.gym.auth.dto.LoginRequest;
import com.gym.auth.dto.RegisterRequest;
import com.gym.auth.dto.RefreshTokenRequest;
import com.gym.auth.dto.TokenRefreshResponse;
import com.gym.auth.dto.VerifyEmailRequest;
import com.gym.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@RequestBody VerifyEmailRequest request) {
        log.info("Verification attempt for email: {}", request.getEmail());
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        TokenRefreshResponse response = authService.refreshToken(request);
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
}
