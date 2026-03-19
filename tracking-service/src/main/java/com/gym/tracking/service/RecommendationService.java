package com.gym.tracking.service;

import com.gym.tracking.dto.RecommendationDTO;
import com.gym.tracking.dto.RecommendationRequestDTO;
import com.gym.tracking.entity.DietComponent;
import com.gym.tracking.entity.Recommendation;
import com.gym.tracking.entity.TrainingComponent;
import com.gym.tracking.repository.DietComponentRepository;
import com.gym.tracking.repository.RecommendationRepository;
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
public class RecommendationService {
    
    private final RecommendationRepository recommendationRepository;
    private final TrainingComponentRepository trainingComponentRepository;
    private final DietComponentRepository dietComponentRepository;
    
    /**
     * Get recommendation by ID
     */
    public RecommendationDTO getRecommendationById(Long id) {
        log.info("Fetching recommendation: {}", id);
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + id));
        return toDTO(recommendation);
    }
    
    /**
     * Get recommendations by training component
     */
    public List<RecommendationDTO> getRecommendationsByTrainingComponent(Long trainingComponentId) {
        log.info("Fetching recommendations for training component: {}", trainingComponentId);
        return recommendationRepository.findByTrainingComponentId(trainingComponentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get recommendations by diet component
     */
    public List<RecommendationDTO> getRecommendationsByDietComponent(Long dietComponentId) {
        log.info("Fetching recommendations for diet component: {}", dietComponentId);
        return recommendationRepository.findByDietComponentId(dietComponentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Create recommendation
     */
    @Transactional
    public RecommendationDTO createRecommendation(RecommendationRequestDTO request) {
        log.info("Creating recommendation");
        
        TrainingComponent trainingComponent = null;
        DietComponent dietComponent = null;
        
        if (request.getTrainingComponentId() != null) {
            trainingComponent = trainingComponentRepository.findById(request.getTrainingComponentId())
                    .orElseThrow(() -> new IllegalArgumentException("Training component not found: " + request.getTrainingComponentId()));
        }
        
        if (request.getDietComponentId() != null) {
            dietComponent = dietComponentRepository.findById(request.getDietComponentId())
                    .orElseThrow(() -> new IllegalArgumentException("Diet component not found: " + request.getDietComponentId()));
        }
        
        if (trainingComponent == null && dietComponent == null) {
            throw new IllegalArgumentException("At least one component (training or diet) is required");
        }
        
        Recommendation recommendation = Recommendation.builder()
                .trainingComponent(trainingComponent)
                .dietComponent(dietComponent)
                .title(request.getTitle())
                .description(request.getDescription())
                .professionalName(request.getProfessionalName())
                .createdAt(LocalDateTime.now())
                .build();
        
        Recommendation saved = recommendationRepository.save(recommendation);
        log.info("Recommendation created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update recommendation
     */
    @Transactional
    public RecommendationDTO updateRecommendation(Long id, RecommendationRequestDTO request) {
        log.info("Updating recommendation: {}", id);
        
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + id));
        
        recommendation.setTitle(request.getTitle());
        recommendation.setDescription(request.getDescription());
        recommendation.setProfessionalName(request.getProfessionalName());
        
        Recommendation updated = recommendationRepository.save(recommendation);
        log.info("Recommendation updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete recommendation
     */
    @Transactional
    public void deleteRecommendation(Long id) {
        log.info("Deleting recommendation: {}", id);
        
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + id));
        
        recommendationRepository.delete(recommendation);
        log.info("Recommendation deleted: {}", id);
    }
    
    /**
     * Convert Recommendation entity to DTO
     */
    private RecommendationDTO toDTO(Recommendation recommendation) {
        return RecommendationDTO.builder()
                .id(recommendation.getId())
                .trainingComponentId(recommendation.getTrainingComponent() != null ? recommendation.getTrainingComponent().getId() : null)
                .dietComponentId(recommendation.getDietComponent() != null ? recommendation.getDietComponent().getId() : null)
                .title(recommendation.getTitle())
                .description(recommendation.getDescription())
                .professionalName(recommendation.getProfessionalName())
                .createdAt(recommendation.getCreatedAt())
                .build();
    }
}
