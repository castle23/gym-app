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
Complete Postman collection for testing all Training Service endpoints.

**How to import:**
1. Open Postman
2. Click **Import** button
3. Select the JSON file
4. Click **Import**

**Collection Structure:**
```
├── 🔧 Setup
│   └── Health Check
├── 📋 Exercise Management (6 endpoints)
├── 🎯 Routine Template Management (6 endpoints)
├── 👤 User Routine Management (7 endpoints)
└── 💪 Exercise Session Management (6 endpoints)
```

**Total endpoints: 25 requests**

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
**Collection Version:** 1.0.0 (Phase 4a - Services Only)
