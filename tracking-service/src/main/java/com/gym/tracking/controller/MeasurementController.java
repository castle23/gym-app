package com.gym.tracking.controller;

import com.gym.common.dto.ErrorResponse;
import com.gym.tracking.dto.MeasurementValueDTO;
import com.gym.tracking.dto.MeasurementValueRequestDTO;
import com.gym.tracking.dto.MeasurementTypeDTO;
import com.gym.tracking.dto.MeasurementTypeRequestDTO;
import com.gym.tracking.service.MeasurementService;
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
 * REST Controller for Measurement Management
 * 
 * Handles CRUD operations for user measurements including:
 * - Recording measurement values
 * - Managing measurement types
 * - Filtering by date range and measurement type
 * - User-specific authorization checks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
@Tag(name = "Measurements", description = "Body measurement tracking and history")
public class MeasurementController {
    
    private final MeasurementService measurementService;
    
    /**
     * GET /api/v1/measurements - Get all user measurements
     * 
     * Requires: X-User-Id header
     */
    @GetMapping
    public ResponseEntity<List<MeasurementValueDTO>> getUserMeasurements(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/measurements - Fetch measurements for user: {}", userId);
        
        List<MeasurementValueDTO> measurements = measurementService.getUserMeasurements(userId);
        return ResponseEntity.ok(measurements);
    }
    
    /**
     * GET /api/v1/measurements/{id} - Get measurement by ID
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMeasurementById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/measurements/{} - Fetch measurement for user: {}", id, userId);
        
        try {
            MeasurementValueDTO measurement = measurementService.getMeasurementValueById(id, userId);
            return ResponseEntity.ok(measurement);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching measurement: {}", e.getMessage());
            
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
     * GET /api/v1/measurement-types/{typeId} - Get measurement type by ID
     */
    @GetMapping("/types/{typeId}")
    public ResponseEntity<?> getMeasurementType(@PathVariable Long typeId) {
        log.info("GET /api/v1/measurement-types/{} - Fetch measurement type", typeId);
        
        try {
            MeasurementTypeDTO type = measurementService.getMeasurementTypeById(typeId);
            return ResponseEntity.ok(type);
        } catch (IllegalArgumentException e) {
            log.warn("Measurement type not found: {}", typeId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .status("NOT_FOUND")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/measurement-types - Create a new measurement type
     */
    @PostMapping("/types")
    public ResponseEntity<?> createMeasurementType(
            @Valid @RequestBody MeasurementTypeRequestDTO request) {
        log.info("POST /api/v1/measurement-types - Create measurement type");
        
        try {
            MeasurementTypeDTO type = measurementService.createMeasurementType(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(type);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating measurement type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * GET /api/v1/measurements/type/{typeId} - Get measurements by type for user
     * 
     * Requires: X-User-Id header
     */
    @GetMapping("/by-type/{typeId}")
    public ResponseEntity<?> getMeasurementsByType(
            @PathVariable Long typeId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/v1/measurements/by-type/{} - Fetch measurements by type for user: {}", typeId, userId);
        
        try {
            List<MeasurementValueDTO> measurements = measurementService.getUserMeasurementsByType(userId, typeId);
            return ResponseEntity.ok(measurements);
        } catch (IllegalArgumentException e) {
            log.warn("Error fetching measurements by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * POST /api/v1/measurements - Record a new measurement
     * 
     * Requires: X-User-Id header
     */
    @PostMapping
    public ResponseEntity<?> recordMeasurement(
            @Valid @RequestBody MeasurementValueRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("POST /api/v1/measurements - Record measurement for user: {}", userId);
        
        try {
            MeasurementValueDTO measurement = measurementService.recordMeasurement(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(measurement);
        } catch (IllegalArgumentException e) {
            log.warn("Error recording measurement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .status("BAD_REQUEST")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }
    
    /**
     * PUT /api/v1/measurements/{id} - Update measurement
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeasurement(
            @PathVariable Long id,
            @Valid @RequestBody MeasurementValueRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("PUT /api/v1/measurements/{} - Update measurement for user: {}", id, userId);
        
        try {
            MeasurementValueDTO measurement = measurementService.updateMeasurement(id, userId, request);
            return ResponseEntity.ok(measurement);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating measurement: {}", e.getMessage());
            
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
     * DELETE /api/v1/measurements/{id} - Delete measurement
     * 
     * Requires: X-User-Id header (must be the owner)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeasurement(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("DELETE /api/v1/measurements/{} - Delete measurement for user: {}", id, userId);
        
        try {
            measurementService.deleteMeasurement(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting measurement: {}", e.getMessage());
            
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
