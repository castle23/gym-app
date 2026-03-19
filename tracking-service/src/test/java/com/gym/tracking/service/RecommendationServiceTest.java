package com.gym.tracking.service;

import com.gym.tracking.dto.RecommendationDTO;
import com.gym.tracking.dto.RecommendationRequestDTO;
import com.gym.tracking.entity.DietComponent;
import com.gym.tracking.entity.Recommendation;
import com.gym.tracking.entity.TrainingComponent;
import com.gym.tracking.repository.DietComponentRepository;
import com.gym.tracking.repository.RecommendationRepository;
import com.gym.tracking.repository.TrainingComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    
    @Mock
    private RecommendationRepository recommendationRepository;
    
    @Mock
    private TrainingComponentRepository trainingComponentRepository;
    
    @Mock
    private DietComponentRepository dietComponentRepository;
    
    @InjectMocks
    private RecommendationService recommendationService;
    
    private Recommendation recommendation;
    private TrainingComponent trainingComponent;
    private DietComponent dietComponent;
    
    @BeforeEach
    void setUp() {
        trainingComponent = TrainingComponent.builder()
                .id(1L)
                .focus("Cardio")
                .intensity("High")
                .frequencyPerWeek(5)
                .build();
        
        dietComponent = DietComponent.builder()
                .id(1L)
                .dietType("Balanced")
                .dailyCalories(2000)
                .macroDistribution("40-40-20")
                .build();
        
        recommendation = Recommendation.builder()
                .id(1L)
                .trainingComponent(trainingComponent)
                .dietComponent(dietComponent)
                .title("Cardio Recommendation")
                .description("Do cardio 5 times per week")
                .professionalName("John Trainer")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetRecommendationById() {
        when(recommendationRepository.findById(1L))
                .thenReturn(Optional.of(recommendation));
        
        RecommendationDTO result = recommendationService.getRecommendationById(1L);
        
        assertNotNull(result);
        assertEquals("Cardio Recommendation", result.getTitle());
    }
    
    @Test
    void testGetRecommendationById_NotFound() {
        when(recommendationRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> recommendationService.getRecommendationById(999L));
    }
    
    @Test
    void testGetRecommendationsByTrainingComponent() {
        when(recommendationRepository.findByTrainingComponentId(1L))
                .thenReturn(List.of(recommendation));
        
        List<RecommendationDTO> result = recommendationService.getRecommendationsByTrainingComponent(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetRecommendationsByDietComponent() {
        when(recommendationRepository.findByDietComponentId(1L))
                .thenReturn(List.of(recommendation));
        
        List<RecommendationDTO> result = recommendationService.getRecommendationsByDietComponent(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testCreateRecommendation() {
        RecommendationRequestDTO request = RecommendationRequestDTO.builder()
                .trainingComponentId(1L)
                .dietComponentId(1L)
                .title("New Recommendation")
                .description("New description")
                .professionalName("Jane Trainer")
                .build();
        
        when(trainingComponentRepository.findById(1L))
                .thenReturn(Optional.of(trainingComponent));
        when(dietComponentRepository.findById(1L))
                .thenReturn(Optional.of(dietComponent));
        when(recommendationRepository.save(any(Recommendation.class)))
                .thenReturn(recommendation);
        
        RecommendationDTO result = recommendationService.createRecommendation(1L, request);
        
        assertNotNull(result);
        assertEquals("Cardio Recommendation", result.getTitle());
    }
    
    @Test
    void testUpdateRecommendation() {
        RecommendationRequestDTO request = RecommendationRequestDTO.builder()
                .title("Updated Recommendation")
                .description("Updated description")
                .professionalName("Updated Trainer")
                .build();
        
        when(recommendationRepository.findById(1L))
                .thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class)))
                .thenReturn(recommendation);
        
        RecommendationDTO result = recommendationService.updateRecommendation(1L, request);
        
        assertNotNull(result);
        verify(recommendationRepository, times(1)).save(any(Recommendation.class));
    }
    
    @Test
    void testDeleteRecommendation() {
        when(recommendationRepository.findById(1L))
                .thenReturn(Optional.of(recommendation));
        doNothing().when(recommendationRepository).delete(any(Recommendation.class));
        
        recommendationService.deleteRecommendation(1L);
        
        verify(recommendationRepository, times(1)).delete(any(Recommendation.class));
    }
    
    @Test
    void testCreateRecommendation_NoComponents() {
        RecommendationRequestDTO request = RecommendationRequestDTO.builder()
                .title("Invalid Recommendation")
                .description("No components")
                .professionalName("Trainer")
                .build();
        
        assertThrows(IllegalArgumentException.class,
                () -> recommendationService.createRecommendation(1L, request));
    }
}
