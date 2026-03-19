package com.gym.notification.service;

import com.gym.notification.dto.PushTokenResponseDTO;
import com.gym.notification.entity.PushToken;
import com.gym.notification.exception.InvalidDataException;
import com.gym.notification.exception.ResourceNotFoundException;
import com.gym.notification.exception.UnauthorizedException;
import com.gym.notification.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    /**
     * Register a push token for a user
     * - If token is new: create new PushToken entity
     * - If token exists and is inactive: reactivate it
     * - If token exists and is active: update lastUsedAt
     *
     * @param userId the user ID (required)
     * @param token the push token (required)
     * @param deviceType the device type (required)
     * @return PushTokenResponseDTO with token details
     * @throws InvalidDataException if userId, token, or deviceType is null/blank
     */
    public PushTokenResponseDTO registerToken(Long userId, String token, String deviceType) {
        // Validate required fields
        if (userId == null) {
            throw new InvalidDataException("User ID is required");
        }
        if (token == null || token.isBlank()) {
            throw new InvalidDataException("Token is required");
        }
        if (deviceType == null || deviceType.isBlank()) {
            throw new InvalidDataException("Device type is required");
        }

        // Check if token already exists
        Optional<PushToken> existingToken = pushTokenRepository.findByToken(token);
        
        PushToken pushToken;
        if (existingToken.isPresent()) {
            pushToken = existingToken.get();
            
            // Verify this token belongs to the user
            if (!pushToken.getUserId().equals(userId)) {
                throw new UnauthorizedException("This token is registered to a different user");
            }
            
            // If inactive, reactivate; always update lastUsedAt
            if (!pushToken.getIsActive()) {
                pushToken.setIsActive(true);
                log.info("Reactivating push token for user {}", userId);
            } else {
                log.debug("Push token already registered for user {}, updating lastUsedAt", userId);
            }
            pushToken.setLastUsedAt(LocalDateTime.now());
        } else {
            // Create new token
            pushToken = PushToken.builder()
                    .userId(userId)
                    .token(token)
                    .deviceType(deviceType)
                    .isActive(true)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            log.info("Registering new push token for user {} with device type {}", userId, deviceType);
        }

        PushToken savedToken = pushTokenRepository.save(pushToken);
        log.info("Push token registered successfully for user {}", userId);
        
        return convertToDTO(savedToken);
    }

    /**
     * Get all tokens for a user (active and inactive)
     *
     * @param userId the user ID
     * @return list of PushTokenResponseDTO sorted by createdAt descending
     */
    public List<PushTokenResponseDTO> getTokensForUser(Long userId) {
        log.debug("Retrieving all tokens for user {}", userId);
        
        List<PushToken> tokens = pushTokenRepository.findByUserId(userId);
        
        return tokens.stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active tokens for a user
     *
     * @param userId the user ID
     * @return list of active PushTokenResponseDTO sorted by createdAt descending
     */
    public List<PushTokenResponseDTO> getActiveTokens(Long userId) {
        log.debug("Retrieving active tokens for user {}", userId);
        
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
        
        return tokens.stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active tokens for a specific device type
     *
     * @param userId the user ID
     * @param deviceType the device type (required)
     * @return list of active tokens for the device type, sorted by createdAt descending
     * @throws InvalidDataException if userId or deviceType is null
     */
    public List<PushTokenResponseDTO> getActiveTokensByDeviceType(Long userId, String deviceType) {
        if (userId == null || deviceType == null) {
            throw new InvalidDataException("User ID and device type are required");
        }
        
        log.debug("Retrieving active tokens for user {} with device type {}", userId, deviceType);
        
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, deviceType);
        
        return tokens.stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate a push token
     *
     * @param userId the user ID (for authorization)
     * @param token the push token string
     * @throws ResourceNotFoundException if token not found
     * @throws UnauthorizedException if userId doesn't match token owner
     */
    public void deactivateToken(Long userId, String token) {
        PushToken pushToken = pushTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Push token not found"));

        // Authorization check
        if (!pushToken.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to deactivate this token");
        }

        pushToken.setIsActive(false);
        pushTokenRepository.save(pushToken);
        
        log.info("Push token deactivated for user {}", userId);
    }

    /**
     * Delete a push token permanently
     *
     * @param userId the user ID (for authorization)
     * @param token the push token string
     * @throws ResourceNotFoundException if token not found
     * @throws UnauthorizedException if userId doesn't match token owner
     */
    public void deleteToken(Long userId, String token) {
        PushToken pushToken = pushTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Push token not found"));

        // Authorization check
        if (!pushToken.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to delete this token");
        }

        pushTokenRepository.deleteByUserIdAndToken(userId, token);
        
        log.info("Push token deleted for user {}", userId);
    }

    /**
     * Delete all tokens for a user
     *
     * @param userId the user ID
     */
    public void deleteAllTokensForUser(Long userId) {
        List<PushToken> userTokens = pushTokenRepository.findByUserId(userId);
        
        for (PushToken token : userTokens) {
            pushTokenRepository.delete(token);
        }
        
        log.info("Deleted {} push tokens for user {}", userTokens.size(), userId);
    }

    /**
     * Convert PushToken entity to PushTokenResponseDTO
     *
     * @param token the push token entity
     * @return PushTokenResponseDTO
     */
    private PushTokenResponseDTO convertToDTO(PushToken token) {
        return PushTokenResponseDTO.builder()
                .id(token.getId())
                .userId(token.getUserId())
                .token(token.getToken())
                .deviceType(token.getDeviceType())
                .isActive(token.getIsActive())
                .lastUsedAt(token.getLastUsedAt())
                .createdAt(token.getCreatedAt())
                .build();
    }
}
