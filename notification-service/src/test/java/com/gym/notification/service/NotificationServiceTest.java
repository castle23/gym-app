package com.gym.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.api.core.ApiFuture;
import com.google.common.collect.ImmutableList;
import com.gym.notification.dto.NotificationRequestDTO;
import com.gym.notification.dto.NotificationResponseDTO;
import com.gym.notification.entity.Notification;
import com.gym.notification.entity.NotificationPreference;
import com.gym.notification.entity.NotificationType;
import com.gym.notification.entity.PushToken;
import com.gym.notification.exception.InvalidDataException;
import com.gym.notification.exception.ResourceNotFoundException;
import com.gym.notification.exception.UnauthorizedException;
import com.gym.notification.repository.NotificationPreferenceRepository;
import com.gym.notification.repository.NotificationRepository;
import com.gym.notification.repository.PushTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequestDTO validRequest;
    private PushToken validToken;
    private Notification savedNotification;

    @BeforeEach
    void setUp() {
        validRequest = NotificationRequestDTO.builder()
                .userId(1L)
                .title("Test Title")
                .body("Test Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .build();

        validToken = PushToken.builder()
                .id(1L)
                .userId(1L)
                .token("token123")
                .deviceType("android")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        savedNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Test Title")
                .body("Test Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();
    }

    // ============= sendNotification Tests (8 tests) =============

    @Test
    void testSendNotification_Success() throws Exception {
        // ARRANGE
        List<PushToken> tokens = List.of(validToken);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(tokens);
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .notificationType(NotificationType.WORKOUT_REMINDER)
                        .isEnabled(true)
                        .build()));
        
     // Mock Firebase response
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse));
        when(firebaseMessaging.sendMulticast(any(MulticastMessage.class)))
                .thenReturn(batchResponse);
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(validToken);

        // ACT
        NotificationResponseDTO response = notificationService.sendNotification(validRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Test Title", response.getTitle());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(pushTokenRepository, times(1)).save(any(PushToken.class));
    }

    @Test
    void testSendNotification_NoTokens() {
        // ARRANGE
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> notificationService.sendNotification(validRequest));
        assertEquals("No active push tokens found for user", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testSendNotification_PreferenceDisabled() throws FirebaseMessagingException {
        // ARRANGE
        List<PushToken> tokens = List.of(validToken);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(tokens);
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .notificationType(NotificationType.WORKOUT_REMINDER)
                        .isEnabled(false)
                        .build()));

        // ACT
        NotificationResponseDTO response = notificationService.sendNotification(validRequest);

        // ASSERT
        assertNotNull(response);
        verify(firebaseMessaging, never()).sendMulticast(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testSendNotification_DuringQuietHours() throws FirebaseMessagingException {
        // ARRANGE
        LocalTime now = LocalTime.now();
        LocalTime quietStart = now.minusMinutes(30);
        LocalTime quietEnd = now.plusMinutes(30);
        
        List<PushToken> tokens = List.of(validToken);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(tokens);
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .notificationType(NotificationType.WORKOUT_REMINDER)
                        .isEnabled(true)
                        .quietHoursStart(quietStart)
                        .quietHoursEnd(quietEnd)
                        .build()));

        // ACT
        NotificationResponseDTO response = notificationService.sendNotification(validRequest);

        // ASSERT
        assertNotNull(response);
        verify(firebaseMessaging, never()).sendMulticast(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testSendNotification_InvalidTokenHandling() throws Exception {
        // ARRANGE
        List<PushToken> tokens = List.of(validToken);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(tokens);
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .notificationType(NotificationType.WORKOUT_REMINDER)
                        .isEnabled(true)
                        .build()));
        
        // Mock Firebase error response
        SendResponse errorResponse = mock(SendResponse.class);
        when(errorResponse.isSuccessful()).thenReturn(false);
        BatchResponse errorBatchResponse = mock(BatchResponse.class);
        when(errorBatchResponse.getResponses()).thenReturn(List.of(errorResponse));
        when(firebaseMessaging.sendMulticast(any(MulticastMessage.class)))
                .thenReturn(errorBatchResponse);
        
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(validToken);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // ACT
        NotificationResponseDTO response = notificationService.sendNotification(validRequest);

        // ASSERT
        assertNotNull(response);
        
        // Verify token was marked as inactive
        ArgumentCaptor<PushToken> tokenCaptor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository, times(1)).save(tokenCaptor.capture());
        
        PushToken savedToken = tokenCaptor.getValue();
        assertFalse(savedToken.getIsActive());
    }

    @Test
    void testSendNotification_MultipleTokens() throws Exception {
        // ARRANGE
        PushToken token2 = PushToken.builder()
                .id(2L)
                .userId(1L)
                .token("token456")
                .deviceType("ios")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        List<PushToken> tokens = List.of(validToken, token2);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(tokens);
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .notificationType(NotificationType.WORKOUT_REMINDER)
                        .isEnabled(true)
                        .build()));
        
        // Mock Firebase success response
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);
        BatchResponse multiBatchResponse = mock(BatchResponse.class);
        when(multiBatchResponse.getResponses()).thenReturn(List.of(successResponse, successResponse));
        when(firebaseMessaging.sendMulticast(any(MulticastMessage.class)))
                .thenReturn(multiBatchResponse);
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(validToken);

        // ACT
        NotificationResponseDTO response = notificationService.sendNotification(validRequest);

        // ASSERT
        assertNotNull(response);
        verify(firebaseMessaging, times(1)).sendMulticast(any(MulticastMessage.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendNotification_NullUserId() {
        // ARRANGE
        NotificationRequestDTO invalidRequest = NotificationRequestDTO.builder()
                .userId(null)
                .title("Test")
                .body("Test")
                .type(NotificationType.WORKOUT_REMINDER)
                .build();

        // ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> notificationService.sendNotification(invalidRequest));
        assertEquals("User ID is required", exception.getMessage());
        verify(pushTokenRepository, never()).findByUserIdAndIsActiveTrue(any());
    }

    @Test
    void testSendNotification_UpdatesLastUsedAt() throws Exception {
        // ARRANGE
        List<PushToken> tokens = List.of(validToken);
        when(pushTokenRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(tokens);
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .notificationType(NotificationType.WORKOUT_REMINDER)
                        .isEnabled(true)
                        .build()));
        
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);
        BatchResponse lastUsedBatchResponse = mock(BatchResponse.class);
        when(lastUsedBatchResponse.getResponses()).thenReturn(List.of(successResponse));
        when(firebaseMessaging.sendMulticast(any(MulticastMessage.class)))
                .thenReturn(lastUsedBatchResponse);
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(validToken);

        // ACT
        notificationService.sendNotification(validRequest);

        // ASSERT
        ArgumentCaptor<PushToken> tokenCaptor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository, times(1)).save(tokenCaptor.capture());
        
        PushToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getLastUsedAt());
    }

    // ============= getNotifications Tests (3 tests) =============

    @Test
    void testGetNotifications_Success() {
        // ARRANGE
        Notification notification1 = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Title 1")
                .body("Body 1")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        Notification notification2 = Notification.builder()
                .id(2L)
                .userId(1L)
                .title("Title 2")
                .body("Body 2")
                .type(NotificationType.ACHIEVEMENT)
                .isRead(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        List<Notification> notifications = List.of(notification2, notification1); // Descending order
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notifications);

        // ACT
        List<NotificationResponseDTO> result = notificationService.getNotifications(1L);

        // ASSERT
        assertEquals(2, result.size());
        assertEquals("Title 2", result.get(0).getTitle());
        assertEquals("Title 1", result.get(1).getTitle());
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void testGetNotifications_EmptyList() {
        // ARRANGE
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        // ACT
        List<NotificationResponseDTO> result = notificationService.getNotifications(1L);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void testGetNotifications_CorrectOrder() {
        // ARRANGE
        LocalDateTime now = LocalDateTime.now();
        Notification notification1 = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Oldest")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(now.minusHours(3))
                .build();

        Notification notification2 = Notification.builder()
                .id(2L)
                .userId(1L)
                .title("Middle")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(now.minusHours(2))
                .build();

        Notification notification3 = Notification.builder()
                .id(3L)
                .userId(1L)
                .title("Newest")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(now.minusHours(1))
                .build();

        List<Notification> notifications = List.of(notification3, notification2, notification1); // Descending
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notifications);

        // ACT
        List<NotificationResponseDTO> result = notificationService.getNotifications(1L);

        // ASSERT
        assertEquals(3, result.size());
        assertEquals("Newest", result.get(0).getTitle());
        assertEquals("Middle", result.get(1).getTitle());
        assertEquals("Oldest", result.get(2).getTitle());
    }

    // ============= getUnreadNotifications Tests (2 tests) =============

    @Test
    void testGetUnreadNotifications_Success() {
        // ARRANGE
        Notification unreadNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Unread")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        List<Notification> unreadNotifications = List.of(unreadNotification);
        when(notificationRepository.findByUserIdAndIsReadFalse(1L)).thenReturn(unreadNotifications);

        // ACT
        List<NotificationResponseDTO> result = notificationService.getUnreadNotifications(1L);

        // ASSERT
        assertEquals(1, result.size());
        assertEquals("Unread", result.get(0).getTitle());
        assertFalse(result.get(0).getIsRead());
        verify(notificationRepository, times(1)).findByUserIdAndIsReadFalse(1L);
    }

    @Test
    void testGetUnreadNotifications_FiltersReadNotifications() {
        // ARRANGE
        List<Notification> unreadNotifications = Collections.emptyList();
        when(notificationRepository.findByUserIdAndIsReadFalse(1L)).thenReturn(unreadNotifications);

        // ACT
        List<NotificationResponseDTO> result = notificationService.getUnreadNotifications(1L);

        // ASSERT
        assertTrue(result.isEmpty());
        verify(notificationRepository, times(1)).findByUserIdAndIsReadFalse(1L);
    }

    // ============= getUnreadCount Tests (2 tests) =============

    @Test
    void testGetUnreadCount_Success() {
        // ARRANGE
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        // ACT
        Long result = notificationService.getUnreadCount(1L);

        // ASSERT
        assertEquals(3L, result);
        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalse(1L);
    }

    @Test
    void testGetUnreadCount_ZeroWhenNoUnread() {
        // ARRANGE
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0L);

        // ACT
        Long result = notificationService.getUnreadCount(1L);

        // ASSERT
        assertEquals(0L, result);
        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalse(1L);
    }

    // ============= markAsRead Tests (3 tests) =============

    @Test
    void testMarkAsRead_Success() {
        // ARRANGE
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Test")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification readNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Test")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(readNotification);

        // ACT
        NotificationResponseDTO result = notificationService.markAsRead(1L, 1L);

        // ASSERT
        assertTrue(result.getIsRead());
        assertEquals(1L, result.getId());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_UnauthorizedUser() {
        // ARRANGE
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Test")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> notificationService.markAsRead(1L, 999L));
        assertEquals("You do not have permission to access this notification", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAsRead_NotificationNotFound() {
        // ARRANGE
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(999L, 1L));
        assertEquals("Notification not found", exception.getMessage());
    }

    // ============= deleteNotification Tests (2 tests) =============

    @Test
    void testDeleteNotification_Success() {
        // ARRANGE
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Test")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // ACT
        notificationService.deleteNotification(1L, 1L);

        // ASSERT
        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteNotification_UnauthorizedUser() {
        // ARRANGE
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .title("Test")
                .body("Body")
                .type(NotificationType.WORKOUT_REMINDER)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> notificationService.deleteNotification(1L, 999L));
        assertEquals("You do not have permission to delete this notification", exception.getMessage());
        verify(notificationRepository, never()).deleteById(any());
    }
}
