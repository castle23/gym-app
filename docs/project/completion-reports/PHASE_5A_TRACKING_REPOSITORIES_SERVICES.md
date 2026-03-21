# Phase 5a: Tracking Service - Repositories, DTOs & Services

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete Tracking Service repositories, DTOs, and services with 85%+ test coverage.

**Architecture:** Following Auth Service and Training Service pattern with clear separation of concerns.

**Tech Stack:** Spring Boot 3.2.0, PostgreSQL, JUnit 5, Mockito, TestContainers

---

## File Structure

### Repositories:
- `MeasurementTypeRepository.java`
- `MeasurementValueRepository.java`
- `ObjectiveRepository.java`
- `PlanRepository.java`
- `TrainingComponentRepository.java`
- `DietComponentRepository.java`
- `RecommendationRepository.java`
- `DietLogRepository.java`

### DTOs:
- `MeasurementTypeDTO.java`, `MeasurementTypeRequestDTO.java`
- `MeasurementValueDTO.java`, `MeasurementValueRequestDTO.java`
- `ObjectiveDTO.java`, `ObjectiveRequestDTO.java`
- `PlanDTO.java`, `PlanRequestDTO.java`
- `TrainingComponentDTO.java`, `TrainingComponentRequestDTO.java`
- `DietComponentDTO.java`, `DietComponentRequestDTO.java`
- `RecommendationDTO.java`, `RecommendationRequestDTO.java`
- `DietLogDTO.java`, `DietLogRequestDTO.java`

### Services:
- `MeasurementService.java`
- `ObjectiveService.java`
- `PlanService.java`
- `TrainingComponentService.java`
- `DietComponentService.java`
- `RecommendationService.java`
- `DietLogService.java`

### Tests:
- Service unit tests (7 services × 8-10 tests each = 60+ tests)
- Repository integration tests with TestContainers
- Target: 85%+ coverage

---

## Implementation Steps (Abbreviated)

### Step 1-2: Create Repositories

```java
// Example for PlanRepository
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserId(Long userId);
    Optional<Plan> findByUserIdAndStatus(Long userId, PlanStatus status);
    Optional<Plan> findByIdAndUserId(Long id, Long userId);
}

// Follow same pattern for all 8 repositories
// Include custom query methods for complex lookups
```

### Step 2-3: Create DTOs

Create request/response DTOs for all 8 entities following Auth Service pattern:
- Use @NotNull, @NotBlank for validation
- Use builders for complex objects
- Separate Request (input) and Response (output) DTOs

### Step 3-4: Create Services

Implement CRUD + business logic for:
- **MeasurementService:** Track user measurements over time
- **ObjectiveService:** Manage fitness objectives
- **PlanService:** Create/update user plans with components
- **TrainingComponentService:** Manage training aspects of plans
- **DietComponentService:** Manage diet aspects of plans
- **RecommendationService:** Create recommendations for components
- **DietLogService:** Log daily food intake

Each service should have:
- @Transactional annotations
- Proper authorization checks (userId validation)
- Clear logging
- Error handling

### Step 4-5: Create Unit Tests

For each service, create:
- 8-10 unit tests with Mockito (mocking repositories)
- Test CRUD operations
- Test authorization scenarios
- Test edge cases and error conditions
- Target: 85%+ line/method coverage per service

### Step 5-6: Create Integration Tests

- TestContainers configuration for real PostgreSQL
- Repository integration tests
- End-to-end service tests with database

### Step 6-7: Verify Coverage

Run: `mvn clean test jacoco:report -pl tracking-service`
Verify: Line coverage >= 85%, Branch coverage >= 80%, Method coverage >= 85%

### Step 7-8: Commit All Changes

```bash
git add tracking-service/
git commit -m "feat: complete Phase 5a - Tracking Service repos, DTOs, services with 85%+ coverage"
```

---

## Key Design Notes

1. **Plan with Components:**
   - A Plan can have: 0-1 TrainingComponent + 0-1 DietComponent
   - Each component has its own professional and recommendation set

2. **Measurements:**
   - MeasurementType defines what can be tracked (weight, body_fat%, etc.)
   - MeasurementValue stores actual measurements over time
   - Allows unlimited custom measurement types

3. **Recommendations:**
   - Always tied to a component (training or diet)
   - Never standalone
   - Created by professionals for specific components

4. **Authorization:**
   - All operations require userId validation
   - Users can only access their own plans/measurements/logs

---

## Expected Output

✅ 8 Repositories created and tested
✅ 16 DTOs created (request + response for each entity)
✅ 7 Services created with full CRUD operations
✅ 60+ Unit tests created with 85%+ coverage
✅ Integration tests with TestContainers
✅ All tests passing
✅ Code committed to git
