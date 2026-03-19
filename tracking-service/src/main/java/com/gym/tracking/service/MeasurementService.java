package com.gym.tracking.service;

import com.gym.tracking.dto.MeasurementTypeDTO;
import com.gym.tracking.dto.MeasurementTypeRequestDTO;
import com.gym.tracking.dto.MeasurementValueDTO;
import com.gym.tracking.dto.MeasurementValueRequestDTO;
import com.gym.tracking.entity.MeasurementType;
import com.gym.tracking.entity.MeasurementValue;
import com.gym.tracking.repository.MeasurementTypeRepository;
import com.gym.tracking.repository.MeasurementValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeasurementService {
    
    private final MeasurementTypeRepository measurementTypeRepository;
    private final MeasurementValueRepository measurementValueRepository;
    
    /**
     * Get all measurement types
     */
    public List<MeasurementTypeDTO> getAllMeasurementTypes() {
        log.info("Fetching all measurement types");
        return measurementTypeRepository.findAll()
                .stream()
                .map(this::toMeasurementTypeDTO)
                .toList();
    }
    
    /**
     * Get measurement type by ID
     */
    public MeasurementTypeDTO getMeasurementTypeById(Long id) {
        log.info("Fetching measurement type: {}", id);
        MeasurementType type = measurementTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Measurement type not found: " + id));
        return toMeasurementTypeDTO(type);
    }
    
    /**
     * Create new measurement type
     */
    @Transactional
    public MeasurementTypeDTO createMeasurementType(MeasurementTypeRequestDTO request) {
        log.info("Creating measurement type: {}", request.getType());
        
        MeasurementType type = MeasurementType.builder()
                .type(request.getType())
                .unit(request.getUnit())
                .isSystem(request.getIsSystem() != null ? request.getIsSystem() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        MeasurementType saved = measurementTypeRepository.save(type);
        log.info("Measurement type created with ID: {}", saved.getId());
        return toMeasurementTypeDTO(saved);
    }
    
    /**
     * Get user measurement values
     */
    public List<MeasurementValueDTO> getUserMeasurements(Long userId) {
        log.info("Fetching measurements for user: {}", userId);
        return measurementValueRepository.findByUserId(userId)
                .stream()
                .map(this::toMeasurementValueDTO)
                .toList();
    }
    
    /**
     * Get measurement values by type for user
     */
    public List<MeasurementValueDTO> getUserMeasurementsByType(Long userId, Long measurementTypeId) {
        log.info("Fetching measurements for user: {} and type: {}", userId, measurementTypeId);
        verifyMeasurementTypeExists(measurementTypeId);
        return measurementValueRepository.findByUserIdAndMeasurementTypeId(userId, measurementTypeId)
                .stream()
                .map(this::toMeasurementValueDTO)
                .toList();
    }
    
    /**
     * Get measurement values by date range
     */
    public List<MeasurementValueDTO> getUserMeasurementsByDateRange(
            Long userId, Long measurementTypeId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching measurements for user: {} from {} to {}", userId, startDate, endDate);
        verifyMeasurementTypeExists(measurementTypeId);
        return measurementValueRepository.findByUserIdAndMeasurementTypeIdAndMeasurementDateBetween(
                userId, measurementTypeId, startDate, endDate)
                .stream()
                .map(this::toMeasurementValueDTO)
                .toList();
    }
    
    /**
     * Record user measurement
     */
    @Transactional
    public MeasurementValueDTO recordMeasurement(Long userId, MeasurementValueRequestDTO request) {
        log.info("Recording measurement for user: {}", userId);
        
        verifyMeasurementTypeExists(request.getMeasurementTypeId());
        MeasurementType type = measurementTypeRepository.findById(request.getMeasurementTypeId()).get();
        
        MeasurementValue value = MeasurementValue.builder()
                .userId(userId)
                .measurementType(type)
                .value(request.getValue())
                .measurementDate(request.getMeasurementDate())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
        
        MeasurementValue saved = measurementValueRepository.save(value);
        log.info("Measurement recorded with ID: {}", saved.getId());
        return toMeasurementValueDTO(saved);
    }
    
    /**
     * Delete measurement value
     */
    @Transactional
    public void deleteMeasurement(Long userId, Long measurementId) {
        log.info("Deleting measurement: {} for user: {}", measurementId, userId);
        
        MeasurementValue value = measurementValueRepository.findById(measurementId)
                .orElseThrow(() -> new IllegalArgumentException("Measurement not found: " + measurementId));
        
        if (!value.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to delete this measurement");
        }
        
        measurementValueRepository.delete(value);
        log.info("Measurement deleted: {}", measurementId);
    }
    
    /**
     * Verify measurement type exists
     */
    private void verifyMeasurementTypeExists(Long measurementTypeId) {
        if (!measurementTypeRepository.existsById(measurementTypeId)) {
            throw new IllegalArgumentException("Measurement type not found: " + measurementTypeId);
        }
    }
    
    /**
     * Convert MeasurementType entity to DTO
     */
    private MeasurementTypeDTO toMeasurementTypeDTO(MeasurementType type) {
        return MeasurementTypeDTO.builder()
                .id(type.getId())
                .type(type.getType())
                .unit(type.getUnit())
                .isSystem(type.getIsSystem())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert MeasurementValue entity to DTO
     */
    private MeasurementValueDTO toMeasurementValueDTO(MeasurementValue value) {
        return MeasurementValueDTO.builder()
                .id(value.getId())
                .userId(value.getUserId())
                .measurementTypeId(value.getMeasurementType().getId())
                .measurementType(value.getMeasurementType().getType())
                .value(value.getValue())
                .measurementDate(value.getMeasurementDate())
                .notes(value.getNotes())
                .createdAt(value.getCreatedAt())
                .build();
    }
}
