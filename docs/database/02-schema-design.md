# Schema Design

## Overview

The Gym Platform database schema is organized into four main schemas (auth, training, tracking, common) with normalized relational tables, proper indexing, constraints, and referential integrity. This guide provides complete DDL (Data Definition Language), entity-relationship diagrams, naming conventions, design patterns, and best practices for the Gym Platform database.

The schema is designed for scalability, performance, data integrity, and maintainability across four microservices: Auth Service, Training Service, Tracking Service, and Notification Service.

## Table of Contents

- [Schema Organization](#schema-organization)
- [Entity-Relationship Diagram](#entity-relationship-diagram)
- [Naming Conventions](#naming-conventions)
- [Auth Schema](#auth-schema)
- [Training Schema](#training-schema)
- [Tracking Schema](#tracking-schema)
- [Common Schema](#common-schema)
- [Relationships & Constraints](#relationships--constraints)
- [Indexes & Performance](#indexes--performance)
- [Design Patterns](#design-patterns)
- [Best Practices](#best-practices)

---

## Schema Organization

### Schema Structure

```
gym_platform (database)
├── auth (schema)
│   ├── users
│   ├── roles
│   ├── permissions
│   ├── user_roles
│   ├── tokens
│   └── audit_log
├── training (schema)
│   ├── trainers
│   ├── training_plans
│   ├── exercises
│   ├── plan_exercises
│   └── training_sessions
├── tracking (schema)
│   ├── workouts
│   ├── workout_exercises
│   ├── workout_metrics
│   └── personal_records
└── common (schema)
    ├── settings
    ├── notifications
    ├── audit_events
    └── system_logs
```

### Schema Access

```sql
-- Verify schema creation
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('auth', 'training', 'tracking', 'common');

-- Check schema permissions
SELECT grantee, privilege_type FROM role_table_grants 
WHERE table_schema IN ('auth', 'training', 'tracking', 'common');
```

---

## Entity-Relationship Diagram

```
┌─────────────────┐
│   auth.users    │
├─────────────────┤
│ id (PK)         │
│ username        │
│ email           │
│ password_hash   │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │ N:M
         ├───────────────────┐
         │                   │
    ┌────▼──────────────┐   ┌─────────────────────┐
    │ auth.user_roles   │   │ auth.tokens         │
    ├────────────────────┤   ├─────────────────────┤
    │ user_id (FK)      │   │ id (PK)             │
    │ role_id (FK)      │   │ user_id (FK)        │
    └────┬──────────────┘   │ token_value         │
         │                   │ token_type          │
         │ N:1               │ expires_at          │
    ┌────▼──────────────┐   └─────────────────────┘
    │ auth.roles        │
    ├────────────────────┤
    │ id (PK)            │
    │ name               │
    │ description        │
    └─┬──────────────────┘
      │
      │ N:M
      ├────────────────────────────┐
      │                            │
    ┌─▼────────────────────────────┴─┐
    │ auth.role_permissions           │
    ├─────────────────────────────────┤
    │ role_id (FK)                    │
    │ permission_id (FK)              │
    └─────────────────────────────────┘
      │
      │ N:1
    ┌─▼────────────────────┐
    │ auth.permissions     │
    ├──────────────────────┤
    │ id (PK)              │
    │ code                 │
    │ description          │
    └──────────────────────┘

┌──────────────────────────┐
│  training.training_plans │
├──────────────────────────┤
│ id (PK)                  │
│ trainer_id (FK)          │
│ user_id (FK)             │
│ name                     │
│ description              │
│ created_at               │
└──────┬───────────────────┘
       │
       │ 1:N
    ┌──▼────────────────────────┐
    │ training.plan_exercises   │
    ├───────────────────────────┤
    │ plan_id (FK)              │
    │ exercise_id (FK)          │
    │ sets                       │
    │ reps                       │
    │ weight                     │
    └──┬────────────────────────┘
       │
       │ N:1
    ┌──▼───────────────────┐
    │ training.exercises   │
    ├──────────────────────┤
    │ id (PK)              │
    │ name                 │
    │ description          │
    │ muscle_group         │
    │ difficulty           │
    └──────────────────────┘

┌─────────────────────────┐
│  tracking.workouts      │
├─────────────────────────┤
│ id (PK)                 │
│ user_id (FK)            │
│ plan_id (FK, nullable)  │
│ start_time              │
│ end_time                │
│ duration_minutes        │
│ calories_burned         │
│ created_at              │
└────────┬────────────────┘
         │
         │ 1:N
    ┌────▼──────────────────────┐
    │ tracking.workout_exercises│
    ├──────────────────────────┤
    │ workout_id (FK)          │
    │ exercise_id (FK)         │
    │ sets_completed           │
    │ reps_per_set             │
    │ weight_used              │
    └────┬─────────────────────┘
         │
         │ N:M
    ┌────▼──────────────────────┐
    │ tracking.workout_metrics  │
    ├──────────────────────────┤
    │ workout_id (FK)          │
    │ metric_name              │
    │ metric_value             │
    │ recorded_at              │
    └──────────────────────────┘
```

---

## Naming Conventions

### General Rules

```yaml
naming-conventions:
  tables:
    format: "singular_noun_lowercase"
    example: "user, workout, exercise"
    rationale: "Clear entity names"

  columns:
    format: "lowercase_with_underscores"
    example: "first_name, created_at, total_duration"
    rationale: "Readable and SQL-friendly"

  primary-keys:
    format: "id"
    example: "user.id, workout.id"
    rationale: "Consistent, universally recognized"

  foreign-keys:
    format: "table_name_id or entity_id"
    example: "user_id, trainer_id"
    rationale: "Clear relationship indication"

  indexes:
    format: "idx_{table}_{columns}"
    example: "idx_user_email, idx_workout_user_created"
    rationale: "Searchable and descriptive"

  constraints:
    unique: "uq_{table}_{column}"
    check: "ck_{table}_{condition}"
    foreign-key: "fk_{table}_{parent_table}"
    example: "uq_user_email, ck_workout_duration_positive"
    rationale: "Constraint type and location clear"
```

### Column Naming

```sql
-- Identifier columns
id, uuid, code

-- Timestamp columns
created_at, updated_at, deleted_at, published_at

-- Boolean columns
is_active, is_deleted, is_verified, has_completed

-- Amount columns
total_amount, unit_price, quantity, percentage

-- Status columns
status, state, type, category

-- Text columns
name, title, description, content, message

-- Link/Foreign key columns
user_id, parent_id, trainer_id
```

---

## Auth Schema

### Users Table

```sql
CREATE TABLE auth.users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    phone_number VARCHAR(20),
    avatar_url TEXT,
    bio TEXT,
    
    -- Status
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED')),
    
    -- Authentication
    last_login_at TIMESTAMP,
    login_attempts INT DEFAULT 0,
    login_locked_until TIMESTAMP,
    
    -- Account verification
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    phone_verified BOOLEAN DEFAULT FALSE,
    phone_verified_at TIMESTAMP,
    
    -- Security
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT ck_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Indexes
CREATE UNIQUE INDEX idx_users_uuid ON auth.users(uuid);
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_users_username ON auth.users(username);
CREATE INDEX idx_users_status ON auth.users(status);
CREATE INDEX idx_users_created_at ON auth.users(created_at DESC);
```

### Roles Table

```sql
CREATE TABLE auth.roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO auth.roles (name, description) VALUES
    ('ROLE_ADMIN', 'Administrator with full access'),
    ('ROLE_TRAINER', 'Trainer who creates training plans'),
    ('ROLE_USER', 'Regular user who tracks workouts'),
    ('ROLE_SUPPORT', 'Support staff who helps users');
```

### Permissions Table

```sql
CREATE TABLE auth.permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO auth.permissions (code, description) VALUES
    ('users.read', 'Read user information'),
    ('users.write', 'Create/update user information'),
    ('users.delete', 'Delete user account'),
    ('workouts.read', 'Read workout data'),
    ('workouts.write', 'Create/update workouts'),
    ('plans.read', 'Read training plans'),
    ('plans.write', 'Create/update training plans'),
    ('reports.read', 'Read reports and analytics');
```

### User Roles Junction Table

```sql
CREATE TABLE auth.user_roles (
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT REFERENCES auth.users(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON auth.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON auth.user_roles(role_id);
```

### Tokens Table

```sql
CREATE TABLE auth.tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_value VARCHAR(500) UNIQUE NOT NULL,
    token_type VARCHAR(50) NOT NULL CHECK (token_type IN ('ACCESS', 'REFRESH', 'RESET', 'VERIFY')),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    CONSTRAINT ck_tokens_expires_future CHECK (expires_at > created_at)
);

CREATE INDEX idx_tokens_user_id ON auth.tokens(user_id);
CREATE INDEX idx_tokens_token_value ON auth.tokens(token_value);
CREATE INDEX idx_tokens_expires_at ON auth.tokens(expires_at);
CREATE INDEX idx_tokens_token_type ON auth.tokens(token_type);
```

---

## Training Schema

### Trainers Table

```sql
CREATE TABLE training.trainers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    specialization VARCHAR(100),
    certification VARCHAR(255),
    bio TEXT,
    hourly_rate DECIMAL(10, 2),
    is_available BOOLEAN DEFAULT TRUE,
    years_of_experience INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trainers_user_id ON training.trainers(user_id);
CREATE INDEX idx_trainers_specialization ON training.trainers(specialization);
```

### Training Plans Table

```sql
CREATE TABLE training.training_plans (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    trainer_id BIGINT NOT NULL REFERENCES training.trainers(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    goal VARCHAR(100) CHECK (goal IN ('STRENGTH', 'CARDIO', 'FLEXIBILITY', 'WEIGHT_LOSS', 'ENDURANCE')),
    difficulty_level VARCHAR(50) CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    duration_weeks INT CHECK (duration_weeks > 0),
    frequency_per_week INT CHECK (frequency_per_week BETWEEN 1 AND 7),
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED')),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_plans_user_id ON training.training_plans(user_id);
CREATE INDEX idx_plans_trainer_id ON training.training_plans(trainer_id);
CREATE INDEX idx_plans_status ON training.training_plans(status);
CREATE INDEX idx_plans_created_at ON training.training_plans(created_at DESC);
```

### Exercises Table

```sql
CREATE TABLE training.exercises (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    muscle_group VARCHAR(100) NOT NULL,
    equipment_required VARCHAR(255),
    difficulty VARCHAR(50) CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    exercise_type VARCHAR(100) CHECK (exercise_type IN ('STRENGTH', 'CARDIO', 'FLEXIBILITY', 'BALANCE', 'PLYOMETRIC')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exercises_muscle_group ON training.exercises(muscle_group);
CREATE INDEX idx_exercises_difficulty ON training.exercises(difficulty);
CREATE INDEX idx_exercises_type ON training.exercises(exercise_type);
```

### Plan Exercises Junction Table

```sql
CREATE TABLE training.plan_exercises (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES training.training_plans(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES training.exercises(id) ON DELETE CASCADE,
    sequence_number INT NOT NULL,
    sets INT NOT NULL CHECK (sets > 0),
    reps INT CHECK (reps IS NULL OR reps > 0),
    duration_seconds INT CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    rest_seconds INT DEFAULT 60 CHECK (rest_seconds > 0),
    weight_kg DECIMAL(10, 2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plan_exercise UNIQUE (plan_id, exercise_id, sequence_number)
);

CREATE INDEX idx_plan_exercises_plan_id ON training.plan_exercises(plan_id);
CREATE INDEX idx_plan_exercises_exercise_id ON training.plan_exercises(exercise_id);
```

---

## Tracking Schema

### Workouts Table

```sql
CREATE TABLE tracking.workouts (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    training_plan_id BIGINT REFERENCES training.training_plans(id) ON DELETE SET NULL,
    name VARCHAR(255),
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_minutes INT CHECK (duration_minutes > 0),
    calories_burned INT,
    mood_before VARCHAR(50),
    mood_after VARCHAR(50),
    notes TEXT,
    weather_condition VARCHAR(100),
    location VARCHAR(255),
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_workout_times CHECK (end_time IS NULL OR end_time >= start_time)
);

CREATE INDEX idx_workouts_user_id ON tracking.workouts(user_id);
CREATE INDEX idx_workouts_created_at ON tracking.workouts(created_at DESC);
CREATE INDEX idx_workouts_plan_id ON tracking.workouts(training_plan_id);
CREATE INDEX idx_workouts_user_created ON tracking.workouts(user_id, created_at DESC);
```

### Workout Exercises Junction Table

```sql
CREATE TABLE tracking.workout_exercises (
    id BIGSERIAL PRIMARY KEY,
    workout_id BIGINT NOT NULL REFERENCES tracking.workouts(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES training.exercises(id) ON DELETE CASCADE,
    sequence_number INT NOT NULL,
    sets_planned INT,
    sets_completed INT CHECK (sets_completed IS NULL OR sets_completed >= 0),
    reps_per_set INT,
    weight_used_kg DECIMAL(10, 2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workout_exercises_workout_id ON tracking.workout_exercises(workout_id);
CREATE INDEX idx_workout_exercises_exercise_id ON tracking.workout_exercises(exercise_id);
```

### Workout Metrics Table

```sql
CREATE TABLE tracking.workout_metrics (
    id BIGSERIAL PRIMARY KEY,
    workout_id BIGINT NOT NULL REFERENCES tracking.workouts(id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15, 2) NOT NULL,
    unit VARCHAR(50),
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workout_metrics_workout_id ON tracking.workout_metrics(workout_id);
CREATE INDEX idx_workout_metrics_name ON tracking.workout_metrics(metric_name);
```

### Personal Records Table

```sql
CREATE TABLE tracking.personal_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES training.exercises(id) ON DELETE CASCADE,
    record_type VARCHAR(50) CHECK (record_type IN ('MAX_WEIGHT', 'MAX_REPS', 'MAX_TIME', 'MAX_DISTANCE')),
    record_value DECIMAL(15, 2) NOT NULL,
    unit VARCHAR(50),
    achieved_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_personal_record UNIQUE (user_id, exercise_id, record_type)
);

CREATE INDEX idx_pr_user_id ON tracking.personal_records(user_id);
CREATE INDEX idx_pr_exercise_id ON tracking.personal_records(exercise_id);
CREATE INDEX idx_pr_achieved_at ON tracking.personal_records(achieved_at DESC);
```

---

## Common Schema

### Settings Table

```sql
CREATE TABLE common.settings (
    id SERIAL PRIMARY KEY,
    key VARCHAR(255) UNIQUE NOT NULL,
    value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO common.settings (key, value, description) VALUES
    ('app.version', '1.0.0', 'Application version'),
    ('db.schema.version', '1.0.0', 'Database schema version'),
    ('maintenance_mode', 'false', 'Enable maintenance mode'),
    ('api.rate_limit', '100', 'API rate limit per minute');
```

### Audit Events Table

```sql
CREATE TABLE common.audit_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth.users(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_user_id ON common.audit_events(user_id);
CREATE INDEX idx_audit_events_timestamp ON common.audit_events(timestamp DESC);
CREATE INDEX idx_audit_events_entity ON common.audit_events(entity_type, entity_id);
```

---

## Relationships & Constraints

### Referential Integrity

All foreign keys are defined with appropriate ON DELETE and ON UPDATE actions:

```sql
-- CASCADE: Delete child records when parent is deleted
-- SET NULL: Set foreign key to NULL when parent is deleted
-- RESTRICT: Prevent deletion if child records exist (default)

-- Example: Training plans cascade-delete when trainer is deleted
ALTER TABLE training.training_plans
    ADD CONSTRAINT fk_plans_trainer
    FOREIGN KEY (trainer_id) REFERENCES training.trainers(id)
    ON DELETE CASCADE;

-- Example: Workouts set plan to NULL if plan is deleted
ALTER TABLE tracking.workouts
    ADD CONSTRAINT fk_workouts_plan
    FOREIGN KEY (training_plan_id) REFERENCES training.training_plans(id)
    ON DELETE SET NULL;
```

---

## Indexes & Performance

### Index Strategy

```sql
-- Single column indexes on frequently queried columns
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_workouts_user_id ON tracking.workouts(user_id);

-- Composite indexes for WHERE + ORDER BY combinations
CREATE INDEX idx_workouts_user_date ON tracking.workouts(user_id, created_at DESC);
CREATE INDEX idx_plans_user_status ON training.training_plans(user_id, status);

-- Index for foreign keys
CREATE INDEX idx_user_roles_user_id ON auth.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON auth.user_roles(role_id);

-- JSONB indexes for audit data
CREATE INDEX idx_audit_events_new_values ON common.audit_events USING GIN (new_values);

-- Partial indexes for common conditions
CREATE INDEX idx_users_active ON auth.users(id) WHERE status = 'ACTIVE';
CREATE INDEX idx_plans_active ON training.training_plans(id) WHERE status = 'ACTIVE';
```

### Index Analysis

```sql
-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Identify unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- Check index size
SELECT schemaname, tablename, indexname, pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## Design Patterns

### UUID Primary Keys

```sql
-- Use UUIDs for distributed systems and data synchronization
ALTER TABLE auth.users ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;

-- Benefits:
-- - Can generate IDs before inserting (useful for distributed systems)
-- - Prevents ID guessing/enumeration attacks
-- - Easier to merge data from different sources
```

### Soft Deletes

```sql
-- Soft delete instead of hard delete for audit trail
ALTER TABLE auth.users ADD COLUMN deleted_at TIMESTAMP;

-- Query active records
SELECT * FROM auth.users WHERE deleted_at IS NULL;

-- Restore deleted record
UPDATE auth.users SET deleted_at = NULL WHERE id = 123;
```

### Audit Logging

```sql
-- Automatic audit logging with triggers
CREATE TRIGGER audit_users_changes
AFTER INSERT OR UPDATE OR DELETE ON auth.users
FOR EACH ROW
EXECUTE FUNCTION log_audit_event();

CREATE OR REPLACE FUNCTION log_audit_event()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO common.audit_events (user_id, action, entity_type, entity_id, old_values, new_values)
    VALUES (
        CURRENT_USER_ID(),
        TG_OP,
        TG_TABLE_NAME,
        COALESCE(NEW.id, OLD.id),
        to_jsonb(OLD),
        to_jsonb(NEW)
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;
```

---

## Best Practices

### Data Types

- Use appropriate data types (INT vs BIGINT, VARCHAR vs TEXT)
- Use DECIMAL for monetary amounts (not FLOAT)
- Use TIMESTAMP with timezone for audit trails
- Use UUID for distributed systems
- Use JSONB for semi-structured data (not VARCHAR for JSON)

### Naming

- Use lowercase with underscores (snake_case)
- Use singular nouns for table names
- Use meaningful column names
- Use consistent naming across similar tables

### Constraints

- Always define NOT NULL for required columns
- Use CHECK constraints for data validation
- Use UNIQUE constraints for unique business requirements
- Use FOREIGN KEYS for referential integrity
- Document constraint purposes

### Performance

- Index columns used in WHERE clauses
- Index columns used in JOIN conditions
- Use EXPLAIN ANALYZE to verify query plans
- Regularly vacuum and analyze tables
- Monitor slow queries

---

## Related Documentation

- [Database Overview](01-database-overview.md) - Database architecture
- [Performance Tuning](04-performance-tuning.md) - Query optimization
- [Migration Guide](05-migration-guide.md) - Schema migrations
- [Stack Documentation](../stack/02-database-postgresql.md) - PostgreSQL setup

## References

- [PostgreSQL Data Types](https://www.postgresql.org/docs/14/datatype.html)
- [PostgreSQL Constraints](https://www.postgresql.org/docs/14/ddl-constraints.html)
- [Database Normalization](https://en.wikipedia.org/wiki/Database_normalization)
- [Indexes in PostgreSQL](https://www.postgresql.org/docs/14/sql-createindex.html)
