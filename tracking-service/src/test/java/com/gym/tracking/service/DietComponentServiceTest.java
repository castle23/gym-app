package com.gym.tracking.service;

import com.gym.tracking.dto.DietComponentDTO;
import com.gym.tracking.dto.DietComponentRequestDTO;
import com.gym.tracking.entity.DietComponent;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.repository.DietComponentRepository;
import com.gym.tracking.repository.PlanRepository;
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
class DietComponentServiceTest {
    
    @Mock
    private DietComponentRepository dietComponentRepository;
    
    @Mock
    private PlanRepository planRepository;
    
    @InjectMocks
    private DietComponentService dietComponentService;
    
    private DietComponent dietComponent;
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
        
        dietComponent = DietComponent.builder()
                .id(1L)
                .plan(plan)
                .dietType("Balanced")
                .dailyCalories(2000)
                .macroDistribution("40-40-20")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetDietComponentById_Success() {
        when(dietComponentRepository.findById(1L))
                .thenReturn(Optional.of(dietComponent));
        
        DietComponentDTO result = dietComponentService.getDietComponentById(1L, 1L);
        
        assertNotNull(result);
        assertEquals("Balanced", result.getDietType());
    }
    
    @Test
    void testGetDietComponentById_NotFound() {
        when(dietComponentRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> dietComponentService.getDietComponentById(999L, 1L));
    }
    
    @Test
    void testGetDietComponentById_Unauthorized() {
        when(dietComponentRepository.findById(1L))
                .thenReturn(Optional.of(dietComponent));
        
        assertThrows(IllegalArgumentException.class,
                () -> dietComponentService.getDietComponentById(1L, 2L));
    }
    
    @Test
    void testGetDietComponentByPlanId() {
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        when(dietComponentRepository.findByPlanId(1L))
                .thenReturn(Optional.of(dietComponent));
        
        DietComponentDTO result = dietComponentService.getDietComponentByPlanId(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getPlanId());
    }
    
    @Test
    void testCreateDietComponent() {
        DietComponentRequestDTO request = DietComponentRequestDTO.builder()
                .planId(1L)
                .dietType("Keto")
                .dailyCalories(1800)
                .macroDistribution("75-20-5")
                .build();
        
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        when(dietComponentRepository.save(any(DietComponent.class)))
                .thenReturn(dietComponent);
        
        DietComponentDTO result = dietComponentService.createDietComponent(1L, request);
        
        assertNotNull(result);
        assertEquals("Balanced", result.getDietType());
    }
    
    @Test
    void testUpdateDietComponent() {
        DietComponentRequestDTO request = DietComponentRequestDTO.builder()
                .planId(1L)
                .dietType("Updated Diet")
                .dailyCalories(2200)
                .macroDistribution("50-30-20")
                .build();
        
        when(dietComponentRepository.findById(1L))
                .thenReturn(Optional.of(dietComponent));
        when(dietComponentRepository.save(any(DietComponent.class)))
                .thenReturn(dietComponent);
        
        DietComponentDTO result = dietComponentService.updateDietComponent(1L, 1L, request);
        
        assertNotNull(result);
        verify(dietComponentRepository, times(1)).save(any(DietComponent.class));
    }
    
    @Test
    void testDeleteDietComponent() {
        when(dietComponentRepository.findById(1L))
                .thenReturn(Optional.of(dietComponent));
        doNothing().when(dietComponentRepository).delete(any(DietComponent.class));
        
        dietComponentService.deleteDietComponent(1L, 1L);
        
        verify(dietComponentRepository, times(1)).delete(any(DietComponent.class));
    }
    
    @Test
    void testDeleteDietComponent_Unauthorized() {
        when(dietComponentRepository.findById(1L))
                .thenReturn(Optional.of(dietComponent));
        
        assertThrows(IllegalArgumentException.class,
                () -> dietComponentService.deleteDietComponent(1L, 2L));
    }
}
