package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.DietComponentDTO;
import com.gym.tracking.dto.DietComponentRequestDTO;
import com.gym.tracking.service.DietComponentService;
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

/**
 * REST Controller for Diet Component Management
 * 
 * Handles CRUD operations for diet components including:
 * - Getting diet components by ID
 * - Getting diet components by plan ID
 * - Creating diet components
 * - Updating diet components
 * - Deleting diet components
 * - User-specific authorization checks
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Diet Components", description = "Diet component management and nutrient tracking")
public class DietComponentController {
    
    private final DietComponentService dietComponentService;
    
    /**
     * GET /api/v1/diet-components/{id} - Get diet component by ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @GetMapping("/api/v1/diet-components/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get diet component by ID", description = "Retrieves a diet component by its ID (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Diet component retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the component owner"),
             @ApiResponse(responseCode = "404", description = "Diet component not found")
     })
     public ResponseEntity<?> getDietComponentById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/diet-components/{} - Fetch diet component for user: {}", id, userId);
        
        try {
            DietComponentDTO component = dietComponentService.getDietComponentById(id, userId);
            return ResponseEntity.ok(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching diet component: {}", e.getMessage());
            
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
     * GET /api/v1/plans/{planId}/diet-component - Get diet component by plan ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @GetMapping("/api/v1/plans/{planId}/diet-component")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get diet component by plan ID", description = "Retrieves a diet component associated with a plan (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Diet component retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the plan owner"),
             @ApiResponse(responseCode = "404", description = "Diet component or plan not found")
     })
     public ResponseEntity<?> getDietComponentByPlanId(
            @PathVariable Long planId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/plans/{}/diet-component - Fetch diet component for user: {}", planId, userId);
        
        try {
            DietComponentDTO component = dietComponentService.getDietComponentByPlanId(planId, userId);
            return ResponseEntity.ok(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching diet component by plan: {}", e.getMessage());
            
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
     * POST /api/v1/diet-components - Create a new diet component
     * 
     * Requires: X-User-Id header
     */
     @PostMapping("/api/v1/diet-components")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Create a new diet component", description = "Creates a new diet component for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Diet component created successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid diet component data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<?> createDietComponent(
            @Valid @RequestBody DietComponentRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/diet-components - Create diet component for user: {}", userId);
        
        try {
            DietComponentDTO component = dietComponentService.createDietComponent(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating diet component: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/diet-components/{id} - Update diet component
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @PutMapping("/api/v1/diet-components/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Update a diet component", description = "Updates an existing diet component (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Diet component updated successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid diet component data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the component owner"),
             @ApiResponse(responseCode = "404", description = "Diet component not found")
     })
     public ResponseEntity<?> updateDietComponent(
            @PathVariable Long id,
            @Valid @RequestBody DietComponentRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/diet-components/{} - Update diet component for user: {}", id, userId);
        
        try {
            DietComponentDTO component = dietComponentService.updateDietComponent(id, userId, request);
            return ResponseEntity.ok(component);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating diet component: {}", e.getMessage());
            
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
     * DELETE /api/v1/diet-components/{id} - Delete diet component
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @DeleteMapping("/api/v1/diet-components/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Delete a diet component", description = "Deletes an existing diet component (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Diet component deleted successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the component owner"),
             @ApiResponse(responseCode = "404", description = "Diet component not found")
     })
     public ResponseEntity<?> deleteDietComponent(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/diet-components/{} - Delete diet component for user: {}", id, userId);
        
        try {
            dietComponentService.deleteDietComponent(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting diet component: {}", e.getMessage());
            
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
