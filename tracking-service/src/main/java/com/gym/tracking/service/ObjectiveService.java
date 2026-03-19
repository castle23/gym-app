package com.gym.tracking.service;

import com.gym.tracking.dto.ObjectiveDTO;
import com.gym.tracking.dto.ObjectiveRequestDTO;
import com.gym.tracking.entity.Objective;
import com.gym.tracking.repository.ObjectiveRepository;
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
public class ObjectiveService {
    
    private final ObjectiveRepository objectiveRepository;
    
    /**
     * Get all objectives for a user
     */
    public List<ObjectiveDTO> getUserObjectives(Long userId) {
        log.info("Fetching all objectives for user: {}", userId);
        return objectiveRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get active objectives for a user
     */
    public List<ObjectiveDTO> getActiveObjectives(Long userId) {
        log.info("Fetching active objectives for user: {}", userId);
        return objectiveRepository.findByUserIdAndIsActive(userId, true)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get objective by ID
     */
    public ObjectiveDTO getObjectiveById(Long objectiveId, Long userId) {
        log.info("Fetching objective: {} for user: {}", objectiveId, userId);
        Objective objective = objectiveRepository.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found or unauthorized"));
        return toDTO(objective);
    }
    
    /**
     * Create new objective
     */
    @Transactional
    public ObjectiveDTO createObjective(Long userId, ObjectiveRequestDTO request) {
        log.info("Creating objective for user: {}", userId);
        
        Objective objective = Objective.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Objective saved = objectiveRepository.save(objective);
        log.info("Objective created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update objective
     */
    @Transactional
    public ObjectiveDTO updateObjective(Long objectiveId, Long userId, ObjectiveRequestDTO request) {
        log.info("Updating objective: {} for user: {}", objectiveId, userId);
        
        Objective objective = objectiveRepository.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found or unauthorized"));
        
        objective.setTitle(request.getTitle());
        objective.setDescription(request.getDescription());
        objective.setCategory(request.getCategory());
        if (request.getIsActive() != null) {
            objective.setIsActive(request.getIsActive());
        }
        objective.setUpdatedAt(LocalDateTime.now());
        
        Objective updated = objectiveRepository.save(objective);
        log.info("Objective updated: {}", objectiveId);
        return toDTO(updated);
    }
    
    /**
     * Delete objective
     */
    @Transactional
    public void deleteObjective(Long objectiveId, Long userId) {
        log.info("Deleting objective: {} for user: {}", objectiveId, userId);
        
        Objective objective = objectiveRepository.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found or unauthorized"));
        
        objectiveRepository.delete(objective);
        log.info("Objective deleted: {}", objectiveId);
    }
    
    /**
     * Convert Objective entity to DTO
     */
    private ObjectiveDTO toDTO(Objective objective) {
        return ObjectiveDTO.builder()
                .id(objective.getId())
                .userId(objective.getUserId())
                .title(objective.getTitle())
                .description(objective.getDescription())
                .category(objective.getCategory())
                .isActive(objective.getIsActive())
                .createdAt(objective.getCreatedAt())
                .updatedAt(objective.getUpdatedAt())
                .build();
    }
}
