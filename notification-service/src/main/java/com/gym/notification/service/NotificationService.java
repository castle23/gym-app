package com.gym.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.BatchResponse;
import com.gym.notification.dto.NotificationRequestDTO;
import com.gym.notification.dto.NotificationResponseDTO;
import com.gym.notification.entity.Notification;
import com.gym.notification.entity.NotificationPreference;
import com.gym.notification.entity.PushToken;
import com.gym.notification.exception.InvalidDataException;
import com.gym.notification.exception.ResourceNotFoundException;
import com.gym.notification.exception.UnauthorizedException;
import com.gym.notification.repository.NotificationPreferenceRepository;
import com.gym.notification.repository.NotificationRepository;
import com.gym.notification.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final PushTokenRepository pushTokenRepository;

    /**
     * Sends a notification to a user's active push tokens.
     * 
     * @param request the notification request containing userId, title, body, and type
     * @return NotificationResponseDTO with the sent notification details
     * @throws InvalidDataException if userId is null
     * @throws ResourceNotFoundException if user has no active push tokens
     */
    public NotificationResponseDTO sendNotification(NotificationRequestDTO request) {
        // Validate input
        if (request.getUserId() == null) {
            throw new InvalidDataException("User ID is required");
        }

        // Find active push tokens for user
        List<PushToken> activeTokens = pushTokenRepository.findByUserIdAndIsActiveTrue(request.getUserId());
        
        if (activeTokens.isEmpty()) {
            log.warn("No active push tokens found for user: {}", request.getUserId());
            throw new ResourceNotFoundException("No active push tokens found for user");
        }

        // Check notification preferences
        Optional<NotificationPreference> preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(request.getUserId(), request.getType());

        if (preference.isPresent() && !preference.get().getIsEnabled()) {
            log.warn("Notification type {} is disabled for user: {}", request.getType(), request.getUserId());
            return buildNotificationResponse(null);
        }

        // Check quiet hours
        if (preference.isPresent()) {
            NotificationPreference pref = preference.get();
            if (pref.getQuietHoursStart() != null && pref.getQuietHoursEnd() != null) {
                if (isWithinQuietHours(pref.getQuietHoursStart(), pref.getQuietHoursEnd())) {
                    log.warn("Notification blocked by quiet hours for user: {}", request.getUserId());
                    return buildNotificationResponse(null);
                }
            }
        }

        // Send notification to all active tokens
        List<String> tokenList = activeTokens.stream()
                .map(PushToken::getToken)
                .collect(Collectors.toList());

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokenList)
                    .putData("title", request.getTitle())
                    .putData("body", request.getBody())
                    .putData("type", request.getType().toString())
                    .build();

            BatchResponse batchResponse = firebaseMessaging.sendMulticast(message);
            List<SendResponse> responses = batchResponse.getResponses();

            // Process responses and update tokens
            for (int i = 0; i < responses.size(); i++) {
                SendResponse response = responses.get(i);
                PushToken token = activeTokens.get(i);

                if (response.isSuccessful()) {
                    // Update lastUsedAt
                    token.setLastUsedAt(LocalDateTime.now());
                    pushTokenRepository.save(token);
                    log.info("Notification sent successfully to token: {}", token.getToken());
                } else {
                    // Mark token as inactive on error
                    token.setIsActive(false);
                    pushTokenRepository.save(token);
                    log.warn("Failed to send notification to token: {}. Marking as inactive.", token.getToken());
                }
            }

            // Save notification record
            Notification notification = Notification.builder()
                    .userId(request.getUserId())
                    .title(request.getTitle())
                    .body(request.getBody())
                    .type(request.getType())
                    .isRead(false)
                    .sentAt(LocalDateTime.now())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification saved with ID: {} for user: {}", savedNotification.getId(), request.getUserId());

            return buildNotificationResponse(savedNotification);

        } catch (Exception e) {
            log.error("Error sending notification for user: {}", request.getUserId(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    /**
     * Gets all notifications for a user, sorted by creation date descending.
     * 
     * @param userId the user ID
     * @return list of NotificationResponseDTO
     */
    public List<NotificationResponseDTO> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::buildNotificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets all unread notifications for a user.
     * 
     * @param userId the user ID
     * @return list of unread NotificationResponseDTO
     */
    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        return unreadNotifications.stream()
                .map(this::buildNotificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets the count of unread notifications for a user.
     * 
     * @param userId the user ID
     * @return count of unread notifications
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Marks a notification as read.
     * 
     * @param notificationId the notification ID
     * @param userId the user ID (for authorization)
     * @return NotificationResponseDTO with updated notification
     * @throws ResourceNotFoundException if notification not found
     * @throws UnauthorizedException if userId doesn't match notification owner
     */
    public NotificationResponseDTO markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Check authorization
        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this notification");
        }

        notification.setIsRead(true);
        Notification updatedNotification = notificationRepository.save(notification);
        log.info("Notification {} marked as read for user: {}", notificationId, userId);

        return buildNotificationResponse(updatedNotification);
    }

    /**
     * Deletes a notification.
     * 
     * @param notificationId the notification ID
     * @param userId the user ID (for authorization)
     * @throws ResourceNotFoundException if notification not found
     * @throws UnauthorizedException if userId doesn't match notification owner
     */
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Check authorization
        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to delete this notification");
        }

        notificationRepository.deleteById(notificationId);
        log.info("Notification {} deleted for user: {}", notificationId, userId);
    }

    // ============= Helper Methods =============

    /**
     * Checks if the current time is within quiet hours.
     * 
     * @param quietStart the start of quiet hours
     * @param quietEnd the end of quiet hours
     * @return true if current time is within quiet hours, false otherwise
     */
    private boolean isWithinQuietHours(LocalTime quietStart, LocalTime quietEnd) {
        LocalTime now = LocalTime.now();
        
        if (quietStart.isBefore(quietEnd)) {
            // Normal case: quiet hours don't span midnight
            return !now.isBefore(quietStart) && now.isBefore(quietEnd);
        } else {
            // Quiet hours span midnight
            return !now.isBefore(quietStart) || now.isBefore(quietEnd);
        }
    }

    /**
     * Converts a Notification entity to NotificationResponseDTO.
     * 
     * @param notification the notification entity
     * @return NotificationResponseDTO
     */
    private NotificationResponseDTO buildNotificationResponse(Notification notification) {
        if (notification == null) {
            return NotificationResponseDTO.builder().build();
        }

        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}
