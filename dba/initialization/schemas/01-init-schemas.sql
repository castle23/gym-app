-- =============================================================================
-- GYM PLATFORM - DATABASE SCHEMA INITIALIZATION
-- =============================================================================
-- Creates all schemas and tables for all microservices.
-- Hibernate handles DDL in dev (ddl-auto: update), but this script provides
-- a clean baseline for production deployments and fresh environments.
-- =============================================================================

-- Create schemas
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS training_schema;
CREATE SCHEMA IF NOT EXISTS tracking_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- Grant privileges
GRANT ALL PRIVILEGES ON SCHEMA auth_schema         TO gym_admin;
GRANT ALL PRIVILEGES ON SCHEMA training_schema     TO gym_admin;
GRANT ALL PRIVILEGES ON SCHEMA tracking_schema     TO gym_admin;
GRANT ALL PRIVILEGES ON SCHEMA notification_schema TO gym_admin;

-- =============================================================================
-- AUTH SCHEMA
-- =============================================================================
SET search_path TO auth_schema;

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    user_type       VARCHAR(20)  NOT NULL CHECK (user_type IN ('USER','PROFESSIONAL','ADMIN')),
    account_status  VARCHAR(20)  NOT NULL CHECK (account_status IN ('PENDING','ACTIVE','SUSPENDED','DELETED')),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50)  NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS verifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL CHECK (type IN ('EMAIL','PHONE','SMS','OAUTH')),
    code        VARCHAR(255) NOT NULL,
    verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP   NOT NULL,
    verified_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS professional_requests (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_id  BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED')),
    rejection_reason VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email           ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_account_status  ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_verifications_user_id ON verifications(user_id);
CREATE INDEX IF NOT EXISTS idx_prof_requests_user_id ON professional_requests(user_id);

-- =============================================================================
-- TRAINING SCHEMA
-- =============================================================================
SET search_path TO training_schema;

CREATE TABLE IF NOT EXISTS disciplines (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('STRENGTH','CARDIO','FLEXIBILITY','SPORTS','MIND_BODY','OTHER'))
);

CREATE TABLE IF NOT EXISTS exercises (
    id            BIGSERIAL    PRIMARY KEY,
    discipline_id BIGINT       NOT NULL REFERENCES disciplines(id),
    name          VARCHAR(255) NOT NULL UNIQUE,
    description   TEXT         NOT NULL,
    type          VARCHAR(20)  NOT NULL CHECK (type IN ('SYSTEM','PROFESSIONAL','USER')),
    created_by    BIGINT       NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS routine_templates (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT         NOT NULL,
    created_by  BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('SYSTEM','PROFESSIONAL','USER')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS routine_template_exercises (
    template_id BIGINT NOT NULL REFERENCES routine_templates(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    PRIMARY KEY (template_id, exercise_id)
);

CREATE TABLE IF NOT EXISTS user_routines (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    template_id BIGINT       REFERENCES routine_templates(id) ON DELETE SET NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date  TIMESTAMP    NOT NULL,
    end_date    TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS exercise_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT    NOT NULL,
    user_routine_id BIGINT    NOT NULL REFERENCES user_routines(id) ON DELETE CASCADE,
    exercise_id     BIGINT    NOT NULL REFERENCES exercises(id),
    sets_completed  INTEGER   NOT NULL,
    reps_completed  INTEGER   NOT NULL,
    weight_used     NUMERIC(7,2),
    duration_seconds BIGINT,
    notes           TEXT,
    session_date    TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_exercises_discipline    ON exercises(discipline_id);
CREATE INDEX IF NOT EXISTS idx_exercises_type          ON exercises(type);
CREATE INDEX IF NOT EXISTS idx_exercises_created_by    ON exercises(created_by);
CREATE INDEX IF NOT EXISTS idx_routine_templates_type  ON routine_templates(type);
CREATE INDEX IF NOT EXISTS idx_user_routines_user      ON user_routines(user_id);
CREATE INDEX IF NOT EXISTS idx_user_routines_active    ON user_routines(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_exercise_sessions_user  ON exercise_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_exercise_sessions_date  ON exercise_sessions(user_id, session_date);

-- =============================================================================
-- TRACKING SCHEMA
-- =============================================================================
SET search_path TO tracking_schema;

CREATE TABLE IF NOT EXISTS objectives (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT         NOT NULL,
    category    VARCHAR(100) NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS plans (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    objective_id BIGINT      REFERENCES objectives(id) ON DELETE SET NULL,
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','CANCELLED')),
    start_date  TIMESTAMP    NOT NULL,
    end_date    TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS training_components (
    id                  BIGSERIAL PRIMARY KEY,
    plan_id             BIGINT    NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    focus               VARCHAR(100) NOT NULL,
    intensity           VARCHAR(100) NOT NULL,
    frequency_per_week  INTEGER   NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS diet_components (
    id                   BIGSERIAL PRIMARY KEY,
    plan_id              BIGINT    NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    diet_type            VARCHAR(100) NOT NULL,
    daily_calories       INTEGER   NOT NULL,
    macro_distribution   TEXT      NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recommendations (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT    NOT NULL,
    training_component_id BIGINT    REFERENCES training_components(id) ON DELETE CASCADE,
    diet_component_id     BIGINT    REFERENCES diet_components(id) ON DELETE CASCADE,
    title                 VARCHAR(255) NOT NULL,
    description           TEXT      NOT NULL,
    professional_name     VARCHAR(255) NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS measurement_types (
    id          BIGSERIAL    PRIMARY KEY,
    type        VARCHAR(100) NOT NULL,
    unit        VARCHAR(50)  NOT NULL,
    is_system   BOOLEAN,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS measurement_values (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    measurement_type_id BIGINT       NOT NULL REFERENCES measurement_types(id),
    value               DOUBLE PRECISION NOT NULL,
    measurement_date    DATE         NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS diet_logs (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    diet_component_id BIGINT       REFERENCES diet_components(id) ON DELETE SET NULL,
    log_date          DATE         NOT NULL,
    meal              VARCHAR(100) NOT NULL,
    food_items        TEXT         NOT NULL,
    calories          DOUBLE PRECISION NOT NULL,
    macros            TEXT,
    notes             TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_objectives_user        ON objectives(user_id);
CREATE INDEX IF NOT EXISTS idx_plans_user             ON plans(user_id);
CREATE INDEX IF NOT EXISTS idx_plans_status           ON plans(user_id, status);
CREATE INDEX IF NOT EXISTS idx_training_comp_plan     ON training_components(plan_id);
CREATE INDEX IF NOT EXISTS idx_diet_comp_plan         ON diet_components(plan_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_user   ON recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_measurements_user      ON measurement_values(user_id);
CREATE INDEX IF NOT EXISTS idx_measurements_date      ON measurement_values(user_id, measurement_date);
CREATE INDEX IF NOT EXISTS idx_diet_logs_user         ON diet_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_diet_logs_date         ON diet_logs(user_id, log_date);

-- =============================================================================
-- NOTIFICATION SCHEMA
-- =============================================================================
SET search_path TO notification_schema;

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    type       VARCHAR(30)  NOT NULL CHECK (type IN ('WORKOUT_REMINDER','ACHIEVEMENT','MESSAGE','ALERT','OTHER')),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS push_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token        VARCHAR(512) NOT NULL UNIQUE,
    device_type  VARCHAR(50)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id                  BIGSERIAL   PRIMARY KEY,
    user_id             BIGINT      NOT NULL,
    notification_type   VARCHAR(30) NOT NULL CHECK (notification_type IN ('WORKOUT_REMINDER','ACHIEVEMENT','MESSAGE','ALERT','OTHER')),
    is_enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    quiet_hours_start   TIME,
    quiet_hours_end     TIME,
    UNIQUE (user_id, notification_type)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user      ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread    ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_push_tokens_user        ON push_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_push_tokens_active      ON push_tokens(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_notif_prefs_user        ON notification_preferences(user_id);

-- Reset to public
SET search_path TO public;
