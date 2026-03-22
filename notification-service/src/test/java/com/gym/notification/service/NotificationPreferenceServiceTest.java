package com.gym.notification.service;

import com.gym.notification.dto.NotificationPreferenceRequestDTO;
import com.gym.notification.dto.NotificationPreferenceResponseDTO;
import com.gym.notification.entity.NotificationPreference;
import com.gym.notification.entity.NotificationType;
import com.gym.common.exception.InvalidDataException;
import com.gym.common.exception.ResourceNotFoundException;
import com.gym.common.exception.UnauthorizedException;
import com.gym.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @InjectMocks
    private NotificationPreferenceService notificationPreferenceService;

    private NotificationPreferenceRequestDTO validRequest;
    private NotificationPreference savedPreference;

    @BeforeEach
    void setUp() {
        validRequest = NotificationPreferenceRequestDTO.builder()
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        savedPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();
    }

    // ============= saveOrUpdatePreference Tests (5 tests) =============

    @Test
    void testSaveOrUpdatePreference_NewPreference_Success() {
        // ARRANGE
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenReturn(savedPreference);

        // ACT
        NotificationPreferenceResponseDTO response = notificationPreferenceService.saveOrUpdatePreference(1L, validRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(NotificationType.WORKOUT_REMINDER, response.getNotificationType());
        assertTrue(response.getIsEnabled());
        
        // Verify that save was called
        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository, times(1)).save(captor.capture());
        
        NotificationPreference savedPref = captor.getValue();
        assertEquals(1L, savedPref.getUserId());
        assertEquals(NotificationType.WORKOUT_REMINDER, savedPref.getNotificationType());
    }

    @Test
    void testSaveOrUpdatePreference_ExistingPreference_Updated() {
        // ARRANGE
        NotificationPreference existingPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(false)
                .build();

        NotificationPreference updatedPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(existingPreference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenReturn(updatedPreference);

        // ACT
        NotificationPreferenceResponseDTO response = notificationPreferenceService.saveOrUpdatePreference(1L, validRequest);

        // ASSERT
        assertNotNull(response);
        assertTrue(response.getIsEnabled());
        
        // Verify that existing preference was updated
        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository, times(1)).save(captor.capture());
        
        NotificationPreference savedPref = captor.getValue();
        assertTrue(savedPref.getIsEnabled());
    }

    @Test
    void testSaveOrUpdatePreference_InvalidQuietHours_StartWithoutEnd() {
        // ARRANGE
        NotificationPreferenceRequestDTO invalidRequest = NotificationPreferenceRequestDTO.builder()
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .quietHoursStart(LocalTime.of(22, 0))
                .quietHoursEnd(null)
                .build();

        // ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> notificationPreferenceService.saveOrUpdatePreference(1L, invalidRequest));
        
        assertEquals("Both quiet hours start and end must be set together, or both must be null", exception.getMessage());
        verify(notificationPreferenceRepository, never()).save(any());
    }

    @Test
    void testSaveOrUpdatePreference_InvalidQuietHours_EndBeforeStart() {
        // ARRANGE
        NotificationPreferenceRequestDTO invalidRequest = NotificationPreferenceRequestDTO.builder()
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .quietHoursStart(LocalTime.of(22, 0))
                .quietHoursEnd(LocalTime.of(21, 0))
                .build();

        // ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> notificationPreferenceService.saveOrUpdatePreference(1L, invalidRequest));
        
        assertEquals("Quiet hours start time must be before end time", exception.getMessage());
        verify(notificationPreferenceRepository, never()).save(any());
    }

    @Test
    void testSaveOrUpdatePreference_NullUserId() {
        // ARRANGE
        // userId is null - passed explicitly
        
        // ACT & ASSERT
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> notificationPreferenceService.saveOrUpdatePreference(null, validRequest));
        
        assertEquals("User ID is required", exception.getMessage());
        verify(notificationPreferenceRepository, never()).save(any());
    }

    // ============= getPreference Tests (2 tests) =============

    @Test
    void testGetPreference_Success() {
        // ARRANGE
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(savedPreference));

        // ACT
        NotificationPreferenceResponseDTO response = notificationPreferenceService.getPreference(1L, NotificationType.WORKOUT_REMINDER);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(NotificationType.WORKOUT_REMINDER, response.getNotificationType());
        assertTrue(response.getIsEnabled());
        verify(notificationPreferenceRepository, times(1)).findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER);
    }

    @Test
    void testGetPreference_NotFound() {
        // ARRANGE
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> notificationPreferenceService.getPreference(1L, NotificationType.WORKOUT_REMINDER));
        
        assertEquals("Notification preference not found", exception.getMessage());
    }

    // ============= getAllPreferencesForUser Tests (2 tests) =============

    @Test
    void testGetAllPreferencesForUser_Success() {
        // ARRANGE
        NotificationPreference pref1 = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        NotificationPreference pref2 = NotificationPreference.builder()
                .id(2L)
                .userId(1L)
                .notificationType(NotificationType.ACHIEVEMENT)
                .isEnabled(false)
                .build();

        NotificationPreference pref3 = NotificationPreference.builder()
                .id(3L)
                .userId(1L)
                .notificationType(NotificationType.MESSAGE)
                .isEnabled(true)
                .build();

        // Return in non-alphabetical order to test sorting
        when(notificationPreferenceRepository.findByUserId(1L))
                .thenReturn(List.of(pref3, pref1, pref2));

        // ACT
        List<NotificationPreferenceResponseDTO> responses = notificationPreferenceService.getAllPreferencesForUser(1L);

        // ASSERT
        assertNotNull(responses);
        assertEquals(3, responses.size());
        
        // Verify sorting by notificationType alphabetically
        assertEquals(NotificationType.ACHIEVEMENT, responses.get(0).getNotificationType());
        assertEquals(NotificationType.MESSAGE, responses.get(1).getNotificationType());
        assertEquals(NotificationType.WORKOUT_REMINDER, responses.get(2).getNotificationType());
        
        verify(notificationPreferenceRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetAllPreferencesForUser_EmptyList() {
        // ARRANGE
        when(notificationPreferenceRepository.findByUserId(1L))
                .thenReturn(List.of());

        // ACT
        List<NotificationPreferenceResponseDTO> responses = notificationPreferenceService.getAllPreferencesForUser(1L);

        // ASSERT
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(notificationPreferenceRepository, times(1)).findByUserId(1L);
    }

    // ============= enableNotificationType Tests (1 test) =============

    @Test
    void testEnableNotificationType_Success() {
        // ARRANGE
        NotificationPreference disabledPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(false)
                .build();

        NotificationPreference enabledPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(disabledPreference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenReturn(enabledPreference);

        // ACT
        NotificationPreferenceResponseDTO response = notificationPreferenceService.enableNotificationType(1L, NotificationType.WORKOUT_REMINDER);

        // ASSERT
        assertNotNull(response);
        assertTrue(response.getIsEnabled());
        
        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository, times(1)).save(captor.capture());
        
        NotificationPreference savedPref = captor.getValue();
        assertTrue(savedPref.getIsEnabled());
    }

    // ============= disableNotificationType Tests (1 test) =============

    @Test
    void testDisableNotificationType_Success() {
        // ARRANGE
        NotificationPreference enabledPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        NotificationPreference disabledPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(false)
                .build();

        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(enabledPreference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenReturn(disabledPreference);

        // ACT
        NotificationPreferenceResponseDTO response = notificationPreferenceService.disableNotificationType(1L, NotificationType.WORKOUT_REMINDER);

        // ASSERT
        assertNotNull(response);
        assertFalse(response.getIsEnabled());
        
        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository, times(1)).save(captor.capture());
        
        NotificationPreference savedPref = captor.getValue();
        assertFalse(savedPref.getIsEnabled());
    }

    // ============= deletePreference Tests (1 test) =============

    @Test
    void testDeletePreference_Success() {
        // ARRANGE
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(savedPreference));

        // ACT
        notificationPreferenceService.deletePreference(1L, NotificationType.WORKOUT_REMINDER);

        // ASSERT
        verify(notificationPreferenceRepository, times(1)).deleteById(1L);
    }

    // ============= Authorization Tests =============

    @Test
    void testEnableNotificationType_UnauthorizedUser() {
        // ARRANGE
        NotificationPreference preference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(false)
                .build();

        when(notificationPreferenceRepository.findByUserIdAndNotificationType(999L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(preference));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> notificationPreferenceService.enableNotificationType(999L, NotificationType.WORKOUT_REMINDER));
        
        assertEquals("You do not have permission to modify this preference", exception.getMessage());
        verify(notificationPreferenceRepository, never()).save(any());
    }

    @Test
    void testDisableNotificationType_UnauthorizedUser() {
        // ARRANGE
        NotificationPreference preference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        when(notificationPreferenceRepository.findByUserIdAndNotificationType(999L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(preference));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> notificationPreferenceService.disableNotificationType(999L, NotificationType.WORKOUT_REMINDER));
        
        assertEquals("You do not have permission to modify this preference", exception.getMessage());
        verify(notificationPreferenceRepository, never()).save(any());
    }

    @Test
    void testDeletePreference_UnauthorizedUser() {
        // ARRANGE
        NotificationPreference preference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.WORKOUT_REMINDER)
                .isEnabled(true)
                .build();

        when(notificationPreferenceRepository.findByUserIdAndNotificationType(999L, NotificationType.WORKOUT_REMINDER))
                .thenReturn(Optional.of(preference));

        // ACT & ASSERT
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> notificationPreferenceService.deletePreference(999L, NotificationType.WORKOUT_REMINDER));
        
        assertEquals("You do not have permission to modify this preference", exception.getMessage());
        verify(notificationPreferenceRepository, never()).deleteById(any());
    }
}
