# Gym Platform API - Complete Testing Guide

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Environment Setup](#environment-setup)
4. [Running Tests Locally](#running-tests-locally)
5. [Running Tests with Newman](#running-tests-with-newman)
6. [Running Tests in CI/CD](#running-tests-in-cicd)
7. [Collection Structure](#collection-structure)
8. [Test Data & Fixtures](#test-data--fixtures)
9. [Writing New Tests](#writing-new-tests)
10. [Pre-Request & Post-Request Scripts](#pre-request--post-request-scripts)
11. [Troubleshooting](#troubleshooting)
12. [Best Practices](#best-practices)
13. [API Endpoint Reference](#api-endpoint-reference)
14. [Contributing](#contributing)

---

## Overview

The Gym Platform API testing suite provides comprehensive coverage of all microservices:
- **🔐 Auth Service**: User authentication, registration, token management
- **🏃 Training Service**: Exercises, routines, workout sessions, routines tracking
- **📊 Tracking Service**: Diet logs, weight tracking, workout metrics
- **🔔 Notification Service**: Notifications, push token management

### Test Coverage

- **101 API Endpoints** across 4 microservices
- **Consolidated Master Collection** (`Gym-Platform-API-Master.postman_collection.json`)
- **3 Environment Files**: Local, Staging, Production
- **Test Fixtures**: Realistic seed data in `test-data/seed-data.json`
- **Multi-Format Reporting**: CLI, JSON, HTML

### Key Features

- ✅ Automated testing via Newman (CLI runner)
- ✅ Pre-request scripts for token management
- ✅ Post-request scripts for data validation
- ✅ Environment-based configuration
- ✅ CI/CD integration ready
- ✅ Comprehensive error case coverage
- ✅ Performance monitoring capabilities

---

## Quick Start

### Prerequisites

- **Postman**: [Download here](https://www.postman.com/downloads/)
- **Node.js**: Version 16+ ([Download](https://nodejs.org/))
- **npm**: Comes with Node.js
- **Gym Platform Services**: Running locally or accessible

### 5-Minute Setup

```bash
# 1. Navigate to tests directory
cd tests

# 2. Install dependencies (if using Newman)
npm install

# 3. Import the master collection into Postman
# File > Import > collections/Gym-Platform-API-Master.postman_collection.json

# 4. Import environment
# File > Import > environments/local.postman_environment.json

# 5. Run first test
npm run test:local
```

---

## Environment Setup

### Local Development Environment

**File**: `environments/local.postman_environment.json`

```json
{
  "base_url_auth": "http://localhost:8081",
  "base_url_training": "http://localhost:8082/training",
  "base_url_tracking": "http://localhost:8083/tracking",
  "base_url_notification": "http://localhost:8084/notifications",
  "auth_token": "",
  "user_id": "",
  "test_email": "testuser_{{$timestamp}}@example.com",
  "test_password": "TestPassword123!"
}
```

#### Starting Local Services

```bash
# Terminal 1: Auth Service
cd auth-service
npm start

# Terminal 2: Training Service
cd training-service
npm start

# Terminal 3: Tracking Service
cd tracking-service
npm start

# Terminal 4: Notification Service
cd notification-service
npm start
```

Verify all services are running:

```bash
curl http://localhost:8081/health
curl http://localhost:8082/training/health
curl http://localhost:8083/tracking/health
curl http://localhost:8084/notifications/health
```

### Staging Environment

**File**: `environments/staging.postman_environment.json`

For testing against staging infrastructure:
- Base URLs point to `api-staging.gymplatform.dev`
- Requires valid staging credentials
- Use for pre-production testing

### Production Environment

**File**: `environments/production.postman_environment.json`

**⚠️ CRITICAL: Production testing considerations**
- Only run read-only tests in production
- Never create test data in production
- Requires special permissions
- Use for monitoring and validation only

---

## Running Tests Locally

### Method 1: Postman UI (Manual Testing)

#### 1. Import Collection & Environment

```
1. Open Postman
2. Click "Import"
3. Select: collections/Gym-Platform-API-Master.postman_collection.json
4. Click "Import"
5. Repeat for environment: environments/local.postman_environment.json
```

#### 2. Select Environment

```
Top-right corner: Dropdown > Select "Gym Platform API - Local Environment"
```

#### 3. Run Individual Request

```
1. Navigate to desired endpoint in left sidebar
2. Click the request
3. Review request details (method, URL, headers, body)
4. Click "Send"
5. Inspect response (Status, Headers, Body)
```

#### 4. Run Entire Collection

```
1. Right-click collection name in left sidebar
2. Select "Run collection"
3. Configure runner:
   - Environment: Select local
   - Iterations: 1
   - Delay: 100ms
   - Data: seed-data.json (optional)
4. Click "Run"
5. Monitor results in real-time
```

#### 5. View Results

The Collection Runner shows:
- ✅ Passed requests (green)
- ❌ Failed requests (red)
- ⏱️ Response times
- 📊 Summary statistics

### Method 2: Postman Collection Runner

#### Complete Flow Testing

```javascript
// Pre-request Script (Auth Service > Login)
const register_response = pm.environment.get("register_response");
if (!register_response) {
    pm.sendRequest({
        url: pm.environment.get("base_url_auth") + "/api/v1/auth/register",
        method: "POST",
        header: {
            "Content-Type": "application/json"
        },
        body: {
            mode: "raw",
            raw: JSON.stringify({
                email: pm.variables.replaceIn("{{test_email}}"),
                password: pm.environment.get("test_password"),
                firstName: "Test",
                lastName: "User"
            })
        }
    }, (err, response) => {
        if (!err) {
            pm.environment.set("register_response", response.json());
        }
    });
}
```

#### Test Assertions

```javascript
// Post-request Script (Login)
pm.test("Login successful", function() {
    pm.response.to.have.status(200);
});

pm.test("Response contains auth token", function() {
    let jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("token");
    pm.environment.set("auth_token", jsonData.token);
});

pm.test("Response time < 500ms", function() {
    pm.expect(pm.response.responseTime).to.be.below(500);
});
```

---

## Running Tests with Newman

### Installation

```bash
cd tests

# Install Newman and reporters
npm install

# Or install globally
npm install -g newman newman-reporter-html
```

### Basic CLI Testing

#### Run against Local Environment

```bash
npm run test:local
```

Output example:
```
    ┌─────────────────────────────────┐
    │         Test Results            │
    ├─────────────────────────────────┤
    │ requests sent:     101          │
    │ requests received: 101          │
    │ assertions:        456          │
    │ test failures:     2            │
    │ runtime (ms):      12340        │
    │ failed tests:      2            │
    └─────────────────────────────────┘
```

#### Run against Staging

```bash
npm run test:staging
```

#### Run specific collection

```bash
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json
```

### Advanced Newman Options

```bash
# Run with custom timeout (10 seconds)
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json \
  --timeout 10000

# Run specific test folder
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json \
  --folder "🔐 Auth Service"

# Run N iterations
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json \
  -n 3

# Run with custom data file
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json \
  -d test-data/seed-data.json
```

### Reporting

#### Generate HTML Report

```bash
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json \
  -r html

# Opens in: results/newman-report.html
```

#### Generate JSON Report

```bash
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json \
  -r json

# Output: results/newman-results.json
```

#### Generate Multiple Reports

```bash
npm run test:ci
```

This generates:
- CLI output (stdout)
- JSON report: `results/newman-results.json`
- HTML report: `results/newman-report.html`

---

## Running Tests in CI/CD

### GitHub Actions Integration

Create `.github/workflows/api-tests.yml`:

```yaml
name: API Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      auth-service:
        image: gym-auth-service:latest
        ports:
          - 8081:8081
      training-service:
        image: gym-training-service:latest
        ports:
          - 8082:8082
      tracking-service:
        image: gym-tracking-service:latest
        ports:
          - 8083:8083
      notification-service:
        image: gym-notification-service:latest
        ports:
          - 8084:8084

    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd tests
          npm install
      
      - name: Wait for services
        run: |
          npm run wait:services
      
      - name: Run API tests
        run: |
          cd tests
          npm run test:ci
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: tests/results/
      
      - name: Comment PR with results
        if: always()
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const results = JSON.parse(fs.readFileSync('tests/results/newman-results.json', 'utf8'));
            const passed = results.run.stats.tests.failed === 0;
            const summary = `
            ✅ Tests: ${results.run.stats.requests.total}
            ⏱️ Duration: ${results.run.timings.completed}ms
            ${passed ? '✔️ All tests passed' : '❌ Some tests failed'}
            `;
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: summary
            });
```

### GitLab CI Integration

Create `.gitlab-ci.yml`:

```yaml
stages:
  - test

api_tests:
  stage: test
  image: node:18
  services:
    - auth-service:latest
    - training-service:latest
    - tracking-service:latest
    - notification-service:latest
  before_script:
    - cd tests
    - npm install
  script:
    - npm run test:ci
  artifacts:
    paths:
      - tests/results/
    reports:
      junit: tests/results/newman-results.json
```

### Jenkins Integration

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Setup') {
            steps {
                dir('tests') {
                    sh 'npm install'
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                dir('tests') {
                    sh 'npm run test:ci'
                }
            }
        }
        
        stage('Publish Results') {
            steps {
                publishHTML([
                    reportDir: 'tests/results',
                    reportFiles: 'newman-report.html',
                    reportName: 'API Test Report'
                ])
                junit 'tests/results/newman-results.json'
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'tests/results/**'
        }
    }
}
```

---

## Collection Structure

### Folder Organization

```
Gym Platform - Complete API
│
├── 🔐 Auth Service (5 requests)
│   ├── Register User
│   ├── Register Professional
│   ├── Login
│   ├── Get Profile
│   ├── Refresh Token
│   └── Verify Email
│
├── 🏃 Training Service (48 requests)
│   ├── 🔧 Setup
│   │   └── Health Check
│   ├── 📋 Exercise Management (12 requests)
│   │   ├── Get All System Exercises
│   │   ├── Get Exercises by Discipline
│   │   ├── Get Exercise by ID
│   │   ├── Create Custom Exercise
│   │   ├── Update Exercise
│   │   └── Delete Exercise
│   ├── 🎯 Routine Template Management (8 requests)
│   │   ├── Get All System Routine Templates
│   │   ├── Get Routine Template by ID
│   │   ├── Create Custom Routine Template
│   │   ├── Update Routine Template
│   │   └── Delete Routine Template
│   ├── 👤 User Routine Management (12 requests)
│   ├── 📅 Exercise Sessions (8 requests)
│   │   ├── Get Sessions by Routine
│   │   ├── Get Sessions by Date
│   │   ├── Get Session by ID
│   │   ├── Create Exercise Session
│   │   ├── Update Exercise Session
│   │   └── Delete Exercise Session
│   └── 📈 Metrics (8 requests)
│
├── 📊 Tracking Service (26 requests)
│   ├── 🍽️ Diet Logs (8 requests)
│   │   ├── Get Diet Logs
│   │   ├── Get Diet Logs by Date
│   │   ├── Create Diet Log
│   │   └── Delete Diet Log
│   ├── ⚖️ Weight Tracking (6 requests)
│   ├── 📊 Workout Logs (6 requests)
│   └── 📈 Progress Metrics (6 requests)
│
├── 🔔 Notification Service (16 requests)
│   ├── 🔔 Notifications (6 requests)
│   ├── 📱 Push Tokens (6 requests)
│   │   ├── Register Push Token
│   │   ├── Get Push Tokens
│   │   ├── Update Push Token
│   │   └── Remove Push Token
│   └── ⚙️ Settings (4 requests)
│
└── 📦 Common (6 requests)
    ├── Health Checks
    └── Service Status
```

### URL Structure

All requests use environment variables for base URLs:

```
{{base_url_auth}}        = http://localhost:8081
{{base_url_training}}    = http://localhost:8082/training
{{base_url_tracking}}    = http://localhost:8083/tracking
{{base_url_notification}} = http://localhost:8084/notifications
```

Example request:
```
GET {{base_url_training}}/api/v1/exercises?page=1&limit=10
```

---

## Test Data & Fixtures

### Seed Data Location

File: `test-data/seed-data.json`

Contains pre-configured test data for all 4 microservices:

#### Auth Service Fixtures

```json
{
  "valid_user": {
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "first_name": "John",
    "last_name": "Doe",
    "user_type": "regular"
  },
  "test_users": [
    {"email": "user1@example.com", "id": "user-001"},
    {"email": "user2@example.com", "id": "user-002"}
  ]
}
```

#### Training Service Fixtures

```json
{
  "exercises": [
    {
      "id": "ex-001",
      "name": "Bench Press",
      "discipline": "Chest",
      "difficulty": "intermediate"
    }
  ],
  "routine_templates": [
    {
      "id": "rt-001",
      "name": "Full Body A",
      "exercises": ["ex-001", "ex-002"]
    }
  ]
}
```

#### Tracking Service Fixtures

```json
{
  "diet_logs": [
    {
      "id": "diet-001",
      "user_id": "user-001",
      "date": "2026-03-20T00:00:00Z",
      "daily_total": {
        "calories": 1850,
        "protein": 120
      }
    }
  ]
}
```

#### Notification Service Fixtures

```json
{
  "notifications": [
    {
      "id": "notif-001",
      "user_id": "user-001",
      "type": "workout_reminder",
      "title": "Time for your workout!"
    }
  ],
  "push_tokens": [
    {
      "id": "token-001",
      "user_id": "user-001",
      "token": "ExponentPushToken[abc123def456]"
    }
  ]
}
```

### Using Fixtures in Tests

#### Option 1: Dynamic Variables

```javascript
// Pre-request script
const testEmail = `test_${Date.now()}@example.com`;
pm.environment.set("test_email_dynamic", testEmail);
```

#### Option 2: From Seed Data

```javascript
// Pre-request script
const seedData = JSON.parse(pm.environment.get("seed_data"));
pm.environment.set("test_user_email", seedData.auth_service.valid_user.email);
```

---

## Writing New Tests

### Test Anatomy

Each request can have:
1. **Pre-request Script**: Runs before request
2. **Request**: Method, URL, headers, body
3. **Post-request Script**: Runs after response

### Adding a New Request

#### Step 1: Create Request

```
1. Right-click folder in collection
2. Select "Add Request"
3. Name: "Get User Profile"
4. Method: GET
5. URL: {{base_url_auth}}/api/v1/users/profile
```

#### Step 2: Add Headers

```
Authorization: Bearer {{auth_token}}
Content-Type: application/json
```

#### Step 3: Pre-request Script

```javascript
// Ensure auth token exists
if (!pm.environment.get("auth_token")) {
    console.error("No auth token found. Run login test first.");
}
```

#### Step 4: Post-request Script (Tests)

```javascript
// Test: Status code
pm.test("Status is 200", function() {
    pm.response.to.have.status(200);
});

// Test: Response structure
pm.test("Response has user object", function() {
    let jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("user");
    pm.expect(jsonData.user).to.have.property("id");
    pm.expect(jsonData.user).to.have.property("email");
});

// Test: Response time
pm.test("Response time under 500ms", function() {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// Save data for next request
pm.test("Save user data", function() {
    let jsonData = pm.response.json();
    pm.environment.set("user_id", jsonData.user.id);
    pm.environment.set("user_email", jsonData.user.email);
});
```

### Common Test Patterns

#### Authentication Tests

```javascript
// Test: Auth token refresh
pm.test("Token contains JWT signature", function() {
    let token = pm.environment.get("auth_token");
    let parts = token.split('.');
    pm.expect(parts).to.have.lengthOf(3);
});
```

#### Data Validation Tests

```javascript
// Test: Email format
pm.test("Response email is valid", function() {
    let jsonData = pm.response.json();
    let emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    pm.expect(jsonData.email).to.match(emailRegex);
});

// Test: Date format
pm.test("Response date is ISO 8601", function() {
    let jsonData = pm.response.json();
    pm.expect(new Date(jsonData.created_at).toISOString()).to.be.a('string');
});
```

#### Error Handling Tests

```javascript
// Test: 400 Bad Request
pm.test("Bad request returns 400", function() {
    pm.response.to.have.status(400);
});

pm.test("Error response has message", function() {
    let jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("error");
    pm.expect(jsonData.error).to.have.property("message");
});

// Test: 401 Unauthorized
pm.test("Missing token returns 401", function() {
    pm.response.to.have.status(401);
});
```

#### Pagination Tests

```javascript
// Test: Pagination metadata
pm.test("Response has pagination data", function() {
    let jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("pagination");
    pm.expect(jsonData.pagination).to.have.property("page");
    pm.expect(jsonData.pagination).to.have.property("limit");
    pm.expect(jsonData.pagination).to.have.property("total");
});
```

---

## Pre-Request & Post-Request Scripts

### Common Pre-Request Patterns

#### 1. Token Management

```javascript
// Check if token exists and is not expired
let token = pm.environment.get("auth_token");
let tokenExpiry = pm.environment.get("auth_token_expiry");

if (!token || (new Date() > new Date(tokenExpiry))) {
    // Token missing or expired - would normally refresh
    console.warn("Auth token missing or expired");
}
```

#### 2. Data Setup

```javascript
// Set dynamic email for each test run
let timestamp = Date.now();
pm.environment.set("test_email", `test_${timestamp}@example.com`);

// Set test user data
let testUser = {
    email: pm.environment.get("test_email"),
    password: pm.environment.get("test_password"),
    firstName: "Test",
    lastName: "User"
};
```

#### 3. Request Body Generation

```javascript
// Generate request body from environment variables
let requestBody = {
    name: pm.environment.get("exercise_name") || "Test Exercise",
    discipline: pm.environment.get("exercise_discipline") || "Chest",
    difficulty: "intermediate"
};

pm.environment.set("request_body", JSON.stringify(requestBody));
```

### Common Post-Request Patterns

#### 1. Extract Data for Next Request

```javascript
// Save ID from response for use in next request
let jsonData = pm.response.json();
pm.environment.set("created_exercise_id", jsonData.id);
pm.environment.set("exercise_name", jsonData.name);
```

#### 2. Validate Response Structure

```javascript
// Comprehensive response validation
pm.test("Response structure is valid", function() {
    let jsonData = pm.response.json();
    
    // Check required fields
    pm.expect(jsonData).to.have.property("id");
    pm.expect(jsonData).to.have.property("created_at");
    pm.expect(jsonData).to.have.property("updated_at");
    
    // Check data types
    pm.expect(jsonData.id).to.be.a('string');
    pm.expect(jsonData.created_at).to.be.a('string');
});
```

#### 3. Performance Monitoring

```javascript
// Track response times
let responseTime = pm.response.responseTime;
pm.environment.set("last_response_time", responseTime);

pm.test("Performance - Response time < 1000ms", function() {
    pm.expect(responseTime).to.be.below(1000);
});

// Warning if slow
if (responseTime > 500) {
    console.warn(`Slow response: ${responseTime}ms`);
}
```

#### 4. Data Assertion Chains

```javascript
// Chain multiple assertions
pm.test("Comprehensive validation", function() {
    pm.response.to.have.status(200);
    pm.response.to.have.header("content-type");
    pm.response.to.have.header("content-type", /application\/json/);
    
    let jsonData = pm.response.json();
    pm.expect(jsonData).to.be.an('object');
    pm.expect(jsonData).to.not.be.empty;
});
```

---

## Troubleshooting

### Common Issues

#### Issue: "Cannot read property of undefined"

**Cause**: Environment variable not set

**Solution**:
```javascript
// Add safety checks
let authToken = pm.environment.get("auth_token");
if (!authToken) {
    console.error("auth_token not found in environment");
    // Use default or skip
    pm.environment.set("auth_token", "default_token");
}
```

#### Issue: "Status code is not 200"

**Cause**: Service not running or endpoint error

**Solution**:
```bash
# Check service health
curl http://localhost:8081/health
curl http://localhost:8082/training/health

# Check logs
docker logs auth-service
docker logs training-service
```

#### Issue: "Request timeout"

**Cause**: Service slow or not responding

**Solution**:
```javascript
// Increase timeout in Newman
// newman run ... --timeout 30000

// Or in request
// Set "Timeout" in request settings to 30000ms
```

#### Issue: "401 Unauthorized"

**Cause**: Invalid or missing auth token

**Solution**:
```
1. Run "Login" request in Auth Service folder
2. Verify auth_token is set in environment
3. Check token hasn't expired
```

#### Issue: "404 Not Found"

**Cause**: Endpoint path or method incorrect

**Solution**:
```
1. Check collection has correct endpoints
2. Verify URL format: {{base_url}}/api/v1/path
3. Check request method (GET/POST/PUT/DELETE)
```

#### Issue: "CORS errors"

**Cause**: Backend CORS configuration issue

**Solution**:
```
In collection, add headers:
- Access-Control-Allow-Origin: *
- Access-Control-Allow-Methods: *
```

### Diagnostic Commands

```bash
# Check all services running
for port in 8081 8082 8083 8084; do
  curl -s http://localhost:$port/health || echo "Port $port down"
done

# Show environment variables
echo $NODE_ENV
echo $DATABASE_URL

# Check Newman version
newman --version

# Run with verbose logging
newman run collection.json -e env.json --verbose
```

### Debug Mode

Enable detailed logging in Postman:
```
Settings > General > Show console on error
```

Or in scripts:
```javascript
console.log("Debug:", pm.environment.get("auth_token"));
console.info("Info message");
console.warn("Warning message");
console.error("Error message");
```

---

## Best Practices

### 1. Test Naming Conventions

✅ **Good**:
- "Get Exercise by ID - Success"
- "Create Exercise with invalid discipline - 400 error"
- "Login with expired token - 401 unauthorized"

❌ **Bad**:
- "Test1"
- "Check stuff"
- "GET request"

### 2. Assertion Best Practices

✅ **Do**:
```javascript
// Specific assertions
pm.test("Response is object with user data", function() {
    pm.response.to.have.status(200);
    let jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("user");
});
```

❌ **Don't**:
```javascript
// Too generic
pm.test("Everything works", function() {
    pm.response.to.be.ok;
});
```

### 3. Test Data Management

✅ **Do**:
```javascript
// Use realistic, repeatable test data
const testUser = {
    email: `test_${Date.now()}@example.com`,
    password: "SecurePass123!",
    firstName: "Test"
};
```

❌ **Don't**:
```javascript
// Hardcoded, non-repeatable data
const testUser = {
    email: "test@example.com",
    password: "test123"
};
```

### 4. Error Handling

✅ **Do**:
```javascript
// Handle errors gracefully
pm.test("Verify response is JSON", function() {
    pm.response.to.have.header("content-type", /json/);
});

if (pm.response.code === 200) {
    let data = pm.response.json();
    pm.environment.set("saved_id", data.id);
}
```

### 5. Collection Organization

✅ **Do**:
```
Keep related requests in folders:
- Auth Service
  - Register
  - Login
  - Refresh Token
  - Get Profile
```

❌ **Don't**:
```
Random order:
- Register
- Get Profile
- Update Exercise
- Login
```

### 6. Environment Variables

✅ **Do**:
- Use `{{variable}}` for all dynamic data
- Keep variables consistent across environments
- Document environment setup

❌ **Don't**:
- Hardcode URLs: `http://localhost:8081`
- Use different variable names per environment
- Store secrets in variables

### 7. Performance Testing

✅ **Do**:
```javascript
pm.test("Performance - API responds quickly", function() {
    pm.expect(pm.response.responseTime).to.be.below(500);
});
```

❌ **Don't**:
```javascript
// No performance checks
pm.test("Request successful", function() {
    pm.response.to.have.status(200);
});
```

---

## API Endpoint Reference

### Auth Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/register-professional` | Register professional |
| POST | `/api/v1/auth/login` | Login user |
| POST | `/api/v1/auth/refresh` | Refresh token |
| GET | `/api/v1/auth/profile` | Get user profile |
| POST | `/api/v1/auth/verify-email` | Verify email |
| POST | `/api/v1/auth/logout` | Logout user |

### Training Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/exercises` | List exercises |
| GET | `/api/v1/exercises/{id}` | Get exercise |
| POST | `/api/v1/exercises` | Create exercise |
| PUT | `/api/v1/exercises/{id}` | Update exercise |
| DELETE | `/api/v1/exercises/{id}` | Delete exercise |
| GET | `/api/v1/routine-templates` | List routine templates |
| POST | `/api/v1/routine-templates` | Create routine template |
| GET | `/api/v1/sessions` | List sessions |
| POST | `/api/v1/sessions` | Create session |
| PUT | `/api/v1/sessions/{id}` | Update session |

### Tracking Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/diet-logs` | List diet logs |
| GET | `/api/v1/diet-logs/date/{date}` | Get diet logs by date |
| POST | `/api/v1/diet-logs` | Create diet log |
| DELETE | `/api/v1/diet-logs/{id}` | Delete diet log |
| GET | `/api/v1/weight-logs` | List weight logs |
| POST | `/api/v1/weight-logs` | Create weight log |
| GET | `/api/v1/workout-logs` | List workout logs |
| POST | `/api/v1/workout-logs` | Create workout log |

### Notification Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/notifications` | List notifications |
| GET | `/api/v1/notifications/{id}` | Get notification |
| POST | `/api/v1/push-tokens` | Register push token |
| GET | `/api/v1/push-tokens` | List push tokens |
| DELETE | `/api/v1/push-tokens/{id}` | Remove push token |

---

## Contributing

### Adding New Tests

1. **Identify gap**: Find untested endpoint
2. **Create request**: Add to appropriate folder
3. **Write tests**: Add pre/post scripts
4. **Test locally**: Run collection
5. **Submit PR**: Include test results

### Reporting Issues

When reporting test failures:

```
- Environment: local/staging/production
- Request name: [Full path in collection]
- Error: [Error message]
- Steps to reproduce: [Exact steps]
- Expected result: [What should happen]
- Actual result: [What happened]
```

### Updating Collections

When API changes:

1. Update endpoint in collection
2. Update tests if response changed
3. Update environment if URLs changed
4. Commit with clear message
5. Update this guide if significant changes

---

## Quick Reference Card

```bash
# Quick Commands
npm run test:local          # Run against local services
npm run test:staging        # Run against staging
npm run test:ci              # Run with all reports
npm run test:watch           # Watch mode (re-run on change)

# Newman Commands
newman run collections/Gym-Platform-API-Master.postman_collection.json \
  -e environments/local.postman_environment.json

# Generate Reports
newman run collection.json -e env.json -r html,json

# Specific Folder
newman run collection.json -e env.json --folder "🔐 Auth Service"

# With Data
newman run collection.json -e env.json -d test-data/seed-data.json
```

---

**Last Updated**: March 21, 2026  
**Test Collection Version**: 1.0.0 (101 requests)  
**Postman Version**: 10.0+  
**Newman Version**: 6.1.0+

For questions or updates, see the project repository or contact the development team.
