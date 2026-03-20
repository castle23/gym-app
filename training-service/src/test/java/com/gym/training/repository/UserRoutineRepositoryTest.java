package com.gym.training.repository;

import com.gym.training.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRoutineRepositoryTest {
    
    @Autowired
    private UserRoutineRepository userRoutineRepository;
    
    @Autowired
    private RoutineTemplateRepository routineTemplateRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private RoutineTemplate template;
    private UserRoutine userRoutine;
    
    @BeforeEach
    void setUp() {
        template = RoutineTemplate.builder()
                .name("Beginner")
                .description("Beginner routine")
                .type(RoutineTemplate.TemplateType.SYSTEM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        routineTemplateRepository.save(template);
        
        userRoutine = UserRoutine.builder()
                .userId(1L)
                .routineTemplate(template)
                .isActive(true)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRoutineRepository.save(userRoutine);
        
        entityManager.flush();
    }
    
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void testFindByUserId() {
        List<UserRoutine> result = userRoutineRepository.findByUserId(1L, pageable).getContent();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }
    
    @Test
    void testFindByUserIdNotFound() {
        List<UserRoutine> result = userRoutineRepository.findByUserId(999L, pageable).getContent();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByUserIdAndIsActive() {
        List<UserRoutine> result = userRoutineRepository.findByUserIdAndIsActive(1L, true);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }
    
    @Test
    void testFindByUserIdAndIsActiveInactive() {
        List<UserRoutine> result = userRoutineRepository.findByUserIdAndIsActive(1L, false);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIdAndUserId() {
        Optional<UserRoutine> result = userRoutineRepository.findByIdAndUserId(userRoutine.getId(), 1L);
        
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getUserId());
    }
    
    @Test
    void testFindByIdAndUserIdUnauthorized() {
        Optional<UserRoutine> result = userRoutineRepository.findByIdAndUserId(userRoutine.getId(), 999L);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIdAndUserIdNotFound() {
        Optional<UserRoutine> result = userRoutineRepository.findByIdAndUserId(999L, 1L);
        
        assertTrue(result.isEmpty());
    }
}
