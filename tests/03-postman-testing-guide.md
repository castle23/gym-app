# Postman Testing Guide - Gym Platform API

## Table of Contents
1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Initial Setup](#initial-setup)
4. [Authentication](#authentication)
5. [Testing Workflows](#testing-workflows)
6. [Response Validation](#response-validation)
7. [Troubleshooting](#troubleshooting)

---

## Introduction

This guide provides comprehensive instructions for testing the Gym Platform API using Postman. The Gym Platform is a microservices-based fitness management system with 4 independent services and 80 documented endpoints.

### Services Overview

| Service | Port | Base URL | Context Path |
|---------|------|----------|--------------|
| Auth Service | 8081 | http://localhost:8081 | / |
| Training Service | 8082 | http://localhost:8082 | /training |
| Tracking Service | 8083 | http://localhost:8083 | /tracking |
| Notification Service | 8084 | http://localhost:8084 | /notifications |

---

## Prerequisites

### Required Software
- **Postman** (v9.0+) - [Download](https://www.postman.com/downloads/)
- **Docker Desktop** - Services running via docker-compose
- All services must be started: `docker-compose up -d`

### Verify Docker Containers

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Expected: 6 containers in "Up" state

---

## Initial Setup

### Step 1: Import Postman Collection

1. Open Postman
2. Click **Import** button
3. Select `Gym_Platform_API.postman_collection.json`
4. Verify collection appears in left sidebar

### Step 2: Configure Environment

Create new environment with variables:

```json
{
  "baseUrl": "http://localhost:8080",
  "authBaseUrl": "http://localhost:8081",
  "trainingBaseUrl": "http://localhost:8082/training",
  "trackingBaseUrl": "http://localhost:8083/tracking",
  "notificationBaseUrl": "http://localhost:8084/notifications",
  "userId": "1",
  "token": "",
  "email": "testuser@example.com",
  "password": "SecurePassword123!"
}
```

### Step 3: Select Environment

Choose environment from dropdown (top-right corner)

---

## Authentication

### Login Workflow

**Step 1: Register User**
- Endpoint: `POST /auth/register`
- Body: 
  ```json
  {
    "email": "test@example.com",
    "password": "SecurePassword123!",
    "firstName": "Test",
    "lastName": "User"
  }
  ```
- Expected: `201 Created`

**Step 2: Login**
- Endpoint: `POST /auth/login`
- Body:
  ```json
  {
    "email": "test@example.com",
    "password": "SecurePassword123!"
  }
  ```
- Expected: `200 OK` with token

**Step 3: Save Token**
- Copy token from response
- Go to Environments
- Update `token` variable
- Save

**Step 4: Configure Authorization**
- In collection: Click **Authorization** tab
- Type: `Bearer Token`
- Token: `{{token}}`
- Save

---

## Testing Workflows

### Auth Workflow
1. Register → 201
2. Login → 200 + token
3. Get Profile → 200
4. Update Profile → 200
5. Logout → 204

### Training Workflow
1. Create Exercise → 201
2. Get Exercises → 200
3. Create Routine Template → 201
4. Assign Routine → 201
5. Log Exercise Session → 201

### Tracking Workflow
1. Create Objective → 201
2. Create Plan → 201
3. Log Measurement → 201
4. Get Measurements → 200

### Notification Workflow
1. Register Push Token → 201
2. Get Tokens → 200
3. Send Notification → 201
4. Get Notifications → 200

---

## Response Validation

### Status Code Reference

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### Adding Tests in Postman

Click **Tests** tab and add:

```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has id", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("id");
});

pm.test("Save id to environment", function () {
    var jsonData = pm.response.json();
    pm.environment.set("objectId", jsonData.id);
});
```

---

## Troubleshooting

### Connection Failed
- Check Docker containers: `docker ps`
- Restart: `docker-compose restart`
- Check logs: `docker logs [container-name]`

### 401 Unauthorized
- Verify token not expired
- Re-login to get new token
- Update environment variable

### 403 Forbidden
- Check if you own resource
- Verify userId in headers

### 400 Bad Request
- Check request body syntax
- Validate field types
- Use Swagger UI to see schema

### 404 Not Found
- Verify resource ID exists
- Check endpoint URL
- Review request path

---

## Quick Reference

### Environment Variables
- `baseUrl`: Main API Gateway
- `authBaseUrl`: Auth Service
- `trainingBaseUrl`: Training Service
- `trackingBaseUrl`: Tracking Service
- `notificationBaseUrl`: Notification Service
- `userId`: Current user ID
- `token`: JWT Authorization token

### Common Endpoints

**Auth**
- `POST /auth/register` - Create account
- `POST /auth/login` - Get token
- `GET /auth/profile` - Get user info
- `PUT /auth/profile` - Update user

**Training**
- `POST /api/v1/exercises` - Create exercise
- `POST /api/v1/exercise-sessions` - Log workout
- `POST /api/v1/routine-templates` - Create routine
- `POST /api/v1/user-routines` - Assign routine

**Tracking**
- `POST /api/v1/objectives` - Create goal
- `POST /api/v1/measurements` - Log measurement
- `POST /api/v1/plans` - Create plan
- `GET /api/v1/plans` - Get plans

**Notification**
- `POST /api/v1/push-tokens` - Register device
- `POST /api/v1/notifications` - Send notification
- `GET /api/v1/notifications` - Get notifications

---

## Swagger UI Endpoints

For quick API exploration:

- Auth Service: http://localhost:8081/swagger-ui.html
- Training Service: http://localhost:8082/training/swagger-ui.html
- Tracking Service: http://localhost:8083/tracking/swagger-ui.html
- Notification Service: http://localhost:8084/notifications/swagger-ui.html

---

## Best Practices

- ✅ Use environment variables for all URLs and credentials
- ✅ Add tests to every request
- ✅ Save tokens in environment (don't commit to git)
- ✅ Use descriptive request names
- ✅ Organize requests in folders
- ✅ Document complex workflows
- ✅ Run full workflow regularly for regression testing

---

Last Updated: March 21, 2026
