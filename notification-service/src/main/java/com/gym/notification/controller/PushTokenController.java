package com.gym.notification.controller;

import com.gym.notification.dto.PushTokenRequestDTO;
import com.gym.notification.dto.PushTokenResponseDTO;
import com.gym.notification.service.PushTokenService;
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
@RequestMapping("/api/v1/push-tokens")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Push Tokens", description = "Push notification device token management")
public class PushTokenController {

    private final PushTokenService pushTokenService;

     /**
      * Register a new push token for a user
      * 
      * @param userId the user ID from X-User-Id header
      * @param request the push token request DTO
      * @return 201 CREATED with push token response
      */
     @PostMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Register a push token", description = "Registers a new push notification token for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Push token registered successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid push token data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<PushTokenResponseDTO> registerToken(
            @RequestHeader(value = "X-User-Id") Long userId,
            @Valid @RequestBody PushTokenRequestDTO request) {
        log.info("Registering push token for user: {}", userId);
        PushTokenResponseDTO response = pushTokenService.registerToken(
                userId,
                request.getToken(),
                request.getDeviceType());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

     /**
      * Get all tokens (active and inactive) for a user
      * 
      * @param userId the user ID from X-User-Id header
      * @return 200 OK with list of all push tokens
      */
     @GetMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get all push tokens", description = "Retrieves all push notification tokens (active and inactive) for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Push tokens retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<List<PushTokenResponseDTO>> getTokens(
            @RequestHeader(value = "X-User-Id") Long userId) {
        log.info("Fetching all tokens for user: {}", userId);
        List<PushTokenResponseDTO> tokens = pushTokenService.getTokensForUser(userId);
        return ResponseEntity.ok(tokens);
    }

     /**
      * Get all active tokens for a user
      * 
      * @param userId the user ID from X-User-Id header
      * @return 200 OK with list of active push tokens
      */
     @GetMapping("/active")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get active push tokens", description = "Retrieves all active push notification tokens for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Active push tokens retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<List<PushTokenResponseDTO>> getActiveTokens(
            @RequestHeader(value = "X-User-Id") Long userId) {
        log.info("Fetching active tokens for user: {}", userId);
        List<PushTokenResponseDTO> activeTokens = pushTokenService.getActiveTokens(userId);
        return ResponseEntity.ok(activeTokens);
    }

     /**
      * Deactivate a push token
      * 
      * @param userId the user ID from X-User-Id header
      * @param token the push token to deactivate (query parameter)
      * @return 204 NO CONTENT
      * @throws ResourceNotFoundException if token not found
      * @throws UnauthorizedException if user doesn't own the token
      */
     @DeleteMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Deactivate a push token", description = "Deactivates a push notification token (requires authentication and token ownership)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Push token deactivated successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the token owner"),
             @ApiResponse(responseCode = "404", description = "Push token not found")
     })
     public ResponseEntity<Void> deactivateToken(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam(value = "token") String token) {
        log.info("Deactivating token for user: {}", userId);
        pushTokenService.deactivateToken(userId, token);
        return ResponseEntity.noContent().build();
    }
}
