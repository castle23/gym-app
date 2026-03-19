package com.gym.tracking.service;

import com.gym.tracking.dto.PlanDTO;
import com.gym.tracking.dto.PlanRequestDTO;
import com.gym.tracking.entity.Objective;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.repository.ObjectiveRepository;
import com.gym.tracking.repository.PlanRepository;
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
public class PlanService {
    
    private final PlanRepository planRepository;
    private final ObjectiveRepository objectiveRepository;
    
    /**
     * Get all user plans
     */
    public List<PlanDTO> getUserPlans(Long userId) {
        log.info("Fetching all plans for user: {}", userId);
        return planRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get active plan for user
     */
    public PlanDTO getActivePlan(Long userId) {
        log.info("Fetching active plan for user: {}", userId);
        Plan plan = planRepository.findByUserIdAndStatus(userId, Plan.PlanStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active plan found for user"));
        return toDTO(plan);
    }
    
    /**
     * Get plans by status
     */
    public List<PlanDTO> getPlansByStatus(Long userId, Plan.PlanStatus status) {
        log.info("Fetching plans with status {} for user: {}", status, userId);
        return planRepository.findByUserIdAndStatus(userId, status)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get plan by ID
     */
    public PlanDTO getPlanById(Long planId, Long userId) {
        log.info("Fetching plan: {} for user: {}", planId, userId);
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        return toDTO(plan);
    }
    
    /**
     * Create new plan
     */
    @Transactional
    public PlanDTO createPlan(Long userId, PlanRequestDTO request) {
        log.info("Creating plan for user: {}", userId);
        
        Objective objective = objectiveRepository.findById(request.getObjectiveId())
                .orElseThrow(() -> new IllegalArgumentException("Objective not found: " + request.getObjectiveId()));
        
        if (!objective.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Objective does not belong to user");
        }
        
        Plan plan = Plan.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .objective(objective)
                .status(Plan.PlanStatus.valueOf(request.getStatus()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Plan saved = planRepository.save(plan);
        log.info("Plan created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update plan
     */
    @Transactional
    public PlanDTO updatePlan(Long planId, Long userId, PlanRequestDTO request) {
        log.info("Updating plan: {} for user: {}", planId, userId);
        
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setStatus(Plan.PlanStatus.valueOf(request.getStatus()));
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setUpdatedAt(LocalDateTime.now());
        
        Plan updated = planRepository.save(plan);
        log.info("Plan updated: {}", planId);
        return toDTO(updated);
    }
    
    /**
     * Delete plan
     */
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        log.info("Deleting plan: {} for user: {}", planId, userId);
        
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        planRepository.delete(plan);
        log.info("Plan deleted: {}", planId);
    }
    
    /**
     * Convert Plan entity to DTO
     */
    private PlanDTO toDTO(Plan plan) {
        return PlanDTO.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .name(plan.getName())
                .description(plan.getDescription())
                .objectiveId(plan.getObjective() != null ? plan.getObjective().getId() : null)
                .objectiveTitle(plan.getObjective() != null ? plan.getObjective().getTitle() : null)
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
