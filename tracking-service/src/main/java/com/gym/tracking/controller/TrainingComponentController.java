package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.TrainingComponentDTO;
import com.gym.tracking.dto.TrainingComponentRequestDTO;
import com.gym.tracking.service.TrainingComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Training Component Management
 * 
 * Handles CRUD operations for training components including:
 * - Getting training components by ID
 * - Getting training components by plan ID
 * - Creating training components
 * - Updating training components
 * - Deleting training components
 * - User-specific authorization checks
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TrainingComponentController {
    
    private final TrainingComponentService trainingComponentService;
    
    /**
     * GET /api/v1/training-components/{id} - Get training component by ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @GetMapping("/api/v1/training-components/{id}")
    public ResponseEntity<?> getTrainingComponentById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/training-components/{} - Fetch training component for user: {}", id, userId);
        
        try {
            TrainingComponentDTO component = trainingComponentService.getTrainingComponentById(id, userId);
            return ResponseEntity.ok(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching training component: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized") || e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ErrorResponse.builder()
                                .status("FORBIDDEN")
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build());
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * GET /api/v1/plans/{planId}/training-component - Get training component by plan ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @GetMapping("/api/v1/plans/{planId}/training-component")
    public ResponseEntity<?> getTrainingComponentByPlanId(
            @PathVariable Long planId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/plans/{}/training-component - Fetch training component for user: {}", planId, userId);
        
        try {
            TrainingComponentDTO component = trainingComponentService.getTrainingComponentByPlanId(planId, userId);
            return ResponseEntity.ok(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching training component by plan: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized") || e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ErrorResponse.builder()
                                .status("FORBIDDEN")
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build());
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/training-components - Create a new training component
     * 
     * Requires: X-User-Id header
     */
    @PostMapping("/api/v1/training-components")
    public ResponseEntity<?> createTrainingComponent(
            @Valid @RequestBody TrainingComponentRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/training-components - Create training component for user: {}", userId);
        
        try {
            TrainingComponentDTO component = trainingComponentService.createTrainingComponent(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating training component: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/training-components/{id} - Update training component
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @PutMapping("/api/v1/training-components/{id}")
    public ResponseEntity<?> updateTrainingComponent(
            @PathVariable Long id,
            @Valid @RequestBody TrainingComponentRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/training-components/{} - Update training component for user: {}", id, userId);
        
        try {
            TrainingComponentDTO component = trainingComponentService.updateTrainingComponent(id, userId, request);
            return ResponseEntity.ok(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating training component: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized") || e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ErrorResponse.builder()
                                .status("FORBIDDEN")
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build());
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * DELETE /api/v1/training-components/{id} - Delete training component
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @DeleteMapping("/api/v1/training-components/{id}")
    public ResponseEntity<?> deleteTrainingComponent(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/training-components/{} - Delete training component for user: {}", id, userId);
        
        try {
            trainingComponentService.deleteTrainingComponent(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting training component: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized") || e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ErrorResponse.builder()
                                .status("FORBIDDEN")
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build());
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
}
