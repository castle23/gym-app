package com.gym.notification.controller;

import com.gym.notification.dto.PushTokenRequestDTO;
import com.gym.notification.dto.PushTokenResponseDTO;
import com.gym.notification.service.PushTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/push-tokens")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<Void> deactivateToken(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam(value = "token") String token) {
        log.info("Deactivating token for user: {}", userId);
        pushTokenService.deactivateToken(userId, token);
        return ResponseEntity.noContent().build();
    }
}
