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
    @Mock private ProfessionalRegistrationRequestRepository registrationRepository;
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


    @Test
    void testRequestProfessionalStatusSuccess() {
        User user = User.builder().id(1L).email("user@test.com").build();
        ProfessionalRegistrationDto dto = new ProfessionalRegistrationDto("Specialty", "License123");

        when(registrationRepository.existsByUser(user)).thenReturn(false);

        AuthResponse response = authService.requestProfessionalStatus(user, dto);

        assertNotNull(response);
        assertEquals("Registration request submitted", response.getMessage());
        verify(registrationRepository, times(1)).save(any(ProfessionalRegistrationRequest.class));
    }

    @Test
    void testRequestProfessionalStatusDuplicate() {
        User user = User.builder().id(1L).email("user@test.com").build();
        ProfessionalRegistrationDto dto = new ProfessionalRegistrationDto("Specialty", "License123");

        when(registrationRepository.existsByUser(user)).thenReturn(true);

        assertThrows(InvalidDataException.class, () -> authService.requestProfessionalStatus(user, dto));
    }

    @Test
    void testGetRegistrationRequestsSuccess() {
        authService.getRegistrationRequests();
        verify(registrationRepository, times(1)).findByStatus(ProfessionalRegistrationRequest.RequestStatus.PENDING);
    }

    @Test
    void testApproveRegistrationSuccess() {
        Long requestId = 1L;
        User user = User.builder().roles(new java.util.HashSet<>()).build();
        ProfessionalRegistrationRequest request = ProfessionalRegistrationRequest.builder()
                .user(user)
                .status(ProfessionalRegistrationRequest.RequestStatus.PENDING)
                .build();
        ProfessionalApprovalDto dto = new ProfessionalApprovalDto(null, null);

        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));

        AuthResponse response = authService.approveRegistration(requestId, dto);

        assertNotNull(response);
        assertEquals("Registration approved", response.getMessage());
        assertEquals(ProfessionalRegistrationRequest.RequestStatus.APPROVED, request.getStatus());
        assertTrue(user.getRoles().contains(User.UserType.PROFESSIONAL.name()));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testApproveRegistrationNotFound() {
        Long requestId = 1L;
        when(registrationRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.approveRegistration(requestId, new ProfessionalApprovalDto(null, null)));
    }

    @Test
    void testRejectRegistrationSuccess() {
        Long requestId = 1L;
        User user = User.builder().email("user@test.com").build();
        ProfessionalRegistrationRequest request = ProfessionalRegistrationRequest.builder()
                .user(user)
                .status(ProfessionalRegistrationRequest.RequestStatus.PENDING)
                .build();
        ProfessionalApprovalDto dto = new ProfessionalApprovalDto(null, "Rejected reason");

        when(registrationRepository.findById(requestId)).thenReturn(Optional.of(request));

        AuthResponse response = authService.rejectRegistration(requestId, dto);

        assertNotNull(response);
        assertEquals("Registration rejected", response.getMessage());
        assertEquals(ProfessionalRegistrationRequest.RequestStatus.REJECTED, request.getStatus());
        assertEquals("Rejected reason", request.getRejectionReason());
        verify(registrationRepository, times(1)).save(request);
    }

    @Test
    void testRejectRegistrationNotFound() {
        Long requestId = 1L;
        when(registrationRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.rejectRegistration(requestId, new ProfessionalApprovalDto(null, "reason")));
    }
}
