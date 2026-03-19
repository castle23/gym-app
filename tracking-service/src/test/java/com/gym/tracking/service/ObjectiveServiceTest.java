package com.gym.tracking.service;

import com.gym.tracking.dto.ObjectiveDTO;
import com.gym.tracking.dto.ObjectiveRequestDTO;
import com.gym.tracking.entity.Objective;
import com.gym.tracking.repository.ObjectiveRepository;
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
class ObjectiveServiceTest {
    
    @Mock
    private ObjectiveRepository objectiveRepository;
    
    @InjectMocks
    private ObjectiveService objectiveService;
    
    private Objective objective;
    
    @BeforeEach
    void setUp() {
        objective = Objective.builder()
                .id(1L)
                .userId(1L)
                .title("Lose Weight")
                .description("Lose 10kg in 3 months")
                .category("Weight")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetUserObjectives() {
        when(objectiveRepository.findByUserId(1L))
                .thenReturn(List.of(objective));
        
        List<ObjectiveDTO> result = objectiveService.getUserObjectives(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Lose Weight", result.get(0).getTitle());
    }
    
    @Test
    void testGetActiveObjectives() {
        when(objectiveRepository.findByUserIdAndIsActive(1L, true))
                .thenReturn(List.of(objective));
        
        List<ObjectiveDTO> result = objectiveService.getActiveObjectives(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }
    
    @Test
    void testGetObjectiveById_Success() {
        when(objectiveRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(objective));
        
        ObjectiveDTO result = objectiveService.getObjectiveById(1L, 1L);
        
        assertNotNull(result);
        assertEquals("Lose Weight", result.getTitle());
    }
    
    @Test
    void testGetObjectiveById_NotFound() {
        when(objectiveRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> objectiveService.getObjectiveById(999L, 1L));
    }
    
    @Test
    void testCreateObjective() {
        ObjectiveRequestDTO request = ObjectiveRequestDTO.builder()
                .title("Build Muscle")
                .description("Gain 5kg muscle")
                .category("Strength")
                .isActive(true)
                .build();
        
        Objective saved = Objective.builder()
                .id(2L)
                .userId(1L)
                .title("Build Muscle")
                .description("Gain 5kg muscle")
                .category("Strength")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(objectiveRepository.save(any(Objective.class)))
                .thenReturn(saved);
        
        ObjectiveDTO result = objectiveService.createObjective(1L, request);
        
        assertNotNull(result);
        assertEquals("Build Muscle", result.getTitle());
    }
    
    @Test
    void testUpdateObjective() {
        ObjectiveRequestDTO request = ObjectiveRequestDTO.builder()
                .title("Updated Title")
                .description("Updated description")
                .category("Updated")
                .isActive(false)
                .build();
        
        when(objectiveRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(objective));
        when(objectiveRepository.save(any(Objective.class)))
                .thenReturn(objective);
        
        ObjectiveDTO result = objectiveService.updateObjective(1L, 1L, request);
        
        assertNotNull(result);
        verify(objectiveRepository, times(1)).save(any(Objective.class));
    }
    
    @Test
    void testDeleteObjective() {
        when(objectiveRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(objective));
        doNothing().when(objectiveRepository).delete(any(Objective.class));
        
        objectiveService.deleteObjective(1L, 1L);
        
        verify(objectiveRepository, times(1)).delete(any(Objective.class));
    }
    
    @Test
    void testDeleteObjective_Unauthorized() {
        when(objectiveRepository.findByIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> objectiveService.deleteObjective(1L, 2L));
    }
    
    @Test
    void testCreateObjective_DefaultActive() {
        ObjectiveRequestDTO request = ObjectiveRequestDTO.builder()
                .title("New Objective")
                .description("Description")
                .category("Category")
                .build();
        
        Objective saved = Objective.builder()
                .id(3L)
                .userId(1L)
                .title("New Objective")
                .description("Description")
                .category("Category")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(objectiveRepository.save(any(Objective.class)))
                .thenReturn(saved);
        
        ObjectiveDTO result = objectiveService.createObjective(1L, request);
        
        assertNotNull(result);
        assertTrue(result.getIsActive());
    }
}
