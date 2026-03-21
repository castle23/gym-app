-- =============================================================================
-- GYM NOTIFICATION SERVICE - INITIAL DATA SCRIPT
-- =============================================================================
-- Seed data for notification_schema.
-- Uses subqueries against auth_schema.users so IDs are not hardcoded.
-- Run AFTER 02-init-auth-data.sql.
-- =============================================================================

SET search_path TO notification_schema;

-- =============================================================================
-- 1. DEFAULT NOTIFICATION PREFERENCES
--    All types enabled, quiet hours 22:00-07:00 for every active user.
-- =============================================================================
DO $$
DECLARE
    r      RECORD;
    ntype  VARCHAR(30);
    ntypes VARCHAR(30)[] := ARRAY[
        'WORKOUT_REMINDER', 'ACHIEVEMENT', 'MESSAGE', 'ALERT', 'OTHER'
    ];
BEGIN
    FOR r IN
        SELECT id FROM auth_schema.users WHERE account_status = 'ACTIVE'
    LOOP
        FOREACH ntype IN ARRAY ntypes LOOP
            INSERT INTO notification_preferences
                (user_id, notification_type, is_enabled, quiet_hours_start, quiet_hours_end)
            VALUES
                (r.id, ntype, TRUE, '22:00', '07:00')
            ON CONFLICT (user_id, notification_type) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- 2. PUSH TOKENS - One per test user (Android or iOS)
-- =============================================================================
INSERT INTO push_tokens (user_id, token, device_type, is_active, created_at)
SELECT u.id, 'ExponentPushToken[john_android_dev_001]', 'android', TRUE, NOW()
FROM   auth_schema.users u WHERE u.email = 'john.doe@example.com'
ON CONFLICT (token) DO NOTHING;

INSERT INTO push_tokens (user_id, token, device_type, is_active, created_at)
SELECT u.id, 'ExponentPushToken[jane_ios_dev_002]', 'ios', TRUE, NOW()
FROM   auth_schema.users u WHERE u.email = 'jane.smith@example.com'
ON CONFLICT (token) DO NOTHING;

INSERT INTO push_tokens (user_id, token, device_type, is_active, created_at)
SELECT u.id, 'ExponentPushToken[mike_android_dev_003]', 'android', TRUE, NOW()
FROM   auth_schema.users u WHERE u.email = 'mike.johnson@example.com'
ON CONFLICT (token) DO NOTHING;

INSERT INTO push_tokens (user_id, token, device_type, is_active, created_at)
SELECT u.id, 'ExponentPushToken[carlos_ios_dev_004]', 'ios', TRUE, NOW()
FROM   auth_schema.users u WHERE u.email = 'trainer.carlos@example.com'
ON CONFLICT (token) DO NOTHING;

INSERT INTO push_tokens (user_id, token, device_type, is_active, created_at)
SELECT u.id, 'ExponentPushToken[ana_android_dev_005]', 'android', TRUE, NOW()
FROM   auth_schema.users u WHERE u.email = 'nutritionist.ana@example.com'
ON CONFLICT (token) DO NOTHING;

-- =============================================================================
-- 3. SAMPLE NOTIFICATIONS - Welcome + reminders for test users
-- =============================================================================
INSERT INTO notifications (user_id, title, body, type, is_read, sent_at, created_at)
SELECT u.id,
       'Welcome to GymTracker!',
       'Your account is set up. Start logging your workouts today!',
       'OTHER', FALSE, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
FROM   auth_schema.users u
WHERE  u.email IN ('john.doe@example.com', 'jane.smith@example.com', 'mike.johnson@example.com');

INSERT INTO notifications (user_id, title, body, type, is_read, sent_at, created_at)
SELECT u.id,
       'Workout Reminder',
       'Don''t forget your scheduled workout today!',
       'WORKOUT_REMINDER', FALSE, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
FROM   auth_schema.users u WHERE u.email = 'john.doe@example.com';

INSERT INTO notifications (user_id, title, body, type, is_read, sent_at, created_at)
SELECT u.id,
       'Achievement Unlocked!',
       'You completed your first week of workouts. Keep it up!',
       'ACHIEVEMENT', TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'
FROM   auth_schema.users u WHERE u.email = 'john.doe@example.com';

INSERT INTO notifications (user_id, title, body, type, is_read, sent_at, created_at)
SELECT u.id,
       'New Recommendation from your Trainer',
       'Your trainer has added a new training recommendation for you.',
       'MESSAGE', FALSE, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'
FROM   auth_schema.users u WHERE u.email = 'jane.smith@example.com';

-- =============================================================================
-- 4. SUMMARY
-- =============================================================================
SELECT COUNT(*) AS total_preferences  FROM notification_preferences;
SELECT COUNT(*) AS total_push_tokens  FROM push_tokens;
SELECT COUNT(*) AS total_notifications FROM notifications;

SELECT 'Preferences by type:' AS info;
SELECT notification_type, COUNT(*) AS total
FROM   notification_preferences
GROUP  BY notification_type
ORDER  BY notification_type;
