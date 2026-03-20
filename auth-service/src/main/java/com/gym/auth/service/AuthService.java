package com.gym.auth.service;

import com.gym.auth.dto.AuthResponse;
import com.gym.auth.dto.LoginRequest;
import com.gym.auth.dto.RegisterRequest;
import com.gym.auth.dto.RefreshTokenRequest;
import com.gym.auth.dto.TokenRefreshResponse;
import com.gym.auth.dto.VerifyEmailRequest;
import com.gym.auth.entity.User;
import com.gym.auth.entity.Verification;
import com.gym.auth.repository.UserRepository;
import com.gym.auth.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email {} already exists", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("Email already registered")
                    .build();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .accountStatus(User.AccountStatus.PENDING)
                .roles(Set.of(request.getUserType().name()))
                .build();

        user = userRepository.save(user);

        String verificationCode = generateVerificationCode();
        Verification verification = Verification.builder()
                .user(user)
                .type(Verification.VerificationType.EMAIL)
                .code(verificationCode)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        verificationRepository.save(verification);

        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        } catch (Exception e) {
            log.error("Failed to send verification email, but user was created", e);
        }

        log.info("User registered successfully: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .message("Registration successful. Please check your email to verify.")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.warn("Login failed: user not found {}", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for {}", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
        }

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            log.warn("Login failed: account not active for {}", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("Account is not verified. Please verify your email.")
                    .build();
        }

        String roles = String.join(",", user.getRoles());
        String token = jwtService.generateToken(user.getId().toString(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());

        log.info("User logged in successfully: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }

    /**
     * Refresh access token using a valid refresh token
     */
    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getToken();

        if (refreshToken == null || !jwtService.isRefreshTokenValid(refreshToken)) {
            log.warn("Token refresh failed: invalid or expired refresh token");
            return TokenRefreshResponse.builder()
                    .success(false)
                    .message("Invalid or expired refresh token")
                    .build();
        }

        String userId = jwtService.extractSubject(refreshToken);
        User user = userRepository.findById(Long.parseLong(userId))
                .orElse(null);

        if (user == null || user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            log.warn("Token refresh failed: user not found or account inactive");
            return TokenRefreshResponse.builder()
                    .success(false)
                    .message("User not found or account inactive")
                    .build();
        }

        String roles = String.join(",", user.getRoles());
        String newAccessToken = jwtService.generateToken(userId, roles);
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        Long expiresIn = jwtService.getTokenExpiration(newAccessToken);

        log.info("Token refreshed successfully for user: {}", userId);
        return TokenRefreshResponse.builder()
                .success(true)
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(expiresIn)
                .message("Token refreshed successfully")
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.warn("Verification failed: user not found {}", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build();
        }

        Verification verification = verificationRepository
                .findByUserAndCode(user, request.getCode())
                .orElse(null);

        if (verification == null) {
            log.warn("Verification failed: invalid code for {}", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid verification code")
                    .build();
        }

        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            log.warn("Verification failed: code expired for {}", request.getEmail());
            return AuthResponse.builder()
                    .success(false)
                    .message("Verification code has expired")
                    .build();
        }

        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        user.setAccountStatus(User.AccountStatus.ACTIVE);

        verificationRepository.save(verification);
        userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
        }

        log.info("Email verified successfully: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .message("Email verified successfully. You can now login.")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }

    private String generateVerificationCode() {
        int code = new Random().nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
