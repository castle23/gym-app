package com.gym.notification.service;

import com.gym.notification.dto.NotificationPreferenceRequestDTO;
import com.gym.notification.dto.NotificationPreferenceResponseDTO;
import com.gym.notification.entity.NotificationPreference;
import com.gym.notification.entity.NotificationType;
import com.gym.notification.exception.InvalidDataException;
import com.gym.notification.exception.ResourceNotFoundException;
import com.gym.notification.exception.UnauthorizedException;
import com.gym.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    /**
     * Saves or updates a notification preference for a user.
     *
     * @param userId the user ID
     * @param request the notification preference request
     * @return NotificationPreferenceResponseDTO
     * @throws InvalidDataException if userId or notificationType is null, or quiet hours are invalid
     */
    public NotificationPreferenceResponseDTO saveOrUpdatePreference(Long userId, NotificationPreferenceRequestDTO request) {
        // Validate userId
        if (userId == null) {
            log.warn("Attempt to save preference with null userId");
            throw new InvalidDataException("User ID is required");
        }

        // Validate notificationType
        if (request.getNotificationType() == null) {
            log.warn("Attempt to save preference with null notificationType for user: {}", userId);
            throw new InvalidDataException("Notification type is required");
        }

        // Validate quiet hours
        validateQuietHours(request.getQuietHoursStart(), request.getQuietHoursEnd());

        // Find existing preference or create new one
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(userId, request.getNotificationType())
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .notificationType(request.getNotificationType())
                        .build());

        // Update preference
        preference.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true);
        preference.setQuietHoursStart(request.getQuietHoursStart());
        preference.setQuietHoursEnd(request.getQuietHoursEnd());

        NotificationPreference savedPreference = notificationPreferenceRepository.save(preference);
        log.info("Notification preference saved/updated for user: {} with type: {}", userId, request.getNotificationType());

        return buildResponseDTO(savedPreference);
    }

    /**
     * Gets a notification preference by userId and notificationType.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return NotificationPreferenceResponseDTO
     * @throws InvalidDataException if userId or notificationType is null
     * @throws ResourceNotFoundException if preference not found
     */
    public NotificationPreferenceResponseDTO getPreference(Long userId, NotificationType notificationType) {
        // Validate inputs
        if (userId == null) {
            throw new InvalidDataException("User ID is required");
        }

        if (notificationType == null) {
            throw new InvalidDataException("Notification type is required");
        }

        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(userId, notificationType)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found"));

        log.debug("Retrieved preference for user: {} with type: {}", userId, notificationType);
        return buildResponseDTO(preference);
    }

    /**
     * Gets all notification preferences for a user, sorted by notificationType alphabetically.
     *
     * @param userId the user ID
     * @return list of NotificationPreferenceResponseDTO sorted by notificationType
     */
    public List<NotificationPreferenceResponseDTO> getAllPreferencesForUser(Long userId) {
        List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);

        log.debug("Retrieved {} preferences for user: {}", preferences.size(), userId);

        return preferences.stream()
                .sorted(Comparator.comparing(p -> p.getNotificationType().name()))
                .map(this::buildResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Enables a notification type for a user.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return NotificationPreferenceResponseDTO
     * @throws ResourceNotFoundException if preference not found
     * @throws UnauthorizedException if userId doesn't match preference owner
     */
    public NotificationPreferenceResponseDTO enableNotificationType(Long userId, NotificationType notificationType) {
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(userId, notificationType)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found"));

        // Authorize: verify userId matches
        if (!preference.getUserId().equals(userId)) {
            log.warn("Unauthorized attempt to enable preference. Requested userId: {}, Preference userId: {}", userId, preference.getUserId());
            throw new UnauthorizedException("You do not have permission to modify this preference");
        }

        preference.setIsEnabled(true);
        NotificationPreference updatedPreference = notificationPreferenceRepository.save(preference);
        log.info("Notification type {} enabled for user: {}", notificationType, userId);

        return buildResponseDTO(updatedPreference);
    }

    /**
     * Disables a notification type for a user.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return NotificationPreferenceResponseDTO
     * @throws ResourceNotFoundException if preference not found
     * @throws UnauthorizedException if userId doesn't match preference owner
     */
    public NotificationPreferenceResponseDTO disableNotificationType(Long userId, NotificationType notificationType) {
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(userId, notificationType)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found"));

        // Authorize: verify userId matches
        if (!preference.getUserId().equals(userId)) {
            log.warn("Unauthorized attempt to disable preference. Requested userId: {}, Preference userId: {}", userId, preference.getUserId());
            throw new UnauthorizedException("You do not have permission to modify this preference");
        }

        preference.setIsEnabled(false);
        NotificationPreference updatedPreference = notificationPreferenceRepository.save(preference);
        log.info("Notification type {} disabled for user: {}", notificationType, userId);

        return buildResponseDTO(updatedPreference);
    }

    /**
     * Deletes a notification preference.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @throws ResourceNotFoundException if preference not found
     * @throws UnauthorizedException if userId doesn't match preference owner
     */
    public void deletePreference(Long userId, NotificationType notificationType) {
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(userId, notificationType)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found"));

        // Authorize: verify userId matches
        if (!preference.getUserId().equals(userId)) {
            log.warn("Unauthorized attempt to delete preference. Requested userId: {}, Preference userId: {}", userId, preference.getUserId());
            throw new UnauthorizedException("You do not have permission to modify this preference");
        }

        notificationPreferenceRepository.deleteById(preference.getId());
        log.info("Notification preference deleted for user: {} with type: {}", userId, notificationType);
    }

    // ============= Helper Methods =============

    /**
     * Validates quiet hours logic.
     * Both must be set or both must be null.
     * If both set: start < end
     *
     * @param quietHoursStart the start of quiet hours
     * @param quietHoursEnd the end of quiet hours
     * @throws InvalidDataException if quiet hours are invalid
     */
    private void validateQuietHours(LocalTime quietHoursStart, LocalTime quietHoursEnd) {
        // Check if only one is set
        if ((quietHoursStart != null && quietHoursEnd == null) ||
            (quietHoursStart == null && quietHoursEnd != null)) {
            log.warn("Invalid quiet hours: both start and end must be set or both must be null");
            throw new InvalidDataException("Both quiet hours start and end must be set together, or both must be null");
        }

        // Check if both are set and start < end
        if (quietHoursStart != null && quietHoursEnd != null) {
            if (!quietHoursStart.isBefore(quietHoursEnd)) {
                log.warn("Invalid quiet hours: start time must be before end time. Start: {}, End: {}", quietHoursStart, quietHoursEnd);
                throw new InvalidDataException("Quiet hours start time must be before end time");
            }
        }
    }

    /**
     * Converts a NotificationPreference entity to NotificationPreferenceResponseDTO.
     *
     * @param preference the preference entity
     * @return NotificationPreferenceResponseDTO
     */
    private NotificationPreferenceResponseDTO buildResponseDTO(NotificationPreference preference) {
        return NotificationPreferenceResponseDTO.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .notificationType(preference.getNotificationType())
                .isEnabled(preference.getIsEnabled())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .createdAt(preference.getId() != null ? LocalDateTime.now() : null)
                .build();
    }
}
