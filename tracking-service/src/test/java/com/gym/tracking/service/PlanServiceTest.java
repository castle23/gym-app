package com.gym.tracking.service;

import com.gym.tracking.dto.PlanDTO;
import com.gym.tracking.dto.PlanRequestDTO;
import com.gym.tracking.entity.Objective;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.repository.ObjectiveRepository;
import com.gym.tracking.repository.PlanRepository;
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
class PlanServiceTest {
    
    @Mock
    private PlanRepository planRepository;
    
    @Mock
    private ObjectiveRepository objectiveRepository;
    
    @InjectMocks
    private PlanService planService;
    
    private Plan plan;
    private Objective objective;
    
    @BeforeEach
    void setUp() {
        objective = Objective.builder()
                .id(1L)
                .userId(1L)
                .title("Lose Weight")
                .description("Lose 10kg")
                .category("Weight")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        plan = Plan.builder()
                .id(1L)
                .userId(1L)
                .name("Weight Loss Plan")
                .description("3-month weight loss plan")
                .objective(objective)
                .status(Plan.PlanStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(3))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetUserPlans() {
        when(planRepository.findByUserId(1L))
                .thenReturn(List.of(plan));
        
        List<PlanDTO> result = planService.getUserPlans(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Weight Loss Plan", result.get(0).getName());
    }
    
    @Test
    void testGetActivePlan() {
        when(planRepository.findByUserIdAndStatus(1L, Plan.PlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        
        PlanDTO result = planService.getActivePlan(1L);
        
        assertNotNull(result);
        assertEquals(Plan.PlanStatus.ACTIVE, result.getStatus());
    }
    
    @Test
    void testGetActivePlan_NotFound() {
        when(planRepository.findByUserIdAndStatus(1L, Plan.PlanStatus.ACTIVE))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> planService.getActivePlan(1L));
    }
    
    @Test
    void testGetPlansByStatus() {
        when(planRepository.findAllByUserIdAndStatus(1L, Plan.PlanStatus.ACTIVE))
                .thenReturn(List.of(plan));
        
        List<PlanDTO> result = planService.getPlansByStatus(1L, Plan.PlanStatus.ACTIVE);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetPlanById_Success() {
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        
        PlanDTO result = planService.getPlanById(1L, 1L);
        
        assertNotNull(result);
        assertEquals("Weight Loss Plan", result.getName());
    }
    
    @Test
    void testGetPlanById_NotFound() {
        when(planRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> planService.getPlanById(999L, 1L));
    }
    
    @Test
    void testCreatePlan() {
        PlanRequestDTO request = PlanRequestDTO.builder()
                .name("New Plan")
                .description("New plan description")
                .objectiveId(1L)
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(1))
                .build();
        
        when(objectiveRepository.findById(1L))
                .thenReturn(Optional.of(objective));
        when(planRepository.save(any(Plan.class)))
                .thenReturn(plan);
        
        PlanDTO result = planService.createPlan(1L, request);
        
        assertNotNull(result);
        assertEquals("Weight Loss Plan", result.getName());
    }
    
    @Test
    void testUpdatePlan() {
        PlanRequestDTO request = PlanRequestDTO.builder()
                .name("Updated Plan")
                .description("Updated description")
                .objectiveId(1L)
                .status("PAUSED")
                .startDate(LocalDateTime.now())
                .build();
        
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class)))
                .thenReturn(plan);
        
        PlanDTO result = planService.updatePlan(1L, 1L, request);
        
        assertNotNull(result);
        verify(planRepository, times(1)).save(any(Plan.class));
    }
    
    @Test
    void testDeletePlan() {
        when(planRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(plan));
        doNothing().when(planRepository).delete(any(Plan.class));
        
        planService.deletePlan(1L, 1L);
        
        verify(planRepository, times(1)).delete(any(Plan.class));
    }
    
    @Test
    void testCreatePlan_ObjectiveNotFound() {
        PlanRequestDTO request = PlanRequestDTO.builder()
                .name("New Plan")
                .description("Description")
                .objectiveId(999L)
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .build();
        
        when(objectiveRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> planService.createPlan(1L, request));
    }
    
    @Test
    void testDeletePlan_Unauthorized() {
        when(planRepository.findByIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> planService.deletePlan(1L, 2L));
    }
}
