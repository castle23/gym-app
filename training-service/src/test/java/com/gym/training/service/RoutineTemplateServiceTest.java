package com.gym.training.service;

import com.gym.training.dto.RoutineTemplateDTO;
import com.gym.training.dto.RoutineTemplateRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.TemplateType;
import com.gym.training.repository.ExerciseRepository;
import com.gym.training.repository.RoutineTemplateRepository;
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
class RoutineTemplateServiceTest {
    
    @Mock
    private RoutineTemplateRepository routineTemplateRepository;
    
    @Mock
    private ExerciseRepository exerciseRepository;
    
    @InjectMocks
    private RoutineTemplateService routineTemplateService;
    
    private RoutineTemplate begginerTemplate;
    
    @BeforeEach
    void setUp() {
        begginerTemplate = RoutineTemplate.builder()
                .id(1L)
                .name("Beginner Routine")
                .type(TemplateType.SYSTEM)
                .exercises(List.of())
                .createdBy(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetAllSystemTemplates() {
        when(routineTemplateRepository.findByType(TemplateType.SYSTEM))
                .thenReturn(List.of(begginerTemplate));
        
        List<RoutineTemplateDTO> result = routineTemplateService.getAllSystemTemplates();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Beginner Routine", result.get(0).getName());
    }
    
    @Test
    void testGetUserTemplates() {
        when(routineTemplateRepository.findByCreatedBy(1L))
                .thenReturn(List.of(begginerTemplate));
        
        List<RoutineTemplateDTO> result = routineTemplateService.getUserTemplates(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetTemplateById() {
        when(routineTemplateRepository.findById(1L))
                .thenReturn(Optional.of(begginerTemplate));
        
        RoutineTemplateDTO result = routineTemplateService.getTemplateById(1L);
        
        assertNotNull(result);
        assertEquals("Beginner Routine", result.getName());
    }
    
    @Test
    void testGetTemplateByIdNotFound() {
        when(routineTemplateRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            routineTemplateService.getTemplateById(999L);
        });
    }
    
    @Test
    void testCreateTemplate() {
        RoutineTemplateRequestDTO request = RoutineTemplateRequestDTO.builder()
                .name("Intermediate Routine")
                .type(TemplateType.PROFESSIONAL)
                .exerciseIds(List.of(1L, 2L))
                .build();
        
        Exercise exercise1 = Exercise.builder().id(1L).name("Exercise 1").build();
        Exercise exercise2 = Exercise.builder().id(2L).name("Exercise 2").build();
        
        RoutineTemplate newTemplate = RoutineTemplate.builder()
                .id(2L)
                .name(request.getName())
                .type(request.getType())
                .exercises(List.of(exercise1, exercise2))
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(exerciseRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(exercise1, exercise2));
        when(routineTemplateRepository.save(any(RoutineTemplate.class)))
                .thenReturn(newTemplate);
        
        RoutineTemplateDTO result = routineTemplateService.createTemplate(request, 1L);
        
        assertNotNull(result);
        assertEquals("Intermediate Routine", result.getName());
        assertEquals(2, result.getExerciseIds().size());
    }
    
    @Test
    void testUpdateTemplate() {
        RoutineTemplateRequestDTO request = RoutineTemplateRequestDTO.builder()
                .name("Updated Routine")
                .type(TemplateType.PROFESSIONAL)
                .exerciseIds(List.of())
                .build();
        
        RoutineTemplate updatedTemplate = RoutineTemplate.builder()
                .id(1L)
                .name(request.getName())
                .type(request.getType())
                .exercises(List.of())
                .createdBy(1L)
                .build();
        
        when(routineTemplateRepository.findByIdAndCreatedBy(1L, 1L))
                .thenReturn(Optional.of(begginerTemplate));
        when(exerciseRepository.findAllById(List.of()))
                .thenReturn(List.of());
        when(routineTemplateRepository.save(any(RoutineTemplate.class)))
                .thenReturn(updatedTemplate);
        
        RoutineTemplateDTO result = routineTemplateService.updateTemplate(1L, request, 1L);
        
        assertNotNull(result);
        assertEquals("Updated Routine", result.getName());
    }
    
    @Test
    void testDeleteTemplate() {
        when(routineTemplateRepository.findByIdAndCreatedBy(1L, 1L))
                .thenReturn(Optional.of(begginerTemplate));
        
        routineTemplateService.deleteTemplate(1L, 1L);
        
        verify(routineTemplateRepository, times(1)).findByIdAndCreatedBy(1L, 1L);
        verify(routineTemplateRepository, times(1)).delete(begginerTemplate);
    }
}
