package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.RecommendationDTO;
import com.gym.tracking.dto.RecommendationRequestDTO;
import com.gym.tracking.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for Recommendation Management
 * 
 * Handles CRUD operations for recommendations including:
 * - Getting recommendations by ID
 * - Getting recommendations by training component
 * - Getting recommendations by diet component
 * - Creating recommendations
 * - Updating recommendations
 * - Deleting recommendations
 * 
 * Note: Recommendations are system-wide (no user-specific authorization)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    
    /**
     * GET /api/v1/recommendations/{id} - Get recommendation by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecommendationById(@PathVariable Long id) {
        log.info("GET /api/v1/recommendations/{} - Fetch recommendation", id);
        
        try {
            RecommendationDTO recommendation = recommendationService.getRecommendationById(id);
            return ResponseEntity.ok(recommendation);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * GET /api/v1/training-components/{trainingComponentId}/recommendations - Get by training component
     */
    @GetMapping("/training-component/{trainingComponentId}")
    public ResponseEntity<?> getByTrainingComponentId(@PathVariable Long trainingComponentId) {
        log.info("GET /api/v1/training-components/{}/recommendations - Fetch recommendations", trainingComponentId);
        
        try {
            List<RecommendationDTO> recommendations = recommendationService.getRecommendationsByTrainingComponent(trainingComponentId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.warn("Error fetching recommendations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * GET /api/v1/diet-components/{dietComponentId}/recommendations - Get by diet component
     */
    @GetMapping("/diet-component/{dietComponentId}")
    public ResponseEntity<?> getByDietComponentId(@PathVariable Long dietComponentId) {
        log.info("GET /api/v1/diet-components/{}/recommendations - Fetch recommendations", dietComponentId);
        
        try {
            List<RecommendationDTO> recommendations = recommendationService.getRecommendationsByDietComponent(dietComponentId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.warn("Error fetching recommendations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/recommendations - Create a new recommendation
     */
    @PostMapping
    public ResponseEntity<?> createRecommendation(@Valid @RequestBody RecommendationRequestDTO request) {
        log.info("POST /api/v1/recommendations - Create recommendation");
        
        try {
            RecommendationDTO recommendation = recommendationService.createRecommendation(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(recommendation);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/recommendations/{id} - Update recommendation
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecommendation(
            @PathVariable Long id,
            @Valid @RequestBody RecommendationRequestDTO request) {
        log.info("PUT /api/v1/recommendations/{} - Update recommendation", id);
        
        try {
            RecommendationDTO recommendation = recommendationService.updateRecommendation(id, request);
            return ResponseEntity.ok(recommendation);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * DELETE /api/v1/recommendations/{id} - Delete recommendation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecommendation(@PathVariable Long id) {
        log.info("DELETE /api/v1/recommendations/{} - Delete recommendation", id);
        
        try {
            recommendationService.deleteRecommendation(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
}
