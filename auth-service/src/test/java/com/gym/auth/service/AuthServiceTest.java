package com.gym.auth.service;

import com.gym.auth.dto.*;
import com.gym.auth.entity.*;
import com.gym.auth.repository.*;
import com.gym.common.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationRepository verificationRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRequestPasswordResetSuccess() {
        PasswordResetRequest request = new PasswordResetRequest("test@example.com");
        User user = User.builder().email("test@example.com").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.requestPasswordReset(request);

        assertNotNull(response);
        assertEquals("Password reset link sent", response.getMessage());
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void testRequestPasswordResetUserNotFound() {
        PasswordResetRequest request = new PasswordResetRequest("notfound@example.com");

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.requestPasswordReset(request));
    }

    @Test
    void testConfirmPasswordResetSuccess() {
        String token = UUID.randomUUID().toString();
        PasswordResetConfirm request = new PasswordResetConfirm(token, "newPassword");
        User user = User.builder().password("oldPassword").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        AuthResponse response = authService.confirmPasswordReset(request);

        assertNotNull(response);
        assertEquals("Password reset successful", response.getMessage());
        assertEquals("encodedNewPassword", user.getPassword());
        assertTrue(resetToken.isUsed());
        verify(userRepository, times(1)).save(user);
        verify(passwordResetTokenRepository, times(1)).save(resetToken);
    }

    @Test
    void testConfirmPasswordResetInvalidToken() {
        PasswordResetConfirm request = new PasswordResetConfirm("invalidToken", "newPassword");

        when(passwordResetTokenRepository.findByToken("invalidToken")).thenReturn(Optional.empty());

        assertThrows(InvalidDataException.class, () -> authService.confirmPasswordReset(request));
    }

    @Test
    void testConfirmPasswordResetExpiredToken() {
        String token = UUID.randomUUID().toString();
        PasswordResetConfirm request = new PasswordResetConfirm(token, "newPassword");
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThrows(InvalidDataException.class, () -> authService.confirmPasswordReset(request));
    }

    @Test
    void testConfirmPasswordResetAlreadyUsedToken() {
        String token = UUID.randomUUID().toString();
        PasswordResetConfirm request = new PasswordResetConfirm(token, "newPassword");
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThrows(InvalidDataException.class, () -> authService.confirmPasswordReset(request));
    }
}
