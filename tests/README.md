# Gym Platform API - Testing Guide

Complete testing suite for Gym Platform microservices with 101 API endpoints across 4 services.

## 📋 Contents

### Documentation
- **TESTING.md** - ⭐ **START HERE** - Comprehensive 800+ line testing guide with all procedures
- **01-testing-guide.md** - Unit/integration testing guide
- **02-testing-resources.md** - Testing tools and resources
- **03-postman-testing-guide.md** - Additional Postman resources

### Collections & Environments
- **collections/** - Postman collections
  - `Gym-Platform-API-Master.postman_collection.json` - Consolidated master collection (101 requests)
- **environments/** - Environment configurations
  - `local.postman_environment.json` - Local development (localhost:8081-8084)
  - `staging.postman_environment.json` - Staging environment
  - `production.postman_environment.json` - Production environment (read-only)

### Test Data & Configuration
- **test-data/** - Test fixtures and seed data
  - `seed-data.json` - Realistic test data for all 4 microservices
  - `fixtures/` - Organized test fixtures by entity
- **package.json** - npm scripts for automation
- **newman-config.json** - Newman CLI configuration

### Subdirectories
- **pre-request-scripts/** - Shared pre-request scripts
- **post-request-scripts/** - Shared post-request scripts
- **results/** - Test execution reports and results
- **_archive/** - Old collections (consolidated into master)

## 🚀 Quick Start

### Prerequisites
- Postman ([Download](https://www.postman.com/downloads/))
- Node.js 16+ ([Download](https://nodejs.org/))
- npm (comes with Node.js)
- Gym Platform services running locally

### 5-Minute Setup

```bash
# Navigate to tests directory
cd tests

# Install dependencies (for Newman CLI automation)
npm install

# Run tests against local services
npm run test:local

# Generate reports
npm run test:ci
```

### Manual Postman Testing

1. **Import collection**: File → Import → `collections/Gym-Platform-API-Master.postman_collection.json`
2. **Import environment**: File → Import → `environments/local.postman_environment.json`
3. **Select environment**: Top-right dropdown → "Gym Platform API - Local Environment"
4. **Run requests**: Click any request → Send

### Starting Local Services

```bash
# Terminal 1: Auth Service
cd auth-service && npm start

# Terminal 2: Training Service
cd training-service && npm start

# Terminal 3: Tracking Service
cd tracking-service && npm start

# Terminal 4: Notification Service
cd notification-service && npm start
```

Verify: `curl http://localhost:8081/health`

---

## 📚 Documentation Guide

### For Manual Testing
→ See **TESTING.md** Section: "Running Tests Locally"

### For CLI Automation
→ See **TESTING.md** Section: "Running Tests with Newman"

### For CI/CD Integration
→ See **TESTING.md** Section: "Running Tests in CI/CD"

### For Writing New Tests
→ See **TESTING.md** Section: "Writing New Tests"

### For Troubleshooting
→ See **TESTING.md** Section: "Troubleshooting"

---

## 📊 Collection Structure

The master collection includes 101 requests organized by service:

- **🔐 Auth Service** (7 requests) - Authentication, registration, token management
- **🏃 Training Service** (48 requests) - Exercises, routines, sessions, metrics
- **📊 Tracking Service** (26 requests) - Diet logs, weight, workouts, progress
- **🔔 Notification Service** (16 requests) - Notifications, push tokens, settings

All organized in clear, navigable folders. ⭐ See **TESTING.md** for full structure.

---

## 🛠️ Available Commands

```bash
# Run tests
npm run test:local              # Local services
npm run test:staging            # Staging environment
npm run test:production         # Production (read-only)
npm run test:ci                 # With all reports (JSON + HTML)

# Development
npm run test:watch              # Watch mode - auto-run on changes
npm run lint:collection         # Validate collection syntax
```

---

## 📈 Test Coverage

- **101 API Endpoints** - All 4 microservices fully covered
- **4 Microservices** - Auth, Training, Tracking, Notifications
- **Multi-environment** - Local, staging, production support
- **Realistic Fixtures** - Complete test data in `test-data/seed-data.json`
- **Comprehensive Reports** - CLI, JSON, and HTML output formats

---

## 🔄 Test Execution Workflow

```
1. Select Environment (local/staging/production)
   ↓
2. Set Auth Token (via Login request or environment)
   ↓
3. Run Collection or Individual Requests
   ↓
4. View Results (Status, response time, assertions)
   ↓
5. Review Reports (results/ directory)
```

---
