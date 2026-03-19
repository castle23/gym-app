# Phase 5A: Tracking Service - Repositories, DTOs & Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all repositories, DTOs, and services for the Tracking Service with full CRUD operations, authorization checks, and 85%+ test coverage.

**Architecture:** Following the established Training Service pattern with clear separation of concerns (Repository → Service → DTO). Each service validates user ownership and includes comprehensive logging. All entities feature Lombok builders for easy test data creation. Domain entities are managed by Spring Data JPA with custom query methods for complex lookups.

**Tech Stack:** Spring Boot 3.2.0, Spring Data JPA, PostgreSQL, JUnit 5, Mockito, Lombok, Jakarta Persistence API

---

## File Structure

### Repositories (8 interfaces - `tracking-service/src/main/java/com/gym/tracking/repository/`)
- `MeasurementTypeRepository.java` - Query measurement types (system and custom)
- `MeasurementValueRepository.java` - Query user measurements over time
- `ObjectiveRepository.java` - Query user objectives
- `PlanRepository.java` - Query user plans with status and date filtering
- `TrainingComponentRepository.java` - Query training plan components
- `DietComponentRepository.java` - Query diet plan components
- `RecommendationRepository.java` - Query recommendations by component
- `DietLogRepository.java` - Query diet logs by date range

### DTOs (16 classes - `tracking-service/src/main/java/com/gym/tracking/dto/`)
- Response DTOs: `*DTO.java` (output from services)
- Request DTOs: `*RequestDTO.java` (input to services)
- One pair for each of 8 entities

### Services (7 classes - `tracking-service/src/main/java/com/gym/tracking/service/`)
- `MeasurementService.java` - Track measurements, manage measurement types
- `ObjectiveService.java` - CRUD operations for user objectives
- `PlanService.java` - Create/update/query plans with status management
- `TrainingComponentService.java` - Manage training aspects of plans
- `DietComponentService.java` - Manage diet aspects of plans
- `RecommendationService.java` - Create and manage recommendations
- `DietLogService.java` - Log daily food intake, query by date

### Tests (60+ tests - `tracking-service/src/test/java/com/gym/tracking/`)
- Service unit tests: 8-10 tests per service (7 services = 60+ tests)
- Repository integration tests (optional, lower priority)
- Target: 85%+ line/method coverage per service

---

## Task 1: Create Repositories

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/MeasurementTypeRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/MeasurementValueRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/ObjectiveRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/PlanRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/TrainingComponentRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/DietComponentRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/RecommendationRepository.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/repository/DietLogRepository.java`

- [ ] **Step 1: Create MeasurementTypeRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.MeasurementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementTypeRepository extends JpaRepository<MeasurementType, Long> {
    List<MeasurementType> findByType(String type);
    Optional<MeasurementType> findByTypeAndSystemType(String type, Boolean isSystem);
}
```

- [ ] **Step 2: Create MeasurementValueRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.MeasurementValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeasurementValueRepository extends JpaRepository<MeasurementValue, Long> {
    List<MeasurementValue> findByUserIdAndMeasurementTypeId(Long userId, Long measurementTypeId);
    List<MeasurementValue> findByUserIdAndMeasurementTypeIdAndMeasurementDateBetween(
            Long userId, Long measurementTypeId, LocalDate startDate, LocalDate endDate);
    List<MeasurementValue> findByUserId(Long userId);
}
```

- [ ] **Step 3: Create ObjectiveRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.Objective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, Long> {
    List<Objective> findByUserId(Long userId);
    Optional<Objective> findByIdAndUserId(Long id, Long userId);
    List<Objective> findByUserIdAndIsActive(Long userId, Boolean isActive);
}
```

- [ ] **Step 4: Create PlanRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserId(Long userId);
    Optional<Plan> findByIdAndUserId(Long id, Long userId);
    Optional<Plan> findByUserIdAndStatus(Long userId, Plan.PlanStatus status);
    List<Plan> findByUserIdAndStatus(Long userId, Plan.PlanStatus status);
}
```

- [ ] **Step 5: Create TrainingComponentRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.TrainingComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingComponentRepository extends JpaRepository<TrainingComponent, Long> {
    Optional<TrainingComponent> findByPlanId(Long planId);
}
```

- [ ] **Step 6: Create DietComponentRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.DietComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DietComponentRepository extends JpaRepository<DietComponent, Long> {
    Optional<DietComponent> findByPlanId(Long planId);
}
```

- [ ] **Step 7: Create RecommendationRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByTrainingComponentId(Long trainingComponentId);
    List<Recommendation> findByDietComponentId(Long dietComponentId);
}
```

- [ ] **Step 8: Create DietLogRepository**

```java
package com.gym.tracking.repository;

import com.gym.tracking.entity.DietLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DietLogRepository extends JpaRepository<DietLog, Long> {
    List<DietLog> findByUserId(Long userId);
    List<DietLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);
    List<DietLog> findByUserIdAndLogDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
```

- [ ] **Step 9: Commit repositories**

```bash
git add tracking-service/src/main/java/com/gym/tracking/repository/
git commit -m "feat(phase-5a): add 8 repository interfaces for tracking service"
```

---

## Task 2: Create Response DTOs

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/MeasurementTypeDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/MeasurementValueDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/ObjectiveDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/PlanDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/TrainingComponentDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/DietComponentDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/RecommendationDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/DietLogDTO.java`

- [ ] **Step 1: Create MeasurementTypeDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementTypeDTO {
    private Long id;
    private String type;
    private String unit;
    private Boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Create MeasurementValueDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementValueDTO {
    private Long id;
    private Long userId;
    private Long measurementTypeId;
    private String measurementType;
    private Double value;
    private LocalDate measurementDate;
    private String notes;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Create ObjectiveDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectiveDTO {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create PlanDTO**

```java
package com.gym.tracking.dto;

import com.gym.tracking.entity.Plan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDTO {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Long objectiveId;
    private String objectiveTitle;
    private Plan.PlanStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: Create TrainingComponentDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingComponentDTO {
    private Long id;
    private Long planId;
    private String focus;
    private String intensity;
    private Integer frequencyPerWeek;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: Create DietComponentDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietComponentDTO {
    private Long id;
    private Long planId;
    private String dietType;
    private Integer dailyCalories;
    private String macroDistribution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 7: Create RecommendationDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    private Long id;
    private Long trainingComponentId;
    private Long dietComponentId;
    private String title;
    private String description;
    private String professionalName;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: Create DietLogDTO**

```java
package com.gym.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietLogDTO {
    private Long id;
    private Long userId;
    private LocalDate logDate;
    private String meal;
    private String foodItems;
    private Double calories;
    private String macros;
    private String notes;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 9: Commit response DTOs**

```bash
git add tracking-service/src/main/java/com/gym/tracking/dto/*DTO.java
git commit -m "feat(phase-5a): add 8 response DTOs for tracking service"
```

---

## Task 3: Create Request DTOs

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/MeasurementTypeRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/MeasurementValueRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/ObjectiveRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/PlanRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/TrainingComponentRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/DietComponentRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/RecommendationRequestDTO.java`
- Create: `tracking-service/src/main/java/com/gym/tracking/dto/DietLogRequestDTO.java`

- [ ] **Step 1: Create MeasurementTypeRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementTypeRequestDTO {
    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Unit is required")
    private String unit;

    private Boolean isSystem;
}
```

- [ ] **Step 2: Create MeasurementValueRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementValueRequestDTO {
    @NotNull(message = "Measurement type ID is required")
    private Long measurementTypeId;

    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    private Double value;

    @NotNull(message = "Measurement date is required")
    private LocalDate measurementDate;

    private String notes;
}
```

- [ ] **Step 3: Create ObjectiveRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectiveRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private Boolean isActive;
}
```

- [ ] **Step 4: Create PlanRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
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
public class PlanRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Objective ID is required")
    private Long objectiveId;

    @NotNull(message = "Status is required")
    private String status;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
```

- [ ] **Step 5: Create TrainingComponentRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingComponentRequestDTO {
    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Focus is required")
    private String focus;

    @NotBlank(message = "Intensity is required")
    private String intensity;

    @NotNull(message = "Frequency per week is required")
    @Positive(message = "Frequency must be positive")
    private Integer frequencyPerWeek;
}
```

- [ ] **Step 6: Create DietComponentRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietComponentRequestDTO {
    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Diet type is required")
    private String dietType;

    @NotNull(message = "Daily calories is required")
    @Positive(message = "Daily calories must be positive")
    private Integer dailyCalories;

    @NotBlank(message = "Macro distribution is required")
    private String macroDistribution;
}
```

- [ ] **Step 7: Create RecommendationRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequestDTO {
    private Long trainingComponentId;
    private Long dietComponentId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Professional name is required")
    private String professionalName;
}
```

- [ ] **Step 8: Create DietLogRequestDTO**

```java
package com.gym.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietLogRequestDTO {
    @NotNull(message = "Log date is required")
    private LocalDate logDate;

    @NotBlank(message = "Meal is required")
    private String meal;

    @NotBlank(message = "Food items are required")
    private String foodItems;

    @NotNull(message = "Calories are required")
    @Positive(message = "Calories must be positive")
    private Double calories;

    private String macros;
    private String notes;
}
```

- [ ] **Step 9: Commit request DTOs**

```bash
git add tracking-service/src/main/java/com/gym/tracking/dto/*RequestDTO.java
git commit -m "feat(phase-5a): add 8 request DTOs with validation for tracking service"
```

---

## Task 4: Create MeasurementService

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/service/MeasurementService.java`
- Create: `tracking-service/src/test/java/com/gym/tracking/service/MeasurementServiceTest.java`

- [ ] **Step 1: Create MeasurementService class**

```java
package com.gym.tracking.service;

import com.gym.tracking.dto.MeasurementTypeDTO;
import com.gym.tracking.dto.MeasurementTypeRequestDTO;
import com.gym.tracking.dto.MeasurementValueDTO;
import com.gym.tracking.dto.MeasurementValueRequestDTO;
import com.gym.tracking.entity.MeasurementType;
import com.gym.tracking.entity.MeasurementValue;
import com.gym.tracking.repository.MeasurementTypeRepository;
import com.gym.tracking.repository.MeasurementValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeasurementService {
    
    private final MeasurementTypeRepository measurementTypeRepository;
    private final MeasurementValueRepository measurementValueRepository;
    
    /**
     * Get all measurement types
     */
    public List<MeasurementTypeDTO> getAllMeasurementTypes() {
        log.info("Fetching all measurement types");
        return measurementTypeRepository.findAll()
                .stream()
                .map(this::toMeasurementTypeDTO)
                .toList();
    }
    
    /**
     * Get measurement type by ID
     */
    public MeasurementTypeDTO getMeasurementTypeById(Long id) {
        log.info("Fetching measurement type: {}", id);
        MeasurementType type = measurementTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Measurement type not found: " + id));
        return toMeasurementTypeDTO(type);
    }
    
    /**
     * Create new measurement type
     */
    @Transactional
    public MeasurementTypeDTO createMeasurementType(MeasurementTypeRequestDTO request) {
        log.info("Creating measurement type: {}", request.getType());
        
        MeasurementType type = MeasurementType.builder()
                .type(request.getType())
                .unit(request.getUnit())
                .isSystem(request.getIsSystem() != null ? request.getIsSystem() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        MeasurementType saved = measurementTypeRepository.save(type);
        log.info("Measurement type created with ID: {}", saved.getId());
        return toMeasurementTypeDTO(saved);
    }
    
    /**
     * Get user measurement values
     */
    public List<MeasurementValueDTO> getUserMeasurements(Long userId) {
        log.info("Fetching measurements for user: {}", userId);
        return measurementValueRepository.findByUserId(userId)
                .stream()
                .map(this::toMeasurementValueDTO)
                .toList();
    }
    
    /**
     * Get measurement values by type for user
     */
    public List<MeasurementValueDTO> getUserMeasurementsByType(Long userId, Long measurementTypeId) {
        log.info("Fetching measurements for user: {} and type: {}", userId, measurementTypeId);
        verifyMeasurementTypeExists(measurementTypeId);
        return measurementValueRepository.findByUserIdAndMeasurementTypeId(userId, measurementTypeId)
                .stream()
                .map(this::toMeasurementValueDTO)
                .toList();
    }
    
    /**
     * Get measurement values by date range
     */
    public List<MeasurementValueDTO> getUserMeasurementsByDateRange(
            Long userId, Long measurementTypeId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching measurements for user: {} from {} to {}", userId, startDate, endDate);
        verifyMeasurementTypeExists(measurementTypeId);
        return measurementValueRepository.findByUserIdAndMeasurementTypeIdAndMeasurementDateBetween(
                userId, measurementTypeId, startDate, endDate)
                .stream()
                .map(this::toMeasurementValueDTO)
                .toList();
    }
    
    /**
     * Record user measurement
     */
    @Transactional
    public MeasurementValueDTO recordMeasurement(Long userId, MeasurementValueRequestDTO request) {
        log.info("Recording measurement for user: {}", userId);
        
        verifyMeasurementTypeExists(request.getMeasurementTypeId());
        MeasurementType type = measurementTypeRepository.findById(request.getMeasurementTypeId()).get();
        
        MeasurementValue value = MeasurementValue.builder()
                .userId(userId)
                .measurementType(type)
                .value(request.getValue())
                .measurementDate(request.getMeasurementDate())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
        
        MeasurementValue saved = measurementValueRepository.save(value);
        log.info("Measurement recorded with ID: {}", saved.getId());
        return toMeasurementValueDTO(saved);
    }
    
    /**
     * Delete measurement value
     */
    @Transactional
    public void deleteMeasurement(Long userId, Long measurementId) {
        log.info("Deleting measurement: {} for user: {}", measurementId, userId);
        
        MeasurementValue value = measurementValueRepository.findById(measurementId)
                .orElseThrow(() -> new IllegalArgumentException("Measurement not found: " + measurementId));
        
        if (!value.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to delete this measurement");
        }
        
        measurementValueRepository.delete(value);
        log.info("Measurement deleted: {}", measurementId);
    }
    
    /**
     * Verify measurement type exists
     */
    private void verifyMeasurementTypeExists(Long measurementTypeId) {
        if (!measurementTypeRepository.existsById(measurementTypeId)) {
            throw new IllegalArgumentException("Measurement type not found: " + measurementTypeId);
        }
    }
    
    /**
     * Convert MeasurementType entity to DTO
     */
    private MeasurementTypeDTO toMeasurementTypeDTO(MeasurementType type) {
        return MeasurementTypeDTO.builder()
                .id(type.getId())
                .type(type.getType())
                .unit(type.getUnit())
                .isSystem(type.getIsSystem())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert MeasurementValue entity to DTO
     */
    private MeasurementValueDTO toMeasurementValueDTO(MeasurementValue value) {
        return MeasurementValueDTO.builder()
                .id(value.getId())
                .userId(value.getUserId())
                .measurementTypeId(value.getMeasurementType().getId())
                .measurementType(value.getMeasurementType().getType())
                .value(value.getValue())
                .measurementDate(value.getMeasurementDate())
                .notes(value.getNotes())
                .createdAt(value.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Write unit tests for MeasurementService**

```java
package com.gym.tracking.service;

import com.gym.tracking.dto.MeasurementTypeDTO;
import com.gym.tracking.dto.MeasurementTypeRequestDTO;
import com.gym.tracking.dto.MeasurementValueDTO;
import com.gym.tracking.dto.MeasurementValueRequestDTO;
import com.gym.tracking.entity.MeasurementType;
import com.gym.tracking.entity.MeasurementValue;
import com.gym.tracking.repository.MeasurementTypeRepository;
import com.gym.tracking.repository.MeasurementValueRepository;
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
class MeasurementServiceTest {
    
    @Mock
    private MeasurementTypeRepository measurementTypeRepository;
    
    @Mock
    private MeasurementValueRepository measurementValueRepository;
    
    @InjectMocks
    private MeasurementService measurementService;
    
    private MeasurementType weight;
    private MeasurementValue weightValue;
    
    @BeforeEach
    void setUp() {
        weight = MeasurementType.builder()
                .id(1L)
                .type("weight")
                .unit("kg")
                .isSystem(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        weightValue = MeasurementValue.builder()
                .id(1L)
                .userId(1L)
                .measurementType(weight)
                .value(75.5)
                .measurementDate(LocalDate.now())
                .notes("Morning weight")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetAllMeasurementTypes() {
        when(measurementTypeRepository.findAll())
                .thenReturn(List.of(weight));
        
        List<MeasurementTypeDTO> result = measurementService.getAllMeasurementTypes();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("weight", result.get(0).getType());
        verify(measurementTypeRepository, times(1)).findAll();
    }
    
    @Test
    void testGetMeasurementTypeById_Success() {
        when(measurementTypeRepository.findById(1L))
                .thenReturn(Optional.of(weight));
        
        MeasurementTypeDTO result = measurementService.getMeasurementTypeById(1L);
        
        assertNotNull(result);
        assertEquals("weight", result.getType());
    }
    
    @Test
    void testGetMeasurementTypeById_NotFound() {
        when(measurementTypeRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
                () -> measurementService.getMeasurementTypeById(999L));
    }
    
    @Test
    void testCreateMeasurementType() {
        MeasurementTypeRequestDTO request = MeasurementTypeRequestDTO.builder()
                .type("height")
                .unit("cm")
                .isSystem(true)
                .build();
        
        MeasurementType saved = MeasurementType.builder()
                .id(2L)
                .type("height")
                .unit("cm")
                .isSystem(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(measurementTypeRepository.save(any(MeasurementType.class)))
                .thenReturn(saved);
        
        MeasurementTypeDTO result = measurementService.createMeasurementType(request);
        
        assertNotNull(result);
        assertEquals("height", result.getType());
        verify(measurementTypeRepository, times(1)).save(any(MeasurementType.class));
    }
    
    @Test
    void testGetUserMeasurements() {
        when(measurementValueRepository.findByUserId(1L))
                .thenReturn(List.of(weightValue));
        
        List<MeasurementValueDTO> result = measurementService.getUserMeasurements(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(75.5, result.get(0).getValue());
    }
    
    @Test
    void testGetUserMeasurementsByType() {
        when(measurementTypeRepository.existsById(1L))
                .thenReturn(true);
        when(measurementValueRepository.findByUserIdAndMeasurementTypeId(1L, 1L))
                .thenReturn(List.of(weightValue));
        
        List<MeasurementValueDTO> result = measurementService.getUserMeasurementsByType(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testRecordMeasurement() {
        MeasurementValueRequestDTO request = MeasurementValueRequestDTO.builder()
                .measurementTypeId(1L)
                .value(76.0)
                .measurementDate(LocalDate.now())
                .notes("Afternoon weight")
                .build();
        
        when(measurementTypeRepository.existsById(1L))
                .thenReturn(true);
        when(measurementTypeRepository.findById(1L))
                .thenReturn(Optional.of(weight));
        when(measurementValueRepository.save(any(MeasurementValue.class)))
                .thenReturn(weightValue);
        
        MeasurementValueDTO result = measurementService.recordMeasurement(1L, request);
        
        assertNotNull(result);
        assertEquals(75.5, result.getValue());
    }
    
    @Test
    void testDeleteMeasurement() {
        when(measurementValueRepository.findById(1L))
                .thenReturn(Optional.of(weightValue));
        doNothing().when(measurementValueRepository).delete(any(MeasurementValue.class));
        
        measurementService.deleteMeasurement(1L, 1L);
        
        verify(measurementValueRepository, times(1)).delete(any(MeasurementValue.class));
    }
    
    @Test
    void testDeleteMeasurement_Unauthorized() {
        when(measurementValueRepository.findById(1L))
                .thenReturn(Optional.of(weightValue));
        
        assertThrows(IllegalArgumentException.class, 
                () -> measurementService.deleteMeasurement(2L, 1L));
    }
    
    @Test
    void testGetUserMeasurementsByDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(measurementTypeRepository.existsById(1L))
                .thenReturn(true);
        when(measurementValueRepository.findByUserIdAndMeasurementTypeIdAndMeasurementDateBetween(
                1L, 1L, startDate, endDate))
                .thenReturn(List.of(weightValue));
        
        List<MeasurementValueDTO> result = measurementService.getUserMeasurementsByDateRange(
                1L, 1L, startDate, endDate);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
```

- [ ] **Step 3: Commit MeasurementService**

```bash
git add tracking-service/src/main/java/com/gym/tracking/service/MeasurementService.java
git add tracking-service/src/test/java/com/gym/tracking/service/MeasurementServiceTest.java
git commit -m "feat(phase-5a): add MeasurementService with 8 unit tests"
```

---

## Task 5: Create ObjectiveService

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/service/ObjectiveService.java`
- Create: `tracking-service/src/test/java/com/gym/tracking/service/ObjectiveServiceTest.java`

- [ ] **Step 1: Create ObjectiveService**

```java
package com.gym.tracking.service;

import com.gym.tracking.dto.ObjectiveDTO;
import com.gym.tracking.dto.ObjectiveRequestDTO;
import com.gym.tracking.entity.Objective;
import com.gym.tracking.repository.ObjectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObjectiveService {
    
    private final ObjectiveRepository objectiveRepository;
    
    /**
     * Get all objectives for a user
     */
    public List<ObjectiveDTO> getUserObjectives(Long userId) {
        log.info("Fetching all objectives for user: {}", userId);
        return objectiveRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get active objectives for a user
     */
    public List<ObjectiveDTO> getActiveObjectives(Long userId) {
        log.info("Fetching active objectives for user: {}", userId);
        return objectiveRepository.findByUserIdAndIsActive(userId, true)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get objective by ID
     */
    public ObjectiveDTO getObjectiveById(Long objectiveId, Long userId) {
        log.info("Fetching objective: {} for user: {}", objectiveId, userId);
        Objective objective = objectiveRepository.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found or unauthorized"));
        return toDTO(objective);
    }
    
    /**
     * Create new objective
     */
    @Transactional
    public ObjectiveDTO createObjective(Long userId, ObjectiveRequestDTO request) {
        log.info("Creating objective for user: {}", userId);
        
        Objective objective = Objective.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Objective saved = objectiveRepository.save(objective);
        log.info("Objective created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update objective
     */
    @Transactional
    public ObjectiveDTO updateObjective(Long objectiveId, Long userId, ObjectiveRequestDTO request) {
        log.info("Updating objective: {} for user: {}", objectiveId, userId);
        
        Objective objective = objectiveRepository.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found or unauthorized"));
        
        objective.setTitle(request.getTitle());
        objective.setDescription(request.getDescription());
        objective.setCategory(request.getCategory());
        if (request.getIsActive() != null) {
            objective.setIsActive(request.getIsActive());
        }
        objective.setUpdatedAt(LocalDateTime.now());
        
        Objective updated = objectiveRepository.save(objective);
        log.info("Objective updated: {}", objectiveId);
        return toDTO(updated);
    }
    
    /**
     * Delete objective
     */
    @Transactional
    public void deleteObjective(Long objectiveId, Long userId) {
        log.info("Deleting objective: {} for user: {}", objectiveId, userId);
        
        Objective objective = objectiveRepository.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found or unauthorized"));
        
        objectiveRepository.delete(objective);
        log.info("Objective deleted: {}", objectiveId);
    }
    
    /**
     * Convert Objective entity to DTO
     */
    private ObjectiveDTO toDTO(Objective objective) {
        return ObjectiveDTO.builder()
                .id(objective.getId())
                .userId(objective.getUserId())
                .title(objective.getTitle())
                .description(objective.getDescription())
                .category(objective.getCategory())
                .isActive(objective.getIsActive())
                .createdAt(objective.getCreatedAt())
                .updatedAt(objective.getUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Write unit tests for ObjectiveService**

```java
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
```

- [ ] **Step 3: Commit ObjectiveService**

```bash
git add tracking-service/src/main/java/com/gym/tracking/service/ObjectiveService.java
git add tracking-service/src/test/java/com/gym/tracking/service/ObjectiveServiceTest.java
git commit -m "feat(phase-5a): add ObjectiveService with 8 unit tests"
```

---

## Task 6: Create PlanService

**Files:**
- Create: `tracking-service/src/main/java/com/gym/tracking/service/PlanService.java`
- Create: `tracking-service/src/test/java/com/gym/tracking/service/PlanServiceTest.java`

- [ ] **Step 1: Create PlanService**

```java
package com.gym.tracking.service;

import com.gym.tracking.dto.PlanDTO;
import com.gym.tracking.dto.PlanRequestDTO;
import com.gym.tracking.entity.Objective;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.repository.ObjectiveRepository;
import com.gym.tracking.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {
    
    private final PlanRepository planRepository;
    private final ObjectiveRepository objectiveRepository;
    
    /**
     * Get all user plans
     */
    public List<PlanDTO> getUserPlans(Long userId) {
        log.info("Fetching all plans for user: {}", userId);
        return planRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get active plan for user
     */
    public PlanDTO getActivePlan(Long userId) {
        log.info("Fetching active plan for user: {}", userId);
        Plan plan = planRepository.findByUserIdAndStatus(userId, Plan.PlanStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active plan found for user"));
        return toDTO(plan);
    }
    
    /**
     * Get plans by status
     */
    public List<PlanDTO> getPlansByStatus(Long userId, Plan.PlanStatus status) {
        log.info("Fetching plans with status {} for user: {}", status, userId);
        return planRepository.findByUserIdAndStatus(userId, status)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * Get plan by ID
     */
    public PlanDTO getPlanById(Long planId, Long userId) {
        log.info("Fetching plan: {} for user: {}", planId, userId);
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        return toDTO(plan);
    }
    
    /**
     * Create new plan
     */
    @Transactional
    public PlanDTO createPlan(Long userId, PlanRequestDTO request) {
        log.info("Creating plan for user: {}", userId);
        
        Objective objective = objectiveRepository.findById(request.getObjectiveId())
                .orElseThrow(() -> new IllegalArgumentException("Objective not found: " + request.getObjectiveId()));
        
        if (!objective.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Objective does not belong to user");
        }
        
        Plan plan = Plan.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .objective(objective)
                .status(Plan.PlanStatus.valueOf(request.getStatus()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Plan saved = planRepository.save(plan);
        log.info("Plan created with ID: {}", saved.getId());
        return toDTO(saved);
    }
    
    /**
     * Update plan
     */
    @Transactional
    public PlanDTO updatePlan(Long planId, Long userId, PlanRequestDTO request) {
        log.info("Updating plan: {} for user: {}", planId, userId);
        
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setStatus(Plan.PlanStatus.valueOf(request.getStatus()));
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setUpdatedAt(LocalDateTime.now());
        
        Plan updated = planRepository.save(plan);
        log.info("Plan updated: {}", planId);
        return toDTO(updated);
    }
    
    /**
     * Delete plan
     */
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        log.info("Deleting plan: {} for user: {}", planId, userId);
        
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or unauthorized"));
        
        planRepository.delete(plan);
        log.info("Plan deleted: {}", planId);
    }
    
    /**
     * Convert Plan entity to DTO
     */
    private PlanDTO toDTO(Plan plan) {
        return PlanDTO.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .name(plan.getName())
                .description(plan.getDescription())
                .objectiveId(plan.getObjective() != null ? plan.getObjective().getId() : null)
                .objectiveTitle(plan.getObjective() != null ? plan.getObjective().getTitle() : null)
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 2: Write unit tests for PlanService**

```java
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
        when(planRepository.findByUserIdAndStatus(1L, Plan.PlanStatus.ACTIVE))
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
}
```

- [ ] **Step 3: Commit PlanService**

```bash
git add tracking-service/src/main/java/com/gym/tracking/service/PlanService.java
git add tracking-service/src/test/java/com/gym/tracking/service/PlanServiceTest.java
git commit -m "feat(phase-5a): add PlanService with 8 unit tests"
```

---

## Task 7: Create Remaining Services (TrainingComponent, DietComponent, Recommendation, DietLog)

Due to length constraints, remaining services follow identical pattern to above. Each service includes:
- CRUD operations
- Authorization checks (userId validation)
- Proper logging
- 8-10 unit tests per service

- [ ] **Create TrainingComponentService** (similar pattern)
- [ ] **Create TrainingComponentServiceTest** (8 tests)
- [ ] **Create DietComponentService** (similar pattern)
- [ ] **Create DietComponentServiceTest** (8 tests)
- [ ] **Create RecommendationService** (similar pattern)
- [ ] **Create RecommendationServiceTest** (8 tests)
- [ ] **Create DietLogService** (similar pattern)
- [ ] **Create DietLogServiceTest** (8 tests)

- [ ] **Commit all remaining services**

```bash
git add tracking-service/src/main/java/com/gym/tracking/service/
git add tracking-service/src/test/java/com/gym/tracking/service/
git commit -m "feat(phase-5a): add TrainingComponent, DietComponent, Recommendation, DietLog services with 32 tests"
```

---

## Task 8: Run Tests & Verify Coverage

- [ ] **Run all unit tests**

```bash
mvn clean test -pl tracking-service -q
```

Expected: All 60+ tests pass

- [ ] **Generate JaCoCo coverage report**

```bash
mvn jacoco:report -pl tracking-service
```

- [ ] **Verify coverage metrics**

Expected: Line coverage >= 85%, Method coverage >= 85%

- [ ] **View coverage report**

Open: `tracking-service/target/site/jacoco/index.html`

---

## Task 9: Final Commit

- [ ] **Verify all files are created and tests pass**

```bash
git status
git log --oneline -5
```

- [ ] **Final commit**

```bash
git add .
git commit -m "feat: complete Phase 5a - Tracking Service repositories, DTOs, and services with 85%+ coverage"
```

---

## Expected Output

✅ 8 Repositories created and tested
✅ 16 DTOs created (request + response for each entity)
✅ 7 Services created with full CRUD operations
✅ 60+ Unit tests created with 85%+ coverage
✅ All tests passing
✅ Code committed to git
✅ Ready for Phase 5B (Controllers)

