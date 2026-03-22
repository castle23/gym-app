package com.gym.training.service;

import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.entity.Discipline;
import com.gym.training.entity.Discipline.DisciplineType;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.Exercise.ExerciseType;
import com.gym.training.repository.DisciplineRepository;
import com.gym.training.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.isNull;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {
    
    @Mock
    private ExerciseRepository exerciseRepository;
    
    @Mock
    private DisciplineRepository disciplineRepository;
    
    @InjectMocks
    private ExerciseService exerciseService;
    
    private Discipline strength;
    private Exercise pushUp;
    
    @BeforeEach
    void setUp() {
        strength = Discipline.builder()
                .id(1L)
                .type(DisciplineType.STRENGTH)
                .build();
        
        pushUp = Exercise.builder()
                .id(1L)
                .name("Push Up")
                .description("Upper body exercise")
                .type(ExerciseType.SYSTEM)
                .discipline(strength)
                .createdBy(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void testGetAllSystemExercises() {
        when(exerciseRepository.findByType(ExerciseType.SYSTEM, pageable))
                .thenReturn(new PageImpl<>(List.of(pushUp)));
        
        List<ExerciseDTO> result = exerciseService.getAllSystemExercises(pageable).getData();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Push Up", result.get(0).getName());
        verify(exerciseRepository, times(1)).findByType(ExerciseType.SYSTEM, pageable);
    }
    
    @Test
    void testGetExercisesByDiscipline() {
        when(disciplineRepository.findById(1L))
                .thenReturn(Optional.of(strength));
        when(exerciseRepository.findByDiscipline(strength, pageable))
                .thenReturn(new PageImpl<>(List.of(pushUp)));
        
        List<ExerciseDTO> result = exerciseService.getExercisesByDiscipline(1L, pageable).getData();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(disciplineRepository, times(1)).findById(1L);
        verify(exerciseRepository, times(1)).findByDiscipline(strength, pageable);
    }
    
    @Test
    void testGetExercisesByDisciplineNotFound() {
        when(disciplineRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () ->
            exerciseService.getExercisesByDiscipline(999L, pageable));
    }
    
    @Test
    void testGetUserExercises() {
        when(exerciseRepository.findByCreatedBy(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(pushUp)));
        
        List<ExerciseDTO> result = exerciseService.getUserExercises(1L, pageable).getData();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exerciseRepository, times(1)).findByCreatedBy(1L, pageable);
    }
    
    @Test
    void testGetExerciseById() {
        when(exerciseRepository.findById(1L))
                .thenReturn(Optional.of(pushUp));
        
        ExerciseDTO result = exerciseService.getExerciseById(1L);
        
        assertNotNull(result);
        assertEquals("Push Up", result.getName());
        verify(exerciseRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetExerciseByIdNotFound() {
        when(exerciseRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            exerciseService.getExerciseById(999L);
        });
    }
    
    @Test
    void testCreateExercise() {
        ExerciseRequestDTO request = ExerciseRequestDTO.builder()
                .name("Squat")
                .description("Lower body exercise")
                .type(ExerciseType.PROFESSIONAL)
                .disciplineId(1L)
                .build();
        
        Exercise newExercise = Exercise.builder()
                .id(2L)
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .discipline(strength)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(disciplineRepository.findById(1L))
                .thenReturn(Optional.of(strength));
        when(exerciseRepository.save(any(Exercise.class)))
                .thenReturn(newExercise);
        
        ExerciseDTO result = exerciseService.createExercise(request, 1L);
        
        assertNotNull(result);
        assertEquals("Squat", result.getName());
        assertEquals(ExerciseType.PROFESSIONAL, result.getType());
        verify(disciplineRepository, times(1)).findById(1L);
        verify(exerciseRepository, times(1)).save(any(Exercise.class));
    }
    
    @Test
    void testCreateExerciseDisciplineNotFound() {
        ExerciseRequestDTO request = ExerciseRequestDTO.builder()
                .name("Squat")
                .disciplineId(999L)
                .type(ExerciseType.PROFESSIONAL)
                .build();
        
        when(disciplineRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            exerciseService.createExercise(request, 1L);
        });
    }
    
    @Test
    void testUpdateExercise() {
        ExerciseRequestDTO request = ExerciseRequestDTO.builder()
                .name("Updated Push Up")
                .description("Updated description")
                .type(ExerciseType.SYSTEM)
                .disciplineId(1L)
                .build();
        
        Exercise updatedExercise = Exercise.builder()
                .id(1L)
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .discipline(strength)
                .createdBy(1L)
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(exerciseRepository.findByIdAndCreatedBy(1L, 1L))
                .thenReturn(Optional.of(pushUp));
        when(disciplineRepository.findById(1L))
                .thenReturn(Optional.of(strength));
        when(exerciseRepository.save(any(Exercise.class)))
                .thenReturn(updatedExercise);
        
        ExerciseDTO result = exerciseService.updateExercise(1L, request, 1L);
        
        assertNotNull(result);
        assertEquals("Updated Push Up", result.getName());
        verify(exerciseRepository, times(1)).findByIdAndCreatedBy(1L, 1L);
        verify(exerciseRepository, times(1)).save(any(Exercise.class));
    }
    
    @Test
    void testUpdateExerciseNotFound() {
        ExerciseRequestDTO request = ExerciseRequestDTO.builder()
                .name("Updated")
                .disciplineId(1L)
                .type(ExerciseType.SYSTEM)
                .build();
        
        when(exerciseRepository.findByIdAndCreatedBy(999L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            exerciseService.updateExercise(999L, request, 1L);
        });
    }
    
    @Test
    void testDeleteExercise() {
        when(exerciseRepository.findByIdAndCreatedBy(1L, 1L))
                .thenReturn(Optional.of(pushUp));
        
        exerciseService.deleteExercise(1L, 1L);
        
        verify(exerciseRepository, times(1)).findByIdAndCreatedBy(1L, 1L);
        verify(exerciseRepository, times(1)).delete(pushUp);
    }
    
    @Test
    void testDeleteExerciseNotFound() {
        when(exerciseRepository.findByIdAndCreatedBy(999L, 1L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            exerciseService.deleteExercise(999L, 1L);
        });
    }

    // --- searchExercises ---

    @Test
    void shouldReturnMatchingExercisesWhenSearchByName() {
        // Arrange
        when(exerciseRepository.searchByNameAndType(eq("push"), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pushUp)));

        // Act
        List<ExerciseDTO> result = exerciseService.searchExercises("push", null, pageable).getData();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Push Up", result.get(0).getName());
        verify(exerciseRepository, times(1)).searchByNameAndType("push", null, pageable);
    }

    @Test
    void shouldReturnMatchingExercisesWhenSearchByType() {
        // Arrange
        when(exerciseRepository.searchByNameAndType(isNull(), eq(ExerciseType.SYSTEM), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pushUp)));

        // Act
        List<ExerciseDTO> result = exerciseService.searchExercises(null, ExerciseType.SYSTEM, pageable).getData();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ExerciseType.SYSTEM, result.get(0).getType());
        verify(exerciseRepository, times(1)).searchByNameAndType(null, ExerciseType.SYSTEM, pageable);
    }

    @Test
    void shouldReturnAllExercisesWhenSearchWithNoFilters() {
        // Arrange
        when(exerciseRepository.searchByNameAndType(isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pushUp)));

        // Act
        List<ExerciseDTO> result = exerciseService.searchExercises(null, null, pageable).getData();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exerciseRepository, times(1)).searchByNameAndType(null, null, pageable);
    }

    @Test
    void shouldNormalizeBlankNameToNullWhenSearching() {
        // Arrange — blank string should be treated the same as null
        when(exerciseRepository.searchByNameAndType(isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pushUp)));

        // Act
        exerciseService.searchExercises("   ", null, pageable);

        // Assert — blank name normalized to null
        verify(exerciseRepository, times(1)).searchByNameAndType(null, null, pageable);
    }

    @Test
    void shouldReturnEmptyPageWhenSearchMatchesNothing() {
        // Arrange
        when(exerciseRepository.searchByNameAndType(eq("xyz"), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        List<ExerciseDTO> result = exerciseService.searchExercises("xyz", null, pageable).getData();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
