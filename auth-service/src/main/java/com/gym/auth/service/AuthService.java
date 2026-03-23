package com.gym.auth.service;

import com.gym.auth.dto.*;
import com.gym.auth.entity.PasswordResetToken;
import com.gym.auth.entity.ProfessionalRegistrationRequest;
import com.gym.auth.entity.User;
import com.gym.auth.entity.Verification;
import com.gym.auth.repository.PasswordResetTokenRepository;
import com.gym.auth.repository.ProfessionalRegistrationRequestRepository;
import com.gym.auth.repository.UserRepository;
import com.gym.auth.repository.VerificationRepository;
import com.gym.common.exception.AuthenticationException;
import com.gym.common.exception.InvalidDataException;
import com.gym.common.exception.ResourceNotFoundException;
import com.gym.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final ProfessionalRegistrationRequestRepository registrationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email {} already exists", request.getEmail());
            throw new InvalidDataException("Email already registered");
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
                .message("Registration successful. Please check your email to verify.")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid credentials for {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            log.warn("Login failed: account not active for {}", request.getEmail());
            throw new AuthenticationException("Account is not verified. Please verify your email.");
        }

        String roles = String.join(",", user.getRoles());
        String token = jwtService.generateToken(user.getId().toString(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());

        log.info("User logged in successfully: {}", user.getEmail());
        return AuthResponse.builder()
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
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        String userId = jwtService.extractSubject(refreshToken);
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            log.warn("Token refresh failed: account inactive for user {}", userId);
            throw new AuthenticationException("Account is inactive");
        }

        String roles = String.join(",", user.getRoles());
        String newAccessToken = jwtService.generateToken(userId, roles);
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        Long expiresIn = jwtService.getTokenExpiration(newAccessToken);

        log.info("Token refreshed successfully for user: {}", userId);
        return TokenRefreshResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(expiresIn)
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.warn("Verification failed: user not found {}", request.getEmail());
            throw new ResourceNotFoundException("User not found");
        }

        Verification verification = verificationRepository
                .findByUserAndCode(user, request.getCode())
                .orElseThrow(() -> new InvalidDataException("Invalid verification code"));

        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            log.warn("Verification failed: code expired for {}", request.getEmail());
            throw new InvalidDataException("Verification code has expired");
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
                .message("Email verified successfully. You can now login.")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse requestResendCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Verification verification = verificationRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No verification found"));

        LocalDateTime now = LocalDateTime.now();

        // Check 60s throttling
        if (verification.getLastResendAt() != null && now.isBefore(verification.getLastResendAt().plusSeconds(60))) {
            throw new InvalidDataException("Please wait 60 seconds before requesting again");
        }

        // Check daily limit (5)
        if (verification.getLastResendAt() != null && verification.getLastResendAt().toLocalDate().equals(now.toLocalDate()) 
            && verification.getResendCount() >= 5) {
            throw new InvalidDataException("Daily limit for verification code requests reached");
        }

        // Reset/Update
        if (verification.getLastResendAt() == null || !verification.getLastResendAt().toLocalDate().equals(now.toLocalDate())) {
            verification.setResendCount(1);
        } else {
            verification.setResendCount(verification.getResendCount() + 1);
        }
        
        String newCode = generateVerificationCode();
        verification.setCode(newCode);
        verification.setExpiresAt(now.plusMinutes(15));
        verification.setLastResendAt(now);
        
        verificationRepository.save(verification);
        emailService.sendVerificationEmail(user.getEmail(), newCode);
        
        return AuthResponse.builder().message("New verification code sent").build();
    }

    private String generateVerificationCode() {
        int code = new Random().nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    @Transactional
    public AuthResponse requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
        }
        log.info("Password reset requested for email: {}", user.getEmail());
        return AuthResponse.builder().message("Password reset link sent").build();
    }

    @Transactional
    public AuthResponse confirmPasswordReset(PasswordResetConfirm request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidDataException("Invalid reset token"));

        if (resetToken.isUsed() || LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            throw new InvalidDataException("Reset token is expired or already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
        return AuthResponse.builder().message("Password reset successful").build();
    }

    @Transactional
    public AuthResponse requestProfessionalStatus(User user, ProfessionalRegistrationDto dto) {
        if (registrationRepository.existsByUser(user)) {
            throw new InvalidDataException("Request already pending");
        }
        
        ProfessionalRegistrationRequest request = ProfessionalRegistrationRequest.builder()
                .user(user)
                .specialty(dto.getSpecialty())
                .licenseNumber(dto.getLicenseNumber())
                .status(ProfessionalRegistrationRequest.RequestStatus.PENDING)
                .build();
        
        registrationRepository.save(request);
        log.info("Professional request submitted for user: {}", user.getEmail());
        return AuthResponse.builder().message("Registration request submitted").build();
    }
    
    @Transactional(readOnly = true)
    public List<ProfessionalRegistrationRequest> getRegistrationRequests() {
        return registrationRepository.findByStatus(ProfessionalRegistrationRequest.RequestStatus.PENDING);
    }
    
    @Transactional
    public AuthResponse approveRegistration(Long requestId, ProfessionalApprovalDto dto) {
        ProfessionalRegistrationRequest request = registrationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        request.setStatus(ProfessionalRegistrationRequest.RequestStatus.APPROVED);
        registrationRepository.save(request);
        
        User user = request.getUser();
        user.getRoles().add(User.UserType.PROFESSIONAL.name());
        userRepository.save(user);
        
        log.info("Professional registration approved for user: {}", user.getEmail());
        return AuthResponse.builder().message("Registration approved").build();
    }
    
    @Transactional
    public AuthResponse rejectRegistration(Long requestId, ProfessionalApprovalDto dto) {
        ProfessionalRegistrationRequest request = registrationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        request.setStatus(ProfessionalRegistrationRequest.RequestStatus.REJECTED);
        request.setRejectionReason(dto.getRejectionReason());
        registrationRepository.save(request);
        
        log.info("Professional registration rejected for user: {}", request.getUser().getEmail());
        return AuthResponse.builder().message("Registration rejected").build();
    }
}
