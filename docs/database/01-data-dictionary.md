# Data Dictionary - Gym Platform API

Complete reference of all database entities, relationships, and schemas across the Gym Platform microservices.

**Last Updated:** March 21, 2026  
**Services Covered:** Auth, Training, Tracking, Notification Services

---

## Table of Contents

1. [Overview](#overview)
2. [Auth Service Entities](#auth-service-entities)
3. [Training Service Entities](#training-service-entities)
4. [Tracking Service Entities](#tracking-service-entities)
5. [Notification Service Entities](#notification-service-entities)
6. [Entity Relationships](#entity-relationships)
7. [Common Queries](#common-queries)
8. [Data Integrity Rules](#data-integrity-rules)
9. [Performance Notes](#performance-notes)

---

## Overview

### Data Model Philosophy

The Gym Platform uses PostgreSQL with:
- UUID primary keys (distributed system friendly)
- Timestamp fields (created_at, updated_at) on all entities
- Foreign key constraints (relational integrity)
- Indexes on frequently queried fields
- Encrypted sensitive columns (passwords, health data)

### High-Level Architecture

```
Auth Service (manages identity)
  ├── users (core accounts)
  ├── roles (admin, trainer, user)
  ├── permissions (granular access)
  └── sessions (active login sessions)

Training Service (manages workouts)
  ├── disciplines (exercise categories)
  ├── exercises (individual movements)
  ├── routines (pre-built programs)
  └── workouts (user sessions)

Tracking Service (manages metrics)
  ├── diet_logs (food tracking)
  ├── weight_logs (weight measurements)
  ├── metrics (computed statistics)
  ├── goals (user objectives)
  └── progress (goal tracking)

Notification Service (manages communications)
  ├── notifications (notification records)
  ├── notification_preferences (user settings)
  ├── delivery_logs (delivery tracking)
  └── notification_templates (message templates)
```

Each service has its own database (ADR-001: Microservices Architecture).

---

## Auth Service Entities

### Table: users

**Purpose:** Store user account information (core identity)

**Location:** Auth Service Database

**Schema:**
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash BYTEA NOT NULL,  -- encrypted with pgcrypto
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  date_of_birth DATE,
  phone_number VARCHAR(20),
  profile_picture_url VARCHAR(500),
  bio TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  is_active BOOLEAN DEFAULT true,
  last_login TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-----------|-------------|
| id | UUID | PK, Default: gen_random_uuid() | Unique identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User email (login credential) |
| password_hash | BYTEA | NOT NULL | Encrypted password (bcrypt + pgcrypto) |
| first_name | VARCHAR(100) | NULL | User's first name |
| last_name | VARCHAR(100) | NULL | User's last name |
| date_of_birth | DATE | NULL | Birth date (for age calculation) |
| phone_number | VARCHAR(20) | NULL | Contact phone number |
| profile_picture_url | VARCHAR(500) | NULL | URL to profile image |
| bio | TEXT | NULL | User bio/description |
| created_at | TIMESTAMP | NOT NULL | Account creation time (UTC) |
| updated_at | TIMESTAMP | NOT NULL | Last update time (UTC) |
| is_active | BOOLEAN | DEFAULT true | Account active status |
| last_login | TIMESTAMP | NULL | Last login timestamp |

**Relationships:**
- One-to-many: `user_roles` (user can have multiple roles)
- One-to-many: `sessions` (user can have multiple active sessions)
- Referenced by: Training Service workouts, Tracking Service logs, Notification Service notifications

**Indexes:**
- `email` (UNIQUE) - for login lookups
- `created_at` - for user list pagination

**Security Notes:**
- Password stored as hash only (never plaintext)
- Email validated and trimmed
- PII (date_of_birth) should be masked in logs
- Encryption key stored separately (ADR-011)

**Example Query:**
```sql
-- Get user profile with email
SELECT id, email, first_name, last_name, created_at, is_active
FROM users
WHERE id = $1 AND is_active = true;
```

---

### Table: roles

**Purpose:** Define role types in the system

**Schema:**
```sql
CREATE TABLE roles (
  id INT PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO roles (id, name, description) VALUES
  (1, 'admin', 'Full platform access, user management'),
  (2, 'trainer', 'Can manage assigned users and content'),
  (3, 'user', 'Standard user, access own data only');
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| id | INT | PK, Fixed values (1=admin, 2=trainer, 3=user) |
| name | VARCHAR(50) | Role name (admin, trainer, user) |
| description | TEXT | Role description and permissions |
| created_at | TIMESTAMP | Record creation time |

**Pre-loaded Values:**
- `1, 'admin'` - Full platform access
- `2, 'trainer'` - Can manage assigned users
- `3, 'user'` - Basic user access

**Relationships:**
- One-to-many: `user_roles` (users can have multiple roles)

---

### Table: user_roles

**Purpose:** Map users to roles (many-to-many relationship)

**Schema:**
```sql
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id INT NOT NULL REFERENCES roles(id),
  assigned_at TIMESTAMP DEFAULT NOW(),
  assigned_by UUID REFERENCES users(id),
  PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| user_id | UUID | FK to users, part of composite PK |
| role_id | INT | FK to roles, part of composite PK |
| assigned_at | TIMESTAMP | When role was assigned |
| assigned_by | UUID | Which admin assigned the role |

**Example Query:**
```sql
-- Get all roles for a user
SELECT r.id, r.name, r.description
FROM user_roles ur
JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = $1;
```

---

### Table: permissions

**Purpose:** Define granular permissions (resource + action)

**Schema:**
```sql
CREATE TABLE permissions (
  id INT PRIMARY KEY,
  resource VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL,
  description TEXT,
  UNIQUE(resource, action)
);
```

**Common Permissions:**
- `users` / `read` - Read user data
- `users` / `write` - Create/update users
- `exercises` / `read` - View exercises
- `workouts` / `write` - Create workouts
- `admin` / `*` - All admin operations

**Note:** Currently, roles have implicit permissions. Can be expanded to explicit permission mappings if needed.

---

### Table: sessions

**Purpose:** Track active user sessions and JWT tokens

**Schema:**
```sql
CREATE TABLE sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  jwt_token TEXT NOT NULL,
  device_info VARCHAR(500),
  ip_address VARCHAR(45),
  user_agent TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMP NOT NULL,
  last_activity TIMESTAMP NOT NULL DEFAULT NOW(),
  is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at);
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | PK, Session identifier |
| user_id | UUID | FK to users |
| jwt_token | TEXT | JWT token (stored for blacklist purposes) |
| device_info | VARCHAR(500) | Device type/model |
| ip_address | VARCHAR(45) | Client IP (IPv4/IPv6) |
| user_agent | TEXT | Browser/client information |
| created_at | TIMESTAMP | Session creation time |
| expires_at | TIMESTAMP | Token expiration time (usually 24h) |
| last_activity | TIMESTAMP | Last request time |
| is_active | BOOLEAN | Session is still valid |

**Purpose:** Track JWT tokens for:
- Session management
- Token blacklist (revocation)
- Device/location tracking
- Security audits

**Cleanup:** Sessions older than 7 days can be archived or deleted.

---

## Training Service Entities

### Table: disciplines

**Purpose:** Define exercise categories/muscle groups

**Schema:**
```sql
CREATE TABLE disciplines (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) UNIQUE NOT NULL,
  description TEXT,
  icon_url VARCHAR(500),
  order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_disciplines_name ON disciplines(name);
```

**Common Disciplines:**
- Chest
- Back
- Legs
- Shoulders
- Arms (Biceps, Triceps)
- Core/Abs
- Cardio
- Stretching

**Relationships:**
- One-to-many: `exercises` (multiple exercises per discipline)

---

### Table: exercises

**Purpose:** Define individual exercises/movements

**Schema:**
```sql
CREATE TABLE exercises (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  discipline_id UUID NOT NULL REFERENCES disciplines(id),
  description TEXT,
  instructions TEXT,
  equipment_needed VARCHAR(500),
  difficulty_level INT CHECK (difficulty_level BETWEEN 1 AND 5),
  muscle_groups TEXT[],
  video_url VARCHAR(500),
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(name, discipline_id)
);

CREATE INDEX idx_exercises_discipline_id ON exercises(discipline_id);
CREATE INDEX idx_exercises_name ON exercises(name);
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(255) | Exercise name (e.g., "Bench Press") |
| discipline_id | UUID | FK to disciplines |
| description | TEXT | Detailed description |
| instructions | TEXT | How to perform the exercise |
| equipment_needed | VARCHAR(500) | Equipment (Barbell, Dumbbell, Machine, etc.) |
| difficulty_level | INT | 1 (beginner) to 5 (expert) |
| muscle_groups | TEXT[] | Array of targeted muscle groups |
| video_url | VARCHAR(500) | URL to instructional video |
| is_active | BOOLEAN | Exercise available for use |
| created_at | TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | Last modification |

**Example Exercises:**
- Bench Press (Chest, Intermediate)
- Squat (Legs, Intermediate)
- Deadlift (Back, Advanced)
- Barbell Curl (Arms, Beginner)

---

### Table: routines

**Purpose:** Pre-built workout programs

**Schema:**
```sql
CREATE TABLE routines (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  difficulty_level INT CHECK (difficulty_level BETWEEN 1 AND 5),
  duration_weeks INT NOT NULL,
  goal VARCHAR(100),  -- strength, hypertrophy, endurance, weight_loss
  created_by UUID REFERENCES users(id),
  is_published BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_routines_goal ON routines(goal);
```

**Pre-built Programs:**
- 5x5 Strength Program (8 weeks, Strength)
- PPL Split (12 weeks, Hypertrophy)
- Full Body 3x Week (6 weeks, General)
- Couch to 5K (8 weeks, Cardio)

---

### Table: workouts

**Purpose:** Individual user workout sessions

**Schema:**
```sql
CREATE TABLE workouts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  routine_id UUID REFERENCES routines(id),  -- NULL for custom workouts
  name VARCHAR(255),
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  notes TEXT,
  is_completed BOOLEAN DEFAULT false,
  total_volume DECIMAL(10,2),  -- computed: sum of weight*reps*sets
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_workouts_user_id ON workouts(user_id);
CREATE INDEX idx_workouts_start_time ON workouts(start_time);
CREATE INDEX idx_workouts_completed ON workouts(is_completed);
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK to users |
| routine_id | UUID | FK to routines (NULL for custom) |
| name | VARCHAR(255) | Workout name (e.g., "Chest Day") |
| start_time | TIMESTAMP | Workout start |
| end_time | TIMESTAMP | Workout end (NULL if ongoing) |
| notes | TEXT | Post-workout notes |
| is_completed | BOOLEAN | Workout completed status |
| total_volume | DECIMAL(10,2) | Sum of weight × reps × sets |
| created_at | TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | Last update |

**Relationships:**
- One-to-many: `workout_exercises` (exercises in this workout)
- Many-to-one: `users` and `routines`

---

### Table: workout_exercises

**Purpose:** Individual exercises within a workout session

**Schema:**
```sql
CREATE TABLE workout_exercises (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workout_id UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
  exercise_id UUID NOT NULL REFERENCES exercises(id),
  sets INT NOT NULL DEFAULT 3,
  reps INT,  -- NULL for timed exercises
  weight_kg DECIMAL(8,2),  -- NULL for bodyweight
  duration_seconds INT,  -- NULL for weighted exercises
  rest_seconds INT DEFAULT 60,
  notes TEXT,
  exercise_order INT NOT NULL,  -- sequence in workout
  is_completed BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_workout_exercises_workout_id ON workout_exercises(workout_id);
```

**Example:**
- Exercise: Bench Press, Sets: 4, Reps: 5, Weight: 100kg, Rest: 180s
- Exercise: Treadmill, Duration: 600s (10 min), No weight

---

## Tracking Service Entities

### Table: diet_logs

**Purpose:** Food and calorie tracking

**Schema:**
```sql
CREATE TABLE diet_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  log_date DATE NOT NULL,
  meal_type VARCHAR(50) NOT NULL,  -- breakfast, lunch, dinner, snack
  food_name VARCHAR(255) NOT NULL,
  calories DECIMAL(8,2) NOT NULL,
  protein_g DECIMAL(8,2),
  carbs_g DECIMAL(8,2),
  fat_g DECIMAL(8,2),
  fiber_g DECIMAL(8,2),
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_diet_logs_user_id_date ON diet_logs(user_id, log_date);
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK to users |
| log_date | DATE | Date of meal |
| meal_type | VARCHAR(50) | breakfast, lunch, dinner, snack |
| food_name | VARCHAR(255) | Name of food item |
| calories | DECIMAL(8,2) | Total calories |
| protein_g | DECIMAL(8,2) | Protein in grams |
| carbs_g | DECIMAL(8,2) | Carbohydrates in grams |
| fat_g | DECIMAL(8,2) | Fat in grams |
| fiber_g | DECIMAL(8,2) | Dietary fiber in grams |
| notes | TEXT | Additional notes |
| created_at | TIMESTAMP | Log creation time |
| updated_at | TIMESTAMP | Last update |

---

### Table: weight_logs

**Purpose:** Daily weight measurements

**Schema:**
```sql
CREATE TABLE weight_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  log_date DATE NOT NULL,
  weight_kg DECIMAL(8,2) NOT NULL,
  body_fat_percent DECIMAL(5,2),
  muscle_mass_kg DECIMAL(8,2),
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(user_id, log_date)
);

CREATE INDEX idx_weight_logs_user_date ON weight_logs(user_id, log_date);
```

**Columns:**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK to users |
| log_date | DATE | Date of measurement (UNIQUE per user per day) |
| weight_kg | DECIMAL(8,2) | Body weight in kilograms |
| body_fat_percent | DECIMAL(5,2) | Body fat percentage |
| muscle_mass_kg | DECIMAL(8,2) | Estimated muscle mass |
| notes | TEXT | Notes about measurement |
| created_at | TIMESTAMP | Record creation |

---

### Table: metrics

**Purpose:** Pre-computed daily statistics (for performance)

**Schema:**
```sql
CREATE TABLE metrics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  metric_date DATE NOT NULL,
  total_calories_logged DECIMAL(10,2) DEFAULT 0,
  avg_daily_calories DECIMAL(10,2) DEFAULT 0,
  workouts_completed INT DEFAULT 0,
  total_workout_minutes INT DEFAULT 0,
  weight_kg DECIMAL(8,2),
  weight_change_kg DECIMAL(8,2),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(user_id, metric_date)
);

CREATE INDEX idx_metrics_user_date ON metrics(user_id, metric_date);
```

**Purpose:** Dashboard performance optimization - pre-calculated metrics so dashboard doesn't need to calculate on each load.

**Maintenance:** 
- Updated daily via scheduled job
- Recalculated when diet/weight logs updated
- Lookback: calculate 7-day, 30-day averages

---

### Table: goals

**Purpose:** User fitness goals and objectives

**Schema:**
```sql
CREATE TABLE goals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  goal_type VARCHAR(50) NOT NULL,  -- weight_loss, muscle_gain, strength, endurance
  target_value DECIMAL(10,2) NOT NULL,
  current_value DECIMAL(10,2),
  unit VARCHAR(20),  -- kg, calories, repetitions, minutes
  start_date DATE NOT NULL,
  target_date DATE NOT NULL,
  achieved_date DATE,
  status VARCHAR(50) DEFAULT 'active',  -- active, achieved, abandoned
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_goals_user_status ON goals(user_id, status);
```

**Example Goals:**
- Lose 10kg by 2026-06-21
- Bench press 100kg by 2026-12-31
- Run 5km in under 30 minutes
- 2000 calories per day average

---

### Table: progress

**Purpose:** Track progress checkpoints toward goals

**Schema:**
```sql
CREATE TABLE progress (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
  checkpoint_date DATE NOT NULL,
  value DECIMAL(10,2) NOT NULL,
  percentage_complete DECIMAL(5,2),  -- 0-100
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_progress_goal_date ON progress(goal_id, checkpoint_date);
```

**Purpose:** Track progress snapshots (e.g., every 2 weeks weigh in to check weight loss goal progress)

---

## Notification Service Entities

### Table: notifications

**Purpose:** Store user notifications

**Schema:**
```sql
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type VARCHAR(50) NOT NULL,  -- achievement, reminder, alert, message, milestone
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  data JSONB,  -- extra data (goal_id, achievement_name, etc.)
  is_read BOOLEAN DEFAULT false,
  read_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
```

**Notification Types:**
- `achievement` - User hit a milestone (1000 calories burned)
- `reminder` - Scheduled reminder (time to log weight)
- `alert` - Important notification (goal off-track)
- `message` - Message from coach/trainer
- `milestone` - Goal achieved

---

### Table: notification_preferences

**Purpose:** User notification settings

**Schema:**
```sql
CREATE TABLE notification_preferences (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  notification_type VARCHAR(50) NOT NULL,
  email_enabled BOOLEAN DEFAULT true,
  push_enabled BOOLEAN DEFAULT true,
  sms_enabled BOOLEAN DEFAULT false,
  frequency VARCHAR(50),  -- immediate, daily, weekly
  updated_at TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (user_id, notification_type)
);
```

**Allows users to control:**
- Which notification types they receive
- Delivery channel (email, push, SMS)
- Frequency (immediate, daily digest, weekly)

---

### Table: delivery_logs

**Purpose:** Track notification delivery attempts

**Schema:**
```sql
CREATE TABLE delivery_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  notification_id UUID NOT NULL REFERENCES notifications(id),
  channel VARCHAR(50) NOT NULL,  -- email, push, sms
  status VARCHAR(50),  -- sent, pending, failed, bounced
  error_message TEXT,
  sent_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_delivery_logs_status ON delivery_logs(status);
```

**Purpose:** Track delivery success/failure for troubleshooting and retries

---

### Table: notification_templates

**Purpose:** Message templates for notifications

**Schema:**
```sql
CREATE TABLE notification_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) UNIQUE NOT NULL,
  type VARCHAR(50),  -- achievement, reminder, alert
  channel VARCHAR(50),  -- email, push, sms
  subject VARCHAR(255),
  body_template TEXT,  -- with {{placeholders}}
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

**Example Templates:**
- `achievement_milestone` - "Congratulations {{user_name}}! You've completed {{count}} workouts!"
- `goal_alert` - "Your goal '{{goal_name}}' is off-track. Keep pushing!"
- `weight_reminder` - "Time to log your weight! 📊"

---

## Entity Relationships

### Cross-Service Relationships

```
Auth Service (users table)
    ↓ 
    ├→ Training Service (user_id in workouts)
    ├→ Tracking Service (user_id in diet_logs, weight_logs, goals)
    └→ Notification Service (user_id in notifications)

Training Service (workouts)
    ↓
    └→ Tracking Service (updates metrics when workout completed)

Tracking Service (goals)
    ↓
    └→ Notification Service (sends notification when goal achieved)
```

### One-to-Many Relationships

```
users (1) → (M) user_roles
users (1) → (M) sessions
users (1) → (M) workouts
users (1) → (M) diet_logs
users (1) → (M) weight_logs
users (1) → (M) goals
users (1) → (M) notifications

disciplines (1) → (M) exercises
exercises (1) → (M) workout_exercises
routines (1) → (M) workouts
workouts (1) → (M) workout_exercises
goals (1) → (M) progress

notifications (1) → (M) delivery_logs
```

### Many-to-Many Relationships

```
users (M) ← user_roles → (M) roles
```

---

## Common Queries

### User Management

**Get user profile with roles:**
```sql
SELECT u.id, u.email, u.first_name, u.last_name, 
       array_agg(r.name) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id = $1
GROUP BY u.id, u.email, u.first_name, u.last_name;
```

**Get all admins:**
```sql
SELECT DISTINCT u.id, u.email
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role_id = 1;  -- admin role
```

### Workout Analytics

**Get user's recent workouts with exercise count:**
```sql
SELECT w.id, w.name, w.start_time, w.end_time,
       COUNT(we.id) as exercise_count,
       SUM(COALESCE(we.sets, 0)) as total_sets
FROM workouts w
LEFT JOIN workout_exercises we ON w.id = we.workout_id
WHERE w.user_id = $1 AND w.is_completed = true
GROUP BY w.id
ORDER BY w.start_time DESC
LIMIT 10;
```

**Get workout frequency over time:**
```sql
SELECT DATE_TRUNC('week', w.start_time) as week,
       COUNT(*) as workout_count
FROM workouts w
WHERE w.user_id = $1 
  AND w.is_completed = true
  AND w.start_time >= CURRENT_DATE - INTERVAL '12 weeks'
GROUP BY DATE_TRUNC('week', w.start_time)
ORDER BY week DESC;
```

### Weight Tracking

**Get weight trend (7-day moving average):**
```sql
SELECT log_date, weight_kg,
       ROUND(AVG(weight_kg) OVER (
         ORDER BY log_date 
         ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       ), 2) as avg_7day
FROM weight_logs
WHERE user_id = $1 
  AND log_date >= CURRENT_DATE - INTERVAL '90 days'
ORDER BY log_date;
```

**Calculate weight change:**
```sql
SELECT 
  (SELECT weight_kg FROM weight_logs 
   WHERE user_id = $1 
   ORDER BY log_date DESC LIMIT 1) as current_weight,
  (SELECT weight_kg FROM weight_logs 
   WHERE user_id = $1 
   ORDER BY log_date ASC LIMIT 1) as start_weight,
  (SELECT weight_kg FROM weight_logs 
   WHERE user_id = $1 
   ORDER BY log_date DESC LIMIT 1) -
  (SELECT weight_kg FROM weight_logs 
   WHERE user_id = $1 
   ORDER BY log_date ASC LIMIT 1) as weight_change;
```

### Calorie Tracking

**Daily calorie total:**
```sql
SELECT log_date, 
       SUM(calories) as total_calories,
       COUNT(*) as meal_count
FROM diet_logs
WHERE user_id = $1 AND log_date = $2
GROUP BY log_date;
```

**Calorie average over 30 days:**
```sql
SELECT AVG(daily_total) as avg_daily_calories
FROM (
  SELECT DATE(created_at) as log_date, 
         SUM(calories) as daily_total
  FROM diet_logs
  WHERE user_id = $1 
    AND created_at >= CURRENT_DATE - INTERVAL '30 days'
  GROUP BY DATE(created_at)
) subquery;
```

### Goal Progress

**Check goal achievement:**
```sql
SELECT id, goal_type, target_value, current_value,
       ROUND((current_value / target_value * 100), 2) as percentage_complete,
       target_date,
       CASE 
         WHEN achieved_date IS NOT NULL THEN 'achieved'
         WHEN target_date < CURRENT_DATE THEN 'expired'
         ELSE 'active'
       END as status
FROM goals
WHERE user_id = $1
ORDER BY target_date DESC;
```

---

## Data Integrity Rules

### Domain Constraints

**Users:**
- Email must be valid format and unique
- Password hash cannot be null
- created_at must not be in future

**Workouts:**
- start_time must be ≤ end_time
- user_id must reference existing user
- is_completed cannot be true if end_time is null

**Diet Logs:**
- calories must be > 0
- log_date cannot be in future
- meal_type must be one of: breakfast, lunch, dinner, snack

**Weight Logs:**
- weight_kg must be > 0 and reasonable (30-300 kg)
- One log per user per day (UNIQUE constraint)
- log_date cannot be in future

**Goals:**
- target_date must be after start_date
- target_value must be > 0
- goal_type must be one of: weight_loss, muscle_gain, strength, endurance

### Cascading Rules

- When user deleted: all sessions, workouts, logs, goals cascade deleted
- When goal deleted: all progress records cascade deleted
- When workout deleted: all workout_exercises cascade deleted

### Timestamping Rules

- All timestamps in UTC
- created_at never changes after insert
- updated_at auto-updated on any modification
- No future dates except target_date fields

---

## Performance Notes

### Indexes

**Critical Indexes (must have):**
- `users(email)` - Login lookups
- `workouts(user_id, is_completed)` - User dashboard
- `weight_logs(user_id, log_date)` - Weight tracking
- `diet_logs(user_id, log_date)` - Calorie tracking
- `notifications(user_id, is_read)` - Notification inbox

**Optional Indexes (consider if slow):**
- `workouts(start_time)` - Timeline views
- `exercises(discipline_id)` - Exercise lists
- `goals(user_id, status)` - Goal filtering

### Query Optimization

**N+1 Problem (avoid):**
```sql
-- BAD: N+1 queries
SELECT * FROM workouts WHERE user_id = $1;
-- then for each workout:
SELECT * FROM workout_exercises WHERE workout_id = $2;

-- GOOD: Single query with JOIN
SELECT w.*, count(we.id) as exercise_count
FROM workouts w
LEFT JOIN workout_exercises we ON w.id = we.workout_id
WHERE w.user_id = $1
GROUP BY w.id;
```

**Use Precomputed Metrics:**
- Don't recalculate stats on every dashboard load
- Update metrics table nightly
- Cache dashboard views in Redis (ADR-012)

### Connection Pooling

- Use PgBouncer for connection pooling (ADR-008)
- Config: 25 connections per application instance
- Reuse connections across requests

### Partitioning (Future)

For very large tables (100M+ rows):
- Partition `diet_logs` by month
- Partition `weight_logs` by quarter
- Partition `notifications` by month

---

## Migration Notes

### Current Schema Version

Version 1.0.0 (deployed March 2026)

### Applying Migrations

```bash
# List pending migrations
flyway info

# Apply migrations
flyway migrate

# Rollback (if needed)
flyway undo
```

### Adding New Columns

Always:
1. Add column as nullable
2. Deploy code that uses column
3. Back-populate data
4. Add NOT NULL constraint in separate migration

### Schema Changes

Before any schema change:
1. Backup database (ADR-009)
2. Test on staging
3. Plan rollback procedure
4. Schedule low-traffic window
5. Execute with DBA oversight

---

**Last Updated:** March 21, 2026  
**Maintained by:** Gym Platform Core Team  
**Next Review:** June 21, 2026
