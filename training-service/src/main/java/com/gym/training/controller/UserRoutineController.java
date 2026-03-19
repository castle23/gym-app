package com.gym.training.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.common.dto.PageResponse;
import com.gym.training.dto.UserRoutineDTO;
import com.gym.training.dto.UserRoutineRequestDTO;
import com.gym.training.service.UserRoutineService;
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
 * REST Controller for User Routine Management
 * 
 * Handles operations for user-assigned routines including:
 * - Viewing active and all routines for a user
 * - Assigning routine templates to users
 * - Updating routine assignments
 * - Deactivating routines
 * - Deleting routines
 * 
 * All list endpoints support pagination with configurable page size and sorting.
 * All operations require X-User-Id header for authorization.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user-routines")
@RequiredArgsConstructor
public class UserRoutineController {
    
    private final UserRoutineService userRoutineService;
    
    /**
     * GET /api/v1/user-routines/active - Get user's active routines with pagination
     * 
     * Requires: X-User-Id header
     * Default pagination: page=0, size=20
     * Example: GET /api/v1/user-routines/active?page=0&size=20&sort=startDate,desc
     */
    @GetMapping("/active")
    public ResponseEntity<PageResponse<UserRoutineDTO>> getActiveRoutines(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, page = 0, sort = "startDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/user-routines/active - Fetch active routines for user: {}", userId);
        
        PageResponse<UserRoutineDTO> response = userRoutineService.getUserActiveRoutines(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/user-routines - Get all user routines (active and inactive) with pagination
     * 
     * Requires: X-User-Id header
     * Default pagination: page=0, size=20
     * Example: GET /api/v1/user-routines?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<PageResponse<UserRoutineDTO>> getAllRoutines(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/user-routines - Fetch all routines for user: {}", userId);
        
        PageResponse<UserRoutineDTO> response = userRoutineService.getUserRoutines(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/user-routines/{id} - Get single routine by ID
     * 
     * Requires: X-User-Id header
     * Example: GET /api/v1/user-routines/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoutineById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/user-routines/{} - Fetch routine for user: {}", id, userId);
        
        try {
            UserRoutineDTO routine = userRoutineService.getRoutineById(id, userId);
            return ResponseEntity.ok(routine);
        } catch (IllegalArgumentException e) {
            log.warn("Routine not found or unauthorized: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/user-routines/assign - Assign a routine template to a user
     * 
     * Requires: X-User-Id header
     * Request body validation: routineTemplateId (required), isActive (optional, defaults to true)
     * 
     * Example:
     * {
     *   "routineTemplateId": 1,
     *   "isActive": true
     * }
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignRoutine(
            @Valid @RequestBody UserRoutineRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/user-routines/assign - Assign routine for user: {}", userId);
        
        try {
            UserRoutineDTO routine = userRoutineService.assignRoutine(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(routine);
        } catch (IllegalArgumentException e) {
            log.warn("Error assigning routine: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/user-routines/{id} - Update a routine assignment
     * 
     * Requires: X-User-Id header (must be the routine owner)
     * Update isActive status or endDate
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoutine(
            @PathVariable Long id,
            @Valid @RequestBody UserRoutineRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/user-routines/{} - Update routine for user: {}", id, userId);
        
        try {
            UserRoutineDTO routine = userRoutineService.updateRoutine(id, request, userId);
            return ResponseEntity.ok(routine);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating routine: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PATCH /api/v1/user-routines/{id}/deactivate - Deactivate a routine
     * 
     * Requires: X-User-Id header (must be the routine owner)
     * Sets isActive to false and endDate to current time
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateRoutine(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PATCH /api/v1/user-routines/{}/deactivate - Deactivate routine for user: {}", id, userId);
        
        try {
            UserRoutineDTO routine = userRoutineService.deactivateRoutine(id, userId);
            return ResponseEntity.ok(routine);
        } catch (IllegalArgumentException e) {
            log.warn("Error deactivating routine: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * DELETE /api/v1/user-routines/{id} - Delete a routine
     * 
     * Requires: X-User-Id header (must be the routine owner)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoutine(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/user-routines/{} - Delete routine for user: {}", id, userId);
        
        try {
            userRoutineService.deleteRoutine(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting routine: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
}
