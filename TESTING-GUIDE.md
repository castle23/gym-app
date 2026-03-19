# 📚 Gym Training Service - Testing Guide

## 📋 Overview

This guide explains how to use the SQL initialization script and Postman collection to test the Gym Training Service.

## 🚀 Quick Start

### Prerequisites
- PostgreSQL database running
- Docker Compose setup (see `docker-compose.yml`)
- Postman application installed
- Training Service API running on `http://localhost:8082`

---

## 🗂️ Files

### 1. `init-training-data.sql`
SQL script containing initial master data for the training service.

**What it loads:**
- ✅ 20 Disciplines (muscle groups and exercise categories)
- ✅ 27 System Exercises (pre-defined exercises for all users)
- ✅ 8 Routine Templates (pre-designed training programs)

**How to load:**
```bash
# Using psql command line
psql -U gym_admin -d gym_db -f init-training-data.sql

# Or via Docker
docker exec -it gym-postgres psql -U gym_admin -d gym_db -f /docker-entrypoint-initdb.d/init-training-data.sql
```

**Data Included:**

**Disciplines (20 total):**
- Strength: Chest, Back, Shoulders, Biceps, Triceps, Forearms, Legs, Quads, Hamstrings, Glutes, Calves, Core
- Cardio: Running, Cycling, Swimming
- Flexibility: Stretching
- Mind-Body: Yoga, Pilates
- Sports: CrossFit, Boxing

**System Exercises (27 total):**
- Compound movements: Barbell Bench Press, Deadlift, Squats, Pull-ups
- Isolation exercises: Cable Flyes, Bicep Curls, Tricep Dips
- Cardio: Treadmill Running, Stationary Cycling, Swimming Laps
- Core: Crunches, Planks, Russian Twists

**Routine Templates (8 total):**
1. Beginner Full Body (3 days/week)
2. Upper/Lower Split (4 days/week)
3. Push/Pull/Legs (6 days/week)
4. Strength Focus (4 days/week)
5. Hypertrophy Focus (5 days/week)
6. Cardio & Core (3 days/week)
7. CrossFit Inspired
8. Endurance Training

### 2. `Gym-Training-Service.postman_collection.json`
Complete Postman collection for testing all Training Service endpoints with pagination support.

**How to import:**
1. Open Postman
2. Click **Import** button
3. Select the JSON file
4. Click **Import**

**Collection Structure:**
```
├── 🔧 Setup
│   └── Health Check
├── 📋 Exercise Management (10 endpoints - 4 paginated)
├── 🎯 Routine Template Management (10 endpoints - 4 paginated)
├── 👤 User Routine Management (12 endpoints - 4 paginated)
└── 💪 Exercise Session Management (10 endpoints - 4 paginated)
```

**Total endpoints: 42 requests**
- Single resource endpoints (no pagination): 14
- List endpoints with default pagination: 12
- List endpoints with custom pagination examples: 12
- Non-paginated operations (create, update, delete): 4

---

## 🧪 Testing Workflow

### Step 1: Start the Services
```bash
# Start Docker containers
docker-compose up -d

# Verify services are running
docker-compose ps
```

### Step 2: Load Initial Data
```bash
# Load SQL initial data
psql -U gym_admin -d gym_db -f init-training-data.sql
```

### Step 3: Health Check
In Postman:
1. Go to **Setup** → **Health Check**
2. Click **Send**
3. Verify response status is 200

### Step 4: Test Exercise Endpoints

#### Get System Exercises
```
GET /api/v1/exercises/system
Header: X-User-Id: 1
```

#### Get Exercise by Discipline
```
GET /api/v1/exercises/discipline/1
Header: X-User-Id: 1
```
(Use discipline ID from initial data)

#### Create Custom Exercise
```
POST /api/v1/exercises
Header: X-User-Id: 1
Body:
{
  "name": "My Custom Push-up",
  "description": "Diamond push-ups for tricep focus",
  "type": "USER",
  "disciplineId": 1
}
```

### Step 5: Test Routine Template Endpoints

#### Get System Routine Templates
```
GET /api/v1/routine-templates/system
Header: X-User-Id: 1
```

#### Get Routine Template by ID
```
GET /api/v1/routine-templates/1
Header: X-User-Id: 1
```
(Use template ID from initial data)

### Step 6: Test User Routine Endpoints

#### Assign Routine Template to User
```
POST /api/v1/user-routines/assign
Header: X-User-Id: 1
Body:
{
  "routineTemplateId": 1,
  "isActive": true
}
```

#### Get User's Active Routines
```
GET /api/v1/user-routines/active
Header: X-User-Id: 1
```

### Step 7: Test Exercise Session Endpoints

#### Log Exercise Session
```
POST /api/v1/exercise-sessions
Header: X-User-Id: 1
Body:
{
  "userRoutineId": 1,
  "exerciseId": 1,
  "sets": 3,
  "reps": 12,
  "weight": 100.0,
  "duration": 600,
  "notes": "Good form",
  "sessionDate": "2025-03-18T10:30:00"
}
```

#### Get Sessions by Date
```
GET /api/v1/exercise-sessions/date/2025-03-18
Header: X-User-Id: 1
```

---

## 📄 Pagination Guide

### Overview
All list endpoints in the Gym Training Service support pagination. This allows efficient retrieval of large datasets with configurable page size and sorting.

### Pagination Parameters

All paginated endpoints accept these query parameters:

| Parameter | Type | Default | Max | Description |
|-----------|------|---------|-----|-------------|
| `page` | integer | 0 | N/A | Zero-indexed page number (0 = first page) |
| `size` | integer | 20 | 100 | Number of records per page |
| `sort` | string | createdAt,desc | N/A | Sort order (field,direction) |

### Pagination Response Format

All paginated endpoints return a `PageResponse` object with:

```json
{
  "data": [
    { "id": 1, "name": "Exercise 1", ... },
    { "id": 2, "name": "Exercise 2", ... }
  ],
  "currentPage": 0,
  "pageSize": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

**Response Fields:**
- `data` - Array of results for current page
- `currentPage` - Current page number (0-indexed)
- `pageSize` - Records per page
- `totalElements` - Total records across all pages
- `totalPages` - Total number of pages
- `hasNext` - Whether next page exists
- `hasPrevious` - Whether previous page exists

### Paginated Endpoints

**Exercise Management (3 endpoints):**
- `GET /api/v1/exercises/system` - System exercises list
- `GET /api/v1/exercises/discipline/{id}` - Exercises by discipline
- `GET /api/v1/exercises/my-exercises` - User's custom exercises

**Routine Template Management (2 endpoints):**
- `GET /api/v1/routine-templates/system` - System templates list
- `GET /api/v1/routine-templates/my-templates` - User's custom templates

**User Routine Management (2 endpoints):**
- `GET /api/v1/user-routines/active` - User's active routines
- `GET /api/v1/user-routines` - All user routines

**Exercise Session Management (2 endpoints):**
- `GET /api/v1/exercise-sessions/routine/{id}` - Sessions by routine
- `GET /api/v1/exercise-sessions/date/{date}` - Sessions by date

### Usage Examples

#### Default Pagination (first 20 records)
```bash
curl -X GET "http://localhost:8082/api/v1/exercises/system" \
  -H "X-User-Id: 1"
```

#### Custom Page Size
```bash
# Get 50 records per page, page 0
curl -X GET "http://localhost:8082/api/v1/exercises/system?page=0&size=50" \
  -H "X-User-Id: 1"
```

#### Custom Sorting
```bash
# Sort by name ascending
curl -X GET "http://localhost:8082/api/v1/exercises/system?sort=name,asc" \
  -H "X-User-Id: 1"

# Sort by creation date descending (default)
curl -X GET "http://localhost:8082/api/v1/exercises/system?sort=createdAt,desc" \
  -H "X-User-Id: 1"
```

#### Pagination with Sorting
```bash
# Page 1, size 30, sorted by name
curl -X GET "http://localhost:8082/api/v1/exercises/system?page=1&size=30&sort=name,asc" \
  -H "X-User-Id: 1"
```

#### Get Next Page
```bash
# Response indicates hasNext=true
# Get next page:
curl -X GET "http://localhost:8082/api/v1/exercises/system?page=1&size=20" \
  -H "X-User-Id: 1"
```

### Postman Usage

The collection includes both default and custom pagination examples for each paginated endpoint:

1. **Default Pagination Requests** (e.g., "Get All System Exercises")
   - No query parameters
   - Uses service defaults: page=0, size=20, sort=createdAt,desc

2. **Custom Pagination Requests** (e.g., "Get All System Exercises (Custom Pagination)")
   - Includes example query parameters
   - Shows how to adjust page size and sorting
   - Modify values as needed for testing

**To test pagination in Postman:**
1. Open request "Get All System Exercises (Custom Pagination)"
2. Modify `page`, `size`, or `sort` parameters in the URL
3. Click **Send**
4. Examine `totalElements` and `totalPages` to understand dataset size
5. Navigate to next page by incrementing `page` parameter

### Common Sorting Fields

By endpoint:

**Exercises:**
- `id` - Exercise ID
- `name` - Exercise name
- `createdAt` - Creation date (default)
- `type` - Exercise type (SYSTEM or USER)

**Routine Templates:**
- `id` - Template ID
- `name` - Template name
- `createdAt` - Creation date (default)
- `type` - Template type (SYSTEM or USER)

**User Routines:**
- `id` - Routine ID
- `startDate` - Start date (default)
- `createdAt` - Creation date
- `isActive` - Active status

**Exercise Sessions:**
- `id` - Session ID
- `sessionDate` - Session date (default)
- `createdAt` - Creation date
- `sets` - Sets completed

### Error Handling

**Invalid Page Number:**
```json
{
  "status": "BAD_REQUEST",
  "message": "Page index must not be less than zero"
}
```

**Invalid Page Size (> 100):**
```json
{
  "status": "BAD_REQUEST",
  "message": "Page size must not exceed maximum of 100"
}
```

**Invalid Sort Field:**
```json
{
  "status": "BAD_REQUEST",
  "message": "Unknown sort field: invalidField"
}
```

---

## 📝 Testing Scenarios

### Scenario 1: Create a Complete Training Program

1. **Start with a System Template**
   - Get available templates: `GET /api/v1/routine-templates/system`
   - Assign template: `POST /api/v1/user-routines/assign` with templateId=1

2. **View User Routines**
   - Get all routines: `GET /api/v1/user-routines`
   - Get active routines: `GET /api/v1/user-routines/active`

3. **Log Workout Sessions**
   - Log session: `POST /api/v1/exercise-sessions`
   - Get sessions by date: `GET /api/v1/exercise-sessions/date/2025-03-18`

### Scenario 2: Create Custom Exercise

1. **Create Exercise**
   - `POST /api/v1/exercises` with custom name and discipline

2. **View User Exercises**
   - Get custom exercises: `GET /api/v1/exercises/user`

3. **Use in Session**
   - Log session with custom exercise ID

### Scenario 3: Update and Track Progress

1. **Update Exercise Session**
   - Log initial session: `POST /api/v1/exercise-sessions`
   - Update with new metrics: `PUT /api/v1/exercise-sessions/{id}`

2. **Get Session History**
   - View sessions by routine: `GET /api/v1/exercise-sessions/routine/{id}`
   - View sessions by date: `GET /api/v1/exercise-sessions/date/2025-03-18`

---

## 🔑 Important Headers

All requests require the following header:
```
X-User-Id: <numeric_user_id>
```

This header identifies which user is making the request and is used for authorization checks.

**Example with different users:**
```
# User 1 can only see/modify their own data
X-User-Id: 1

# User 2 has isolated data
X-User-Id: 2
```

---

## ⚠️ Authorization Rules

1. **System Data (Read-Only)**
   - System exercises: Anyone can read
   - System routine templates: Anyone can read
   - No modifications allowed

2. **User Custom Data**
   - Custom exercises: Only creator can modify/delete
   - Custom routines: Only creator can modify/delete
   - User routines: Only user can modify/delete
   - Exercise sessions: Only user can modify/delete

3. **Cross-User Access Prevention**
   - User 1 cannot modify User 2's routines
   - User 1 cannot see User 2's sessions
   - System validates user ownership via X-User-Id header

---

## 📊 Database Schema

### Key Tables
- `disciplines` - Exercise categories
- `exercises` - Individual exercises
- `routine_templates` - Pre-designed routines
- `user_routines` - User assignments to templates
- `exercise_sessions` - Logged workout data

### Relationships
```
Discipline 1←→* Exercise
RoutineTemplate 1←→* UserRoutine
UserRoutine 1←→* ExerciseSession
Exercise 1←→* ExerciseSession
```

---

## 🐛 Troubleshooting

### Error: "Routine not found or unauthorized"
- Check that X-User-Id header matches the user who created the routine
- Verify the routine ID exists

### Error: "Exercise not found: X"
- Verify exercise ID is correct
- Check if exercise belongs to the right discipline

### Empty response from GET requests
- Initial data might not be loaded
- Run `init-training-data.sql` script
- Verify correct X-User-Id header

### 401 Unauthorized
- Check X-User-Id header is present
- Verify header format: `X-User-Id: 1` (numeric)

---

## 📈 Next Steps

After testing the Training Service:

1. **Phase 4b - Controllers**: REST endpoints will be implemented with validation and error handling
2. **Phase 5 - Tracking Service**: Progress tracking and statistics
3. **Phase 6 - Notification Service**: Workout reminders and notifications
4. **Phase 7 - Integration**: Full system deployment and testing

---

## 🔄 Updating Collections

This collection will be updated as new endpoints are added. The update process:

1. New endpoints are added to the controller
2. This Postman collection is updated
3. New test scenarios are documented

**Version Control:**
- Collection version: 1.0.0
- Last updated: 2025-03-18
- Training Service endpoints: 25 (Phase 4a)

---

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review service logs: `docker logs gym-training-service`
3. Verify database connection: `docker exec gym-postgres psql -U gym_admin -d gym_db -c "SELECT COUNT(*) FROM training_schema.disciplines;"`

---

**Last Updated:** 2025-03-18  
**Collection Version:** 2.0.0 (Phase 4b - Controllers with Pagination Support)
**Total Endpoints:** 42 requests
**Paginated Endpoints:** 9 (with 12 example requests including default and custom pagination)
