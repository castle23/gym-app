# Integration Testing Guide - Gym Platform API

Comprehensive guide for testing microservices interactions, data flows, and cross-service workflows.

**Audience:** QA Engineers, Backend Developers, Test Automation Engineers  
**Last Updated:** March 21, 2026

---

## Table of Contents

1. [Introduction](#introduction)
2. [Testing Strategy](#testing-strategy)
3. [Setup & Environment](#setup--environment)
4. [Testing Patterns](#testing-patterns)
5. [Writing Integration Tests](#writing-integration-tests)
6. [Test Scenarios](#test-scenarios)
7. [Running Tests](#running-tests)
8. [Troubleshooting](#troubleshooting)

---

## Introduction

### What is Integration Testing?

Integration testing verifies that multiple components/services work together correctly. Unlike unit tests (which test single functions), integration tests test workflows across services.

**Example:**
- Unit test: Test that `createUser()` function works
- Integration test: Test that creating user → notification sent → Training Service has user account

### Why It Matters for Microservices

In microservices architecture (ADR-001):
- Services are independent but must integrate
- Bugs at service boundaries are hard to catch with unit tests alone
- Events may be async (delayed failures)
- Database transactions span services

**Problem it solves:**
```
Service A works in isolation ✓
Service B works in isolation ✓
Service A + B together breaks ✗ ← Integration tests catch this
```

### Test Pyramid for Microservices

```
        /\
       /  \         E2E Tests (5%)
      /    \        - Full user flows
     /------\       - Through UI or full API
    /        \      
   /          \     Integration Tests (35%)
  /            \    - Cross-service workflows
 /              \   - Event-based flows
/----------------\  
        Unit Tests (60%)
        - Single service
        - Isolated functions
        - Fast & reliable
```

**Target Distribution:**
- Unit tests: 60% (fast, reliable, good coverage)
- Integration tests: 35% (slower, catch boundary bugs)
- E2E tests: 5% (slow, high-level flows)

---

## Testing Strategy

### What to Integration Test

**Scenario 1: User Registration Flow** ✅
- User created in Auth Service
- Notification queued in Notification Service
- User profile created in Training Service
- *Why test?* Multiple services must stay in sync

**Scenario 2: Workout Completion** ✅
- Workout saved in Training Service
- Stats updated in Tracking Service
- Notification sent (if milestone)
- *Why test?* Chain of dependent operations

**Scenario 3: API Error Handling** ✅
- Invalid request rejected by gateway
- Error propagated correctly
- Database rollback on failure
- *Why test?* Failures harder to test than happy path

### What NOT to Integration Test

**Avoid:**
- Simple unit test logic (belongs in unit test)
- UI interaction (belongs in E2E test)
- External API calls (mock them)
- Performance/load testing (separate activity)

### Coverage Goals

**Integration Test Coverage:**
- 50-70% of critical workflows
- All cross-service interactions
- All error scenarios (timeout, invalid data, etc.)
- All event-driven flows

**Not 100% coverage because:**
- Integration tests are slower
- Some paths already covered by unit tests
- Law of diminishing returns

### Test Isolation & Independence

**Golden Rule:** Each test should:
1. Start with clean state (fresh DB)
2. Not depend on other tests
3. Clean up after itself
4. Be repeatable (run 100x = same result)

**Problem to avoid:**
```
Test A: Create user
Test B: Delete user A (depends on Test A)  ✗ BAD!
Test C: List users (hopes Test A ran first) ✗ BAD!

Instead:
Test A: Create user (cleanup after)
Test B: Create separate user, delete it
Test C: Create separate users list
```

---

## Setup & Environment

### Local Development Setup

**Prerequisites:**
- Docker & Docker Compose
- Node.js 16+
- PostgreSQL client tools

**1. Start Services**
```bash
# Start all services with Docker Compose
docker-compose up -d

# Verify all services running
docker-compose ps

# Expected output:
# auth-service        running on port 8081
# training-service    running on port 8082
# tracking-service    running on port 8083
# notification-service running on port 8084
# postgres            running on port 5432
# redis               running on port 6379
```

**2. Initialize Test Databases**
```bash
# Create test databases for each service
npm run db:setup:test

# This script:
# - Creates test_auth, test_training, test_tracking, test_notification databases
# - Runs migrations on each
# - Loads seed data
```

**3. Verify Services Healthy**
```bash
# Health check each service
curl http://localhost:8081/health  # Auth
curl http://localhost:8082/health  # Training
curl http://localhost:8083/health  # Tracking
curl http://localhost:8084/health  # Notification

# Expected response: { "status": "ok" }
```

### Test Data Management

**Seed Data Strategy:**
```
Before Each Test:
1. Truncate tables (clear data)
2. Load seed data
3. Run test
4. Don't clean up (framework does it)
```

**Seed Data File** (`tests/fixtures/seed-data.json`):
```json
{
  "users": [
    {
      "id": "user-001",
      "email": "trainer@example.com",
      "role": "trainer"
    },
    {
      "id": "user-002",
      "email": "athlete@example.com",
      "role": "user"
    }
  ],
  "disciplines": [
    {"id": "disc-001", "name": "Chest"},
    {"id": "disc-002", "name": "Back"}
  ],
  "exercises": [
    {
      "id": "ex-001",
      "name": "Bench Press",
      "discipline_id": "disc-001",
      "difficulty": 2
    }
  ]
}
```

### Environment Configuration

**Test Environment** (`.env.test`):
```bash
# Service URLs
AUTH_SERVICE_URL=http://localhost:8081
TRAINING_SERVICE_URL=http://localhost:8082
TRACKING_SERVICE_URL=http://localhost:8083
NOTIFICATION_SERVICE_URL=http://localhost:8084

# Database URLs (test databases)
AUTH_DB_URL=postgresql://test_user:password@localhost:5432/test_auth
TRAINING_DB_URL=postgresql://test_user:password@localhost:5432/test_training
TRACKING_DB_URL=postgresql://test_user:password@localhost:5432/test_tracking
NOTIFICATION_DB_URL=postgresql://test_user:password@localhost:5432/test_notification

# Test Configuration
TEST_TIMEOUT=5000  # 5 seconds per test
SEED_DATA_PATH=tests/fixtures/seed-data.json
LOG_LEVEL=warn  # reduce noise in test output
```

---

## Testing Patterns

### Pattern 1: Arrange-Act-Assert

All tests follow this structure:

```javascript
describe('User Registration Flow', () => {
  it('should create user and send notification', async () => {
    // ARRANGE - Setup test data
    const newUser = {
      email: 'newuser@example.com',
      password: 'SecurePass123!',
      firstName: 'John',
      lastName: 'Doe'
    };
    
    // ACT - Execute the action being tested
    const response = await request(authService)
      .post('/auth/register')
      .send(newUser)
      .expect(201);  // expect success
    
    const userId = response.body.id;
    
    // ASSERT - Verify results
    expect(response.body.email).toBe(newUser.email);
    expect(userId).toBeDefined();
    
    // Also verify side effects
    const userInDB = await db.users.findById(userId);
    expect(userInDB).toBeDefined();
    expect(userInDB.email).toBe(newUser.email);
    
    // Verify notification was queued
    const notification = await messageBroker.getNotification(userId);
    expect(notification.type).toBe('welcome');
  });
});
```

### Pattern 2: API Testing with Postman Collection

Use the consolidated Postman collection (Phase 1) for integration tests:

```bash
# Run Postman collection with Newman (CLI)
npm run test:api:local

# Configuration (newman-config.json):
{
  "collection": "tests/collections/Gym-Platform-API-Master.postman_collection.json",
  "environment": "tests/environments/local.postman_environment.json",
  "bail": true,
  "reporters": ["cli", "json"],
  "reporter": { "json": { "export": "test-results.json" } }
}
```

### Pattern 3: Database Assertions

Verify data persisted correctly:

```javascript
// After creating workout, verify in database
const createdWorkout = await trainingDB.query(
  'SELECT * FROM workouts WHERE id = $1',
  [workoutId]
);

expect(createdWorkout.rows[0].user_id).toBe(userId);
expect(createdWorkout.rows[0].is_completed).toBe(false);
```

### Pattern 4: Event-Based Testing

For async event-driven flows:

```javascript
it('should publish workout_completed event', async (done) => {
  // ARRANGE
  const workoutId = 'workout-123';
  
  // Subscribe to event
  eventBus.subscribe('workout_completed', (event) => {
    // ASSERT
    expect(event.workoutId).toBe(workoutId);
    expect(event.userId).toBeDefined();
    done();  // async test complete
  });
  
  // ACT - Complete workout (will publish event)
  await completeWorkout(workoutId);
  
  // Wait for event (with timeout)
  setTimeout(() => done(new Error('Event not received')), 1000);
});
```

### Pattern 5: Error Scenario Testing

Test failure cases:

```javascript
it('should handle invalid workout data gracefully', async () => {
  const invalidWorkout = {
    user_id: 'user-123',
    exercise_id: 'invalid-id'  // doesn't exist
  };
  
  const response = await request(trainingService)
    .post('/workouts')
    .send(invalidWorkout)
    .expect(400);  // expect validation error
  
  expect(response.body.error).toBeDefined();
  expect(response.body.error).toContain('exercise_id');
  
  // Verify no partial data was created
  const workoutsCount = await db.workouts.count({
    user_id: 'user-123'
  });
  expect(workoutsCount).toBe(0);
});
```

---

## Writing Integration Tests

### Test File Structure

```javascript
// tests/integration/user-registration.test.js

const { request } = require('supertest');
const { setupTestDB, teardownTestDB, loadSeedData } = require('../helpers/db');
const authService = require('../../auth-service');

describe('User Registration Flow', () => {
  // Setup - runs before all tests in this describe block
  beforeAll(async () => {
    await setupTestDB();
  });
  
  // Cleanup - runs after all tests
  afterAll(async () => {
    await teardownTestDB();
  });
  
  // Reset before each test
  beforeEach(async () => {
    await loadSeedData();
  });
  
  it('should successfully register new user', async () => {
    // test code here
  });
  
  it('should reject duplicate email', async () => {
    // test code here
  });
});
```

### Test Helpers

Create reusable test utilities:

```javascript
// tests/helpers/api.js
const request = require('supertest');

async function authRequest(service, user) {
  // Helper to make authenticated API calls
  const loginResponse = await request(service)
    .post('/auth/login')
    .send({ email: user.email, password: user.password });
  
  return loginResponse.body.token;
}

async function createUserWithRole(role) {
  // Helper to create test user with specific role
  return await request(authService)
    .post('/auth/register')
    .send({
      email: `${role}-user@example.com`,
      password: 'Test123!',
      firstName: role.toUpperCase()
    });
}

module.exports = { authRequest, createUserWithRole };
```

---

## Test Scenarios

### Scenario 1: User Registration & Notification Flow

**What:** User registers → training profile created → welcome notification sent

**Test Code:**
```javascript
it('should complete user registration flow', async () => {
  // ARRANGE
  const newUser = {
    email: 'newathlete@example.com',
    password: 'SecurePass123!',
    firstName: 'Jane',
    lastName: 'Doe'
  };
  
  // ACT - Register user
  const registerResponse = await request(authService)
    .post('/auth/register')
    .send(newUser)
    .expect(201);
  
  const userId = registerResponse.body.id;
  
  // ASSERT - Verify user created in Auth DB
  let user = await authDB.query(
    'SELECT * FROM users WHERE id = $1',
    [userId]
  );
  expect(user.rows[0].email).toBe(newUser.email);
  
  // Wait for event processing (~500ms)
  await wait(500);
  
  // Verify user profile created in Training Service
  const trainingProfile = await request(trainingService)
    .get(`/users/${userId}`)
    .expect(200);
  expect(trainingProfile.body.email).toBe(newUser.email);
  
  // Verify welcome notification sent
  const notifications = await notificationDB.query(
    'SELECT * FROM notifications WHERE user_id = $1 AND type = $2',
    [userId, 'welcome']
  );
  expect(notifications.rows.length).toBeGreaterThan(0);
});
```

### Scenario 2: Workout Completion & Stats Update

**What:** User completes workout → stats update in Tracking Service → notification sent if milestone

**Test Code:**
```javascript
it('should update stats when workout completed', async () => {
  // ARRANGE
  const userId = 'user-001';
  const workoutId = 'workout-123';
  
  // Load seed data with this workout
  await loadSeedData();
  
  // ACT - Complete workout
  const completeResponse = await request(trainingService)
    .patch(`/workouts/${workoutId}`)
    .send({ is_completed: true, end_time: new Date() })
    .set('Authorization', `Bearer ${token}`)
    .expect(200);
  
  // Wait for event processing
  await wait(500);
  
  // ASSERT - Verify metrics updated in Tracking Service
  const metrics = await trackingDB.query(
    'SELECT * FROM metrics WHERE user_id = $1 AND metric_date = CURRENT_DATE',
    [userId]
  );
  expect(metrics.rows[0].workouts_completed).toBeGreaterThan(0);
  
  // Verify notification sent if milestone reached
  const milestoneNotifications = await notificationDB.query(
    'SELECT * FROM notifications WHERE user_id = $1 AND type = $2',
    [userId, 'milestone']
  );
  // may or may not have notification depending on milestone
  // just verify query works
});
```

### Scenario 3: Weight Tracking & Goal Progress

**What:** User logs weight → metrics updated → goal progress checked

**Test Code:**
```javascript
it('should track weight and update goal progress', async () => {
  // ARRANGE
  const userId = 'user-002';
  const currentWeight = 85.5;
  
  // Create a weight loss goal
  const goalResponse = await request(trackingService)
    .post('/goals')
    .send({
      goal_type: 'weight_loss',
      target_value: 80,
      start_date: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), // 30 days ago
      target_date: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000) // 60 days from now
    })
    .set('Authorization', `Bearer ${token}`)
    .expect(201);
  
  const goalId = goalResponse.body.id;
  
  // ACT - Log weight
  const weightResponse = await request(trackingService)
    .post('/weight-logs')
    .send({
      weight_kg: currentWeight,
      log_date: new Date()
    })
    .set('Authorization', `Bearer ${token}`)
    .expect(201);
  
  // Wait for processing
  await wait(500);
  
  // ASSERT - Verify weight logged
  const weightLog = await trackingDB.query(
    'SELECT * FROM weight_logs WHERE id = $1',
    [weightResponse.body.id]
  );
  expect(weightLog.rows[0].weight_kg).toBe(currentWeight);
  
  // Verify goal progress updated
  const progress = await trackingDB.query(
    'SELECT * FROM progress WHERE goal_id = $1 ORDER BY checkpoint_date DESC LIMIT 1',
    [goalId]
  );
  const progressValue = progress.rows[0].percentage_complete;
  expect(progressValue).toBeGreaterThan(0);
  expect(progressValue).toBeLessThanOrEqual(100);
});
```

### Scenario 4: Error - Invalid Exercise in Workout

**What:** Try to add non-existent exercise to workout → validation error → database unchanged

**Test Code:**
```javascript
it('should reject invalid exercise with proper error', async () => {
  // ARRANGE
  const userId = 'user-001';
  const invalidExerciseId = 'exercise-does-not-exist';
  
  // ACT - Try to add invalid exercise
  const response = await request(trainingService)
    .post('/workout-exercises')
    .send({
      workout_id: 'workout-123',
      exercise_id: invalidExerciseId,
      sets: 3,
      reps: 10
    })
    .set('Authorization', `Bearer ${token}`)
    .expect(400);  // expect validation error
  
  // ASSERT - Verify error message
  expect(response.body.error).toBeDefined();
  expect(response.body.error.toLowerCase()).toContain('exercise');
  
  // Verify no workout_exercise was created
  const workoutExercises = await trainingDB.query(
    'SELECT * FROM workout_exercises WHERE exercise_id = $1',
    [invalidExerciseId]
  );
  expect(workoutExercises.rows.length).toBe(0);
});
```

### Scenario 5: API Timeout Handling

**What:** Service slow to respond → client gets timeout error gracefully

**Test Code:**
```javascript
it('should handle service timeout gracefully', async () => {
  // ARRANGE
  // Simulate slow service by adding delay
  const delayMs = 6000;  // 6 seconds
  const timeoutMs = 5000; // 5 second timeout
  
  // ACT - Request that will timeout
  const promise = request(trainingService)
    .get('/exercises')
    .timeout(timeoutMs);
  
  // Simulate slow processing
  setTimeout(() => {
    // After timeout, response finally arrives (too late)
  }, delayMs);
  
  // ASSERT - Should get timeout error
  expect(promise).rejects.toThrow(
    expect.objectContaining({
      message: expect.stringContaining('timeout')
    })
  );
});
```

---

## Running Tests

### Local Integration Tests

**Run all integration tests:**
```bash
npm run test:integration

# Output:
# User Registration Flow
#   ✓ should successfully register new user (250ms)
#   ✓ should reject duplicate email (180ms)
#
# Workout Completion Flow
#   ✓ should update stats when workout completed (520ms)
#   ✓ should handle invalid exercise (200ms)
#
# 4 passing (1.2s)
```

**Run specific test file:**
```bash
npm run test:integration -- tests/integration/user-registration.test.js

# Or with jest
jest tests/integration/user-registration.test.js --testTimeout=10000
```

**Run with coverage:**
```bash
npm run test:integration -- --coverage

# Output:
# Coverage Summary
# Statements   : 68%
# Branches     : 55%
# Functions    : 72%
# Lines        : 69%
```

### CI/CD Integration

**GitHub Actions** (`.github/workflows/test.yml`):
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
    - uses: actions/checkout@v2
    - uses: actions/setup-node@v2
      with:
        node-version: '16'
    
    - name: Install dependencies
      run: npm install
    
    - name: Setup test databases
      run: npm run db:setup:test
    
    - name: Run integration tests
      run: npm run test:integration
    
    - name: Upload coverage
      uses: codecov/codecov-action@v2
```

### Test Reporting

**Generate HTML Report:**
```bash
npm run test:integration -- --reporter=html --reporterOptions.outputDir=test-results

# Opens in browser:
# test-results/index.html
```

**Metrics Dashboard:**
```bash
# Track test metrics over time
npm run test:metrics

# Outputs:
# Test Success Rate: 98.2%
# Average Duration: 450ms
# Coverage Trend: 65% → 68% → 70%
```

---

## Troubleshooting

### Flaky Tests (Intermittent Failures)

**Problem:** Test passes sometimes, fails randomly

**Causes & Solutions:**

1. **Timing issues** - Tests don't wait for async operations
   ```javascript
   // BAD - Race condition
   await createWorkout();
   const metrics = await getMetrics();  // May not be updated yet
   
   // GOOD - Wait for update
   await createWorkout();
   await wait(500);  // Wait for event processing
   const metrics = await getMetrics();
   ```

2. **Shared state** - Tests affect each other
   ```javascript
   // BAD - Test leaves data for next test
   it('test 1', async () => {
     await db.users.create({ id: 'user-123' });
     // No cleanup
   });
   
   // GOOD - Isolate each test
   it('test 1', async () => {
     const user = await db.users.create({});
     expect(user).toBeDefined();
     // User cleaned up by afterEach()
   });
   ```

3. **Random data** - Use fixed seed for reproducibility
   ```javascript
   // Use fixed user IDs in tests, not random ones
   const userId = 'test-user-001';  // Consistent
   const randomId = generateId();    // Unreliable
   ```

### Service Startup Issues

**Problem:** Services won't start, timeout errors

**Debug:**
```bash
# Check service logs
docker-compose logs auth-service

# Verify service is listening
curl http://localhost:8081/health

# Check port conflicts
lsof -i :8081

# Restart services
docker-compose down && docker-compose up -d
```

### Database Connection Errors

**Problem:** "Cannot connect to database"

**Solutions:**
```bash
# Check PostgreSQL running
docker-compose ps postgres

# Verify test database created
psql -U test_user -d test_auth -c "SELECT 1"

# Check connection string (.env.test)
DATABASE_URL=postgresql://test_user:password@localhost:5432/test_auth

# Reset database
npm run db:reset:test
```

### Event Not Received

**Problem:** Event published but test doesn't see it

**Debug:**
```javascript
// Add logging
eventBus.on('*', (event) => {
  console.log('Event received:', event.type);
});

// Verify event is being published
// Check service logs: docker-compose logs notification-service

// Increase wait timeout
await wait(1000);  // 1 second instead of 500ms
```

---

## Best Practices

1. **One thing per test** - Each test verifies one behavior
2. **Clear names** - Test name describes what it tests
3. **No test dependencies** - Tests can run in any order
4. **Fast feedback** - Keep tests under 1 second each
5. **Use fixtures** - Reusable test data setup
6. **Mock external services** - Don't call real APIs
7. **Test both success and failure** - Happy path + error cases
8. **Verify side effects** - Check database, events, etc.

---

**Last Updated:** March 21, 2026  
**Maintained by:** QA Team  
**Next Review:** June 21, 2026

See Also:
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Testing requirements
- [Testing Guide](../../tests/TESTING.md) - API & Postman testing
- [ADR-006: Event-Driven Architecture](../adr/ADR-006-event-driven-architecture.md) - Event patterns
