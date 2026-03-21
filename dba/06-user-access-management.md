# User Access Management and Security

## Overview

Comprehensive guide to role-based access control (RBAC), user management, privilege configuration, and security best practices for PostgreSQL in production. Covers creating and managing database roles, fine-grained permission model, group management, credential handling, audit logging, and security policies for the Gym Platform's multi-service architecture.

## Table of Contents

- [RBAC Fundamentals](#rbac-fundamentals)
- [User Roles and Creation](#user-roles-and-creation)
- [Privilege Model](#privilege-model)
- [Group-Based Access Control](#group-based-access-control)
- [Fine-Grained Access Control](#fine-grained-access-control)
- [Service Accounts](#service-accounts)
- [Credential Management](#credential-management)
- [Audit and Compliance](#audit-and-compliance)
- [Security Best Practices](#security-best-practices)
- [Troubleshooting Access](#troubleshooting-access)

---

## RBAC Fundamentals

### PostgreSQL Role Hierarchy

```
┌─────────────────────────────────────────┐
│         Database Superuser               │
│         (postgres or root)               │
├─────────────────────────────────────────┤
│         Cluster Roles                    │
│  ┌──────────────────────────────────┐   │
│  │  Database Administrators (dba)   │   │
│  │  - CREATEDB, CREATEUSER          │   │
│  │  - Schema ownership              │   │
│  │  - Full privileges               │   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│         Schema-Level Access             │
│  ┌──────────────────────────────────┐   │
│  │  Application Services            │   │
│  │  - SELECT/INSERT/UPDATE/DELETE   │   │
│  │  - Specific table grants         │   │
│  │  - Restricted privileges         │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### Role vs User

```sql
-- CREATE ROLE - Generic role (can be container or user)
CREATE ROLE service_role;

-- CREATE USER - Role with LOGIN privilege
CREATE USER app_user WITH PASSWORD 'password';

-- User is equivalent to:
CREATE ROLE app_user WITH LOGIN PASSWORD 'password';

-- Groups are roles without LOGIN
CREATE ROLE developers;
GRANT developers TO user1, user2, user3;
```

---

## User Roles and Creation

### Creating Database Users

```sql
-- Basic user creation
CREATE USER gym_app WITH 
  PASSWORD 'secure_password_here'
  VALID UNTIL '2025-03-31';

-- User with connection limit
CREATE USER gym_service WITH 
  PASSWORD 'service_password'
  CONNECTION LIMIT 20
  VALID UNTIL '2025-12-31';

-- Superuser (use sparingly!)
CREATE SUPERUSER admin_dba WITH 
  PASSWORD 'admin_password'
  CREATEROLE
  CREATEDB;

-- Non-interactive replication user
CREATE ROLE replication_user WITH 
  REPLICATION 
  BYPASSRLS 
  PASSWORD 'replication_password'
  NOSUPERUSER
  NOCREATEDB
  NOCREATEROLE;

-- Read-only user
CREATE ROLE read_only_user WITH 
  PASSWORD 'readonly_password'
  NOINHERIT
  NOSUPERUSER
  NOCREATEDB
  NOCREATEROLE
  NOLOGIN;

-- Test user for development
CREATE USER test_user WITH 
  PASSWORD 'test_password'
  VALID UNTIL NOW() + INTERVAL '30 days';
```

### User Properties

```sql
-- View all user properties
SELECT 
  rolname,
  rolsuper,
  rolinherit,
  rolcreaterole,
  rolcreatedb,
  rolcanlogin,
  rolbypassrls,
  rolconnlimit,
  rolvaliduntil
FROM pg_roles
WHERE rolname NOT LIKE 'pg_%'
ORDER BY rolname;

-- Modify user properties
ALTER USER gym_app WITH 
  CONNECTION LIMIT 50
  VALID UNTIL '2025-12-31';

-- Lock user (prevent login)
ALTER USER gym_app NOLOGIN;

-- Unlock user
ALTER USER gym_app LOGIN;

-- Rename user
ALTER USER gym_app RENAME TO gym_application;

-- Change password
ALTER USER gym_app WITH PASSWORD 'new_secure_password';

-- Drop user and reassign objects
DROP OWNED BY old_user;
DROP USER old_user;

-- Or transfer ownership
REASSIGN OWNED BY old_user TO new_owner;
```

---

## Privilege Model

### Object Privileges

**PostgreSQL Privilege Types:**

```
Database Level:
  - CONNECT: Allow connection to database
  - CREATE: Allow schema creation
  - TEMP: Allow temporary table creation

Schema Level:
  - CREATE: Create objects in schema
  - USAGE: Search/list schema contents

Table Level:
  - SELECT: Read data
  - INSERT: Add rows
  - UPDATE: Modify rows
  - DELETE: Remove rows
  - TRUNCATE: Clear all rows
  - REFERENCES: Create foreign keys
  - TRIGGER: Create triggers

Column Level:
  - SELECT: Read column
  - INSERT: Write to column
  - UPDATE: Modify column
  - REFERENCES: Reference in FK

Sequence Level:
  - USAGE: Read sequence
  - SELECT: Get sequence value
  - UPDATE: Modify sequence

Function Level:
  - EXECUTE: Call function
```

### Grant Hierarchy

```sql
-- Database level
GRANT CONNECT ON DATABASE gym_auth TO gym_app;
GRANT CREATE ON DATABASE gym_training TO app_developer;

-- Schema level
GRANT USAGE ON SCHEMA public TO gym_app;
GRANT CREATE ON SCHEMA public TO app_developer;

-- Table level
GRANT SELECT ON TABLE users TO read_only_user;
GRANT SELECT, INSERT, UPDATE ON TABLE workouts TO gym_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE workouts TO app_developer;

-- Column-specific grant
GRANT SELECT (id, email, status) ON TABLE users TO read_only_user;

-- Sequence
GRANT USAGE, SELECT ON SEQUENCE user_id_seq TO gym_app;

-- Function
GRANT EXECUTE ON FUNCTION get_user_workouts(INT) TO gym_app;

-- All tables in schema
GRANT SELECT ON ALL TABLES IN SCHEMA public TO read_only_user;

-- Default for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT SELECT ON TABLES TO read_only_user;

-- With GRANT option (can grant to others)
GRANT SELECT ON users TO manager WITH GRANT OPTION;
```

### Revoke Privileges

```sql
-- Revoke simple privilege
REVOKE INSERT, UPDATE, DELETE ON TABLE workouts FROM gym_app;

-- Revoke GRANT option
REVOKE GRANT OPTION FOR SELECT ON users FROM manager;

-- Revoke all privileges
REVOKE ALL PRIVILEGES ON TABLE workouts FROM gym_app;

-- Cascade revoke (revoke from others they granted to)
REVOKE ALL PRIVILEGES ON DATABASE gym_training FROM app_developer CASCADE;

-- Show current grants
SELECT 
  grantee,
  privilege_type,
  is_grantable
FROM role_table_grants
WHERE table_name = 'workouts'
ORDER BY grantee, privilege_type;
```

---

## Group-Based Access Control

### Creating Role Groups

```sql
-- Create role groups
CREATE ROLE developers;
CREATE ROLE analysts;
CREATE ROLE admins;
CREATE ROLE read_only_staff;

-- Add members to groups
GRANT developers TO dev_user1, dev_user2, dev_user3;
GRANT analysts TO analyst_user1, analyst_user2;
GRANT admins TO dba_user1, dba_user2;
GRANT read_only_staff TO support_user1, support_user2, support_user3;

-- Grant privileges to group (all members inherit)
GRANT CONNECT ON DATABASE gym_training TO developers;
GRANT USAGE ON SCHEMA public TO developers;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO developers;

-- Grant to analysts (read-only)
GRANT CONNECT ON DATABASE gym_training TO analysts;
GRANT USAGE ON SCHEMA public TO analysts;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analysts;

-- Setup future defaults
ALTER DEFAULT PRIVILEGES FOR ROLE developers IN SCHEMA public 
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO developers;

ALTER DEFAULT PRIVILEGES FOR ROLE admins IN SCHEMA public 
  GRANT ALL ON TABLES TO admins;
```

### Managing Group Membership

```sql
-- Add user to group
GRANT developers TO new_developer;

-- Remove user from group
REVOKE developers FROM old_developer;

-- Check group members
SELECT 
  member.rolname as member,
  admin_option,
  inherit_option
FROM pg_auth_members
JOIN pg_roles member ON pg_auth_members.member = member.oid
JOIN pg_roles role ON pg_auth_members.roleid = role.oid
WHERE role.rolname = 'developers'
ORDER BY member.rolname;

-- Check groups for user
SELECT 
  role.rolname as group_name,
  admin_option,
  inherit_option
FROM pg_auth_members
JOIN pg_roles member ON pg_auth_members.member = member.oid
JOIN pg_roles role ON pg_auth_members.roleid = role.oid
WHERE member.rolname = 'dev_user1'
ORDER BY role.rolname;
```

---

## Fine-Grained Access Control

### Row-Level Security (RLS)

```sql
-- Enable RLS on sensitive table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own record
CREATE POLICY user_self_access ON users
  FOR SELECT
  USING (id = current_user_id());

-- Policy: Users can only update their own record
CREATE POLICY user_self_update ON users
  FOR UPDATE
  USING (id = current_user_id())
  WITH CHECK (id = current_user_id());

-- Policy: Admins can see all records
CREATE POLICY admin_full_access ON users
  FOR ALL
  USING (current_user_is_admin())
  WITH CHECK (current_user_is_admin());

-- Check RLS policies
SELECT 
  schemaname,
  tablename,
  policyname,
  permissive,
  roles,
  qual,
  with_check
FROM pg_policies
WHERE tablename = 'users';
```

### Column-Level Encryption

```sql
-- Extension for column encryption
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Encrypted storage
CREATE TABLE sensitive_data (
  id BIGSERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  ssn TEXT NOT NULL,
  encrypted_ssn BYTEA,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Store encrypted data
INSERT INTO sensitive_data (user_id, ssn, encrypted_ssn)
VALUES (
  123, 
  '123-45-6789',
  pgp_sym_encrypt('123-45-6789', 'encryption_key_here')
);

-- Query encrypted data (must know key)
SELECT 
  id,
  user_id,
  pgp_sym_decrypt(encrypted_ssn, 'encryption_key_here') as decrypted_ssn
FROM sensitive_data
WHERE user_id = 123;

-- Column access via function (masking)
CREATE OR REPLACE FUNCTION get_user_ssn(user_id INT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  IF current_user != 'compliance_officer' THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;
  RETURN (SELECT pgp_sym_decrypt(encrypted_ssn, 'key') 
          FROM sensitive_data 
          WHERE user_id = $1);
END;
$$;

-- Grant only function access
GRANT EXECUTE ON FUNCTION get_user_ssn(INT) TO compliance_officer;
REVOKE SELECT ON TABLE sensitive_data FROM compliance_officer;
```

---

## Service Accounts

### Application Service Accounts

```sql
-- Auth Service account
CREATE USER gym_auth_service WITH 
  PASSWORD 'auth_service_password'
  CONNECTION LIMIT 20
  NOSUPERUSER
  NOCREATEDB
  NOCREATEROLE
  INHERIT;

-- Configure auth service role
GRANT CONNECT ON DATABASE gym_auth TO gym_auth_service;
GRANT USAGE ON SCHEMA public, auth TO gym_auth_service;

-- Grant specific table access
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE auth.users TO gym_auth_service;
GRANT SELECT, INSERT, UPDATE ON TABLE auth.sessions TO gym_auth_service;
GRANT SELECT ON TABLE auth.roles TO gym_auth_service;

-- Grant sequence access for auto-increment
GRANT USAGE, SELECT ON SEQUENCE auth.users_id_seq TO gym_auth_service;
GRANT USAGE, SELECT ON SEQUENCE auth.sessions_id_seq TO gym_auth_service;

-- Training Service account
CREATE USER gym_training_service WITH PASSWORD 'training_password' CONNECTION LIMIT 15;
GRANT CONNECT ON DATABASE gym_training TO gym_training_service;
GRANT USAGE ON SCHEMA public, training TO gym_training_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA training TO gym_training_service;

-- Tracking Service account
CREATE USER gym_tracking_service WITH PASSWORD 'tracking_password' CONNECTION LIMIT 25;
GRANT CONNECT ON DATABASE gym_tracking TO gym_tracking_service;
GRANT USAGE ON SCHEMA public, tracking TO gym_tracking_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA tracking TO gym_tracking_service;

-- Notification Service account
CREATE USER gym_notification_service WITH PASSWORD 'notification_password' CONNECTION LIMIT 10;
GRANT CONNECT ON DATABASE gym_common TO gym_notification_service;
GRANT USAGE ON SCHEMA public, notifications TO gym_notification_service;
GRANT SELECT, INSERT ON TABLE notifications.queue TO gym_notification_service;
GRANT SELECT ON TABLE auth.users TO gym_notification_service;
```

### Backup Service Account

```sql
-- Backup user with replication privilege
CREATE USER gym_backup WITH 
  REPLICATION
  PASSWORD 'backup_password'
  NOCREATEDB
  NOCREATEUSER
  INHERIT
  CONNECTION LIMIT 5;

-- Grant specific permissions
GRANT CONNECT ON DATABASE gym_auth TO gym_backup;
GRANT CONNECT ON DATABASE gym_training TO gym_backup;
GRANT CONNECT ON DATABASE gym_tracking TO gym_backup;
GRANT CONNECT ON DATABASE gym_common TO gym_backup;

-- View-only access for verification
GRANT USAGE ON SCHEMA public TO gym_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO gym_backup;
```

---

## Credential Management

### Password Policies

```sql
-- Create password verification function
CREATE OR REPLACE FUNCTION validate_password(password TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
  -- Minimum 12 characters
  IF LENGTH(password) < 12 THEN
    RAISE EXCEPTION 'Password must be at least 12 characters';
  END IF;
  
  -- Must contain uppercase
  IF password !~ '[A-Z]' THEN
    RAISE EXCEPTION 'Password must contain uppercase';
  END IF;
  
  -- Must contain lowercase
  IF password !~ '[a-z]' THEN
    RAISE EXCEPTION 'Password must contain lowercase';
  END IF;
  
  -- Must contain number
  IF password !~ '[0-9]' THEN
    RAISE EXCEPTION 'Password must contain number';
  END IF;
  
  -- Must contain special character
  IF password !~ '[!@#$%^&*()_+-=\[\]{};:'"|,.<>?]' THEN
    RAISE EXCEPTION 'Password must contain special character';
  END IF;
  
  RETURN TRUE;
END;
$$;

-- Use in password creation
DO $$
BEGIN
  IF NOT validate_password('NewSecure123!Pwd') THEN
    RAISE EXCEPTION 'Invalid password';
  END IF;
END;
$$;
```

### Password Rotation

```bash
#!/bin/bash
# rotate-db-passwords.sh - Automated password rotation

SERVICES=("gym_auth_service" "gym_training_service" "gym_tracking_service" "gym_notification_service")
VAULT_ADDR="https://vault.gym.local"
VAULT_TOKEN=$(cat /etc/gym/vault-token)
LOG_FILE="/var/log/postgresql/password-rotation.log"

for service in "${SERVICES[@]}"; do
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Rotating password for $service" >> "$LOG_FILE"
  
  # Generate new password
  NEW_PASSWORD=$(openssl rand -base64 24)
  
  # Update in database
  psql -U postgres -d postgres << EOF
  ALTER USER $service WITH PASSWORD '$NEW_PASSWORD';
EOF
  
  # Store in vault
  curl -s -X POST \
    -H "X-Vault-Token: $VAULT_TOKEN" \
    -d "{\"data\": {\"password\": \"$NEW_PASSWORD\"}}" \
    "$VAULT_ADDR/v1/secret/database/$service/password"
  
  # Log success
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Password rotated successfully for $service" >> "$LOG_FILE"
done
```

### Expired User Cleanup

```sql
-- Find expired users
SELECT 
  usename,
  usevaluntil,
  NOW() - usevaluntil as expired_for
FROM pg_user
WHERE usevaluntil < NOW()
ORDER BY usevaluntil DESC;

-- Disable expired users
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN SELECT usename FROM pg_user WHERE usevaluntil < NOW() LOOP
    EXECUTE 'ALTER USER ' || r.usename || ' NOLOGIN';
    RAISE NOTICE 'Disabled user: %', r.usename;
  END LOOP;
END;
$$;

-- Create reminder for users expiring soon
SELECT 
  usename,
  usevaluntil,
  EXTRACT(DAY FROM usevaluntil - NOW()) as days_until_expiry
FROM pg_user
WHERE usevaluntil BETWEEN NOW() AND NOW() + INTERVAL '30 days'
ORDER BY usevaluntil;
```

---

## Audit and Compliance

### Connection Audit

```ini
# postgresql.conf - Audit logging

# Log all connections
log_connections = on
log_disconnections = on

# Log authentication attempts
log_statement = 'all'
log_duration = on
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '

# Log failed attempts
log_error_verbosity = default
log_min_error_statement = error
```

### DDL Audit

```sql
-- Create audit log table
CREATE TABLE audit.ddl_log (
  id BIGSERIAL PRIMARY KEY,
  event_time TIMESTAMP DEFAULT NOW(),
  user_name NAME NOT NULL,
  database_name NAME NOT NULL,
  schema_name NAME,
  object_name NAME,
  object_type TEXT,
  command TEXT,
  query TEXT
);

-- Create trigger for DDL audit
CREATE OR REPLACE FUNCTION audit_ddl()
RETURNS EVENT_TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_rec RECORD;
BEGIN
  FOR v_rec IN 
    SELECT 
      event,
      schema_name,
      object_name,
      object_type,
      command_tag
    FROM pg_event_trigger_ddl_commands()
  LOOP
    INSERT INTO audit.ddl_log (
      user_name,
      database_name,
      schema_name,
      object_name,
      object_type,
      command,
      query
    ) VALUES (
      CURRENT_USER,
      CURRENT_DATABASE(),
      v_rec.schema_name,
      v_rec.object_name,
      v_rec.object_type,
      v_rec.command_tag,
      current_query_context()
    );
  END LOOP;
END;
$$;

CREATE EVENT TRIGGER ddl_audit_trigger
ON DDL_COMMAND_END
EXECUTE FUNCTION audit_ddl();
```

### Data Change Audit

```sql
-- Create change audit trigger
CREATE TABLE audit.data_changes (
  id BIGSERIAL PRIMARY KEY,
  table_name TEXT NOT NULL,
  operation TEXT NOT NULL,  -- INSERT, UPDATE, DELETE
  old_data JSONB,
  new_data JSONB,
  changed_by NAME NOT NULL,
  changed_at TIMESTAMP DEFAULT NOW()
);

-- Generic audit function (for any table)
CREATE OR REPLACE FUNCTION audit_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO audit.data_changes (
    table_name,
    operation,
    old_data,
    new_data,
    changed_by
  ) VALUES (
    TG_TABLE_NAME,
    TG_OP,
    CASE WHEN TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN row_to_json(OLD) ELSE NULL END,
    CASE WHEN TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN row_to_json(NEW) ELSE NULL END,
    CURRENT_USER
  );
  
  RETURN COALESCE(NEW, OLD);
END;
$$;

-- Attach to sensitive tables
CREATE TRIGGER audit_users_changes
AFTER INSERT OR UPDATE OR DELETE ON auth.users
FOR EACH ROW EXECUTE FUNCTION audit_changes();

-- Query audit logs
SELECT 
  changed_at,
  table_name,
  operation,
  changed_by,
  new_data->>'email' as user_email,
  new_data->>'status' as status_change
FROM audit.data_changes
WHERE table_name = 'users'
  AND changed_at > NOW() - INTERVAL '7 days'
ORDER BY changed_at DESC
LIMIT 100;
```

---

## Security Best Practices

### Principle of Least Privilege

```sql
-- DO: Create restricted service account
CREATE USER app_limited WITH PASSWORD 'password';
GRANT CONNECT ON DATABASE gym_training TO app_limited;
GRANT USAGE ON SCHEMA public TO app_limited;
GRANT SELECT, INSERT ON TABLE workouts TO app_limited;
-- Service account has only what it needs

-- DON'T: Give full access
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO app_limited;

-- DON'T: Use superuser account for applications
-- Always use restricted service accounts
```

### Rotate Credentials Regularly

```bash
# Schedule monthly password rotation
0 1 1 * * /usr/local/bin/rotate-db-passwords.sh

# Rotate replication user password quarterly
0 0 1 1,4,7,10 * /usr/local/bin/rotate-replication-password.sh

# Update application secrets after rotation
After running rotation, notify application teams to update connection strings
```

### Monitor Unusual Access

```sql
-- Detect unusual access patterns
SELECT 
  usename,
  datname,
  COUNT(*) as connection_count,
  MAX(backend_start) as latest_connection,
  COUNT(DISTINCT client_addr) as unique_ips
FROM pg_stat_activity
GROUP BY usename, datname
HAVING COUNT(*) > 5  -- Flag if more than 5 connections
ORDER BY connection_count DESC;

-- Alert on failed authentication
-- Check PostgreSQL logs for repeated failed attempts
SELECT 
  datname,
  usename,
  COUNT(*) as failed_attempts
FROM (
  SELECT datname, usename FROM pg_stat_activity
  WHERE state = 'authentication'
) t
GROUP BY datname, usename
HAVING COUNT(*) > 10;
```

---

## Troubleshooting Access

### Permission Denied Errors

```sql
-- Debug: Check user permissions
SELECT 
  schemaname,
  tablename,
  perm,
  has_table_privilege('app_user', schemaname||'.'||tablename, perm)
FROM (
  SELECT 'public' as schemaname, 'users' as tablename, 'SELECT' as perm
  UNION ALL
  SELECT 'public', 'users', 'INSERT'
  UNION ALL
  SELECT 'public', 'workouts', 'SELECT'
  UNION ALL
  SELECT 'public', 'workouts', 'UPDATE'
);

-- Check if user has USAGE on schema
SELECT 
  has_schema_privilege('app_user', 'public', 'USAGE') as schema_usage,
  has_schema_privilege('app_user', 'public', 'CREATE') as schema_create;

-- Check role inheritance
SELECT 
  rolname,
  rolinherit
FROM pg_roles
WHERE rolname = 'app_user';
```

### Connection Limit Issues

```sql
-- Check current connections by user
SELECT 
  usename,
  COUNT(*) as connection_count,
  rolconnlimit as limit_per_user
FROM pg_stat_activity
JOIN pg_roles ON pg_roles.rolname = pg_stat_activity.usename
GROUP BY usename, rolconnlimit
ORDER BY connection_count DESC;

-- Increase connection limit
ALTER USER gym_app WITH CONNECTION LIMIT 50;

-- Or disable limit (use -1)
ALTER USER gym_app WITH CONNECTION LIMIT -1;
```

---

## Related Documentation

- [01-getting-started.md](01-getting-started.md) - Initial setup
- [07-monitoring-alerting.md](07-monitoring-alerting.md) - Monitor access patterns
- [08-troubleshooting.md](08-troubleshooting.md) - General troubleshooting

