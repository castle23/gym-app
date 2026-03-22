package com.gym.notification.service;

import com.gym.notification.dto.PushTokenResponseDTO;
import com.gym.notification.entity.PushToken;
import com.gym.common.exception.InvalidDataException;
import com.gym.common.exception.ResourceNotFoundException;
import com.gym.common.exception.UnauthorizedException;
import com.gym.notification.repository.PushTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @InjectMocks
    private PushTokenService pushTokenService;

    private Long userId;
    private String token;
    private String deviceType;
    private PushToken validToken;

    @BeforeEach
    void setUp() {
        userId = 1L;
        token = "firebase_token_xyz123";
        deviceType = "android";

        validToken = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .lastUsedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============= registerToken Tests (6 tests) =============

    @Test
    void testRegisterToken_NewToken_Success() {
        // ARRANGE
        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.empty());
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(validToken);

        // ACT
        PushTokenResponseDTO result = pushTokenService.registerToken(userId, token, deviceType);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(deviceType, result.getDeviceType());
        assertTrue(result.getIsActive());
        assertNotNull(result.getLastUsedAt());
        
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, times(1)).save(any(PushToken.class));
        
        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).save(captor.capture());
        PushToken savedToken = captor.getValue();
        assertEquals(userId, savedToken.getUserId());
        assertEquals(token, savedToken.getToken());
        assertEquals(deviceType, savedToken.getDeviceType());
        assertTrue(savedToken.getIsActive());
    }

    @Test
    void testRegisterToken_ExistingActiveToken_UpdatesLastUsedAt() {
        // ARRANGE
        LocalDateTime originalTime = LocalDateTime.now().minusHours(1);
        PushToken existingToken = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .lastUsedAt(originalTime)
                .createdAt(originalTime.minusHours(2))
                .build();

        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.of(existingToken));
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(existingToken);

        // ACT
        PushTokenResponseDTO result = pushTokenService.registerToken(userId, token, deviceType);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertTrue(result.getIsActive());
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, times(1)).save(any(PushToken.class));
        
        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).save(captor.capture());
        PushToken updatedToken = captor.getValue();
        assertNotNull(updatedToken.getLastUsedAt());
        // Verify that lastUsedAt was updated (would be more recent than originalTime)
    }

    @Test
    void testRegisterToken_ExistingInactiveToken_Reactivates() {
        // ARRANGE
        PushToken inactiveToken = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .isActive(false)
                .lastUsedAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.of(inactiveToken));
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(inactiveToken);

        // ACT
        PushTokenResponseDTO result = pushTokenService.registerToken(userId, token, deviceType);

        // ASSERT
        assertNotNull(result);
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, times(1)).save(any(PushToken.class));
        
        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).save(captor.capture());
        PushToken reactivatedToken = captor.getValue();
        assertTrue(reactivatedToken.getIsActive());
    }

    @Test
    void testRegisterToken_NullUserId() {
        // ARRANGE & ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> pushTokenService.registerToken(null, token, deviceType));
        
        assertEquals("User ID is required", exception.getMessage());
        verify(pushTokenRepository, never()).findByToken(any());
        verify(pushTokenRepository, never()).save(any());
    }

    @Test
    void testRegisterToken_BlankToken() {
        // ARRANGE & ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> pushTokenService.registerToken(userId, "   ", deviceType));
        
        assertEquals("Token is required", exception.getMessage());
        verify(pushTokenRepository, never()).findByToken(any());
        verify(pushTokenRepository, never()).save(any());
    }

    @Test
    void testRegisterToken_NullDeviceType() {
        // ARRANGE & ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> pushTokenService.registerToken(userId, token, null));
        
        assertEquals("Device type is required", exception.getMessage());
        verify(pushTokenRepository, never()).findByToken(any());
        verify(pushTokenRepository, never()).save(any());
    }

    // ============= getTokensForUser Tests (2 tests) =============

    @Test
    void testGetTokensForUser_Success() {
        // ARRANGE
        LocalDateTime now = LocalDateTime.now();
        PushToken token1 = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token("firebase_token_one_123456789")
                .deviceType("android")
                .isActive(true)
                .createdAt(now.minusHours(1))
                .build();

        PushToken token2 = PushToken.builder()
                .id(2L)
                .userId(userId)
                .token("firebase_token_two_987654321")
                .deviceType("ios")
                .isActive(false)
                .createdAt(now)
                .build();

        List<PushToken> tokens = List.of(token1, token2);
        when(pushTokenRepository.findByUserId(userId)).thenReturn(tokens);

        // ACT
        List<PushTokenResponseDTO> result = pushTokenService.getTokensForUser(userId);

        // ASSERT
        assertEquals(2, result.size());
        // Verify sorted by createdAt descending (token2 first, more recent)
        assertEquals(2L, result.get(0).getId());  // token2 (more recent)
        assertEquals(1L, result.get(1).getId());  // token1 (older)
        assertEquals("android", result.get(1).getDeviceType());
        assertEquals("ios", result.get(0).getDeviceType());
        verify(pushTokenRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetTokensForUser_EmptyList() {
        // ARRANGE
        when(pushTokenRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // ACT
        List<PushTokenResponseDTO> result = pushTokenService.getTokensForUser(userId);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(pushTokenRepository, times(1)).findByUserId(userId);
    }

    // ============= getActiveTokens Tests (2 tests) =============

    @Test
    void testGetActiveTokens_Success() {
        // ARRANGE
        PushToken activeToken = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token("active_token")
                .deviceType("android")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        List<PushToken> activeTokens = List.of(activeToken);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(activeTokens);

        // ACT
        List<PushTokenResponseDTO> result = pushTokenService.getActiveTokens(userId);

        // ASSERT
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(pushTokenRepository, times(1)).findByUserIdAndIsActiveTrue(userId);
    }

    @Test
    void testGetActiveTokens_FiltersInactiveTokens() {
        // ARRANGE
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());

        // ACT
        List<PushTokenResponseDTO> result = pushTokenService.getActiveTokens(userId);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(pushTokenRepository, times(1)).findByUserIdAndIsActiveTrue(userId);
    }

    // ============= getActiveTokensByDeviceType Tests (2 tests) =============

    @Test
    void testGetActiveTokensByDeviceType_Success() {
        // ARRANGE
        String targetDeviceType = "ios";
        PushToken iosToken = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token("ios_token")
                .deviceType(targetDeviceType)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        List<PushToken> iosTokens = List.of(iosToken);
        when(pushTokenRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, targetDeviceType))
                .thenReturn(iosTokens);

        // ACT
        List<PushTokenResponseDTO> result = pushTokenService.getActiveTokensByDeviceType(userId, targetDeviceType);

        // ASSERT
        assertEquals(1, result.size());
        assertEquals(targetDeviceType, result.get(0).getDeviceType());
        assertTrue(result.get(0).getIsActive());
        verify(pushTokenRepository, times(1)).findByUserIdAndDeviceTypeAndIsActiveTrue(userId, targetDeviceType);
    }

    @Test
    void testGetActiveTokensByDeviceType_EmptyList() {
        // ARRANGE
        String targetDeviceType = "ios";
        when(pushTokenRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, targetDeviceType))
                .thenReturn(Collections.emptyList());

        // ACT
        List<PushTokenResponseDTO> result = pushTokenService.getActiveTokensByDeviceType(userId, targetDeviceType);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(pushTokenRepository, times(1)).findByUserIdAndDeviceTypeAndIsActiveTrue(userId, targetDeviceType);
    }

    // ============= deactivateToken Tests (3 tests) =============

    @Test
    void testDeactivateToken_Success() {
        // ARRANGE
        PushToken activeToken = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.of(activeToken));
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(activeToken);

        // ACT
        pushTokenService.deactivateToken(userId, token);

        // ASSERT
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, times(1)).save(any(PushToken.class));
        
        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).save(captor.capture());
        PushToken deactivatedToken = captor.getValue();
        assertFalse(deactivatedToken.getIsActive());
    }

    @Test
    void testDeactivateToken_TokenNotFound() {
        // ARRANGE
        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> pushTokenService.deactivateToken(userId, token));
        
        assertEquals("Push token not found", exception.getMessage());
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, never()).save(any());
    }

    @Test
    void testDeactivateToken_UnauthorizedUser() {
        // ARRANGE
        Long differentUserId = 999L;
        PushToken tokenOwnedByOtherUser = PushToken.builder()
                .id(1L)
                .userId(differentUserId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.of(tokenOwnedByOtherUser));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> pushTokenService.deactivateToken(userId, token));
        
        assertEquals("You do not have permission to deactivate this token", exception.getMessage());
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, never()).save(any());
    }

    // ============= deleteToken Tests (2 tests) =============

    @Test
    void testDeleteToken_Success() {
        // ARRANGE
        PushToken tokenToDelete = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.of(tokenToDelete));

        // ACT
        pushTokenService.deleteToken(userId, token);

        // ASSERT
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, times(1)).deleteByUserIdAndToken(userId, token);
    }

    @Test
    void testDeleteToken_UnauthorizedUser() {
        // ARRANGE
        Long differentUserId = 999L;
        PushToken tokenOwnedByOtherUser = PushToken.builder()
                .id(1L)
                .userId(differentUserId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(pushTokenRepository.findByToken(token)).thenReturn(Optional.of(tokenOwnedByOtherUser));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> pushTokenService.deleteToken(userId, token));
        
        assertEquals("You do not have permission to delete this token", exception.getMessage());
        verify(pushTokenRepository, times(1)).findByToken(token);
        verify(pushTokenRepository, never()).deleteByUserIdAndToken(any(), any());
    }

    // ============= deleteAllTokensForUser Tests (1 test) =============

    @Test
    void testDeleteAllTokensForUser_Success() {
        // ARRANGE
        PushToken token1 = PushToken.builder()
                .id(1L)
                .userId(userId)
                .token("token1")
                .deviceType("android")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        PushToken token2 = PushToken.builder()
                .id(2L)
                .userId(userId)
                .token("token2")
                .deviceType("ios")
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .build();

        List<PushToken> userTokens = List.of(token1, token2);
        when(pushTokenRepository.findByUserId(userId)).thenReturn(userTokens);

        // ACT
        pushTokenService.deleteAllTokensForUser(userId);

        // ASSERT
        verify(pushTokenRepository, times(1)).findByUserId(userId);
        verify(pushTokenRepository, times(2)).delete(any(PushToken.class));
    }
}
