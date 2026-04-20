-- Add account lockout fields to users table
-- These fields support the login attempt tracking and account lockout feature

ALTER TABLE users
ADD COLUMN lockout_time DATETIME NULL COMMENT 'Time when account will be unlocked (NULL if not locked)',
ADD COLUMN failed_login_attempts INT DEFAULT 0 COMMENT 'Counter for consecutive failed login attempts';

-- Add index for efficient lockout time queries
CREATE INDEX idx_lockout_time ON users(lockout_time);
