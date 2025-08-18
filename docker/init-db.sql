-- Database initialization script for AI Code Review System
-- This script sets up the initial database schema and data

-- Ensure we're using the correct database
\connect reviewdb;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges to review_user
GRANT ALL PRIVILEGES ON DATABASE reviewdb TO review_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO review_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO review_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO review_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO review_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO review_user;

-- Insert initial configuration data (if needed)
-- This will be created by Hibernate/JPA, but we can add initial data here

-- Example: Default review configurations
-- INSERT INTO review_configuration (id, name, auto_approve_threshold, auto_reject_threshold) 
-- VALUES (1, 'default', 80, 30);

-- Example: Default team configurations
-- INSERT INTO team_configuration (id, team_name, strict_mode, custom_thresholds)
-- VALUES (1, 'backend', true, '{"maxMethodLength": 20, "maxClassLength": 250}');

-- Add any other initialization SQL here
COMMIT;