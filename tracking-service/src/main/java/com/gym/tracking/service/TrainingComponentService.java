package com.gym.tracking.service;

import com.gym.tracking.dto.TrainingComponentDTO;
import com.gym.tracking.dto.TrainingComponentRequestDTO;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.entity.TrainingComponent;
import com.gym.tracking.repository.PlanRepository;
import com.gym.tracking.repository.TrainingComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingComponentService {
    
    private final TrainingComponentRepository trainingComponentRepository;
    private final PlanRepository planRepository;
    
    /**
     * Get training component by ID
     */
    public TrainingComponentDTO getTrainingComponentById(Long id, Long userId) {
        log.info("Fetching training component: {} for user: {}", id, userId);
        TrainingComponent component = trainingComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Training component not found: " + id));
        
        verifyUserOwnership(component.getPlan().getUserId(), userId);
        return toDTO(component);
    }
    
    /**
     * Get training component by plan ID
     */
    public TrainingComponentDTO getTrainingComponentByPlanId(Long planId, Long userId) {
        log.info("Fetching training component for plan: {} and user: {}", planId, userId);
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        TrainingComponent component = trainingComponentRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Training component not found for plan: " + planId));
        
        return toDTO(component);
    }
    
    /**
     * Create training component
     */
    @Transactional
    public TrainingComponentDTO createTrainingComponent(Long userId, TrainingComponentRequestDTO request) {
        log.info("Creating training component for user: {}", userId);
        
        Plan plan = planRepository.findByIdAndUserId(request.getPlanId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        TrainingComponent component = TrainingComponent.builder()
                .plan(plan)
                .focus(request.getFocus())
                .intensity(request.getIntensity())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        TrainingComponent saved = trainingComponentRepository.save(component);
        log.info("Training component created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update training component
     */
    @Transactional
    public TrainingComponentDTO updateTrainingComponent(Long id, Long userId, TrainingComponentRequestDTO request) {
        log.info("Updating training component: {} for user: {}", id, userId);
        
        TrainingComponent component = trainingComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Training component not found: " + id));
        
        verifyUserOwnership(component.getPlan().getUserId(), userId);
        
        component.setFocus(request.getFocus());
        component.setIntensity(request.getIntensity());
        component.setFrequencyPerWeek(request.getFrequencyPerWeek());
        component.setUpdatedAt(LocalDateTime.now());
        
        TrainingComponent updated = trainingComponentRepository.save(component);
        log.info("Training component updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete training component
     */
    @Transactional
    public void deleteTrainingComponent(Long id, Long userId) {
        log.info("Deleting training component: {} for user: {}", id, userId);
        
        TrainingComponent component = trainingComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Training component not found: " + id));
        
        verifyUserOwnership(component.getPlan().getUserId(), userId);
        
        trainingComponentRepository.delete(component);
        log.info("Training component deleted: {}", id);
    }
    
    /**
     * Verify user ownership
     */
    private void verifyUserOwnership(Long componentUserId, Long requestingUserId) {
        if (!componentUserId.equals(requestingUserId)) {
            throw new IllegalArgumentException("User is not authorized to access this resource");
        }
    }
    
    /**
     * Convert TrainingComponent entity to DTO
     */
    private TrainingComponentDTO toDTO(TrainingComponent component) {
        return TrainingComponentDTO.builder()
                .id(component.getId())
                .planId(component.getPlan().getId())
                .focus(component.getFocus())
                .intensity(component.getIntensity())
                .frequencyPerWeek(component.getFrequencyPerWeek())
                .createdAt(component.getCreatedAt())
                .updatedAt(component.getUpdatedAt())
                .build();
    }
}
