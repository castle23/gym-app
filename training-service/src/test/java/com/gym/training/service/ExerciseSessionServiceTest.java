package com.gym.training.service;

import com.gym.training.dto.ExerciseSessionDTO;
import com.gym.training.dto.ExerciseSessionRequestDTO;
import com.gym.training.entity.*;
import com.gym.training.repository.ExerciseRepository;
import com.gym.training.repository.ExerciseSessionRepository;
import com.gym.training.repository.UserRoutineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseSessionServiceTest {
    
    @Mock
    private ExerciseSessionRepository exerciseSessionRepository;
    
    @Mock
    private UserRoutineRepository userRoutineRepository;
    
    @Mock
    private ExerciseRepository exerciseRepository;
    
    @InjectMocks
    private ExerciseSessionService exerciseSessionService;
    
    private ExerciseSession session;
    private UserRoutine userRoutine;
    private Exercise exercise;
    private RoutineTemplate template;
    private Discipline discipline;
    
    @BeforeEach
    void setUp() {
        discipline = Discipline.builder()
                .id(1L)
                .name("Chest")
                .build();
        
        exercise = Exercise.builder()
                .id(1L)
                .name("Bench Press")
                .description("Barbell bench press")
                .type(ExerciseType.SYSTEM)
                .discipline(discipline)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
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
        
        session = ExerciseSession.builder()
                .id(1L)
                .userId(1L)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .setsCompleted(3)
                .repsCompleted(10)
                .weightUsed(100.0)
                .durationSeconds(600L)
                .notes("Good form")
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetSessionsByRoutineId() {
        when(exerciseSessionRepository.findByUserRoutineId(1L))
                .thenReturn(List.of(session));
        
        List<ExerciseSessionDTO> result = exerciseSessionService.getSessionsByRoutineId(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getExerciseId());
    }
    
    @Test
    void testGetSessionsByRoutineIdEmpty() {
        when(exerciseSessionRepository.findByUserRoutineId(1L))
                .thenReturn(List.of());
        
        List<ExerciseSessionDTO> result = exerciseSessionService.getSessionsByRoutineId(1L);
        
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    void testGetSessionsByUserIdAndDate() {
        LocalDate date = LocalDate.now();
        when(exerciseSessionRepository.findByUserIdAndDate(1L, date))
                .thenReturn(List.of(session));
        
        List<ExerciseSessionDTO> result = exerciseSessionService.getSessionsByUserIdAndDate(1L, date);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetSessionById() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.of(session));
        
        ExerciseSessionDTO result = exerciseSessionService.getSessionById(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Bench Press", result.getExerciseName());
    }
    
    @Test
    void testGetSessionByIdNotFound() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> exerciseSessionService.getSessionById(1L, 1L));
    }
    
    @Test
    void testGetSessionByIdUnauthorized() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> exerciseSessionService.getSessionById(1L, 999L));
    }
    
    @Test
    void testCreateSession() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(1L)
                .sets(3)
                .reps(10)
                .weight(100.0)
                .duration(600)
                .notes("Good form")
                .sessionDate(LocalDateTime.now())
                .build();
        
        when(userRoutineRepository.findById(1L))
                .thenReturn(Optional.of(userRoutine));
        when(exerciseRepository.findById(1L))
                .thenReturn(Optional.of(exercise));
        when(exerciseSessionRepository.save(any(ExerciseSession.class)))
                .thenReturn(session);
        
        ExerciseSessionDTO result = exerciseSessionService.createSession(request, 1L);
        
        assertNotNull(result);
        assertEquals(3, result.getSets());
        assertEquals(10, result.getReps());
        assertTrue(result.getWeight() > 0);
    }
    
    @Test
    void testCreateSessionRoutineNotFound() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .userRoutineId(999L)
                .exerciseId(1L)
                .sets(3)
                .reps(10)
                .build();
        
        when(userRoutineRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> exerciseSessionService.createSession(request, 1L));
    }
    
    @Test
    void testCreateSessionExerciseNotFound() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(999L)
                .sets(3)
                .reps(10)
                .build();
        
        when(userRoutineRepository.findById(1L))
                .thenReturn(Optional.of(userRoutine));
        when(exerciseRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> exerciseSessionService.createSession(request, 1L));
    }
    
    @Test
    void testUpdateSession() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .sets(4)
                .reps(12)
                .weight(110.0)
                .notes("Improved form")
                .build();
        
        ExerciseSession updated = ExerciseSession.builder()
                .id(1L)
                .userId(1L)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .setsCompleted(4)
                .repsCompleted(12)
                .weightUsed(110.0)
                .notes("Improved form")
                .sessionDate(session.getSessionDate())
                .createdAt(session.getCreatedAt())
                .build();
        
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.of(session));
        when(exerciseSessionRepository.save(any(ExerciseSession.class)))
                .thenReturn(updated);
        
        ExerciseSessionDTO result = exerciseSessionService.updateSession(1L, request, 1L);
        
        assertNotNull(result);
        assertEquals(4, result.getSets());
        assertEquals(12, result.getReps());
    }
    
    @Test
    void testUpdateSessionNotFound() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .sets(4)
                .reps(12)
                .build();
        
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(999L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> exerciseSessionService.updateSession(999L, request, 1L));
    }
    
    @Test
    void testDeleteSession() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.of(session));
        
        exerciseSessionService.deleteSession(1L, 1L);
        
        verify(exerciseSessionRepository, times(1)).findByIdAndUserRoutine_UserId(1L, 1L);
        verify(exerciseSessionRepository, times(1)).delete(session);
    }
    
    @Test
    void testDeleteSessionNotFound() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(999L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> exerciseSessionService.deleteSession(999L, 1L));
    }
}
