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
INSERT INTO measurement_types (type, unit, is_system, created_at, updated_at)
VALUES
    ('Body Weight',                'kg',       TRUE,  NOW(), NOW()),
    ('Body Fat Percentage',        '%',        TRUE,  NOW(), NOW()),
    ('BMI',                        'kg/m²',    TRUE,  NOW(), NOW()),
    ('Muscle Mass',                'kg',       TRUE,  NOW(), NOW()),
    ('Waist Circumference',        'cm',       TRUE,  NOW(), NOW()),
    ('Hip Circumference',          'cm',       TRUE,  NOW(), NOW()),
    ('Chest Circumference',        'cm',       TRUE,  NOW(), NOW()),
    ('Arm Circumference',          'cm',       TRUE,  NOW(), NOW()),
    ('Thigh Circumference',        'cm',       TRUE,  NOW(), NOW()),
    ('Neck Circumference',         'cm',       TRUE,  NOW(), NOW()),
    ('Resting Heart Rate',         'bpm',      TRUE,  NOW(), NOW()),
    ('Blood Pressure Systolic',    'mmHg',     TRUE,  NOW(), NOW()),
    ('Blood Pressure Diastolic',   'mmHg',     TRUE,  NOW(), NOW()),
    ('VO2 Max',                    'ml/kg/min',TRUE,  NOW(), NOW()),
    ('Flexibility (Sit & Reach)',  'cm',       TRUE,  NOW(), NOW());

-- =============================================================================
-- 2. OBJECTIVES - One per test user (subquery by email, schema-qualified)
-- =============================================================================
INSERT INTO objectives (user_id, title, description, category, is_active, created_at, updated_at)
SELECT u.id,
       'Weight Loss Goal',
       'Lose 10kg by improving diet and exercising 4 times per week',
       'WEIGHT_LOSS', TRUE, NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'john.doe@example.com'
  AND  NOT EXISTS (SELECT 1 FROM objectives o WHERE o.user_id = u.id);

INSERT INTO objectives (user_id, title, description, category, is_active, created_at, updated_at)
SELECT u.id,
       'Muscle Gain Goal',
       'Gain 5kg of lean muscle mass through progressive overload training',
       'MUSCLE_GAIN', TRUE, NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'jane.smith@example.com'
  AND  NOT EXISTS (SELECT 1 FROM objectives o WHERE o.user_id = u.id);

INSERT INTO objectives (user_id, title, description, category, is_active, created_at, updated_at)
SELECT u.id,
       'General Fitness Goal',
       'Complete a 5km run without stopping and improve overall endurance',
       'GENERAL_FITNESS', TRUE, NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'mike.johnson@example.com'
  AND  NOT EXISTS (SELECT 1 FROM objectives o WHERE o.user_id = u.id);

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
INSERT INTO measurement_values (user_id, measurement_type_id, value, measurement_date, notes, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE type = 'Body Weight' LIMIT 1),
       75.0, NOW()::DATE, 'Starting weight', NOW()
FROM   auth_schema.users u
WHERE  u.email = 'john.doe@example.com'
  AND  NOT EXISTS (SELECT 1 FROM measurement_values mv WHERE mv.user_id = u.id);

INSERT INTO measurement_values (user_id, measurement_type_id, value, measurement_date, notes, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE type = 'Body Weight' LIMIT 1),
       62.0, NOW()::DATE, 'Starting weight', NOW()
FROM   auth_schema.users u
WHERE  u.email = 'jane.smith@example.com'
  AND  NOT EXISTS (SELECT 1 FROM measurement_values mv WHERE mv.user_id = u.id);

INSERT INTO measurement_values (user_id, measurement_type_id, value, measurement_date, notes, created_at)
SELECT u.id,
       (SELECT id FROM measurement_types WHERE type = 'Body Weight' LIMIT 1),
       85.0, NOW()::DATE, 'Starting weight', NOW()
FROM   auth_schema.users u
WHERE  u.email = 'mike.johnson@example.com'
  AND  NOT EXISTS (SELECT 1 FROM measurement_values mv WHERE mv.user_id = u.id);

-- =============================================================================
-- 5. TRAINING COMPONENTS - Strength and cardio programs
-- =============================================================================
INSERT INTO training_components (plan_id, focus, intensity, frequency_per_week, created_at, updated_at)
SELECT p.id, 'Cardio', 'Moderate', 3, NOW(), NOW()
FROM   plans p
WHERE  p.name = 'Weight Loss Plan'
  AND  NOT EXISTS (SELECT 1 FROM training_components tc WHERE tc.plan_id = p.id);

INSERT INTO training_components (plan_id, focus, intensity, frequency_per_week, created_at, updated_at)
SELECT p.id, 'Strength', 'High', 4, NOW(), NOW()
FROM   plans p
WHERE  p.name = 'Muscle Building Plan'
  AND  NOT EXISTS (SELECT 1 FROM training_components tc WHERE tc.plan_id = p.id);

INSERT INTO training_components (plan_id, focus, intensity, frequency_per_week, created_at, updated_at)
SELECT p.id, 'Mixed', 'Moderate', 4, NOW(), NOW()
FROM   plans p
WHERE  p.name = 'Fitness Foundation Plan'
  AND  NOT EXISTS (SELECT 1 FROM training_components tc WHERE tc.plan_id = p.id);

-- =============================================================================
-- 6. DIET COMPONENTS - Nutrition programs
-- =============================================================================
INSERT INTO diet_components (plan_id, diet_type, daily_calories, macro_distribution, created_at, updated_at)
SELECT p.id, 'Calorie Deficit', 2200, 'Protein:30%, Carbs:40%, Fat:30%', NOW(), NOW()
FROM   plans p
WHERE  p.name = 'Weight Loss Plan'
  AND  NOT EXISTS (SELECT 1 FROM diet_components dc WHERE dc.plan_id = p.id);

INSERT INTO diet_components (plan_id, diet_type, daily_calories, macro_distribution, created_at, updated_at)
SELECT p.id, 'High Protein', 2800, 'Protein:40%, Carbs:40%, Fat:20%', NOW(), NOW()
FROM   plans p
WHERE  p.name = 'Muscle Building Plan'
  AND  NOT EXISTS (SELECT 1 FROM diet_components dc WHERE dc.plan_id = p.id);

INSERT INTO diet_components (plan_id, diet_type, daily_calories, macro_distribution, created_at, updated_at)
SELECT p.id, 'Balanced', 2400, 'Protein:30%, Carbs:45%, Fat:25%', NOW(), NOW()
FROM   plans p
WHERE  p.name = 'Fitness Foundation Plan'
  AND  NOT EXISTS (SELECT 1 FROM diet_components dc WHERE dc.plan_id = p.id);

-- =============================================================================
-- 7. DIET LOGS - Daily nutrition tracking
-- =============================================================================
INSERT INTO diet_logs (user_id, diet_component_id, log_date, meal, food_items, calories, macros, notes, created_at, updated_at)
SELECT u.id,
       (SELECT dc.id FROM diet_components dc WHERE dc.plan_id = (SELECT id FROM plans WHERE user_id = u.id LIMIT 1) LIMIT 1),
       NOW()::DATE, 'Breakfast', 'Eggs, toast, orange juice', 500.0, 'Protein: 20g, Carbs: 60g, Fat: 15g', 'Morning meal', NOW(), NOW()
FROM   auth_schema.users u
WHERE  u.email = 'john.doe@example.com'
  AND  NOT EXISTS (SELECT 1 FROM diet_logs dl WHERE dl.user_id = u.id AND dl.log_date = NOW()::DATE);

-- Reset path
SET search_path TO public;
