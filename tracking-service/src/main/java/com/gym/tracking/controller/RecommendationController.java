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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "Personalized fitness and diet recommendations")
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    
    @GetMapping("/{id}")
    @Operation(summary = "Get recommendation by ID", description = "Retrieves a specific recommendation by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Recommendation not found")
    })
    public ResponseEntity<?> getRecommendationById(@PathVariable Long id) {
        log.info("GET /api/v1/recommendations/{} - Fetch recommendation", id);
        try {
            return ResponseEntity.ok(recommendationService.getRecommendationById(id));
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder().status("NOT_FOUND").message(e.getMessage()).timestamp(LocalDateTime.now()).build());
        }
    }
    
    @GetMapping("/training-component/{trainingComponentId}")
    @Operation(summary = "Get recommendations by training component", description = "Retrieves all recommendations for a specific training component")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid training component ID")
    })
    public ResponseEntity<?> getByTrainingComponentId(@PathVariable Long trainingComponentId) {
        log.info("GET /api/v1/recommendations/training-component/{}", trainingComponentId);
        try {
            return ResponseEntity.ok(recommendationService.getRecommendationsByTrainingComponent(trainingComponentId));
        } catch (Exception e) {
            log.warn("Error fetching recommendations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder().status("BAD_REQUEST").message(e.getMessage()).timestamp(LocalDateTime.now()).build());
        }
    }
    
    @GetMapping("/diet-component/{dietComponentId}")
    @Operation(summary = "Get recommendations by diet component", description = "Retrieves all recommendations for a specific diet component")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid diet component ID")
    })
    public ResponseEntity<?> getByDietComponentId(@PathVariable Long dietComponentId) {
        log.info("GET /api/v1/recommendations/diet-component/{}", dietComponentId);
        try {
            return ResponseEntity.ok(recommendationService.getRecommendationsByDietComponent(dietComponentId));
        } catch (Exception e) {
            log.warn("Error fetching recommendations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder().status("BAD_REQUEST").message(e.getMessage()).timestamp(LocalDateTime.now()).build());
        }
    }
    
    @PostMapping
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Create a new recommendation", description = "Creates a new fitness or diet recommendation for the authenticated user (requires authentication)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recommendation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid recommendation data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<?> createRecommendation(
            @Valid @RequestBody RecommendationRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/recommendations - Create recommendation for user: {}", userId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(recommendationService.createRecommendation(userId, request));
        } catch (IllegalArgumentException e) {
            log.warn("Error creating recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder().status("BAD_REQUEST").message(e.getMessage()).timestamp(LocalDateTime.now()).build());
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a recommendation", description = "Updates an existing recommendation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid recommendation data"),
            @ApiResponse(responseCode = "404", description = "Recommendation not found")
    })
    public ResponseEntity<?> updateRecommendation(
            @PathVariable Long id,
            @Valid @RequestBody RecommendationRequestDTO request) {
        log.info("PUT /api/v1/recommendations/{} - Update recommendation", id);
        try {
            return ResponseEntity.ok(recommendationService.updateRecommendation(id, request));
        } catch (IllegalArgumentException e) {
            log.warn("Error updating recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder().status("NOT_FOUND").message(e.getMessage()).timestamp(LocalDateTime.now()).build());
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recommendation", description = "Deletes an existing recommendation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Recommendation deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Recommendation not found")
    })
    public ResponseEntity<?> deleteRecommendation(@PathVariable Long id) {
        log.info("DELETE /api/v1/recommendations/{} - Delete recommendation", id);
        try {
            recommendationService.deleteRecommendation(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting recommendation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder().status("NOT_FOUND").message(e.getMessage()).timestamp(LocalDateTime.now()).build());
        }
    }
}
