package com.tariffsheriff.backend.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit event entity for comprehensive logging
 */
@Entity
@Table(name = "audit_events")
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(name = "event_category", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventCategory category;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "description")
    private String description;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "request_method")
    private String requestMethod;

    @Column(name = "request_uri")
    private String requestUri;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "ai_model_used")
    private String aiModelUsed;

    @Column(name = "ai_tokens_consumed")
    private Integer aiTokensConsumed;

    @Column(name = "data_sources_accessed", columnDefinition = "TEXT")
    private String dataSourcesAccessed;

    @Column(name = "sensitive_data_accessed")
    private Boolean sensitiveDataAccessed = false;

    @Column(name = "compliance_flags", columnDefinition = "TEXT")
    private String complianceFlags;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData; // JSON string for flexible data storage

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (retentionUntil == null) {
            // Default retention period of 7 years for compliance
            retentionUntil = createdAt.plusYears(7);
        }
    }

    public enum AuditEventType {
        AUTHENTICATION,
        AUTHORIZATION,
        AI_INTERACTION,
        DATA_ACCESS,
        SYSTEM_EVENT,
        SECURITY_EVENT,
        COMPLIANCE_EVENT
    }

    public enum AuditEventCategory {
        USER_ACTION,
        SYSTEM_ACTION,
        SECURITY_ACTION,
        AI_ACTION,
        DATA_ACTION,
        ADMIN_ACTION
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { this.eventType = eventType; }
    
    public AuditEventCategory getCategory() { return category; }
    public void setCategory(AuditEventCategory category) { this.category = category; }
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getAiModelUsed() { return aiModelUsed; }
    public void setAiModelUsed(String aiModelUsed) { this.aiModelUsed = aiModelUsed; }
    
    public Integer getAiTokensConsumed() { return aiTokensConsumed; }
    public void setAiTokensConsumed(Integer aiTokensConsumed) { this.aiTokensConsumed = aiTokensConsumed; }
    
    public String getDataSourcesAccessed() { return dataSourcesAccessed; }
    public void setDataSourcesAccessed(String dataSourcesAccessed) { this.dataSourcesAccessed = dataSourcesAccessed; }
    
    public Boolean getSensitiveDataAccessed() { return sensitiveDataAccessed; }
    public void setSensitiveDataAccessed(Boolean sensitiveDataAccessed) { this.sensitiveDataAccessed = sensitiveDataAccessed; }
    
    public String getComplianceFlags() { return complianceFlags; }
    public void setComplianceFlags(String complianceFlags) { this.complianceFlags = complianceFlags; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getRetentionUntil() { return retentionUntil; }
    public void setRetentionUntil(LocalDateTime retentionUntil) { this.retentionUntil = retentionUntil; }
}