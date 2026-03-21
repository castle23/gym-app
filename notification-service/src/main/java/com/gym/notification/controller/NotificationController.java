package com.gym.notification.controller;

import com.gym.notification.dto.NotificationRequestDTO;
import com.gym.notification.dto.NotificationResponseDTO;
import com.gym.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "User notifications and alert management")
public class NotificationController {

    private final NotificationService notificationService;

     /**
      * Get all notifications for a user
      * 
      * @param userId the user ID from X-User-Id header
      * @return 200 OK with list of notifications
      */
     @GetMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get all notifications", description = "Retrieves all notifications for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<List<NotificationResponseDTO>> getAllNotifications(
            @RequestHeader(value = "X-User-Id") Long userId) {
        log.info("Fetching all notifications for user: {}", userId);
        List<NotificationResponseDTO> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

     /**
      * Get all unread notifications for a user
      * 
      * @param userId the user ID from X-User-Id header
      * @return 200 OK with list of unread notifications
      */
     @GetMapping("/unread")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get unread notifications", description = "Retrieves all unread notifications for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications(
            @RequestHeader(value = "X-User-Id") Long userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        List<NotificationResponseDTO> unreadNotifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(unreadNotifications);
    }

     /**
      * Get count of unread notifications for a user
      * 
      * @param userId the user ID from X-User-Id header
      * @return 200 OK with unread count as Long
      */
     @GetMapping("/unread/count")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get unread notification count", description = "Retrieves the count of unread notifications for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<Long> getUnreadCount(
            @RequestHeader(value = "X-User-Id") Long userId) {
        log.info("Fetching unread count for user: {}", userId);
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

     /**
      * Send a new notification
      * 
      * @param userId the user ID from X-User-Id header
      * @param request the notification request DTO
      * @return 201 CREATED with notification response
      */
     @PostMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Send a notification", description = "Sends a new notification for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Notification sent successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid notification data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<NotificationResponseDTO> sendNotification(
            @RequestHeader(value = "X-User-Id") Long userId,
            @Valid @RequestBody NotificationRequestDTO request) {
        log.info("Sending notification for user: {}", userId);
        NotificationResponseDTO response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

     /**
      * Mark a notification as read
      * 
      * @param userId the user ID from X-User-Id header
      * @param id the notification ID
      * @return 200 OK with updated notification response
      * @throws ResourceNotFoundException if notification not found
      * @throws UnauthorizedException if user doesn't own the notification
      */
     @PutMapping("/{id}/read")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Mark notification as read", description = "Marks a notification as read (requires authentication and notification ownership)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Notification marked as read successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the notification owner"),
             @ApiResponse(responseCode = "404", description = "Notification not found")
     })
     public ResponseEntity<NotificationResponseDTO> markAsRead(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("Marking notification {} as read for user: {}", id, userId);
        NotificationResponseDTO response = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(response);
    }

     /**
      * Delete a notification
      * 
      * @param userId the user ID from X-User-Id header
      * @param id the notification ID
      * @return 204 NO CONTENT
      * @throws ResourceNotFoundException if notification not found
      * @throws UnauthorizedException if user doesn't own the notification
      */
     @DeleteMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Delete a notification", description = "Deletes a notification (requires authentication and notification ownership)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the notification owner"),
             @ApiResponse(responseCode = "404", description = "Notification not found")
     })
     public ResponseEntity<Void> deleteNotification(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("Deleting notification {} for user: {}", id, userId);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
}
