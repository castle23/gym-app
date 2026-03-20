package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.DietLogDTO;
import com.gym.tracking.dto.DietLogRequestDTO;
import com.gym.tracking.service.DietLogService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * REST Controller for Diet Log Management
 * 
 * Handles CRUD operations for user diet logs including:
 * - Recording diet entries
 * - Managing diet logs by date
 * - Filtering and retrieving diet logs
 * - User-specific authorization checks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/diet-logs")
@RequiredArgsConstructor
@Tag(name = "Diet Logs", description = "Food logging and diet tracking")
public class DietLogController {
    
    private final DietLogService dietLogService;
    
    /**
     * GET /api/v1/diet-logs/{id} - Get diet log by ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDietLogById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/diet-logs/{} - Fetch diet log for user: {}", id, userId);
        
        try {
            DietLogDTO dietLog = dietLogService.getDietLogById(id, userId);
            return ResponseEntity.ok(dietLog);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching diet log: {}", e.getMessage());
            
            if (e.getMessage().contains("not authorized")) {
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
     * GET /api/v1/diet-logs - Get all user diet logs
     * 
     * Requires: X-User-Id header
     */
    @GetMapping
    public ResponseEntity<List<DietLogDTO>> getUserDietLogs(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/diet-logs - Fetch all diet logs for user: {}", userId);
        
        List<DietLogDTO> dietLogs = dietLogService.getUserDietLogs(userId);
        return ResponseEntity.ok(dietLogs);
    }
    
    /**
     * GET /api/v1/diet-logs/date/{date} - Get diet logs by specific date
     * 
     * Requires: X-User-Id header
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getDietLogsByDate(
            @PathVariable String date,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/diet-logs/date/{} - Fetch diet logs for user: {} on date: {}", date, userId, date);
        
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            List<DietLogDTO> dietLogs = dietLogService.getDietLogsByDate(userId, parsedDate);
            return ResponseEntity.ok(dietLogs);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message("Invalid date format. Please use YYYY-MM-DD")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/diet-logs - Create a new diet log
     * 
     * Requires: X-User-Id header
     */
    @PostMapping
    public ResponseEntity<?> createDietLog(
            @Valid @RequestBody DietLogRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/diet-logs - Create diet log for user: {}", userId);
        
        try {
            DietLogDTO dietLog = dietLogService.createDietLog(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dietLog);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating diet log: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/diet-logs/{id} - Update diet log
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDietLog(
            @PathVariable Long id,
            @Valid @RequestBody DietLogRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/diet-logs/{} - Update diet log for user: {}", id, userId);
        
        try {
            DietLogDTO dietLog = dietLogService.updateDietLog(id, userId, request);
            return ResponseEntity.ok(dietLog);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating diet log: {}", e.getMessage());
            
            if (e.getMessage().contains("not authorized")) {
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
     * DELETE /api/v1/diet-logs/{id} - Delete diet log
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDietLog(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/diet-logs/{} - Delete diet log for user: {}", id, userId);
        
        try {
            dietLogService.deleteDietLog(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting diet log: {}", e.getMessage());
            
            if (e.getMessage().contains("not authorized")) {
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
