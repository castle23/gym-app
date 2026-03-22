# Data Dictionary

Complete reference of all database entities across the Gym Platform microservices.

---

## auth_schema

### users

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| email | VARCHAR UNIQUE NOT NULL | Login credential |
| password | VARCHAR NOT NULL | BCrypt hash |
| user_type | VARCHAR NOT NULL | `USER` \| `PROFESSIONAL` \| `ADMIN` |
| account_status | VARCHAR NOT NULL | `PENDING` \| `ACTIVE` \| `SUSPENDED` \| `DELETED` |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### user_roles

| Column | Type | Notes |
|--------|------|-------|
| user_id | BIGINT FK → users.id | |
| role | VARCHAR NOT NULL | `ROLE_USER` \| `ROLE_PROFESSIONAL` \| `ROLE_ADMIN` |

### verifications

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users.id | |
| type | VARCHAR NOT NULL | `EMAIL` \| `PHONE` \| `SMS` \| `OAUTH` |
| code | VARCHAR NOT NULL | 6-digit numeric code |
| verified | BOOLEAN NOT NULL | |
| expires_at | TIMESTAMP NOT NULL | 15 min from creation |
| verified_at | TIMESTAMP | Set when verified |
| created_at | TIMESTAMP NOT NULL | |

---

## training_schema

### disciplines

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| name | VARCHAR UNIQUE NOT NULL | |
| description | VARCHAR NOT NULL | |
| type | VARCHAR NOT NULL | `STRENGTH` \| `CARDIO` \| `FLEXIBILITY` \| `SPORTS` \| `MIND_BODY` \| `OTHER` |

### exercises

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| discipline_id | BIGINT FK → disciplines.id | |
| name | VARCHAR UNIQUE NOT NULL | |
| description | VARCHAR(1000) NOT NULL | |
| type | VARCHAR NOT NULL | `SYSTEM` \| `PROFESSIONAL` \| `USER` |
| created_by | BIGINT | NULL for SYSTEM exercises |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### routine_templates

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| name | VARCHAR NOT NULL | |
| description | VARCHAR(1000) | |
| created_by | BIGINT | NULL for SYSTEM templates |
| type | VARCHAR NOT NULL | `SYSTEM` \| `PROFESSIONAL` \| `USER` |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### routine_template_exercises *(join table)*

| Column | Type |
|--------|------|
| template_id | BIGINT FK → routine_templates.id |
| exercise_id | BIGINT FK → exercises.id |

### user_routines

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | From X-User-Id header |
| template_id | BIGINT FK → routine_templates.id | |
| name | VARCHAR | |
| description | VARCHAR(1000) | |
| is_active | BOOLEAN NOT NULL | |
| start_date | TIMESTAMP NOT NULL | |
| end_date | TIMESTAMP | Set on deactivation |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### exercise_sessions

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| user_routine_id | BIGINT FK → user_routines.id | |
| exercise_id | BIGINT FK → exercises.id | |
| sets_completed | INTEGER NOT NULL | |
| reps_completed | INTEGER NOT NULL | |
| weight_used | DECIMAL(7,2) | kg, nullable |
| duration_seconds | BIGINT | nullable |
| notes | VARCHAR | |
| session_date | TIMESTAMP NOT NULL | |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

---

## tracking_schema

### measurement_types

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| type | VARCHAR NOT NULL | e.g. `weight`, `body_fat` |
| unit | VARCHAR NOT NULL | e.g. `kg`, `%`, `cm` |
| is_system | BOOLEAN | Default false |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### measurement_values

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| measurement_type_id | BIGINT FK → measurement_types.id | |
| value | DOUBLE PRECISION NOT NULL | |
| measurement_date | DATE NOT NULL | Defaults to today |
| notes | VARCHAR | |
| created_at | TIMESTAMP NOT NULL | |

### objectives

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| title | VARCHAR(500) NOT NULL | |
| description | TEXT NOT NULL | |
| category | VARCHAR(100) NOT NULL | |
| is_active | BOOLEAN NOT NULL | Default true |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### plans

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| name | VARCHAR NOT NULL | |
| description | VARCHAR(1000) NOT NULL | |
| objective_id | BIGINT FK → objectives.id | Optional |
| status | VARCHAR NOT NULL | `ACTIVE` \| `PAUSED` \| `COMPLETED` \| `CANCELLED` |
| start_date | TIMESTAMP NOT NULL | |
| end_date | TIMESTAMP | |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### diet_components

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| plan_id | BIGINT FK → plans.id | |
| diet_type | VARCHAR NOT NULL | |
| daily_calories | INTEGER NOT NULL | |
| macro_distribution | TEXT NOT NULL | |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### training_components

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| plan_id | BIGINT FK → plans.id | |
| focus | VARCHAR NOT NULL | |
| intensity | VARCHAR NOT NULL | |
| frequency_per_week | INTEGER NOT NULL | |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### diet_logs

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| diet_component_id | BIGINT FK → diet_components.id | Optional |
| log_date | DATE NOT NULL | |
| meal | VARCHAR NOT NULL | e.g. `breakfast`, `lunch` |
| food_items | TEXT NOT NULL | |
| calories | DOUBLE PRECISION NOT NULL | |
| macros | VARCHAR | |
| notes | TEXT | |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### recommendations

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| training_component_id | BIGINT FK → training_components.id | Optional |
| diet_component_id | BIGINT FK → diet_components.id | Optional |
| title | VARCHAR(500) NOT NULL | |
| description | TEXT NOT NULL | |
| professional_name | VARCHAR NOT NULL | |
| created_at | TIMESTAMP NOT NULL | |

---

## notification_schema

### notifications

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| title | VARCHAR NOT NULL | |
| body | TEXT NOT NULL | |
| type | VARCHAR NOT NULL | NotificationType enum |
| is_read | BOOLEAN NOT NULL | Default false |
| created_at | TIMESTAMP NOT NULL | |
| sent_at | TIMESTAMP | |

### push_tokens

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| token | VARCHAR UNIQUE NOT NULL | FCM token |
| device_type | VARCHAR NOT NULL | e.g. `android`, `ios` |
| is_active | BOOLEAN NOT NULL | Default true |
| last_used_at | TIMESTAMP | |
| created_at | TIMESTAMP NOT NULL | |

### notification_preferences

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | |
| notification_type | VARCHAR NOT NULL | NotificationType enum |
| is_enabled | BOOLEAN NOT NULL | Default true |
| quiet_hours_start | TIME | |
| quiet_hours_end | TIME | |

---

## Entity Relationships

```
auth_schema:
  users (1) ──< user_roles (N)
  users (1) ──< verifications (N)

training_schema:
  disciplines (1) ──< exercises (N)
  routine_templates (N) >──< exercises (N)  [via routine_template_exercises]
  user_routines (N) >── routine_templates (1)
  exercise_sessions (N) >── user_routines (1)
  exercise_sessions (N) >── exercises (1)

tracking_schema:
  measurement_types (1) ──< measurement_values (N)
  objectives (1) ──< plans (N)
  plans (1) ──< diet_components (N)
  plans (1) ──< training_components (N)
  diet_components (1) ──< diet_logs (N)
  training_components (1) ──< recommendations (N)
  diet_components (1) ──< recommendations (N)

notification_schema:
  (all tables independent, linked only by userId)
```

Cross-service links are by `userId` (Long) only — no cross-schema foreign keys.
