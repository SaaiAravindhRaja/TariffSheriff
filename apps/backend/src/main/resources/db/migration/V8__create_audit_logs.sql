-- V8__create_audit_logs.sql
-- Create audit_logs table for comprehensive security event logging

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint to users table (nullable for system events)
    CONSTRAINT fk_audit_logs_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_logs_ip_address ON audit_logs(ip_address);

-- Composite indexes for common query patterns
CREATE INDEX idx_audit_logs_user_action ON audit_logs(user_id, action);
CREATE INDEX idx_audit_logs_user_created_at ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_logs_action_created_at ON audit_logs(action, created_at);

-- Add constraint for action values (common authentication events)
ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_action
CHECK (action IN (
    'USER_REGISTERED',
    'USER_LOGIN_SUCCESS',
    'USER_LOGIN_FAILED',
    'USER_LOGOUT',
    'USER_EMAIL_VERIFIED',
    'USER_PASSWORD_CHANGED',
    'USER_PASSWORD_RESET_REQUESTED',
    'USER_PASSWORD_RESET_COMPLETED',
    'USER_ACCOUNT_LOCKED',
    'USER_ACCOUNT_UNLOCKED',
    'USER_ROLE_CHANGED',
    'USER_STATUS_CHANGED',
    'TOKEN_REFRESHED',
    'TOKEN_BLACKLISTED',
    'SUSPICIOUS_ACTIVITY_DETECTED',
    'ADMIN_ACTION_PERFORMED',
    'SYSTEM_EVENT'
));

-- Add comment to table for documentation
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for security events and user actions';
COMMENT ON COLUMN audit_logs.user_id IS 'ID of the user who performed the action (NULL for system events)';
COMMENT ON COLUMN audit_logs.action IS 'Type of action performed (see constraint for allowed values)';
COMMENT ON COLUMN audit_logs.resource_type IS 'Type of resource affected (e.g., USER, TOKEN, SYSTEM)';
COMMENT ON COLUMN audit_logs.resource_id IS 'ID of the specific resource affected';
COMMENT ON COLUMN audit_logs.ip_address IS 'IP address from which the action was performed';
COMMENT ON COLUMN audit_logs.user_agent IS 'User agent string from the client';
COMMENT ON COLUMN audit_logs.details IS 'Additional details about the action in JSON or text format';
COMMENT ON COLUMN audit_logs.created_at IS 'Timestamp when the action occurred';