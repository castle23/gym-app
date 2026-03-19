package com.gym.training.service;

import com.gym.training.dto.UserRoutineDTO;
import com.gym.training.dto.UserRoutineRequestDTO;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.TemplateType;
import com.gym.training.entity.UserRoutine;
import com.gym.training.repository.RoutineTemplateRepository;
import com.gym.training.repository.UserRoutineRepository;
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
class UserRoutineServiceTest {
    
    @Mock
    private UserRoutineRepository userRoutineRepository;
    
    @Mock
    private RoutineTemplateRepository routineTemplateRepository;
    
    @InjectMocks
    private UserRoutineService userRoutineService;
    
    private UserRoutine userRoutine;
    private RoutineTemplate template;
    
    @BeforeEach
    void setUp() {
        template = RoutineTemplate.builder()
                .id(1L)
                .name("Beginner")
                .type(TemplateType.SYSTEM)
                .build();
        
        userRoutine = UserRoutine.builder()
                .id(1L)
                .userId(1L)
                .routineTemplate(template)
                .isActive(true)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetUserActiveRoutines() {
        when(userRoutineRepository.findByUserIdAndIsActive(1L, true))
                .thenReturn(List.of(userRoutine));
        
        List<UserRoutineDTO> result = userRoutineService.getUserActiveRoutines(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }
    
    @Test
    void testGetUserRoutines() {
        when(userRoutineRepository.findByUserId(1L))
                .thenReturn(List.of(userRoutine));
        
        List<UserRoutineDTO> result = userRoutineService.getUserRoutines(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetRoutineById() {
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        
        UserRoutineDTO result = userRoutineService.getRoutineById(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    void testAssignRoutine() {
        UserRoutineRequestDTO request = UserRoutineRequestDTO.builder()
                .routineTemplateId(1L)
                .isActive(true)
                .build();
        
        UserRoutine assigned = UserRoutine.builder()
                .id(2L)
                .userId(1L)
                .routineTemplate(template)
                .isActive(true)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(routineTemplateRepository.findById(1L))
                .thenReturn(Optional.of(template));
        when(userRoutineRepository.save(any(UserRoutine.class)))
                .thenReturn(assigned);
        
        UserRoutineDTO result = userRoutineService.assignRoutine(request, 1L);
        
        assertNotNull(result);
        assertTrue(result.getIsActive());
    }
    
    @Test
    void testUpdateRoutine() {
        UserRoutineRequestDTO request = UserRoutineRequestDTO.builder()
                .isActive(false)
                .build();
        
        UserRoutine updated = UserRoutine.builder()
                .id(1L)
                .userId(1L)
                .routineTemplate(template)
                .isActive(false)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        when(userRoutineRepository.save(any(UserRoutine.class)))
                .thenReturn(updated);
        
        UserRoutineDTO result = userRoutineService.updateRoutine(1L, request, 1L);
        
        assertNotNull(result);
        assertFalse(result.getIsActive());
    }
    
    @Test
    void testDeactivateRoutine() {
        UserRoutine deactivated = UserRoutine.builder()
                .id(1L)
                .userId(1L)
                .routineTemplate(template)
                .isActive(false)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        when(userRoutineRepository.save(any(UserRoutine.class)))
                .thenReturn(deactivated);
        
        UserRoutineDTO result = userRoutineService.deactivateRoutine(1L, 1L);
        
        assertNotNull(result);
        assertFalse(result.getIsActive());
    }
    
    @Test
    void testDeleteRoutine() {
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        
        userRoutineService.deleteRoutine(1L, 1L);
        
        verify(userRoutineRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(userRoutineRepository, times(1)).delete(userRoutine);
    }
}
