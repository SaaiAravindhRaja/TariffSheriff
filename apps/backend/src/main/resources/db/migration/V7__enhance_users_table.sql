-- V7__enhance_users_table.sql
-- Enhance users table with authentication fields for email verification, password reset, account locking, and audit trails

-- Add new columns for authentication features
ALTER TABLE users 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN verification_token VARCHAR(255),
ADD COLUMN verification_token_expires TIMESTAMP,
ADD COLUMN password_reset_token VARCHAR(255),
ADD COLUMN password_reset_token_expires TIMESTAMP,
ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN account_locked_until TIMESTAMP,
ADD COLUMN last_login TIMESTAMP,
ADD COLUMN last_login_ip VARCHAR(45),
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update existing admin user to have verified email and active status
UPDATE users 
SET email_verified = true, 
    status = 'ACTIVE',
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@example.com';

-- Create indexes for performance optimization
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_verification_token ON users(verification_token);
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token);
CREATE INDEX idx_users_last_login ON users(last_login);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Add constraints for status values
ALTER TABLE users ADD CONSTRAINT chk_user_status 
CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'LOCKED'));

-- Add constraint to ensure verification token expires is set when token is present
ALTER TABLE users ADD CONSTRAINT chk_verification_token_expires
CHECK ((verification_token IS NULL AND verification_token_expires IS NULL) OR 
       (verification_token IS NOT NULL AND verification_token_expires IS NOT NULL));

-- Add constraint to ensure password reset token expires is set when token is present
ALTER TABLE users ADD CONSTRAINT chk_password_reset_token_expires
CHECK ((password_reset_token IS NULL AND password_reset_token_expires IS NULL) OR 
       (password_reset_token IS NOT NULL AND password_reset_token_expires IS NOT NULL));

-- Add constraint for failed login attempts (non-negative)
ALTER TABLE users ADD CONSTRAINT chk_failed_login_attempts
CHECK (failed_login_attempts >= 0);

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();