package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.ObjectiveDTO;
import com.gym.tracking.dto.ObjectiveRequestDTO;
import com.gym.tracking.service.ObjectiveService;
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
 * REST Controller for Objective Management
 * 
 * Handles CRUD operations for user objectives including:
 * - Creating fitness objectives
 * - Retrieving user objectives
 * - Updating objective details
 * - Deleting objectives
 * - User-specific authorization checks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/objectives")
@RequiredArgsConstructor
@Tag(name = "Objectives", description = "User fitness goal and objective management")
public class ObjectiveController {
    
    private final ObjectiveService objectiveService;
    
    /**
     * GET /api/v1/objectives - Get all user objectives
     * 
     * Requires: X-User-Id header
     */
     @GetMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get all user objectives", description = "Retrieves all fitness objectives for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Objectives retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<List<ObjectiveDTO>> getUserObjectives(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/objectives - Fetch objectives for user: {}", userId);
        
        List<ObjectiveDTO> objectives = objectiveService.getUserObjectives(userId);
        return ResponseEntity.ok(objectives);
    }
    
    /**
     * GET /api/v1/objectives/{id} - Get objective by ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @GetMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get objective by ID", description = "Retrieves a specific objective by ID (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Objective retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the objective owner"),
             @ApiResponse(responseCode = "404", description = "Objective not found")
     })
     public ResponseEntity<?> getObjectiveById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/objectives/{} - Fetch objective for user: {}", id, userId);
        
        try {
            ObjectiveDTO objective = objectiveService.getObjectiveById(id, userId);
            return ResponseEntity.ok(objective);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching objective: {}", e.getMessage());
            
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
     * POST /api/v1/objectives - Create a new objective
     * 
     * Requires: X-User-Id header
     */
     @PostMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Create a new objective", description = "Creates a new fitness objective for the authenticated user (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Objective created successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid objective data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<?> createObjective(
            @Valid @RequestBody ObjectiveRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/objectives - Create objective for user: {}", userId);
        
        try {
            ObjectiveDTO objective = objectiveService.createObjective(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(objective);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating objective: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/objectives/{id} - Update objective
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @PutMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Update an objective", description = "Updates an existing objective (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Objective updated successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid objective data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the objective owner"),
             @ApiResponse(responseCode = "404", description = "Objective not found")
     })
     public ResponseEntity<?> updateObjective(
            @PathVariable Long id,
            @Valid @RequestBody ObjectiveRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/objectives/{} - Update objective for user: {}", id, userId);
        
        try {
            ObjectiveDTO objective = objectiveService.updateObjective(id, userId, request);
            return ResponseEntity.ok(objective);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating objective: {}", e.getMessage());
            
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
     * DELETE /api/v1/objectives/{id} - Delete objective
     * 
     * Requires: X-User-Id header (must be the owner)
     */
     @DeleteMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Delete an objective", description = "Deletes an existing objective (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Objective deleted successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the objective owner"),
             @ApiResponse(responseCode = "404", description = "Objective not found")
     })
     public ResponseEntity<?> deleteObjective(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/objectives/{} - Delete objective for user: {}", id, userId);
        
        try {
            objectiveService.deleteObjective(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting objective: {}", e.getMessage());
            
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
