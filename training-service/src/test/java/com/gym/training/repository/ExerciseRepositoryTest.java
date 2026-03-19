package com.gym.training.repository;

import com.gym.training.config.TestContainerConfig;
import com.gym.training.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
class ExerciseRepositoryTest {
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @Autowired
    private DisciplineRepository disciplineRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private Discipline discipline;
    private Exercise exercise;
    
    @BeforeEach
    void setUp() {
        discipline = Discipline.builder()
                .name("Chest")
                .description("Chest exercises")
                .type(Discipline.DisciplineType.STRENGTH)
                .build();
        disciplineRepository.save(discipline);
        
        exercise = Exercise.builder()
                .name("Bench Press")
                .description("Barbell bench press for chest")
                .type(Exercise.ExerciseType.SYSTEM)
                .discipline(discipline)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();
        exerciseRepository.save(exercise);
        
        entityManager.flush();
    }
    
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void testFindByType() {
        List<Exercise> result = exerciseRepository.findByType(Exercise.ExerciseType.SYSTEM, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(e -> e.getId().equals(exercise.getId())));
    }
    
    @Test
    void testFindByTypeNotFound() {
        List<Exercise> result = exerciseRepository.findByType(Exercise.ExerciseType.USER, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByDiscipline() {
        List<Exercise> result = exerciseRepository.findByDiscipline(discipline, pageable).getContent();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bench Press", result.get(0).getName());
    }
    
    @Test
    void testFindByDisciplineEmpty() {
        Discipline newDiscipline = Discipline.builder()
                .name("Back")
                .description("Back exercises")
                .type(Discipline.DisciplineType.STRENGTH)
                .build();
        disciplineRepository.save(newDiscipline);
        
        List<Exercise> result = exerciseRepository.findByDiscipline(newDiscipline, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByCreatedBy() {
        List<Exercise> result = exerciseRepository.findByCreatedBy(1L, pageable).getContent();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCreatedBy());
    }
    
    @Test
    void testFindByCreatedByNotFound() {
        List<Exercise> result = exerciseRepository.findByCreatedBy(999L, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIdAndCreatedBy() {
        Optional<Exercise> result = exerciseRepository.findByIdAndCreatedBy(exercise.getId(), 1L);
        
        assertTrue(result.isPresent());
        assertEquals("Bench Press", result.get().getName());
    }
    
    @Test
    void testFindByIdAndCreatedByUnauthorized() {
        Optional<Exercise> result = exerciseRepository.findByIdAndCreatedBy(exercise.getId(), 999L);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIdAndCreatedByNotFound() {
        Optional<Exercise> result = exerciseRepository.findByIdAndCreatedBy(999L, 1L);
        
        assertTrue(result.isEmpty());
    }
}
