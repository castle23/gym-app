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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
class ExerciseSessionRepositoryTest {
    
    @Autowired
    private ExerciseSessionRepository exerciseSessionRepository;
    
    @Autowired
    private UserRoutineRepository userRoutineRepository;
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @Autowired
    private DisciplineRepository disciplineRepository;
    
    @Autowired
    private RoutineTemplateRepository routineTemplateRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private ExerciseSession session;
    private UserRoutine userRoutine;
    private Exercise exercise;
    
    @BeforeEach
    void setUp() {
        // Create discipline
        Discipline discipline = Discipline.builder()
                .name("Chest")
                .description("Chest exercises")
                .type(Discipline.DisciplineType.STRENGTH)
                .build();
        disciplineRepository.save(discipline);
        
        // Create exercise
        exercise = Exercise.builder()
                .name("Bench Press")
                .description("Barbell bench press")
                .type(Exercise.ExerciseType.SYSTEM)
                .discipline(discipline)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();
        exerciseRepository.save(exercise);
        
        // Create routine template
        RoutineTemplate template = RoutineTemplate.builder()
                .name("Beginner")
                .description("Beginner routine")
                .type(RoutineTemplate.TemplateType.SYSTEM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        routineTemplateRepository.save(template);
        
        // Create user routine
        userRoutine = UserRoutine.builder()
                .userId(1L)
                .routineTemplate(template)
                .isActive(true)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRoutineRepository.save(userRoutine);
        
        // Create exercise session
        session = ExerciseSession.builder()
                .userId(1L)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .setsCompleted(3)
                .repsCompleted(10)
                .weightUsed(100.0)
                .durationSeconds(600L)
                .notes("Good form")
                .sessionDate(LocalDateTime.now())
                .build();
        exerciseSessionRepository.save(session);
        
        entityManager.flush();
    }
    
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void testFindByUserRoutineId() {
        List<ExerciseSession> result = exerciseSessionRepository.findByUserRoutineId(userRoutine.getId(), pageable).getContent();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userRoutine.getId(), result.get(0).getUserRoutine().getId());
    }
    
    @Test
    void testFindByUserRoutineIdEmpty() {
        List<ExerciseSession> result = exerciseSessionRepository.findByUserRoutineId(999L, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByUserIdAndDate() {
        LocalDate today = LocalDate.now();
        List<ExerciseSession> result = exerciseSessionRepository.findByUserIdAndDate(1L, today, pageable).getContent();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }
    
    @Test
    void testFindByUserIdAndDateDifferentDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<ExerciseSession> result = exerciseSessionRepository.findByUserIdAndDate(1L, tomorrow, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIdAndUserRoutineUserId() {
        Optional<ExerciseSession> result = exerciseSessionRepository.findByIdAndUserRoutine_UserId(session.getId(), 1L);
        
        assertTrue(result.isPresent());
        assertEquals(session.getId(), result.get().getId());
        assertEquals(1L, result.get().getUserRoutine().getUserId());
    }
    
    @Test
    void testFindByIdAndUserRoutineUserIdUnauthorized() {
        Optional<ExerciseSession> result = exerciseSessionRepository.findByIdAndUserRoutine_UserId(session.getId(), 999L);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIdAndUserRoutineUserIdNotFound() {
        Optional<ExerciseSession> result = exerciseSessionRepository.findByIdAndUserRoutine_UserId(999L, 1L);
        
        assertTrue(result.isEmpty());
    }
}
