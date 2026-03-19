-- =============================================================================
-- GYM TRAINING SERVICE - INITIAL DATA SCRIPT
-- =============================================================================
-- This script loads initial master data for the training service
-- Includes: Disciplines, System Exercises, and Routine Templates
-- =============================================================================

-- Switch to training schema
SET search_path TO training_schema;

-- =============================================================================
-- 1. DISCIPLINES - Master data for exercise categories
-- =============================================================================
INSERT INTO disciplines (name, description, type, created_at)
VALUES 
    ('Chest', 'Upper body pushing exercises focusing on chest muscles', 'STRENGTH', NOW()),
    ('Back', 'Upper body pulling exercises focusing on back muscles', 'STRENGTH', NOW()),
    ('Shoulders', 'Exercises targeting deltoids and shoulder stability', 'STRENGTH', NOW()),
    ('Biceps', 'Arm exercises focusing on biceps', 'STRENGTH', NOW()),
    ('Triceps', 'Arm exercises focusing on triceps', 'STRENGTH', NOW()),
    ('Forearms', 'Exercises for forearm strength and endurance', 'STRENGTH', NOW()),
    ('Legs', 'Lower body exercises including quads, glutes, hamstrings', 'STRENGTH', NOW()),
    ('Quads', 'Exercises focusing on quadriceps', 'STRENGTH', NOW()),
    ('Hamstrings', 'Exercises focusing on hamstring muscles', 'STRENGTH', NOW()),
    ('Glutes', 'Exercises targeting glute muscles', 'STRENGTH', NOW()),
    ('Calves', 'Calf muscle exercises', 'STRENGTH', NOW()),
    ('Core', 'Ab and core stability exercises', 'STRENGTH', NOW()),
    ('Running', 'Cardio exercises with running', 'CARDIO', NOW()),
    ('Cycling', 'Cardio exercises with cycling', 'CARDIO', NOW()),
    ('Swimming', 'Cardio exercises with swimming', 'CARDIO', NOW()),
    ('Stretching', 'Flexibility and stretching exercises', 'FLEXIBILITY', NOW()),
    ('Yoga', 'Yoga and mind-body exercises', 'MIND_BODY', NOW()),
    ('Pilates', 'Pilates core and flexibility training', 'MIND_BODY', NOW()),
    ('CrossFit', 'High-intensity functional fitness', 'SPORTS', NOW()),
    ('Boxing', 'Boxing and combat sports training', 'SPORTS', NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 2. SYSTEM EXERCISES - Pre-defined exercises for all users
-- =============================================================================
INSERT INTO exercises (discipline_id, name, description, type, created_by, created_at, updated_at)
VALUES 
    -- CHEST EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Chest'), 
     'Barbell Bench Press', 
     'Classic compound movement for chest, shoulders, and triceps. Lie on a flat bench and press barbell away from chest.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Chest'), 
     'Dumbbell Bench Press', 
     'Similar to barbell bench press but using dumbbells for greater range of motion.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Chest'), 
     'Incline Bench Press', 
     'Bench press performed on an incline to target upper chest and front shoulders.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Chest'), 
     'Cable Flyes', 
     'Isolation exercise for chest using cable machines.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Chest'), 
     'Push-ups', 
     'Bodyweight compound exercise for chest, shoulders, and triceps.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- BACK EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Back'), 
     'Deadlift', 
     'Master compound exercise targeting back, glutes, hamstrings, and overall posterior chain.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Back'), 
     'Pull-ups', 
     'Bodyweight exercise for back and biceps. Requires pull-up bar.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Back'), 
     'Lat Pulldown', 
     'Machine-based exercise targeting latissimus dorsi (lats).',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Back'), 
     'Barbell Rows', 
     'Compound exercise for back thickness using barbell.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Back'), 
     'Dumbbell Rows', 
     'Unilateral back exercise using dumbbells.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- SHOULDER EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Shoulders'), 
     'Military Press', 
     'Overhead pressing exercise for shoulder strength and stability.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Shoulders'), 
     'Lateral Raises', 
     'Isolation exercise for lateral deltoids.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Shoulders'), 
     'Shoulder Shrugs', 
     'Exercise for trapezius muscle using dumbbells or barbell.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- BICEPS EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Biceps'), 
     'Barbell Curls', 
     'Classic bicep exercise using barbell.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Biceps'), 
     'Dumbbell Curls', 
     'Unilateral bicep exercise using dumbbells.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- TRICEPS EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Triceps'), 
     'Tricep Dips', 
     'Bodyweight exercise for triceps using parallel bars.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Triceps'), 
     'Tricep Pushdowns', 
     'Cable machine exercise for triceps.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- LEG EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Legs'), 
     'Squats', 
     'King of leg exercises. Compound movement for quads, glutes, hamstrings.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Legs'), 
     'Leg Press', 
     'Machine-based leg exercise for overall leg development.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Legs'), 
     'Leg Curls', 
     'Isolation exercise for hamstrings.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Legs'), 
     'Leg Extensions', 
     'Isolation exercise for quadriceps.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Legs'), 
     'Lunges', 
     'Single-leg compound exercise for quads and glutes.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- CORE EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Core'), 
     'Crunches', 
     'Basic abdominal exercise for rectus abdominis.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Core'), 
     'Planks', 
     'Isometric core exercise for stability.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Core'), 
     'Russian Twists', 
     'Rotational core exercise for obliques.',
     'SYSTEM', 1, NOW(), NOW()),
    
    -- CARDIO EXERCISES
    ((SELECT id FROM disciplines WHERE name = 'Running'), 
     'Treadmill Running', 
     'Indoor running on treadmill for cardio.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Cycling'), 
     'Stationary Cycling', 
     'Indoor cycling on stationary bike.',
     'SYSTEM', 1, NOW(), NOW()),
    
    ((SELECT id FROM disciplines WHERE name = 'Swimming'), 
     'Swimming Laps', 
     'Full-body cardio exercise in swimming pool.',
     'SYSTEM', 1, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 3. ROUTINE TEMPLATES - Pre-designed training routines for users
-- =============================================================================
INSERT INTO routine_templates (name, description, type, created_at, updated_at)
VALUES 
    ('Beginner Full Body', 
     'Full body routine 3 days per week. Ideal for beginners learning proper form. Focuses on compound movements.',
     'SYSTEM', NOW(), NOW()),
    
    ('Upper/Lower Split', 
     '4-day split routine alternating upper body and lower body workouts. Intermediate level.',
     'SYSTEM', NOW(), NOW()),
    
    ('Push/Pull/Legs (PPL)', 
     '6-day split routine. Push day (chest, shoulders, triceps), Pull day (back, biceps), Legs day.',
     'SYSTEM', NOW(), NOW()),
    
    ('Strength Focus', 
     'Heavy compound focus with 4 days per week. Emphasis on progressive overload and strength gains.',
     'SYSTEM', NOW(), NOW()),
    
    ('Hypertrophy Focus', 
     '5-day high-volume routine designed for muscle growth. Includes compound and isolation exercises.',
     'SYSTEM', NOW(), NOW()),
    
    ('Cardio & Core', 
     '3-day routine focusing on cardiovascular fitness and core strength. Low intensity for beginners.',
     'SYSTEM', NOW(), NOW()),
    
    ('CrossFit Inspired', 
     'High-intensity functional fitness routine combining strength, power, and conditioning.',
     'SYSTEM', NOW(), NOW()),
    
    ('Endurance Training', 
     'Marathon and distance running preparation routine with varied intensities.',
     'SYSTEM', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 4. SUMMARY
-- =============================================================================
-- Display inserted data
SELECT COUNT(*) as total_disciplines FROM disciplines;
SELECT COUNT(*) as total_system_exercises FROM exercises WHERE type = 'SYSTEM';
SELECT COUNT(*) as total_routine_templates FROM routine_templates;

-- Sample data for verification
SELECT 'Sample Disciplines:' as info;
SELECT id, name, type FROM disciplines LIMIT 5;

SELECT 'Sample Exercises:' as info;
SELECT e.id, e.name, d.name as discipline, e.type 
FROM exercises e 
JOIN disciplines d ON e.discipline_id = d.id 
WHERE e.type = 'SYSTEM' LIMIT 5;

SELECT 'Sample Routine Templates:' as info;
SELECT id, name, type FROM routine_templates LIMIT 5;
