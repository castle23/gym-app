package com.gym.training.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.common.dto.PageResponse;
import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.service.ExerciseService;
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
 * REST Controller for Exercise Management
 * 
 * Handles CRUD operations for exercises including:
 * - System exercises (pre-defined, read-only)
 * - User custom exercises (user-specific)
 * - Discipline-based filtering
 * 
 * All list endpoints support pagination with configurable page size and sorting.
 * Single resource endpoints (by ID) do not use pagination.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    
    private final ExerciseService exerciseService;
    
    /**
     * GET /api/v1/exercises/system - Get all system exercises with pagination
     * 
     * Default pagination: page=0, size=20
     * Example: GET /api/v1/exercises/system?page=0&size=20&sort=name,asc
     */
    @GetMapping("/system")
    public ResponseEntity<PageResponse<ExerciseDTO>> getAllSystemExercises(
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/exercises/system - Fetch system exercises: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        PageResponse<ExerciseDTO> response = exerciseService.getAllSystemExercises(pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/exercises/discipline/{disciplineId} - Get exercises by discipline with pagination
     * 
     * Example: GET /api/v1/exercises/discipline/1?page=0&size=20
     */
    @GetMapping("/discipline/{disciplineId}")
    public ResponseEntity<?> getExercisesByDiscipline(
            @PathVariable Long disciplineId,
            @PageableDefault(size = 20, page = 0, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        log.info("GET /api/v1/exercises/discipline/{} - Fetch exercises by discipline", disciplineId);
        
        try {
            PageResponse<ExerciseDTO> response = exerciseService.getExercisesByDiscipline(disciplineId, pageable);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Discipline not found: {}", disciplineId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * GET /api/v1/exercises/my-exercises - Get user's custom exercises with pagination
     * 
     * Requires: X-User-Id header
     * Example: GET /api/v1/exercises/my-exercises?page=0&size=20
     */
    @GetMapping("/my-exercises")
    public ResponseEntity<PageResponse<ExerciseDTO>> getUserExercises(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/exercises/my-exercises - Fetch user exercises for user: {}", userId);
        
        PageResponse<ExerciseDTO> response = exerciseService.getUserExercises(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/exercises/{id} - Get single exercise by ID
     * 
     * Example: GET /api/v1/exercises/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseById(@PathVariable Long id) {
        log.info("GET /api/v1/exercises/{} - Fetch exercise by ID", id);
        
        try {
            ExerciseDTO exercise = exerciseService.getExerciseById(id);
            return ResponseEntity.ok(exercise);
        } catch (IllegalArgumentException e) {
            log.warn("Exercise not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/exercises - Create a new custom exercise
     * 
     * Requires: X-User-Id header
     * Request body validation: name (required), description (required), disciplineId (required), type (required)
     * 
     * Example:
     * {
     *   "name": "My Custom Exercise",
     *   "description": "Exercise description",
     *   "type": "USER",
     *   "disciplineId": 1
     * }
     */
    @PostMapping
    public ResponseEntity<?> createExercise(
            @Valid @RequestBody ExerciseRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/exercises - Create exercise for user: {}", userId);
        
        try {
            ExerciseDTO exercise = exerciseService.createExercise(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(exercise);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating exercise: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/exercises/{id} - Update an exercise
     * 
     * Requires: X-User-Id header (must be the creator)
     * Only the creator of an exercise can update it
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExercise(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/exercises/{} - Update exercise for user: {}", id, userId);
        
        try {
            ExerciseDTO exercise = exerciseService.updateExercise(id, request, userId);
            return ResponseEntity.ok(exercise);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating exercise: {}", e.getMessage());
            
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
     * DELETE /api/v1/exercises/{id} - Delete an exercise
     * 
     * Requires: X-User-Id header (must be the creator)
     * Only the creator of an exercise can delete it
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExercise(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/exercises/{} - Delete exercise for user: {}", id, userId);
        
        try {
            exerciseService.deleteExercise(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting exercise: {}", e.getMessage());
            
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
