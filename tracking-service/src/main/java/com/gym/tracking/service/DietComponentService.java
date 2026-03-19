package com.gym.tracking.service;

import com.gym.tracking.dto.DietComponentDTO;
import com.gym.tracking.dto.DietComponentRequestDTO;
import com.gym.tracking.entity.DietComponent;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.repository.DietComponentRepository;
import com.gym.tracking.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DietComponentService {
    
    private final DietComponentRepository dietComponentRepository;
    private final PlanRepository planRepository;
    
    /**
     * Get diet component by ID
     */
    public DietComponentDTO getDietComponentById(Long id, Long userId) {
        log.info("Fetching diet component: {} for user: {}", id, userId);
        DietComponent component = dietComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet component not found: " + id));
        
        verifyUserOwnership(component.getPlan().getUserId(), userId);
        return toDTO(component);
    }
    
    /**
     * Get diet component by plan ID
     */
    public DietComponentDTO getDietComponentByPlanId(Long planId, Long userId) {
        log.info("Fetching diet component for plan: {} and user: {}", planId, userId);
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        DietComponent component = dietComponentRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Diet component not found for plan: " + planId));
        
        return toDTO(component);
    }
    
    /**
     * Create diet component
     */
    @Transactional
    public DietComponentDTO createDietComponent(Long userId, DietComponentRequestDTO request) {
        log.info("Creating diet component for user: {}", userId);
        
        Plan plan = planRepository.findByIdAndUserId(request.getPlanId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        DietComponent component = DietComponent.builder()
                .plan(plan)
                .dietType(request.getDietType())
                .dailyCalories(request.getDailyCalories())
                .macroDistribution(request.getMacroDistribution())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        DietComponent saved = dietComponentRepository.save(component);
        log.info("Diet component created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update diet component
     */
    @Transactional
    public DietComponentDTO updateDietComponent(Long id, Long userId, DietComponentRequestDTO request) {
        log.info("Updating diet component: {} for user: {}", id, userId);
        
        DietComponent component = dietComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet component not found: " + id));
        
        verifyUserOwnership(component.getPlan().getUserId(), userId);
        
        component.setDietType(request.getDietType());
        component.setDailyCalories(request.getDailyCalories());
        component.setMacroDistribution(request.getMacroDistribution());
        component.setUpdatedAt(LocalDateTime.now());
        
        DietComponent updated = dietComponentRepository.save(component);
        log.info("Diet component updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete diet component
     */
    @Transactional
    public void deleteDietComponent(Long id, Long userId) {
        log.info("Deleting diet component: {} for user: {}", id, userId);
        
        DietComponent component = dietComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet component not found: " + id));
        
        verifyUserOwnership(component.getPlan().getUserId(), userId);
        
        dietComponentRepository.delete(component);
        log.info("Diet component deleted: {}", id);
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
     * Convert DietComponent entity to DTO
     */
    private DietComponentDTO toDTO(DietComponent component) {
        return DietComponentDTO.builder()
                .id(component.getId())
                .planId(component.getPlan().getId())
                .dietType(component.getDietType())
                .dailyCalories(component.getDailyCalories())
                .macroDistribution(component.getMacroDistribution())
                .createdAt(component.getCreatedAt())
                .updatedAt(component.getUpdatedAt())
                .build();
    }
}
