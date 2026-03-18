-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS training_schema;
CREATE SCHEMA IF NOT EXISTS tracking_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- Grant privileges
GRANT ALL PRIVILEGES ON SCHEMA auth_schema TO gym_admin;
GRANT ALL PRIVILEGES ON SCHEMA training_schema TO gym_admin;
GRANT ALL PRIVILEGES ON SCHEMA tracking_schema TO gym_admin;
GRANT ALL PRIVILEGES ON SCHEMA notification_schema TO gym_admin;

-- Set search path
ALTER DATABASE gym_db SET search_path TO public;
