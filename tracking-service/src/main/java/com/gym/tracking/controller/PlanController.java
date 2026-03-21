package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.PlanDTO;
import com.gym.tracking.dto.PlanRequestDTO;
import com.gym.tracking.service.PlanService;
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

/**
 * REST Controller for Plan Management
 * 
 * Handles CRUD operations for user plans including:
 * - Creating and managing fitness plans
 * - User-specific authorization checks
 * - Plan status filtering
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Diet and fitness plan management")
public class PlanController {
    
    private final PlanService planService;
    
    /**
     * GET /api/v1/plans - Get all user plans
     * 
     * Requires: X-User-Id header
     */
     @GetMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get all user plans", description = "Retrieves all fitness and diet plans for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Plans retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<List<PlanDTO>> getUserPlans(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/plans - Fetch plans for user: {}", userId);
        
        List<PlanDTO> plans = planService.getUserPlans(userId);
        return ResponseEntity.ok(plans);
    }
    
     /**
      * GET /api/v1/plans/{id} - Get plan by ID
      * 
      * Requires: X-User-Id header (must be the owner)
      */
     @GetMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get plan by ID", description = "Retrieves a specific plan by ID (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Plan retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the plan owner"),
             @ApiResponse(responseCode = "404", description = "Plan not found")
     })
     public ResponseEntity<?> getPlanById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/plans/{} - Fetch plan for user: {}", id, userId);
        
        try {
            PlanDTO plan = planService.getPlanById(id, userId);
            return ResponseEntity.ok(plan);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching plan: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized")) {
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
      * POST /api/v1/plans - Create a new plan
      * 
      * Requires: X-User-Id header
      */
     @PostMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Create a new plan", description = "Creates a new fitness or diet plan for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Plan created successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid plan data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<?> createPlan(
            @Valid @RequestBody PlanRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/plans - Create plan for user: {}", userId);
        
        try {
            PlanDTO plan = planService.createPlan(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(plan);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
     /**
      * PUT /api/v1/plans/{id} - Update plan
      * 
      * Requires: X-User-Id header (must be the owner)
      */
     @PutMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Update a plan", description = "Updates an existing plan (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Plan updated successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid plan data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the plan owner"),
             @ApiResponse(responseCode = "404", description = "Plan not found")
     })
     public ResponseEntity<?> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/plans/{} - Update plan for user: {}", id, userId);
        
        try {
            PlanDTO plan = planService.updatePlan(id, userId, request);
            return ResponseEntity.ok(plan);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating plan: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized")) {
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
      * DELETE /api/v1/plans/{id} - Delete plan
      * 
      * Requires: X-User-Id header (must be the owner)
      */
     @DeleteMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Delete a plan", description = "Deletes an existing plan (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Plan deleted successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the plan owner"),
             @ApiResponse(responseCode = "404", description = "Plan not found")
     })
     public ResponseEntity<?> deletePlan(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/plans/{} - Delete plan for user: {}", id, userId);
        
        try {
            planService.deletePlan(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting plan: {}", e.getMessage());
            
            if (e.getMessage().contains("unauthorized")) {
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
