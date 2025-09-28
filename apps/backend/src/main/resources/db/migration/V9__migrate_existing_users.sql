-- V9__migrate_existing_users.sql
-- Data migration for existing users to ensure compatibility with authentication system

-- Update any existing users that might not have the new authentication fields properly set
UPDATE users 
SET 
    status = COALESCE(status, 'ACTIVE'),
    email_verified = COALESCE(email_verified, true), -- Assume existing users are verified
    failed_login_attempts = COALESCE(failed_login_attempts, 0),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE id IS NOT NULL;

-- Create initial audit log entries for existing users
INSERT INTO audit_logs (user_id, action, resource_type, resource_id, details, created_at)
SELECT 
    id,
    'USER_MIGRATED',
    'USER',
    id::TEXT,
    'User migrated to new authentication system',
    COALESCE(created_at, CURRENT_TIMESTAMP)
FROM users
WHERE id IS NOT NULL;

-- Ensure all users have a valid role
UPDATE users 
SET role = 'USER' 
WHERE role IS NULL OR role = '';

-- Clean up any invalid data
UPDATE users 
SET 
    verification_token = NULL,
    verification_token_expires = NULL
WHERE verification_token_expires < CURRENT_TIMESTAMP;

UPDATE users 
SET 
    password_reset_token = NULL,
    password_reset_token_expires = NULL
WHERE password_reset_token_expires < CURRENT_TIMESTAMP;

-- Reset account lockouts that have expired
UPDATE users 
SET 
    account_locked_until = NULL,
    failed_login_attempts = 0
WHERE account_locked_until < CURRENT_TIMESTAMP;

-- Add system audit log entry for migration completion
INSERT INTO audit_logs (user_id, action, resource_type, details, created_at)
VALUES (
    NULL,
    'SYSTEM_EVENT',
    'SYSTEM',
    'User authentication system migration completed successfully',
    CURRENT_TIMESTAMP
);