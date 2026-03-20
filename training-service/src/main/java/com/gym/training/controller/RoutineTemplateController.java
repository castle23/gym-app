package com.gym.training.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.common.dto.PageResponse;
import com.gym.training.dto.RoutineTemplateDTO;
import com.gym.training.dto.RoutineTemplateRequestDTO;
import com.gym.training.service.RoutineTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Routine Template Management
 * 
 * Handles CRUD operations for routine templates including:
 * - System templates (pre-designed, read-only)
 * - User custom templates (user-specific)
 * 
 * All list endpoints support pagination with configurable page size and sorting.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/routine-templates")
@RequiredArgsConstructor
@Tag(name = "Routine Templates", description = "Routine template CRUD operations and template management")
public class RoutineTemplateController {
    
    private final RoutineTemplateService routineTemplateService;
    
    /**
     * GET /api/v1/routine-templates/system - Get all system routine templates with pagination
     * 
     * Default pagination: page=0, size=20
     * Example: GET /api/v1/routine-templates/system?page=0&size=20&sort=name,asc
     */
     @GetMapping("/system")
     @Operation(summary = "List all system routine templates", description = "Retrieves all pre-defined system routine templates with pagination support")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "System templates retrieved successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
     })
     public ResponseEntity<PageResponse<RoutineTemplateDTO>> getAllSystemTemplates(
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/routine-templates/system - Fetch system templates: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        PageResponse<RoutineTemplateDTO> response = routineTemplateService.getAllSystemTemplates(pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/routine-templates/my-templates - Get user's custom routine templates with pagination
     * 
     * Requires: X-User-Id header
     * Example: GET /api/v1/routine-templates/my-templates?page=0&size=20
     */
     @GetMapping("/my-templates")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Get user's custom routine templates", description = "Retrieves routine templates created by the authenticated user with pagination (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "User templates retrieved successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<PageResponse<RoutineTemplateDTO>> getUserTemplates(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/routine-templates/my-templates - Fetch user templates for user: {}", userId);
        
        PageResponse<RoutineTemplateDTO> response = routineTemplateService.getUserTemplates(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/routine-templates/{id} - Get single routine template by ID
     * 
     * Example: GET /api/v1/routine-templates/1
     */
     @GetMapping("/{id}")
     @Operation(summary = "Get routine template by ID", description = "Retrieves a single routine template by its unique ID")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Template retrieved successfully"),
             @ApiResponse(responseCode = "404", description = "Template not found")
     })
     public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        log.info("GET /api/v1/routine-templates/{} - Fetch template by ID", id);
        
        try {
            RoutineTemplateDTO template = routineTemplateService.getTemplateById(id);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            log.warn("Routine template not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/routine-templates - Create a new custom routine template
     * 
     * Requires: X-User-Id header
     * Request body validation: name (required), description (optional), type (required)
     * 
     * Example:
     * {
     *   "name": "My Custom Routine",
     *   "description": "5-day strength focused routine",
     *   "type": "USER",
     *   "exerciseIds": [1, 2, 3, 4, 5]
     * }
     */
     @PostMapping
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Create a new custom routine template", description = "Creates a new user-specific routine template (requires authentication)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Template created successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid template data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
     })
     public ResponseEntity<?> createTemplate(
            @Valid @RequestBody RoutineTemplateRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/routine-templates - Create routine template for user: {}", userId);
        
        try {
            RoutineTemplateDTO template = routineTemplateService.createTemplate(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(template);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating routine template: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/routine-templates/{id} - Update a routine template
     * 
     * Requires: X-User-Id header (must be the creator)
     * Only the creator of a template can update it
     */
     @PutMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Update a routine template", description = "Updates an existing routine template (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Template updated successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid template data"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the template owner"),
             @ApiResponse(responseCode = "404", description = "Template not found")
     })
     public ResponseEntity<?> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody RoutineTemplateRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/routine-templates/{} - Update routine template for user: {}", id, userId);
        
        try {
            RoutineTemplateDTO template = routineTemplateService.updateTemplate(id, request, userId);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating routine template: {}", e.getMessage());
            
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
     * DELETE /api/v1/routine-templates/{id} - Delete a routine template
     * 
     * Requires: X-User-Id header (must be the creator)
     * Only the creator of a template can delete it
     */
     @DeleteMapping("/{id}")
     @SecurityRequirement(name = "bearer-jwt")
     @Operation(summary = "Delete a routine template", description = "Deletes an existing routine template (requires authentication and owner permissions)")
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Template deleted successfully"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
             @ApiResponse(responseCode = "403", description = "Forbidden - not the template owner"),
             @ApiResponse(responseCode = "404", description = "Template not found")
     })
     public ResponseEntity<?> deleteTemplate(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/routine-templates/{} - Delete routine template for user: {}", id, userId);
        
        try {
            routineTemplateService.deleteTemplate(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting routine template: {}", e.getMessage());
            
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
