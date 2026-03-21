-- =============================================================================
-- GYM AUTH SERVICE - INITIAL DATA SCRIPT
-- =============================================================================
-- Seed data for auth_schema.
-- Columns match the User, Verification, and ProfessionalRequest entities exactly.
--
-- User entity fields: id, email, password, user_type, account_status, created_at, updated_at
-- user_roles table:   user_id, role
-- Verification:       id, user_id, type, code, verified, expires_at, verified_at, created_at
-- ProfessionalRequest:id, user_id, professional_id, status, rejection_reason, created_at, updated_at
--
-- Password for ALL test users: "password123"
-- BCrypt hash ($2a$10$...): change to a real hash before production use.
-- =============================================================================

SET search_path TO auth_schema;

-- =============================================================================
-- 1. USERS
-- =============================================================================
INSERT INTO users (email, password, user_type, account_status, created_at, updated_at)
VALUES
    ('john.doe@example.com',          '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'USER',         'ACTIVE',  NOW(), NOW()),
    ('jane.smith@example.com',        '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'USER',         'ACTIVE',  NOW(), NOW()),
    ('mike.johnson@example.com',      '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'USER',         'ACTIVE',  NOW(), NOW()),
    ('trainer.carlos@example.com',    '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'PROFESSIONAL', 'ACTIVE',  NOW(), NOW()),
    ('nutritionist.ana@example.com',  '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'PROFESSIONAL', 'ACTIVE',  NOW(), NOW()),
    ('therapist.luis@example.com',    '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'PROFESSIONAL', 'ACTIVE',  NOW(), NOW()),
    ('admin@example.com',             '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'ADMIN',        'ACTIVE',  NOW(), NOW()),
    ('pending.user@example.com',      '$2a$10$PW.PLACEHOLDER.HASH.FOR.DEV.USE.ONLY', 'USER',         'PENDING', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- =============================================================================
-- 2. USER ROLES (Spring Security roles)
-- =============================================================================
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ROLE_USER'
FROM   users u
WHERE  u.user_type = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ROLE_PROFESSIONAL'
FROM   users u
WHERE  u.user_type = 'PROFESSIONAL'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ROLE_ADMIN'
FROM   users u
WHERE  u.user_type = 'ADMIN'
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 3. VERIFICATIONS  (EMAIL verification tokens)
-- =============================================================================
-- Active users already verified
INSERT INTO verifications (user_id, type, code, verified, expires_at, verified_at, created_at)
SELECT u.id, 'EMAIL', 'VERIFIED', TRUE, NOW() + INTERVAL '30 days', NOW(), NOW()
FROM   users u
WHERE  u.account_status = 'ACTIVE'
  AND  NOT EXISTS (SELECT 1 FROM verifications v WHERE v.user_id = u.id);

-- Pending user has an outstanding code
INSERT INTO verifications (user_id, type, code, verified, expires_at, created_at)
SELECT u.id, 'EMAIL', '123456', FALSE, NOW() + INTERVAL '24 hours', NOW()
FROM   users u
WHERE  u.account_status = 'PENDING'
  AND  NOT EXISTS (SELECT 1 FROM verifications v WHERE v.user_id = u.id);

-- =============================================================================
-- 4. PROFESSIONAL REQUESTS  (users requesting a professional link)
-- =============================================================================
-- john.doe has an ACCEPTED request to trainer.carlos
INSERT INTO professional_requests (user_id, professional_id, status, created_at, updated_at)
SELECT req.id, pro.id, 'ACCEPTED', NOW(), NOW()
FROM   users req, users pro
WHERE  req.email = 'john.doe@example.com'
  AND  pro.email = 'trainer.carlos@example.com'
  AND  NOT EXISTS (
         SELECT 1 FROM professional_requests pr
         WHERE  pr.user_id = req.id AND pr.professional_id = pro.id
       );

-- jane.smith has an ACCEPTED request to nutritionist.ana
INSERT INTO professional_requests (user_id, professional_id, status, created_at, updated_at)
SELECT req.id, pro.id, 'ACCEPTED', NOW(), NOW()
FROM   users req, users pro
WHERE  req.email = 'jane.smith@example.com'
  AND  pro.email = 'nutritionist.ana@example.com'
  AND  NOT EXISTS (
         SELECT 1 FROM professional_requests pr
         WHERE  pr.user_id = req.id AND pr.professional_id = pro.id
       );

-- mike.johnson has a PENDING request to therapist.luis
INSERT INTO professional_requests (user_id, professional_id, status, created_at, updated_at)
SELECT req.id, pro.id, 'PENDING', NOW(), NOW()
FROM   users req, users pro
WHERE  req.email = 'mike.johnson@example.com'
  AND  pro.email = 'therapist.luis@example.com'
  AND  NOT EXISTS (
         SELECT 1 FROM professional_requests pr
         WHERE  pr.user_id = req.id AND pr.professional_id = pro.id
       );

-- =============================================================================
-- 5. SUMMARY
-- =============================================================================
SELECT COUNT(*) AS total_users               FROM users;
SELECT COUNT(*) AS total_active              FROM users WHERE account_status = 'ACTIVE';
SELECT COUNT(*) AS total_professionals       FROM users WHERE user_type = 'PROFESSIONAL';
SELECT COUNT(*) AS total_roles               FROM user_roles;
SELECT COUNT(*) AS total_verifications       FROM verifications;
SELECT COUNT(*) AS total_prof_requests       FROM professional_requests;

SELECT 'Users:' AS info;
SELECT id, email, user_type, account_status FROM users ORDER BY id;
