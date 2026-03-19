package com.gym.training.service;

import com.gym.training.dto.UserRoutineDTO;
import com.gym.training.dto.UserRoutineRequestDTO;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.UserRoutine;
import com.gym.training.repository.RoutineTemplateRepository;
import com.gym.training.repository.UserRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRoutineService {
    
    private final UserRoutineRepository userRoutineRepository;
    private final RoutineTemplateRepository routineTemplateRepository;
    
    /**
     * Get all active routines for a user
     */
    public List<UserRoutineDTO> getUserActiveRoutines(Long userId) {
        log.info("Fetching active routines for user: {}", userId);
        return userRoutineRepository.findByUserIdAndIsActive(userId, true)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all routines for a user
     */
    public List<UserRoutineDTO> getUserRoutines(Long userId) {
        log.info("Fetching all routines for user: {}", userId);
        return userRoutineRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get routine by ID with authorization check
     */
    public UserRoutineDTO getRoutineById(Long routineId, Long userId) {
        log.info("Fetching routine: {} for user: {}", routineId, userId);
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found or unauthorized"));
        return toDTO(routine);
    }
    
    /**
     * Assign a routine template to a user
     */
    @Transactional
    public UserRoutineDTO assignRoutine(UserRoutineRequestDTO request, Long userId) {
        log.info("Assigning routine template: {} to user: {}", request.getRoutineTemplateId(), userId);
        
        RoutineTemplate template = routineTemplateRepository.findById(request.getRoutineTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found: " + request.getRoutineTemplateId()));
        
        UserRoutine routine = UserRoutine.builder()
                .userId(userId)
                .routineTemplate(template)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        UserRoutine saved = userRoutineRepository.save(routine);
        log.info("Routine assigned with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update a user routine
     */
    @Transactional
    public UserRoutineDTO updateRoutine(Long routineId, UserRoutineRequestDTO request, Long userId) {
        log.info("Updating routine: {} for user: {}", routineId, userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found or unauthorized"));
        
        if (request.getIsActive() != null) {
            routine.setIsActive(request.getIsActive());
        }
        if (request.getEndDate() != null) {
            routine.setEndDate(request.getEndDate());
        }
        routine.setUpdatedAt(LocalDateTime.now());
        
        UserRoutine updated = userRoutineRepository.save(routine);
        log.info("Routine updated: {}", routineId);
        return toDTO(updated);
    }
    
    /**
     * Deactivate a routine
     */
    @Transactional
    public UserRoutineDTO deactivateRoutine(Long routineId, Long userId) {
        log.info("Deactivating routine: {} for user: {}", routineId, userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found or unauthorized"));
        
        routine.setIsActive(false);
        routine.setEndDate(LocalDateTime.now());
        routine.setUpdatedAt(LocalDateTime.now());
        
        UserRoutine updated = userRoutineRepository.save(routine);
        log.info("Routine deactivated: {}", routineId);
        return toDTO(updated);
    }
    
    /**
     * Delete a routine
     */
    @Transactional
    public void deleteRoutine(Long routineId, Long userId) {
        log.info("Deleting routine: {} for user: {}", routineId, userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found or unauthorized"));
        
        userRoutineRepository.delete(routine);
        log.info("Routine deleted: {}", routineId);
    }
    
    /**
     * Convert UserRoutine entity to DTO
     */
    private UserRoutineDTO toDTO(UserRoutine routine) {
        return UserRoutineDTO.builder()
                .id(routine.getId())
                .userId(routine.getUserId())
                .routineTemplateId(routine.getRoutineTemplate().getId())
                .routineTemplateName(routine.getRoutineTemplate().getName())
                .isActive(routine.getIsActive())
                .startDate(routine.getStartDate())
                .endDate(routine.getEndDate())
                .createdAt(routine.getCreatedAt())
                .updatedAt(routine.getUpdatedAt())
                .build();
    }
}
