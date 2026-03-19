package com.gym.tracking.service;

import com.gym.tracking.dto.DietLogDTO;
import com.gym.tracking.dto.DietLogRequestDTO;
import com.gym.tracking.entity.DietLog;
import com.gym.tracking.repository.DietLogRepository;
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
public class DietLogService {
    
    private final DietLogRepository dietLogRepository;
    
    /**
     * Get diet log by ID
     */
    public DietLogDTO getDietLogById(Long id, Long userId) {
        log.info("Fetching diet log: {} for user: {}", id, userId);
        DietLog log = dietLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet log not found: " + id));
        
        if (!log.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to access this diet log");
        }
        
        return toDTO(log);
    }
    
    /**
     * Get user diet logs
     */
    public List<DietLogDTO> getUserDietLogs(Long userId) {
        log.info("Fetching all diet logs for user: {}", userId);
        return dietLogRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get diet logs for specific date
     */
    public List<DietLogDTO> getDietLogsByDate(Long userId, LocalDate date) {
        log.info("Fetching diet logs for user: {} on date: {}", userId, date);
        return dietLogRepository.findByUserIdAndLogDate(userId, date)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get diet logs by date range
     */
    public List<DietLogDTO> getDietLogsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching diet logs for user: {} from {} to {}", userId, startDate, endDate);
        return dietLogRepository.findByUserIdAndLogDateBetween(userId, startDate, endDate)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Create diet log
     */
    @Transactional
    public DietLogDTO createDietLog(Long userId, DietLogRequestDTO request) {
        log.info("Creating diet log for user: {}", userId);
        
        DietLog log = DietLog.builder()
                .userId(userId)
                .logDate(request.getLogDate())
                .meal(request.getMeal())
                .foodItems(request.getFoodItems())
                .calories(request.getCalories())
                .macros(request.getMacros())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
        
        DietLog saved = dietLogRepository.save(log);
        log.info("Diet log created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update diet log
     */
    @Transactional
    public DietLogDTO updateDietLog(Long id, Long userId, DietLogRequestDTO request) {
        log.info("Updating diet log: {} for user: {}", id, userId);
        
        DietLog log = dietLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet log not found: " + id));
        
        if (!log.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to update this diet log");
        }
        
        log.setLogDate(request.getLogDate());
        log.setMeal(request.getMeal());
        log.setFoodItems(request.getFoodItems());
        log.setCalories(request.getCalories());
        log.setMacros(request.getMacros());
        log.setNotes(request.getNotes());
        
        DietLog updated = dietLogRepository.save(log);
        log.info("Diet log updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete diet log
     */
    @Transactional
    public void deleteDietLog(Long id, Long userId) {
        log.info("Deleting diet log: {} for user: {}", id, userId);
        
        DietLog log = dietLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet log not found: " + id));
        
        if (!log.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to delete this diet log");
        }
        
        dietLogRepository.delete(log);
        log.info("Diet log deleted: {}", id);
    }
    
    /**
     * Convert DietLog entity to DTO
     */
    private DietLogDTO toDTO(DietLog log) {
        return DietLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .logDate(log.getLogDate())
                .meal(log.getMeal())
                .foodItems(log.getFoodItems())
                .calories(log.getCalories())
                .macros(log.getMacros())
                .notes(log.getNotes())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
