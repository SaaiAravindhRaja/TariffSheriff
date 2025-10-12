-- Create audit events table
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    description TEXT,
    client_ip VARCHAR(45),
    user_agent TEXT,
    request_method VARCHAR(10),
    request_uri TEXT,
    response_status INTEGER,
    processing_time_ms BIGINT,
    ai_model_used VARCHAR(100),
    ai_tokens_consumed INTEGER,
    data_sources_accessed TEXT,
    sensitive_data_accessed BOOLEAN DEFAULT FALSE,
    compliance_flags TEXT,
    risk_level VARCHAR(20) DEFAULT 'LOW',
    additional_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMP
);

-- Create indexes for audit events
CREATE INDEX idx_audit_events_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX idx_audit_events_risk_level ON audit_events(risk_level);
CREATE INDEX idx_audit_events_sensitive_data ON audit_events(sensitive_data_accessed);
CREATE INDEX idx_audit_events_retention ON audit_events(retention_until);

-- Create security threats table
CREATE TABLE security_threats (
    id BIGSERIAL PRIMARY KEY,
    threat_id VARCHAR(255) NOT NULL UNIQUE,
    threat_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_id VARCHAR(255),
    client_ip VARCHAR(45),
    user_agent TEXT,
    description TEXT NOT NULL,
    detection_rule VARCHAR(255),
    confidence_score DECIMAL(3,2),
    evidence TEXT,
    mitigation_actions TEXT,
    false_positive BOOLEAN DEFAULT FALSE,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for security threats
CREATE INDEX idx_security_threats_threat_type ON security_threats(threat_type);
CREATE INDEX idx_security_threats_severity ON security_threats(severity);
CREATE INDEX idx_security_threats_status ON security_threats(status);
CREATE INDEX idx_security_threats_user_id ON security_threats(user_id);
CREATE INDEX idx_security_threats_client_ip ON security_threats(client_ip);
CREATE INDEX idx_security_threats_detected_at ON security_threats(detected_at);

-- Add trigger to update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_security_threats_last_updated 
    BEFORE UPDATE ON security_threats 
    FOR EACH ROW 
    EXECUTE FUNCTION update_last_updated_column();

-- Add comments for documentation
COMMENT ON TABLE audit_events IS 'Comprehensive audit log for all system activities';
COMMENT ON TABLE security_threats IS 'Security threats detected by the monitoring system';

COMMENT ON COLUMN audit_events.event_id IS 'Unique identifier for the audit event';
COMMENT ON COLUMN audit_events.risk_level IS 'Risk level: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN audit_events.sensitive_data_accessed IS 'Whether sensitive data was accessed';
COMMENT ON COLUMN audit_events.retention_until IS 'Date when this record can be deleted';

COMMENT ON COLUMN security_threats.threat_id IS 'Unique identifier for the security threat';
COMMENT ON COLUMN security_threats.confidence_score IS 'Confidence score (0.0 to 1.0) for threat detection';
COMMENT ON COLUMN security_threats.false_positive IS 'Whether this threat was determined to be a false positive';