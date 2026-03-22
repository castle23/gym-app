# Schema Design

## Overview

Four schemas in a single `gym_db` database. All tables use `BIGSERIAL` PKs and `TIMESTAMP` audit fields. DDL is generated/validated by Hibernate — the SQL below reflects the actual entity mappings.

For a column-level reference see [Architecture: Database Schema](../arquitectura/03-database-schema.md).

---

## auth_schema

```sql
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    user_type   VARCHAR(50)  NOT NULL,   -- USER | PROFESSIONAL | ADMIN
    account_status VARCHAR(50) NOT NULL, -- PENDING | ACTIVE | SUSPENDED | DELETED
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE auth_schema.user_roles (
    user_id BIGINT      NOT NULL REFERENCES auth_schema.users(id),
    role    VARCHAR(50) NOT NULL,        -- ROLE_USER | ROLE_PROFESSIONAL | ROLE_ADMIN
    PRIMARY KEY (user_id, role)
);

CREATE TABLE auth_schema.verifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES auth_schema.users(id),
    type        VARCHAR(50) NOT NULL,    -- EMAIL | PHONE | SMS | OAUTH
    code        VARCHAR(10) NOT NULL,
    verified    BOOLEAN     NOT NULL,
    expires_at  TIMESTAMP   NOT NULL,
    verified_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL
);
```

---

## training_schema

```sql
CREATE SCHEMA IF NOT EXISTS training_schema;

CREATE TABLE training_schema.disciplines (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL    -- STRENGTH | CARDIO | FLEXIBILITY | SPORTS | MIND_BODY | OTHER
);

CREATE TABLE training_schema.exercises (
    id            BIGSERIAL PRIMARY KEY,
    discipline_id BIGINT        NOT NULL REFERENCES training_schema.disciplines(id),
    name          VARCHAR(255)  NOT NULL UNIQUE,
    description   VARCHAR(1000) NOT NULL,
    type          VARCHAR(50)   NOT NULL,  -- SYSTEM | PROFESSIONAL | USER
    created_by    BIGINT,                  -- NULL for SYSTEM exercises
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);

CREATE TABLE training_schema.routine_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(1000),
    created_by  BIGINT,                   -- NULL for SYSTEM templates
    type        VARCHAR(50)   NOT NULL,   -- SYSTEM | PROFESSIONAL | USER
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE TABLE training_schema.routine_template_exercises (
    template_id BIGINT NOT NULL REFERENCES training_schema.routine_templates(id),
    exercise_id BIGINT NOT NULL REFERENCES training_schema.exercises(id),
    PRIMARY KEY (template_id, exercise_id)
);

CREATE TABLE training_schema.user_routines (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    template_id BIGINT        REFERENCES training_schema.routine_templates(id),
    name        VARCHAR(255),
    description VARCHAR(1000),
    is_active   BOOLEAN       NOT NULL,
    start_date  TIMESTAMP     NOT NULL,
    end_date    TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE TABLE training_schema.exercise_sessions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    user_routine_id  BIGINT         NOT NULL REFERENCES training_schema.user_routines(id),
    exercise_id      BIGINT         NOT NULL REFERENCES training_schema.exercises(id),
    sets_completed   INTEGER        NOT NULL,
    reps_completed   INTEGER        NOT NULL,
    weight_used      DECIMAL(7,2),
    duration_seconds BIGINT,
    notes            VARCHAR(255),
    session_date     TIMESTAMP      NOT NULL,
    created_at       TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP      NOT NULL
);
```

---

## tracking_schema

```sql
CREATE SCHEMA IF NOT EXISTS tracking_schema;

CREATE TABLE tracking_schema.measurement_types (
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(255) NOT NULL,
    unit       VARCHAR(50)  NOT NULL,
    is_system  BOOLEAN,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE tracking_schema.measurement_values (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT    NOT NULL,
    measurement_type_id BIGINT    NOT NULL REFERENCES tracking_schema.measurement_types(id),
    value               DOUBLE PRECISION NOT NULL,
    measurement_date    DATE      NOT NULL,
    notes               VARCHAR(255),
    created_at          TIMESTAMP NOT NULL
);

CREATE TABLE tracking_schema.objectives (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    title       VARCHAR(500)  NOT NULL,
    description TEXT          NOT NULL,
    category    VARCHAR(100)  NOT NULL,
    is_active   BOOLEAN       NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE TABLE tracking_schema.plans (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    description  VARCHAR(1000) NOT NULL,
    objective_id BIGINT        REFERENCES tracking_schema.objectives(id),
    status       VARCHAR(50)   NOT NULL,  -- ACTIVE | PAUSED | COMPLETED | CANCELLED
    start_date   TIMESTAMP     NOT NULL,
    end_date     TIMESTAMP,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);

CREATE TABLE tracking_schema.diet_components (
    id                 BIGSERIAL PRIMARY KEY,
    plan_id            BIGINT        NOT NULL REFERENCES tracking_schema.plans(id),
    diet_type          VARCHAR(255)  NOT NULL,
    daily_calories     INTEGER       NOT NULL,
    macro_distribution TEXT          NOT NULL,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL
);

CREATE TABLE tracking_schema.training_components (
    id                 BIGSERIAL PRIMARY KEY,
    plan_id            BIGINT       NOT NULL REFERENCES tracking_schema.plans(id),
    focus              VARCHAR(255) NOT NULL,
    intensity          VARCHAR(255) NOT NULL,
    frequency_per_week INTEGER      NOT NULL,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL
);

CREATE TABLE tracking_schema.diet_logs (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT    NOT NULL,
    diet_component_id BIGINT    REFERENCES tracking_schema.diet_components(id),
    log_date          DATE      NOT NULL,
    meal              VARCHAR(255) NOT NULL,
    food_items        TEXT      NOT NULL,
    calories          DOUBLE PRECISION NOT NULL,
    macros            VARCHAR(255),
    notes             TEXT,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE TABLE tracking_schema.recommendations (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    training_component_id  BIGINT       REFERENCES tracking_schema.training_components(id),
    diet_component_id      BIGINT       REFERENCES tracking_schema.diet_components(id),
    title                  VARCHAR(500) NOT NULL,
    description            TEXT         NOT NULL,
    professional_name      VARCHAR(255) NOT NULL,
    created_at             TIMESTAMP    NOT NULL
);
```

---

## notification_schema

```sql
CREATE SCHEMA IF NOT EXISTS notification_schema;

CREATE TABLE notification_schema.notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    type       VARCHAR(50)  NOT NULL,   -- FK to NotificationType enum
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    sent_at    TIMESTAMP
);

CREATE TABLE notification_schema.push_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token        VARCHAR(255) NOT NULL UNIQUE,  -- FCM token
    device_type  VARCHAR(50)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL
);

CREATE TABLE notification_schema.notification_preferences (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL,
    notification_type   VARCHAR(50) NOT NULL,
    is_enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    quiet_hours_start   TIME,
    quiet_hours_end     TIME
);
```

---

## Naming Conventions

- Tables: `snake_case`, plural
- PKs: `id BIGSERIAL`
- FKs: `{entity}_id BIGINT`
- Timestamps: `created_at`, `updated_at` (set via `@PrePersist` / `@PreUpdate`)
- Enums: stored as `VARCHAR` via `@Enumerated(EnumType.STRING)`

## Related Documentation

- [Database Overview](01-database-overview.md)
- [Architecture: Database Schema](../arquitectura/03-database-schema.md)
- [Migration Guide](05-migration-guide.md)
