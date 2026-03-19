package com.gym.tracking.service;

import com.gym.tracking.dto.TrainingComponentDTO;
import com.gym.tracking.dto.TrainingComponentRequestDTO;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.entity.TrainingComponent;
import com.gym.tracking.repository.PlanRepository;
import com.gym.tracking.repository.TrainingComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingComponentServiceTest {
    
    @Mock
    private TrainingComponentRepository trainingComponentRepository;
    
    @Mock
    private PlanRepository planRepository;
    
    @InjectMocks
    private TrainingComponentService trainingComponentService;
    
    private TrainingComponent trainingComponent;
    private Plan plan;
    
    @BeforeEach
    void setUp() {
        plan = Plan.builder()
                .id(1L)
                .userId(1L)
                .name("Test Plan")
                .description("Test description")
                .status(Plan.PlanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        trainingComponent = TrainingComponent.builder()
                .id(1L)
                .plan(plan)
                .focus("Cardio")
                .intensity("High")
                .frequencyPerWeek(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetTrainingComponentById_Success() {
        when(trainingComponentRepository.findById(1L))
                .thenReturn(Optional.of(trainingComponent));
        
        TrainingComponentDTO result = trainingComponentService.getTrainingComponentById(1L, 1L);
        
        assertNotNull(result);
        assertEquals("Cardio", result.getFocus());
    }
    
    @Test
    void testGetTrainingComponentById_NotFound() {
        when(trainingComponentRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> trainingComponentService.getTrainingComponentById(999L, 1L));
    }
    
    @Test
    void testGetTrainingComponentById_Unauthorized() {
        when(trainingComponentRepository.findById(1L))
                .thenReturn(Optional.of(trainingComponent));
        
        assertThrows(IllegalArgumentException.class,
                () -> trainingComponentService.getTrainingComponentById(1L, 2L));
    }
    
    @Test
    void testGetTrainingComponentByPlanId() {
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        when(trainingComponentRepository.findByPlanId(1L))
                .thenReturn(Optional.of(trainingComponent));
        
        TrainingComponentDTO result = trainingComponentService.getTrainingComponentByPlanId(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getPlanId());
    }
    
    @Test
    void testCreateTrainingComponent() {
        TrainingComponentRequestDTO request = TrainingComponentRequestDTO.builder()
                .planId(1L)
                .focus("Strength")
                .intensity("Medium")
                .frequencyPerWeek(3)
                .build();
        
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        when(trainingComponentRepository.save(any(TrainingComponent.class)))
                .thenReturn(trainingComponent);
        
        TrainingComponentDTO result = trainingComponentService.createTrainingComponent(1L, request);
        
        assertNotNull(result);
        assertEquals("Cardio", result.getFocus());
    }
    
    @Test
    void testUpdateTrainingComponent() {
        TrainingComponentRequestDTO request = TrainingComponentRequestDTO.builder()
                .planId(1L)
                .focus("Updated Focus")
                .intensity("Low")
                .frequencyPerWeek(2)
                .build();
        
        when(trainingComponentRepository.findById(1L))
                .thenReturn(Optional.of(trainingComponent));
        when(trainingComponentRepository.save(any(TrainingComponent.class)))
                .thenReturn(trainingComponent);
        
        TrainingComponentDTO result = trainingComponentService.updateTrainingComponent(1L, 1L, request);
        
        assertNotNull(result);
        verify(trainingComponentRepository, times(1)).save(any(TrainingComponent.class));
    }
    
    @Test
    void testDeleteTrainingComponent() {
        when(trainingComponentRepository.findById(1L))
                .thenReturn(Optional.of(trainingComponent));
        doNothing().when(trainingComponentRepository).delete(any(TrainingComponent.class));
        
        trainingComponentService.deleteTrainingComponent(1L, 1L);
        
        verify(trainingComponentRepository, times(1)).delete(any(TrainingComponent.class));
    }
    
    @Test
    void testDeleteTrainingComponent_Unauthorized() {
        when(trainingComponentRepository.findById(1L))
                .thenReturn(Optional.of(trainingComponent));
        
        assertThrows(IllegalArgumentException.class,
                () -> trainingComponentService.deleteTrainingComponent(1L, 2L));
    }
}
