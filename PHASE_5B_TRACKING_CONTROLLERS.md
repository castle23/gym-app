# Phase 5b: Tracking Service - Controllers & Integration Tests

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete Tracking Service controllers with REST endpoints and integration tests, achieving 85%+ end-to-end coverage.

**Architecture:** REST controllers following Training Service pattern with proper error handling and validation.

---

## Controllers to Create

1. **MeasurementController** - Get/log measurements
2. **ObjectiveController** - Create/manage objectives
3. **PlanController** - CRUD plans with components
4. **TrainingComponentController** - Manage training components
5. **DietComponentController** - Manage diet components
6. **RecommendationController** - Get/create recommendations
7. **DietLogController** - Log daily food intake

---

## Implementation Steps (Abbreviated)

### Step 1: Create Controllers

For each controller:
- Map HTTP methods to service operations
- Extract userId from X-User-Id header
- Handle validation errors gracefully
- Return appropriate HTTP status codes

Example pattern:
```java
@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
public class MeasurementController {
    
    private final MeasurementService measurementService;
    
    @GetMapping
    public ResponseEntity<List<MeasurementValueDTO>> getUserMeasurements(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(measurementService.getUserMeasurements(userId));
    }
    
    @PostMapping
    public ResponseEntity<MeasurementValueDTO> recordMeasurement(
            @Valid @RequestBody MeasurementValueRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            MeasurementValueDTO result = measurementService.recordMeasurement(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // ... other CRUD endpoints
}
```

Repeat for all 7 controllers.

### Step 2: Create Controller Tests

For each controller:
- Use @WebMvcTest(XyzController.class)
- Mock the service layer
- Test all HTTP methods (GET, POST, PUT, DELETE)
- Test error scenarios and edge cases
- Use MockMvc for assertions

Expected: 50+ controller tests

### Step 3: Integration Tests

- Test controllers with real database (TestContainers)
- Test request validation
- Test authorization (userId header checks)
- Test service layer integration

### Step 4: Verify Coverage

Run: `mvn clean test jacoco:report -pl tracking-service`
Target: 85%+ coverage across all layers

### Step 5: Commit All Changes

```bash
git add tracking-service/
git commit -m "feat: complete Phase 5b - Tracking Service controllers, REST API and 85%+ coverage"
```

---

## Expected Endpoints

### Measurement Endpoints
- `GET /api/v1/measurements` - Get user measurements
- `GET /api/v1/measurements/{typeId}` - Get measurements for a type
- `POST /api/v1/measurements` - Record measurement
- `PUT /api/v1/measurements/{id}` - Update measurement
- `DELETE /api/v1/measurements/{id}` - Delete measurement

### Objective Endpoints
- `GET /api/v1/objectives` - Get user objectives
- `GET /api/v1/objectives/{id}` - Get objective
- `POST /api/v1/objectives` - Create objective
- `PUT /api/v1/objectives/{id}` - Update objective
- `DELETE /api/v1/objectives/{id}` - Delete objective

### Plan Endpoints
- `GET /api/v1/plans` - Get user plans
- `GET /api/v1/plans/{id}` - Get plan
- `POST /api/v1/plans` - Create plan
- `PUT /api/v1/plans/{id}` - Update plan
- `DELETE /api/v1/plans/{id}` - Delete plan

### Component Endpoints
- `POST /api/v1/plans/{id}/training-component` - Add training component
- `POST /api/v1/plans/{id}/diet-component` - Add diet component
- Similar for update/delete

### Diet Log Endpoints
- `GET /api/v1/diet-logs/date/{date}` - Get logs for date
- `POST /api/v1/diet-logs` - Record meal
- `PUT /api/v1/diet-logs/{id}` - Update log
- `DELETE /api/v1/diet-logs/{id}` - Delete log

### Recommendation Endpoints
- `GET /api/v1/components/{componentId}/recommendations` - Get recommendations
- `POST /api/v1/components/{componentId}/recommendations` - Create recommendation

---

## Expected Output

✅ 7 Controllers created with all CRUD endpoints
✅ 50+ Controller tests with MockMvc
✅ Integration tests with TestContainers
✅ 85%+ end-to-end coverage
✅ All tests passing
✅ Code committed to git

**Tracking Service is now COMPLETE and PRODUCTION-READY**
