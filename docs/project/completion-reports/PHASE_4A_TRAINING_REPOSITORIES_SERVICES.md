# Phase 4a: Training Service - Repositories, DTOs & Services

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Training Service repositories, DTOs, and services with 85%+ test coverage before building controllers.

**Architecture:** Following Auth Service pattern with clear separation of concerns:
- Repositories: Data access via Spring Data JPA
- DTOs: Request/Response objects for APIs
- Services: Business logic layer with @Transactional support
- Tests: Unit tests (mocking repos) + Integration tests (TestContainers)

**Tech Stack:** Spring Boot 3.2.0, PostgreSQL, JUnit 5, Mockito, TestContainers

---

## File Structure

### Files to Create:

#### Repositories:
- `training-service/src/main/java/com/gym/training/repository/DisciplineRepository.java`
- `training-service/src/main/java/com/gym/training/repository/ExerciseRepository.java`
- `training-service/src/main/java/com/gym/training/repository/RoutineTemplateRepository.java`
- `training-service/src/main/java/com/gym/training/repository/UserRoutineRepository.java`
- `training-service/src/main/java/com/gym/training/repository/ExerciseSessionRepository.java`

#### DTOs:
- `training-service/src/main/java/com/gym/training/dto/ExerciseDTO.java`
- `training-service/src/main/java/com/gym/training/dto/ExerciseRequestDTO.java`
- `training-service/src/main/java/com/gym/training/dto/RoutineTemplateDTO.java`
- `training-service/src/main/java/com/gym/training/dto/RoutineTemplateRequestDTO.java`
- `training-service/src/main/java/com/gym/training/dto/UserRoutineDTO.java`
- `training-service/src/main/java/com/gym/training/dto/UserRoutineRequestDTO.java`
- `training-service/src/main/java/com/gym/training/dto/ExerciseSessionDTO.java`
- `training-service/src/main/java/com/gym/training/dto/ExerciseSessionRequestDTO.java`

#### Services:
- `training-service/src/main/java/com/gym/training/service/ExerciseService.java`
- `training-service/src/main/java/com/gym/training/service/RoutineTemplateService.java`
- `training-service/src/main/java/com/gym/training/service/UserRoutineService.java`
- `training-service/src/main/java/com/gym/training/service/ExerciseSessionService.java`

#### Tests:
- `training-service/src/test/java/com/gym/training/repository/ExerciseRepositoryTest.java`
- `training-service/src/test/java/com/gym/training/service/ExerciseServiceTest.java`
- `training-service/src/test/java/com/gym/training/service/RoutineTemplateServiceTest.java`
- `training-service/src/test/java/com/gym/training/service/UserRoutineServiceTest.java`
- `training-service/src/test/java/com/gym/training/service/ExerciseSessionServiceTest.java`
- `training-service/src/test/java/com/gym/training/repository/TestContainerConfig.java`

---

## Task Breakdown

### Task 1: Create Repository Interfaces

**Files:**
- Create: `training-service/src/main/java/com/gym/training/repository/DisciplineRepository.java`
- Create: `training-service/src/main/java/com/gym/training/repository/ExerciseRepository.java`
- Create: `training-service/src/main/java/com/gym/training/repository/RoutineTemplateRepository.java`
- Create: `training-service/src/main/java/com/gym/training/repository/UserRoutineRepository.java`
- Create: `training-service/src/main/java/com/gym/training/repository/ExerciseSessionRepository.java`

- [ ] **Step 1: Create DisciplineRepository**

```java
package com.gym.training.repository;

import com.gym.training.entity.Discipline;
import com.gym.training.entity.DisciplineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {
    Optional<Discipline> findByType(DisciplineType type);
}
```

- [ ] **Step 2: Create ExerciseRepository**

```java
package com.gym.training.repository;

import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseType;
import com.gym.training.entity.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByDiscipline(Discipline discipline);
    List<Exercise> findByType(ExerciseType type);
    List<Exercise> findByCreatedBy(Long userId);
    Optional<Exercise> findByIdAndCreatedBy(Long id, Long userId);
}
```

- [ ] **Step 3: Create RoutineTemplateRepository**

```java
package com.gym.training.repository;

import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineTemplateRepository extends JpaRepository<RoutineTemplate, Long> {
    List<RoutineTemplate> findByType(TemplateType type);
    List<RoutineTemplate> findByCreatedBy(Long userId);
    Optional<RoutineTemplate> findByIdAndCreatedBy(Long id, Long userId);
}
```

- [ ] **Step 4: Create UserRoutineRepository**

```java
package com.gym.training.repository;

import com.gym.training.entity.UserRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoutineRepository extends JpaRepository<UserRoutine, Long> {
    List<UserRoutine> findByUserId(Long userId);
    List<UserRoutine> findByUserIdAndIsActive(Long userId, Boolean isActive);
    Optional<UserRoutine> findByIdAndUserId(Long id, Long userId);
}
```

- [ ] **Step 5: Create ExerciseSessionRepository**

```java
package com.gym.training.repository;

import com.gym.training.entity.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
    List<ExerciseSession> findByUserRoutineId(Long userRoutineId);
    
    @Query("SELECT es FROM ExerciseSession es WHERE es.userRoutine.userId = :userId " +
           "AND DATE(es.sessionDate) = :date")
    List<ExerciseSession> findByUserIdAndDate(
        @Param("userId") Long userId,
        @Param("date") LocalDate date
    );
    
    Optional<ExerciseSession> findByIdAndUserRoutine_UserId(Long id, Long userId);
}
```

- [ ] **Step 6: Verify all repositories compile**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit repositories**

```bash
git add training-service/src/main/java/com/gym/training/repository/
git commit -m "feat: create training service repositories"
```

---

### Task 2: Create DTOs

**Files:**
- Create: `training-service/src/main/java/com/gym/training/dto/ExerciseDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/ExerciseRequestDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/RoutineTemplateDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/RoutineTemplateRequestDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/UserRoutineDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/UserRoutineRequestDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/ExerciseSessionDTO.java`
- Create: `training-service/src/main/java/com/gym/training/dto/ExerciseSessionRequestDTO.java`

- [ ] **Step 1: Create ExerciseDTO**

```java
package com.gym.training.dto;

import com.gym.training.entity.ExerciseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseDTO {
    private Long id;
    private String name;
    private String description;
    private ExerciseType type;
    private Long disciplineId;
    private String disciplineName;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Create ExerciseRequestDTO**

```java
package com.gym.training.dto;

import com.gym.training.entity.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseRequestDTO {
    @NotBlank(message = "Exercise name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Exercise type is required")
    private ExerciseType type;
    
    @NotNull(message = "Discipline ID is required")
    private Long disciplineId;
}
```

- [ ] **Step 3: Create RoutineTemplateDTO**

```java
package com.gym.training.dto;

import com.gym.training.entity.TemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineTemplateDTO {
    private Long id;
    private String name;
    private String description;
    private TemplateType type;
    private Long createdBy;
    private List<Long> exerciseIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create RoutineTemplateRequestDTO**

```java
package com.gym.training.dto;

import com.gym.training.entity.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineTemplateRequestDTO {
    @NotBlank(message = "Template name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Template type is required")
    private TemplateType type;
    
    private List<Long> exerciseIds;
}
```

- [ ] **Step 5: Create UserRoutineDTO**

```java
package com.gym.training.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoutineDTO {
    private Long id;
    private Long userId;
    private Long routineTemplateId;
    private String routineTemplateName;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: Create UserRoutineRequestDTO**

```java
package com.gym.training.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoutineRequestDTO {
    @NotNull(message = "Routine template ID is required")
    private Long routineTemplateId;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    @Builder.Default
    private Boolean isActive = true;
}
```

- [ ] **Step 7: Create ExerciseSessionDTO**

```java
package com.gym.training.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseSessionDTO {
    private Long id;
    private Long userRoutineId;
    private Long exerciseId;
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Double weight;
    private Integer duration;
    private String notes;
    private LocalDateTime sessionDate;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: Create ExerciseSessionRequestDTO**

```java
package com.gym.training.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseSessionRequestDTO {
    @NotNull(message = "User routine ID is required")
    private Long userRoutineId;
    
    @NotNull(message = "Exercise ID is required")
    private Long exerciseId;
    
    @Positive(message = "Sets must be greater than 0")
    private Integer sets;
    
    @Positive(message = "Reps must be greater than 0")
    private Integer reps;
    
    private Double weight;
    
    private Integer duration;
    
    private String notes;
    
    private LocalDateTime sessionDate;
}
```

- [ ] **Step 9: Verify all DTOs compile**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit DTOs**

```bash
git add training-service/src/main/java/com/gym/training/dto/
git commit -m "feat: create training service DTOs"
```

---

### Task 3: Create ExerciseService

**Files:**
- Create: `training-service/src/main/java/com/gym/training/service/ExerciseService.java`

- [ ] **Step 1: Create ExerciseService**

```java
package com.gym.training.service;

import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.entity.Discipline;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseType;
import com.gym.training.repository.DisciplineRepository;
import com.gym.training.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {
    
    private final ExerciseRepository exerciseRepository;
    private final DisciplineRepository disciplineRepository;
    
    /**
     * Get all system exercises
     */
    public List<ExerciseDTO> getAllSystemExercises() {
        log.info("Fetching all system exercises");
        return exerciseRepository.findByType(ExerciseType.SYSTEM)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get exercises by discipline
     */
    public List<ExerciseDTO> getExercisesByDiscipline(Long disciplineId) {
        log.info("Fetching exercises for discipline: {}", disciplineId);
        Discipline discipline = disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found: " + disciplineId));
        
        return exerciseRepository.findByDiscipline(discipline)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get exercises created by a user
     */
    public List<ExerciseDTO> getUserExercises(Long userId) {
        log.info("Fetching exercises created by user: {}", userId);
        return exerciseRepository.findByCreatedBy(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get exercise by ID
     */
    public ExerciseDTO getExerciseById(Long id) {
        log.info("Fetching exercise: {}", id);
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + id));
        return toDTO(exercise);
    }
    
    /**
     * Create a new exercise
     */
    @Transactional
    public ExerciseDTO createExercise(ExerciseRequestDTO request, Long userId) {
        log.info("Creating exercise: {} by user: {}", request.getName(), userId);
        
        Discipline discipline = disciplineRepository.findById(request.getDisciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found: " + request.getDisciplineId()));
        
        Exercise exercise = Exercise.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .discipline(discipline)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Exercise saved = exerciseRepository.save(exercise);
        log.info("Exercise created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update an exercise
     */
    @Transactional
    public ExerciseDTO updateExercise(Long id, ExerciseRequestDTO request, Long userId) {
        log.info("Updating exercise: {} by user: {}", id, userId);
        
        Exercise exercise = exerciseRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found or unauthorized"));
        
        Discipline discipline = disciplineRepository.findById(request.getDisciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));
        
        exercise.setName(request.getName());
        exercise.setDescription(request.getDescription());
        exercise.setType(request.getType());
        exercise.setDiscipline(discipline);
        exercise.setUpdatedAt(LocalDateTime.now());
        
        Exercise updated = exerciseRepository.save(exercise);
        log.info("Exercise updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete an exercise
     */
    @Transactional
    public void deleteExercise(Long id, Long userId) {
        log.info("Deleting exercise: {} by user: {}", id, userId);
        
        Exercise exercise = exerciseRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found or unauthorized"));
        
        exerciseRepository.delete(exercise);
        log.info("Exercise deleted: {}", id);
    }
    
    /**
     * Convert Exercise entity to DTO
     */
    private ExerciseDTO toDTO(Exercise exercise) {
        return ExerciseDTO.builder()
                .id(exercise.getId())
                .name(exercise.getName())
                .description(exercise.getDescription())
                .type(exercise.getType())
                .disciplineId(exercise.getDiscipline().getId())
                .disciplineName(exercise.getDiscipline().getType().toString())
                .createdBy(exercise.getCreatedBy())
                .createdAt(exercise.getCreatedAt())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Verify ExerciseService compiles**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit ExerciseService**

```bash
git add training-service/src/main/java/com/gym/training/service/ExerciseService.java
git commit -m "feat: create ExerciseService"
```

---

### Task 4: Create RoutineTemplateService

**Files:**
- Create: `training-service/src/main/java/com/gym/training/service/RoutineTemplateService.java`

- [ ] **Step 1: Create RoutineTemplateService**

```java
package com.gym.training.service;

import com.gym.training.dto.RoutineTemplateDTO;
import com.gym.training.dto.RoutineTemplateRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.TemplateType;
import com.gym.training.repository.ExerciseRepository;
import com.gym.training.repository.RoutineTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineTemplateService {
    
    private final RoutineTemplateRepository routineTemplateRepository;
    private final ExerciseRepository exerciseRepository;
    
    /**
     * Get all system templates
     */
    public List<RoutineTemplateDTO> getAllSystemTemplates() {
        log.info("Fetching all system routine templates");
        return routineTemplateRepository.findByType(TemplateType.SYSTEM)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get templates created by a user
     */
    public List<RoutineTemplateDTO> getUserTemplates(Long userId) {
        log.info("Fetching routine templates created by user: {}", userId);
        return routineTemplateRepository.findByCreatedBy(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get template by ID
     */
    public RoutineTemplateDTO getTemplateById(Long id) {
        log.info("Fetching routine template: {}", id);
        RoutineTemplate template = routineTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found: " + id));
        return toDTO(template);
    }
    
    /**
     * Create a new routine template
     */
    @Transactional
    public RoutineTemplateDTO createTemplate(RoutineTemplateRequestDTO request, Long userId) {
        log.info("Creating routine template: {} by user: {}", request.getName(), userId);
        
        List<Exercise> exercises = exerciseRepository.findAllById(request.getExerciseIds() != null ? request.getExerciseIds() : List.of());
        
        RoutineTemplate template = RoutineTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .exercises(exercises)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        RoutineTemplate saved = routineTemplateRepository.save(template);
        log.info("Routine template created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update a routine template
     */
    @Transactional
    public RoutineTemplateDTO updateTemplate(Long id, RoutineTemplateRequestDTO request, Long userId) {
        log.info("Updating routine template: {} by user: {}", id, userId);
        
        RoutineTemplate template = routineTemplateRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found or unauthorized"));
        
        List<Exercise> exercises = exerciseRepository.findAllById(request.getExerciseIds() != null ? request.getExerciseIds() : List.of());
        
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setType(request.getType());
        template.setExercises(exercises);
        template.setUpdatedAt(LocalDateTime.now());
        
        RoutineTemplate updated = routineTemplateRepository.save(template);
        log.info("Routine template updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete a routine template
     */
    @Transactional
    public void deleteTemplate(Long id, Long userId) {
        log.info("Deleting routine template: {} by user: {}", id, userId);
        
        RoutineTemplate template = routineTemplateRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found or unauthorized"));
        
        routineTemplateRepository.delete(template);
        log.info("Routine template deleted: {}", id);
    }
    
    /**
     * Convert RoutineTemplate entity to DTO
     */
    private RoutineTemplateDTO toDTO(RoutineTemplate template) {
        return RoutineTemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .type(template.getType())
                .createdBy(template.getCreatedBy())
                .exerciseIds(template.getExercises().stream()
                        .map(Exercise::getId)
                        .collect(Collectors.toList()))
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Verify RoutineTemplateService compiles**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit RoutineTemplateService**

```bash
git add training-service/src/main/java/com/gym/training/service/RoutineTemplateService.java
git commit -m "feat: create RoutineTemplateService"
```

---

### Task 5: Create UserRoutineService

**Files:**
- Create: `training-service/src/main/java/com/gym/training/service/UserRoutineService.java`

- [ ] **Step 1: Create UserRoutineService**

```java
package com.gym.training.service;

import com.gym.training.dto.UserRoutineDTO;
import com.gym.training.dto.UserRoutineRequestDTO;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.UserRoutine;
import com.gym.training.repository.RoutineTemplateRepository;
import com.gym.training.repository.UserRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRoutineService {
    
    private final UserRoutineRepository userRoutineRepository;
    private final RoutineTemplateRepository routineTemplateRepository;
    
    /**
     * Get all active routines for a user
     */
    public List<UserRoutineDTO> getUserActiveRoutines(Long userId) {
        log.info("Fetching active routines for user: {}", userId);
        return userRoutineRepository.findByUserIdAndIsActive(userId, true)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all routines for a user
     */
    public List<UserRoutineDTO> getUserRoutines(Long userId) {
        log.info("Fetching all routines for user: {}", userId);
        return userRoutineRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get routine by ID
     */
    public UserRoutineDTO getRoutineById(Long id, Long userId) {
        log.info("Fetching routine: {} for user: {}", id, userId);
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("User routine not found or unauthorized"));
        return toDTO(routine);
    }
    
    /**
     * Assign a routine template to a user
     */
    @Transactional
    public UserRoutineDTO assignRoutine(UserRoutineRequestDTO request, Long userId) {
        log.info("Assigning routine to user: {}", userId);
        
        RoutineTemplate template = routineTemplateRepository.findById(request.getRoutineTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Routine template not found"));
        
        UserRoutine routine = UserRoutine.builder()
                .userId(userId)
                .routineTemplate(template)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        UserRoutine saved = userRoutineRepository.save(routine);
        log.info("Routine assigned with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update a user routine
     */
    @Transactional
    public UserRoutineDTO updateRoutine(Long id, UserRoutineRequestDTO request, Long userId) {
        log.info("Updating routine: {} for user: {}", id, userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("User routine not found or unauthorized"));
        
        if (request.getRoutineTemplateId() != null) {
            RoutineTemplate template = routineTemplateRepository.findById(request.getRoutineTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Routine template not found"));
            routine.setRoutineTemplate(template);
        }
        
        if (request.getIsActive() != null) {
            routine.setIsActive(request.getIsActive());
        }
        
        if (request.getStartDate() != null) {
            routine.setStartDate(request.getStartDate());
        }
        
        if (request.getEndDate() != null) {
            routine.setEndDate(request.getEndDate());
        }
        
        routine.setUpdatedAt(LocalDateTime.now());
        
        UserRoutine updated = userRoutineRepository.save(routine);
        log.info("Routine updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Deactivate a routine
     */
    @Transactional
    public UserRoutineDTO deactivateRoutine(Long id, Long userId) {
        log.info("Deactivating routine: {} for user: {}", id, userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("User routine not found or unauthorized"));
        
        routine.setIsActive(false);
        routine.setUpdatedAt(LocalDateTime.now());
        
        UserRoutine updated = userRoutineRepository.save(routine);
        log.info("Routine deactivated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete a user routine
     */
    @Transactional
    public void deleteRoutine(Long id, Long userId) {
        log.info("Deleting routine: {} for user: {}", id, userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("User routine not found or unauthorized"));
        
        userRoutineRepository.delete(routine);
        log.info("Routine deleted: {}", id);
    }
    
    /**
     * Convert UserRoutine entity to DTO
     */
    private UserRoutineDTO toDTO(UserRoutine routine) {
        return UserRoutineDTO.builder()
                .id(routine.getId())
                .userId(routine.getUserId())
                .routineTemplateId(routine.getRoutineTemplate().getId())
                .routineTemplateName(routine.getRoutineTemplate().getName())
                .isActive(routine.getIsActive())
                .startDate(routine.getStartDate())
                .endDate(routine.getEndDate())
                .createdAt(routine.getCreatedAt())
                .updatedAt(routine.getUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Verify UserRoutineService compiles**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit UserRoutineService**

```bash
git add training-service/src/main/java/com/gym/training/service/UserRoutineService.java
git commit -m "feat: create UserRoutineService"
```

---

### Task 6: Create ExerciseSessionService

**Files:**
- Create: `training-service/src/main/java/com/gym/training/service/ExerciseSessionService.java`

- [ ] **Step 1: Create ExerciseSessionService**

```java
package com.gym.training.service;

import com.gym.training.dto.ExerciseSessionDTO;
import com.gym.training.dto.ExerciseSessionRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseSession;
import com.gym.training.entity.UserRoutine;
import com.gym.training.repository.ExerciseRepository;
import com.gym.training.repository.ExerciseSessionRepository;
import com.gym.training.repository.UserRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseSessionService {
    
    private final ExerciseSessionRepository exerciseSessionRepository;
    private final UserRoutineRepository userRoutineRepository;
    private final ExerciseRepository exerciseRepository;
    
    /**
     * Get all sessions for a user routine
     */
    public List<ExerciseSessionDTO> getRoutineSessions(Long userRoutineId, Long userId) {
        log.info("Fetching sessions for routine: {}", userRoutineId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(userRoutineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User routine not found or unauthorized"));
        
        return exerciseSessionRepository.findByUserRoutineId(userRoutineId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get sessions for a user on a specific date
     */
    public List<ExerciseSessionDTO> getUserSessionsByDate(Long userId, LocalDate date) {
        log.info("Fetching sessions for user: {} on date: {}", userId, date);
        return exerciseSessionRepository.findByUserIdAndDate(userId, date)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get session by ID
     */
    public ExerciseSessionDTO getSessionById(Long id, Long userId) {
        log.info("Fetching session: {} for user: {}", id, userId);
        ExerciseSession session = exerciseSessionRepository.findByIdAndUserRoutine_UserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise session not found or unauthorized"));
        return toDTO(session);
    }
    
    /**
     * Create a new exercise session (log a workout)
     */
    @Transactional
    public ExerciseSessionDTO createSession(ExerciseSessionRequestDTO request, Long userId) {
        log.info("Creating exercise session for user: {}", userId);
        
        UserRoutine routine = userRoutineRepository.findByIdAndUserId(request.getUserRoutineId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("User routine not found or unauthorized"));
        
        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found"));
        
        ExerciseSession session = ExerciseSession.builder()
                .userRoutine(routine)
                .exercise(exercise)
                .sets(request.getSets())
                .reps(request.getReps())
                .weight(request.getWeight())
                .duration(request.getDuration())
                .notes(request.getNotes())
                .sessionDate(request.getSessionDate() != null ? request.getSessionDate() : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        ExerciseSession saved = exerciseSessionRepository.save(session);
        log.info("Exercise session created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update an exercise session
     */
    @Transactional
    public ExerciseSessionDTO updateSession(Long id, ExerciseSessionRequestDTO request, Long userId) {
        log.info("Updating exercise session: {} for user: {}", id, userId);
        
        ExerciseSession session = exerciseSessionRepository.findByIdAndUserRoutine_UserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise session not found or unauthorized"));
        
        if (request.getSets() != null) {
            session.setSets(request.getSets());
        }
        if (request.getReps() != null) {
            session.setReps(request.getReps());
        }
        if (request.getWeight() != null) {
            session.setWeight(request.getWeight());
        }
        if (request.getDuration() != null) {
            session.setDuration(request.getDuration());
        }
        if (request.getNotes() != null) {
            session.setNotes(request.getNotes());
        }
        if (request.getSessionDate() != null) {
            session.setSessionDate(request.getSessionDate());
        }
        
        ExerciseSession updated = exerciseSessionRepository.save(session);
        log.info("Exercise session updated: {}", id);
        return toDTO(updated);
    }
    
    /**
     * Delete an exercise session
     */
    @Transactional
    public void deleteSession(Long id, Long userId) {
        log.info("Deleting exercise session: {} for user: {}", id, userId);
        
        ExerciseSession session = exerciseSessionRepository.findByIdAndUserRoutine_UserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise session not found or unauthorized"));
        
        exerciseSessionRepository.delete(session);
        log.info("Exercise session deleted: {}", id);
    }
    
    /**
     * Convert ExerciseSession entity to DTO
     */
    private ExerciseSessionDTO toDTO(ExerciseSession session) {
        return ExerciseSessionDTO.builder()
                .id(session.getId())
                .userRoutineId(session.getUserRoutine().getId())
                .exerciseId(session.getExercise().getId())
                .exerciseName(session.getExercise().getName())
                .sets(session.getSets())
                .reps(session.getReps())
                .weight(session.getWeight())
                .duration(session.getDuration())
                .notes(session.getNotes())
                .sessionDate(session.getSessionDate())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Verify ExerciseSessionService compiles**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit ExerciseSessionService**

```bash
git add training-service/src/main/java/com/gym/training/service/ExerciseSessionService.java
git commit -m "feat: create ExerciseSessionService"
```

---

### Task 7: Create Test Configuration & Base Test Class

**Files:**
- Create: `training-service/src/test/java/com/gym/training/config/TestContainerConfig.java`

- [ ] **Step 1: Create TestContainerConfig**

```java
package com.gym.training.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestConfiguration
public class TestContainerConfig {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("gym_training_test")
            .withUsername("test_user")
            .withPassword("test_password");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

- [ ] **Step 2: Verify TestContainerConfig compiles**

Run: `mvn clean compile -pl training-service`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit TestContainerConfig**

```bash
git add training-service/src/test/java/com/gym/training/config/TestContainerConfig.java
git commit -m "test: add TestContainer configuration"
```

---

### Task 8: Create Unit Tests for ExerciseService (85%+ Coverage)

**Files:**
- Create: `training-service/src/test/java/com/gym/training/service/ExerciseServiceTest.java`

- [ ] **Step 1: Create ExerciseServiceTest**

```java
package com.gym.training.service;

import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.entity.Discipline;
import com.gym.training.entity.DisciplineType;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseType;
import com.gym.training.repository.DisciplineRepository;
import com.gym.training.repository.ExerciseRepository;
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
                .type(DisciplineType.Strength)
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
    
    @Test
    void testGetAllSystemExercises() {
        when(exerciseRepository.findByType(ExerciseType.SYSTEM))
                .thenReturn(List.of(pushUp));
        
        List<ExerciseDTO> result = exerciseService.getAllSystemExercises();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Push Up", result.get(0).getName());
        verify(exerciseRepository, times(1)).findByType(ExerciseType.SYSTEM);
    }
    
    @Test
    void testGetExercisesByDiscipline() {
        when(disciplineRepository.findById(1L))
                .thenReturn(Optional.of(strength));
        when(exerciseRepository.findByDiscipline(strength))
                .thenReturn(List.of(pushUp));
        
        List<ExerciseDTO> result = exerciseService.getExercisesByDiscipline(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(disciplineRepository, times(1)).findById(1L);
        verify(exerciseRepository, times(1)).findByDiscipline(strength);
    }
    
    @Test
    void testGetExercisesByDisciplineNotFound() {
        when(disciplineRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            exerciseService.getExercisesByDiscipline(999L);
        });
    }
    
    @Test
    void testGetUserExercises() {
        when(exerciseRepository.findByCreatedBy(1L))
                .thenReturn(List.of(pushUp));
        
        List<ExerciseDTO> result = exerciseService.getUserExercises(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exerciseRepository, times(1)).findByCreatedBy(1L);
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
}
```

- [ ] **Step 2: Run ExerciseServiceTest**

Run: `mvn test -pl training-service -Dtest=ExerciseServiceTest`
Expected: All 10 tests PASS

- [ ] **Step 3: Verify test coverage for ExerciseService**

Run: `mvn jacoco:report -pl training-service`
Expected: See jacoco report at `training-service/target/site/jacoco/index.html`

- [ ] **Step 4: Commit ExerciseServiceTest**

```bash
git add training-service/src/test/java/com/gym/training/service/ExerciseServiceTest.java
git commit -m "test: add unit tests for ExerciseService with 85%+ coverage"
```

---

### Task 9: Create Unit Tests for Other Services (RoutineTemplate, UserRoutine, ExerciseSession)

**Files:**
- Create: `training-service/src/test/java/com/gym/training/service/RoutineTemplateServiceTest.java`
- Create: `training-service/src/test/java/com/gym/training/service/UserRoutineServiceTest.java`
- Create: `training-service/src/test/java/com/gym/training/service/ExerciseSessionServiceTest.java`

- [ ] **Step 1: Create RoutineTemplateServiceTest**

```java
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
        verify(routineTemplateRepository, times(1)).findByType(TemplateType.SYSTEM);
    }
    
    @Test
    void testGetUserTemplates() {
        when(routineTemplateRepository.findByCreatedBy(1L))
                .thenReturn(List.of(begginerTemplate));
        
        List<RoutineTemplateDTO> result = routineTemplateService.getUserTemplates(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(routineTemplateRepository, times(1)).findByCreatedBy(1L);
    }
    
    @Test
    void testGetTemplateById() {
        when(routineTemplateRepository.findById(1L))
                .thenReturn(Optional.of(begginerTemplate));
        
        RoutineTemplateDTO result = routineTemplateService.getTemplateById(1L);
        
        assertNotNull(result);
        assertEquals("Beginner Routine", result.getName());
        verify(routineTemplateRepository, times(1)).findById(1L);
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
        verify(routineTemplateRepository, times(1)).save(any(RoutineTemplate.class));
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
        verify(routineTemplateRepository, times(1)).findByIdAndCreatedBy(1L, 1L);
        verify(routineTemplateRepository, times(1)).save(any(RoutineTemplate.class));
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
```

- [ ] **Step 2: Create UserRoutineServiceTest**

```java
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
        verify(userRoutineRepository, times(1)).findByUserIdAndIsActive(1L, true);
    }
    
    @Test
    void testGetUserRoutines() {
        when(userRoutineRepository.findByUserId(1L))
                .thenReturn(List.of(userRoutine));
        
        List<UserRoutineDTO> result = userRoutineService.getUserRoutines(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRoutineRepository, times(1)).findByUserId(1L);
    }
    
    @Test
    void testGetRoutineById() {
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        
        UserRoutineDTO result = userRoutineService.getRoutineById(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRoutineRepository, times(1)).findByIdAndUserId(1L, 1L);
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
        verify(routineTemplateRepository, times(1)).findById(1L);
        verify(userRoutineRepository, times(1)).save(any(UserRoutine.class));
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
        verify(userRoutineRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(userRoutineRepository, times(1)).save(any(UserRoutine.class));
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
```

- [ ] **Step 3: Create ExerciseSessionServiceTest**

```java
package com.gym.training.service;

import com.gym.training.dto.ExerciseSessionDTO;
import com.gym.training.dto.ExerciseSessionRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseSession;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.UserRoutine;
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
    
    @BeforeEach
    void setUp() {
        exercise = Exercise.builder()
                .id(1L)
                .name("Push Up")
                .build();
        
        userRoutine = UserRoutine.builder()
                .id(1L)
                .userId(1L)
                .build();
        
        session = ExerciseSession.builder()
                .id(1L)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .sets(3)
                .reps(10)
                .weight(0.0)
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetRoutineSessions() {
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        when(exerciseSessionRepository.findByUserRoutineId(1L))
                .thenReturn(List.of(session));
        
        List<ExerciseSessionDTO> result = exerciseSessionService.getRoutineSessions(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getSets());
        verify(exerciseSessionRepository, times(1)).findByUserRoutineId(1L);
    }
    
    @Test
    void testGetUserSessionsByDate() {
        LocalDate today = LocalDate.now();
        when(exerciseSessionRepository.findByUserIdAndDate(1L, today))
                .thenReturn(List.of(session));
        
        List<ExerciseSessionDTO> result = exerciseSessionService.getUserSessionsByDate(1L, today);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exerciseSessionRepository, times(1)).findByUserIdAndDate(1L, today);
    }
    
    @Test
    void testGetSessionById() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.of(session));
        
        ExerciseSessionDTO result = exerciseSessionService.getSessionById(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(exerciseSessionRepository, times(1)).findByIdAndUserRoutine_UserId(1L, 1L);
    }
    
    @Test
    void testCreateSession() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(1L)
                .sets(3)
                .reps(10)
                .weight(0.0)
                .build();
        
        ExerciseSession created = ExerciseSession.builder()
                .id(2L)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .sets(request.getSets())
                .reps(request.getReps())
                .weight(request.getWeight())
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        when(userRoutineRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(userRoutine));
        when(exerciseRepository.findById(1L))
                .thenReturn(Optional.of(exercise));
        when(exerciseSessionRepository.save(any(ExerciseSession.class)))
                .thenReturn(created);
        
        ExerciseSessionDTO result = exerciseSessionService.createSession(request, 1L);
        
        assertNotNull(result);
        assertEquals(3, result.getSets());
        verify(exerciseSessionRepository, times(1)).save(any(ExerciseSession.class));
    }
    
    @Test
    void testUpdateSession() {
        ExerciseSessionRequestDTO request = ExerciseSessionRequestDTO.builder()
                .sets(4)
                .reps(15)
                .weight(10.0)
                .build();
        
        ExerciseSession updated = ExerciseSession.builder()
                .id(1L)
                .userRoutine(userRoutine)
                .exercise(exercise)
                .sets(request.getSets())
                .reps(request.getReps())
                .weight(request.getWeight())
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.of(session));
        when(exerciseSessionRepository.save(any(ExerciseSession.class)))
                .thenReturn(updated);
        
        ExerciseSessionDTO result = exerciseSessionService.updateSession(1L, request, 1L);
        
        assertNotNull(result);
        assertEquals(4, result.getSets());
        verify(exerciseSessionRepository, times(1)).save(any(ExerciseSession.class));
    }
    
    @Test
    void testDeleteSession() {
        when(exerciseSessionRepository.findByIdAndUserRoutine_UserId(1L, 1L))
                .thenReturn(Optional.of(session));
        
        exerciseSessionService.deleteSession(1L, 1L);
        
        verify(exerciseSessionRepository, times(1)).findByIdAndUserRoutine_UserId(1L, 1L);
        verify(exerciseSessionRepository, times(1)).delete(session);
    }
}
```

- [ ] **Step 4: Run all service tests**

Run: `mvn test -pl training-service -Dtest=*ServiceTest`
Expected: All tests PASS (40+ tests total)

- [ ] **Step 5: Verify combined test coverage (target: 85%+)**

Run: `mvn jacoco:report -pl training-service`
Expected: View report at `training-service/target/site/jacoco/index.html`

- [ ] **Step 6: Commit all service tests**

```bash
git add training-service/src/test/java/com/gym/training/service/
git commit -m "test: add comprehensive unit tests for all services with 85%+ coverage"
```

---

### Task 10: Create Repository Integration Tests

**Files:**
- Create: `training-service/src/test/java/com/gym/training/repository/ExerciseRepositoryTest.java`

- [ ] **Step 1: Create ExerciseRepositoryTest with TestContainers**

```java
package com.gym.training.repository;

import com.gym.training.config.TestContainerConfig;
import com.gym.training.entity.Discipline;
import com.gym.training.entity.DisciplineType;
import com.gym.training.entity.Exercise;
import com.gym.training.entity.ExerciseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

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
    
    private Discipline strength;
    private Exercise pushUp;
    
    @BeforeEach
    void setUp() {
        // Create and save discipline
        strength = Discipline.builder()
                .type(DisciplineType.Strength)
                .build();
        strength = disciplineRepository.save(strength);
        
        // Create and save exercise
        pushUp = Exercise.builder()
                .name("Push Up")
                .description("Upper body exercise")
                .type(ExerciseType.SYSTEM)
                .discipline(strength)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        pushUp = exerciseRepository.save(pushUp);
    }
    
    @Test
    void testSaveExercise() {
        Exercise squat = Exercise.builder()
                .name("Squat")
                .type(ExerciseType.SYSTEM)
                .discipline(strength)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Exercise saved = exerciseRepository.save(squat);
        
        assertNotNull(saved.getId());
        assertEquals("Squat", saved.getName());
    }
    
    @Test
    void testFindByType() {
        List<Exercise> systemExercises = exerciseRepository.findByType(ExerciseType.SYSTEM);
        
        assertNotNull(systemExercises);
        assertTrue(systemExercises.size() > 0);
        assertTrue(systemExercises.stream().anyMatch(e -> e.getName().equals("Push Up")));
    }
    
    @Test
    void testFindByDiscipline() {
        List<Exercise> strengthExercises = exerciseRepository.findByDiscipline(strength);
        
        assertNotNull(strengthExercises);
        assertEquals(1, strengthExercises.size());
        assertEquals("Push Up", strengthExercises.get(0).getName());
    }
    
    @Test
    void testFindByCreatedBy() {
        Long userId = 1L;
        Exercise userExercise = Exercise.builder()
                .name("Custom Exercise")
                .type(ExerciseType.USER)
                .discipline(strength)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        exerciseRepository.save(userExercise);
        
        List<Exercise> userExercises = exerciseRepository.findByCreatedBy(userId);
        
        assertEquals(1, userExercises.size());
        assertEquals("Custom Exercise", userExercises.get(0).getName());
    }
    
    @Test
    void testFindByIdAndCreatedBy() {
        Long userId = 1L;
        Exercise userExercise = Exercise.builder()
                .name("User Exercise")
                .type(ExerciseType.USER)
                .discipline(strength)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userExercise = exerciseRepository.save(userExercise);
        
        Optional<Exercise> found = exerciseRepository.findByIdAndCreatedBy(userExercise.getId(), userId);
        
        assertTrue(found.isPresent());
        assertEquals("User Exercise", found.get().getName());
    }
    
    @Test
    void testFindByIdAndCreatedByNotFound() {
        Optional<Exercise> found = exerciseRepository.findByIdAndCreatedBy(999L, 1L);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void testDeleteExercise() {
        Long id = pushUp.getId();
        exerciseRepository.delete(pushUp);
        
        Optional<Exercise> found = exerciseRepository.findById(id);
        assertFalse(found.isPresent());
    }
}
```

- [ ] **Step 2: Run ExerciseRepositoryTest with TestContainers**

Run: `mvn test -pl training-service -Dtest=ExerciseRepositoryTest`
Expected: All tests PASS (TestContainers starts PostgreSQL automatically)

- [ ] **Step 3: Commit repository integration tests**

```bash
git add training-service/src/test/java/com/gym/training/repository/ExerciseRepositoryTest.java
git commit -m "test: add integration tests for ExerciseRepository with TestContainers"
```

---

### Task 11: Verify Overall Test Coverage (85%+)

**Files:**
- No new files, just verify existing coverage

- [ ] **Step 1: Run all tests for Training Service**

Run: `mvn clean test -pl training-service`
Expected: All tests PASS

Output should show:
```
Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

- [ ] **Step 2: Generate JaCoCo coverage report**

Run: `mvn jacoco:report -pl training-service`
Expected: Report generated at `training-service/target/site/jacoco/index.html`

- [ ] **Step 3: Verify coverage meets 85% target**

Check the report:
- Line Coverage: >= 85%
- Branch Coverage: >= 80%
- Method Coverage: >= 85%

If coverage is below target, add additional tests for uncovered lines/branches.

- [ ] **Step 4: Run full build to ensure everything compiles and tests pass**

Run: `mvn clean package -pl training-service -DskipTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "test: verify Phase 4a complete with 85%+ test coverage"
```

---

## Summary

✅ **Phase 4a Complete:**
- ✅ 5 Repositories created (DisciplineRepository, ExerciseRepository, RoutineTemplateRepository, UserRoutineRepository, ExerciseSessionRepository)
- ✅ 8 DTOs created (Request/Response objects for all entities)
- ✅ 4 Services created (ExerciseService, RoutineTemplateService, UserRoutineService, ExerciseSessionService)
- ✅ 40+ Unit Tests created with 85%+ coverage
- ✅ Integration tests with TestContainers
- ✅ All tests passing
- ✅ Code committed to git

**Next Phase:** Phase 4b - Controllers & API Endpoints (will build on this foundation)

---

## Coverage Checklist

- [ ] ExerciseService: 85%+ coverage
- [ ] RoutineTemplateService: 85%+ coverage
- [ ] UserRoutineService: 85%+ coverage
- [ ] ExerciseSessionService: 85%+ coverage
- [ ] ExerciseRepository: Integration tests passing
- [ ] All services: CRUD operations tested
- [ ] All edge cases: Not found, unauthorized, invalid input scenarios tested
