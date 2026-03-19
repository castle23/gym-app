package com.gym.training.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.common.dto.PageResponse;
import com.gym.training.dto.ExerciseSessionDTO;
import com.gym.training.dto.ExerciseSessionRequestDTO;
import com.gym.training.service.ExerciseSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for Exercise Session Management
 * 
 * Handles workout logging operations including:
 * - Recording exercise sessions (sets, reps, weight, duration)
 * - Querying sessions by routine or date
 * - Updating completed workout data
 * - Deleting session records
 * 
 * All list endpoints support pagination with configurable page size and sorting.
 * All operations require X-User-Id header for authorization.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/exercise-sessions")
@RequiredArgsConstructor
public class ExerciseSessionController {
    
    private final ExerciseSessionService exerciseSessionService;
    
    /**
     * GET /api/v1/exercise-sessions/routine/{routineId} - Get sessions for a user routine with pagination
     * 
     * Requires: X-User-Id header
     * Default pagination: page=0, size=20
     * Example: GET /api/v1/exercise-sessions/routine/1?page=0&size=20&sort=sessionDate,desc
     */
    @GetMapping("/routine/{routineId}")
    public ResponseEntity<PageResponse<ExerciseSessionDTO>> getSessionsByRoutine(
            @PathVariable Long routineId,
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, page = 0, sort = "sessionDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/exercise-sessions/routine/{} - Fetch sessions for routine for user: {}", routineId, userId);
        
        PageResponse<ExerciseSessionDTO> response = exerciseSessionService.getSessionsByRoutineId(routineId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/exercise-sessions/date/{date} - Get sessions for a user on a specific date with pagination
     * 
     * Requires: X-User-Id header
     * Date format: YYYY-MM-DD (e.g., 2024-03-18)
     * Default pagination: page=0, size=20
     * Example: GET /api/v1/exercise-sessions/date/2024-03-18?page=0&size=20&sort=sessionDate,desc
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getSessionsByDate(
            @PathVariable String date,
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, page = 0, sort = "sessionDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/exercise-sessions/date/{} - Fetch sessions for user: {} on date", date, userId);
        
        try {
            LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
            PageResponse<ExerciseSessionDTO> response = exerciseSessionService.getSessionsByUserIdAndDate(userId, parsedDate, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Invalid date format: {}", date);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message("Invalid date format. Use YYYY-MM-DD format.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * GET /api/v1/exercise-sessions/{id} - Get single session by ID
     * 
     * Requires: X-User-Id header
     * Example: GET /api/v1/exercise-sessions/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSessionById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/exercise-sessions/{} - Fetch session for user: {}", id, userId);
        
        try {
            ExerciseSessionDTO session = exerciseSessionService.getSessionById(id, userId);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            log.warn("Session not found or unauthorized: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/exercise-sessions - Log a new exercise session
     * 
     * Requires: X-User-Id header
     * Request body validation: userRoutineId (required), exerciseId (required), sets (required), reps (required)
     * Optional: weight, duration (in seconds), notes, sessionDate
     * 
     * Example:
     * {
     *   "userRoutineId": 1,
     *   "exerciseId": 5,
     *   "sets": 3,
     *   "reps": 10,
     *   "weight": 50.5,
     *   "duration": 900,
     *   "notes": "Felt strong today",
     *   "sessionDate": "2024-03-18T14:30:00"
     * }
     */
    @PostMapping
    public ResponseEntity<?> createSession(
            @Valid @RequestBody ExerciseSessionRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/exercise-sessions - Create session for user: {}", userId);
        
        try {
            ExerciseSessionDTO session = exerciseSessionService.createSession(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/exercise-sessions/{id} - Update an exercise session
     * 
     * Requires: X-User-Id header (must be the session owner)
     * Partial update: can update sets, reps, weight, duration, notes
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseSessionRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/exercise-sessions/{} - Update session for user: {}", id, userId);
        
        try {
            ExerciseSessionDTO session = exerciseSessionService.updateSession(id, request, userId);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * DELETE /api/v1/exercise-sessions/{id} - Delete an exercise session
     * 
     * Requires: X-User-Id header (must be the session owner)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/exercise-sessions/{} - Delete session for user: {}", id, userId);
        
        try {
            exerciseSessionService.deleteSession(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
}
