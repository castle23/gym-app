-- =============================================================================
-- GYM TRACKING SERVICE - INITIAL DATA SCRIPT
-- =============================================================================
-- Seed data for tracking_schema.
-- Uses subqueries against auth_schema.users so IDs are not hardcoded.
-- Run AFTER 02-init-auth-data.sql.
-- =============================================================================

SET search_path TO tracking_schema;

-- =============================================================================
-- 1. MEASUREMENT TYPES - Standard body metrics catalogue
-- =============================================================================
INSERT INTO measurement_types (name, unit, min_value, max_value, created_at)
VALUES
    ('Body Weight',                'kg',       20.0,   500.0,  NOW()),
    ('Body Fat Percentage',        '%',         1.0,    70.0,  NOW()),
    ('BMI',                        'kg/m²',    10.0,    70.0,  NOW()),
    ('Muscle Mass',                'kg',        5.0,   150.0,  NOW()),
    ('Waist Circumference',        'cm',       40.0,   250.0,  NOW()),
    ('Hip Circumference',          'cm',       40.0,   250.0,  NOW()),
    ('Chest Circumference',        'cm',       40.0,   200.0,  NOW()),
    ('Arm Circumference',          'cm',       10.0,   100.0,  NOW()),
    ('Thigh Circumference',        'cm',       20.0,   150.0,  NOW()),
    ('Neck Circumference',         'cm',       20.0,    80.0,  NOW()),
    ('Resting Heart Rate',         'bpm',      30.0,   200.0,  NOW()),
    ('Blood Pressure Systolic',    'mmHg',     60.0,   250.0,  NOW()),
    ('Blood Pressure Diastolic',   'mmHg',     40.0,   150.0,  NOW()),
    ('VO2 Max',                    'ml/kg/min',10.0,    90.0,  NOW()),
    ('Flexibility (Sit & Reach)',  'cm',      -30.0,    50.0,  NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 2. OBJECTIVES - One per test user (subquery by email, schema-qualified)
-- =============================================================================
INSERT INTO objectives (user_id, type, description, target_date, created_at, updated_at)
SELECT u.id,
       'WEIGHT_LOSS',
       'Lose 10kg by improving diet and exercising 4 times per week',
       NOW() + INTERVAL '3 months', NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'john.doe@example.com'
  AND  NOT EXISTS (SELECT 1 FROM objectives o WHERE o.user_id = u.id AND o.type = 'WEIGHT_LOSS');

INSERT INTO objectives (user_id, type, description, target_date, created_at, updated_at)
SELECT u.id,
       'MUSCLE_GAIN',
       'Gain 5kg of lean muscle mass through progressive overload training',
       NOW() + INTERVAL '6 months', NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'jane.smith@example.com'
  AND  NOT EXISTS (SELECT 1 FROM objectives o WHERE o.user_id = u.id AND o.type = 'MUSCLE_GAIN');

INSERT INTO objectives (user_id, type, description, target_date, created_at, updated_at)
SELECT u.id,
       'GENERAL_FITNESS',
       'Complete a 5km run without stopping and improve overall endurance',
       NOW() + INTERVAL '2 months', NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'mike.johnson@example.com'
  AND  NOT EXISTS (SELECT 1 FROM objectives o WHERE o.user_id = u.id AND o.type = 'GENERAL_FITNESS');

-- =============================================================================
-- 3. PLANS - One active plan per test user
-- =============================================================================
INSERT INTO plans (user_id, name, description, objective_id, status, start_date, created_at, updated_at)
SELECT u.id,
       'Weight Loss Plan',
       '12-week plan combining cardio and strength training with calorie deficit diet.',
       (SELECT id FROM objectives WHERE user_id = u.id LIMIT 1),
       'ACTIVE', NOW(), NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'john.doe@example.com'
  AND  NOT EXISTS (SELECT 1 FROM plans p WHERE p.user_id = u.id);

INSERT INTO plans (user_id, name, description, objective_id, status, start_date, created_at, updated_at)
SELECT u.id,
       'Muscle Building Plan',
       '6-month hypertrophy program with progressive overload and high-protein diet.',
       (SELECT id FROM objectives WHERE user_id = u.id LIMIT 1),
       'ACTIVE', NOW(), NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'jane.smith@example.com'
  AND  NOT EXISTS (SELECT 1 FROM plans p WHERE p.user_id = u.id);

INSERT INTO plans (user_id, name, description, objective_id, status, start_date, created_at, updated_at)
SELECT u.id,
       'Fitness Foundation Plan',
       'Beginner-friendly 8-week plan to build base fitness and running endurance.',
       (SELECT id FROM objectives WHERE user_id = u.id LIMIT 1),
       'ACTIVE', NOW(), NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'mike.johnson@example.com'
  AND  NOT EXISTS (SELECT 1 FROM plans p WHERE p.user_id = u.id);

-- =============================================================================
-- 4. MEASUREMENT VALUES - Baseline readings per test user
-- =============================================================================
INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Body Weight'),
       90.5, 'Initial weigh-in', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'john.doe@example.com';

INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Body Fat Percentage'),
       24.0, 'DEXA scan', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'john.doe@example.com';

INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Waist Circumference'),
       95.0, 'Morning measurement', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'john.doe@example.com';

INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Body Weight'),
       62.0, 'Initial weigh-in', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'jane.smith@example.com';

INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Muscle Mass'),
       28.5, 'InBody scan', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'jane.smith@example.com';

INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Body Weight'),
       78.0, 'Initial weigh-in', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'mike.johnson@example.com';

INSERT INTO measurement_values (user_id, measurement_type_id, value, notes, recorded_at, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE name = 'Resting Heart Rate'),
       72.0, 'Morning resting HR', NOW() - INTERVAL '7 days', NOW()
FROM   auth_schema.users u WHERE u.email = 'mike.johnson@example.com';

-- =============================================================================
-- 5. SUMMARY
-- =============================================================================
SELECT COUNT(*) AS total_measurement_types  FROM measurement_types;
SELECT COUNT(*) AS total_objectives         FROM objectives;
SELECT COUNT(*) AS total_plans              FROM plans;
SELECT COUNT(*) AS total_measurement_values FROM measurement_values;

SELECT 'Measurement Types:' AS info;
SELECT id, name, unit FROM measurement_types ORDER BY id;
