package com.tariffsheriff.backend.audit.service;

import com.tariffsheriff.backend.audit.model.AuditEvent;
import com.tariffsheriff.backend.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Comprehensive audit service for logging and compliance tracking
 */
@Service
@RequiredArgsConstructor
public class AuditService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    // PII patterns for data anonymization
    private static final List<Pattern> PII_PATTERNS = Arrays.asList(
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"), // SSN
        Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"), // Credit card
        Pattern.compile("\\b\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4}\\b"), // Phone number
        Pattern.compile("\\b\\d{1,5}\\s+[A-Za-z\\s]+\\s+(Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln|Boulevard|Blvd)\\b") // Address
    );

    /**
     * Logs an AI interaction event
     */
    @Async
    public CompletableFuture<Void> logAiInteraction(AiInteractionAuditData data) {
        try {
            AuditEvent event = createBaseAuditEvent(data.getUserId(), data.getSessionId(), 
                    data.getClientIp(), data.getUserAgent());
            
            event.setEventType(AuditEvent.AuditEventType.AI_INTERACTION);
            event.setCategory(AuditEvent.AuditEventCategory.AI_ACTION);
            event.setAction(data.getAction());
            event.setDescription(anonymizeData(data.getDescription()));
            event.setAiModelUsed(data.getAiModel());
            event.setAiTokensConsumed(data.getTokensConsumed());
            event.setDataSourcesAccessed(String.join(",", data.getDataSources()));
            event.setSensitiveDataAccessed(data.isSensitiveDataAccessed());
            event.setProcessingTimeMs(data.getProcessingTimeMs());
            event.setRiskLevel(determineRiskLevel(data));
            
            // Store additional data as JSON
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("queryComplexity", data.getQueryComplexity());
            additionalData.put("toolsUsed", data.getToolsUsed());
            additionalData.put("responseLength", data.getResponseLength());
            event.setAdditionalData(objectMapper.writeValueAsString(additionalData));
            
            auditEventRepository.save(event);
            log.debug("AI interaction audit event logged: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to log AI interaction audit event", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Logs a data access event
     */
    @Async
    public CompletableFuture<Void> logDataAccess(DataAccessAuditData data) {
        try {
            AuditEvent event = createBaseAuditEvent(data.getUserId(), data.getSessionId(), 
                    data.getClientIp(), data.getUserAgent());
            
            event.setEventType(AuditEvent.AuditEventType.DATA_ACCESS);
            event.setCategory(AuditEvent.AuditEventCategory.DATA_ACTION);
            event.setAction(data.getAction());
            event.setResourceType(data.getResourceType());
            event.setResourceId(data.getResourceId());
            event.setDescription(anonymizeData(data.getDescription()));
            event.setDataSourcesAccessed(data.getDataSource());
            event.setSensitiveDataAccessed(data.isSensitiveData());
            event.setRiskLevel(data.isSensitiveData() ? AuditEvent.RiskLevel.HIGH : AuditEvent.RiskLevel.LOW);
            
            // Store query parameters and filters
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("queryParameters", data.getQueryParameters());
            additionalData.put("recordsAccessed", data.getRecordsAccessed());
            event.setAdditionalData(objectMapper.writeValueAsString(additionalData));
            
            auditEventRepository.save(event);
            log.debug("Data access audit event logged: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to log data access audit event", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Logs a security event
     */
    @Async
    public CompletableFuture<Void> logSecurityEvent(SecurityAuditData data) {
        try {
            AuditEvent event = createBaseAuditEvent(data.getUserId(), data.getSessionId(), 
                    data.getClientIp(), data.getUserAgent());
            
            event.setEventType(AuditEvent.AuditEventType.SECURITY_EVENT);
            event.setCategory(AuditEvent.AuditEventCategory.SECURITY_ACTION);
            event.setAction(data.getAction());
            event.setDescription(data.getDescription());
            event.setRiskLevel(data.getRiskLevel());
            event.setComplianceFlags(String.join(",", data.getComplianceFlags()));
            
            // Store security-specific data
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("threatType", data.getThreatType());
            additionalData.put("securityRule", data.getSecurityRule());
            additionalData.put("mitigationAction", data.getMitigationAction());
            event.setAdditionalData(objectMapper.writeValueAsString(additionalData));
            
            auditEventRepository.save(event);
            log.warn("Security audit event logged: {} - {}", event.getEventId(), data.getDescription());
            
        } catch (Exception e) {
            log.error("Failed to log security audit event", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Logs an authentication event
     */
    @Async
    public CompletableFuture<Void> logAuthenticationEvent(AuthenticationAuditData data) {
        try {
            AuditEvent event = createBaseAuditEvent(data.getUserId(), data.getSessionId(), 
                    data.getClientIp(), data.getUserAgent());
            
            event.setEventType(AuditEvent.AuditEventType.AUTHENTICATION);
            event.setCategory(AuditEvent.AuditEventCategory.USER_ACTION);
            event.setAction(data.getAction());
            event.setDescription(data.getDescription());
            event.setResponseStatus(data.getResponseStatus());
            event.setRiskLevel(data.isSuccessful() ? AuditEvent.RiskLevel.LOW : AuditEvent.RiskLevel.MEDIUM);
            
            // Store authentication-specific data
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("authMethod", data.getAuthMethod());
            additionalData.put("successful", data.isSuccessful());
            additionalData.put("failureReason", data.getFailureReason());
            event.setAdditionalData(objectMapper.writeValueAsString(additionalData));
            
            auditEventRepository.save(event);
            log.debug("Authentication audit event logged: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to log authentication audit event", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Generates compliance report for a given time period
     */
    public ComplianceReport generateComplianceReport(LocalDateTime startTime, LocalDateTime endTime) {
        ComplianceReport report = new ComplianceReport();
        report.setReportPeriod(startTime, endTime);
        
        // AI usage statistics
        List<Object[]> aiStats = auditEventRepository.getAiUsageStatistics(startTime, endTime);
        report.setAiUsageStatistics(processAiUsageStats(aiStats));
        
        // Data access patterns
        List<Object[]> dataStats = auditEventRepository.getDataAccessPatterns(startTime, endTime);
        report.setDataAccessPatterns(processDataAccessStats(dataStats));
        
        // Security events summary
        Page<AuditEvent> securityEvents = auditEventRepository.findByEventTypeAndCreatedAtBetween(
                AuditEvent.AuditEventType.SECURITY_EVENT, startTime, endTime, Pageable.unpaged());
        report.setSecurityEventsSummary(processSecurityEvents(securityEvents.getContent()));
        
        // High-risk events
        Page<AuditEvent> highRiskEvents = auditEventRepository.findByRiskLevelInAndCreatedAtBetween(
                Arrays.asList(AuditEvent.RiskLevel.HIGH, AuditEvent.RiskLevel.CRITICAL), 
                startTime, endTime, Pageable.unpaged());
        report.setHighRiskEvents(highRiskEvents.getContent());
        
        // Sensitive data access
        Page<AuditEvent> sensitiveDataEvents = auditEventRepository.findBySensitiveDataAccessedTrueAndCreatedAtBetween(
                startTime, endTime, Pageable.unpaged());
        report.setSensitiveDataAccessCount(sensitiveDataEvents.getTotalElements());
        
        log.info("Compliance report generated for period {} to {}", startTime, endTime);
        return report;
    }

    /**
     * Searches audit events with filters
     */
    public Page<AuditEvent> searchAuditEvents(AuditSearchCriteria criteria, Pageable pageable) {
        if (criteria.getUserId() != null) {
            return auditEventRepository.findByUserIdAndCreatedAtBetween(
                    criteria.getUserId(), criteria.getStartTime(), criteria.getEndTime(), pageable);
        } else if (criteria.getEventType() != null) {
            return auditEventRepository.findByEventTypeAndCreatedAtBetween(
                    criteria.getEventType(), criteria.getStartTime(), criteria.getEndTime(), pageable);
        } else if (criteria.getRiskLevels() != null && !criteria.getRiskLevels().isEmpty()) {
            return auditEventRepository.findByRiskLevelInAndCreatedAtBetween(
                    criteria.getRiskLevels(), criteria.getStartTime(), criteria.getEndTime(), pageable);
        } else {
            // Default search by time range
            return auditEventRepository.findAll(pageable);
        }
    }

    /**
     * Anonymizes sensitive data in audit logs
     */
    private String anonymizeData(String data) {
        if (data == null) return null;
        
        String anonymized = data;
        for (Pattern pattern : PII_PATTERNS) {
            anonymized = pattern.matcher(anonymized).replaceAll("[REDACTED]");
        }
        
        return anonymized;
    }

    /**
     * Determines risk level based on AI interaction data
     */
    private AuditEvent.RiskLevel determineRiskLevel(AiInteractionAuditData data) {
        int riskScore = 0;
        
        // High token consumption
        if (data.getTokensConsumed() != null && data.getTokensConsumed() > 10000) {
            riskScore += 2;
        }
        
        // Sensitive data access
        if (data.isSensitiveDataAccessed()) {
            riskScore += 3;
        }
        
        // Complex queries
        if (data.getQueryComplexity() != null && data.getQueryComplexity() > 8) {
            riskScore += 2;
        }
        
        // Multiple data sources
        if (data.getDataSources().size() > 5) {
            riskScore += 1;
        }
        
        // Long processing time
        if (data.getProcessingTimeMs() != null && data.getProcessingTimeMs() > 30000) {
            riskScore += 1;
        }
        
        if (riskScore >= 6) return AuditEvent.RiskLevel.CRITICAL;
        if (riskScore >= 4) return AuditEvent.RiskLevel.HIGH;
        if (riskScore >= 2) return AuditEvent.RiskLevel.MEDIUM;
        return AuditEvent.RiskLevel.LOW;
    }

    /**
     * Creates base audit event with common fields
     */
    private AuditEvent createBaseAuditEvent(String userId, String sessionId, String clientIp, String userAgent) {
        AuditEvent event = new AuditEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setSessionId(sessionId);
        event.setClientIp(clientIp);
        event.setUserAgent(userAgent);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    /**
     * Processes AI usage statistics for compliance reporting
     */
    private Map<String, Object> processAiUsageStats(List<Object[]> stats) {
        Map<String, Object> result = new HashMap<>();
        for (Object[] stat : stats) {
            String model = (String) stat[0];
            Long count = (Long) stat[1];
            Long tokens = (Long) stat[2];
            
            Map<String, Object> modelStats = new HashMap<>();
            modelStats.put("requestCount", count);
            modelStats.put("totalTokens", tokens);
            result.put(model, modelStats);
        }
        return result;
    }

    /**
     * Processes data access statistics for compliance reporting
     */
    private Map<String, Long> processDataAccessStats(List<Object[]> stats) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] stat : stats) {
            String dataSource = (String) stat[0];
            Long count = (Long) stat[1];
            result.put(dataSource, count);
        }
        return result;
    }

    /**
     * Processes security events for compliance reporting
     */
    private Map<String, Object> processSecurityEvents(List<AuditEvent> events) {
        Map<String, Object> summary = new HashMap<>();
        Map<String, Long> eventCounts = new HashMap<>();
        
        for (AuditEvent event : events) {
            eventCounts.merge(event.getAction(), 1L, Long::sum);
        }
        
        summary.put("totalSecurityEvents", events.size());
        summary.put("eventBreakdown", eventCounts);
        return summary;
    }

    /**
     * Scheduled cleanup of old audit events based on retention policy
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void cleanupExpiredAuditEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now();
        List<AuditEvent> expiredEvents = auditEventRepository.findByRetentionUntilBefore(cutoffDate);
        
        if (!expiredEvents.isEmpty()) {
            auditEventRepository.deleteAll(expiredEvents);
            log.info("Cleaned up {} expired audit events", expiredEvents.size());
        }
    }

    // Data classes for audit events
    
    public static class AiInteractionAuditData {
        private String userId;
        private String sessionId;
        private String clientIp;
        private String userAgent;
        private String action;
        private String description;
        private String aiModel;
        private Integer tokensConsumed;
        private List<String> dataSources = new ArrayList<>();
        private boolean sensitiveDataAccessed;
        private Long processingTimeMs;
        private Integer queryComplexity;
        private List<String> toolsUsed = new ArrayList<>();
        private Integer responseLength;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAiModel() { return aiModel; }
        public void setAiModel(String aiModel) { this.aiModel = aiModel; }
        public Integer getTokensConsumed() { return tokensConsumed; }
        public void setTokensConsumed(Integer tokensConsumed) { this.tokensConsumed = tokensConsumed; }
        public List<String> getDataSources() { return dataSources; }
        public void setDataSources(List<String> dataSources) { this.dataSources = dataSources; }
        public boolean isSensitiveDataAccessed() { return sensitiveDataAccessed; }
        public void setSensitiveDataAccessed(boolean sensitiveDataAccessed) { this.sensitiveDataAccessed = sensitiveDataAccessed; }
        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        public Integer getQueryComplexity() { return queryComplexity; }
        public void setQueryComplexity(Integer queryComplexity) { this.queryComplexity = queryComplexity; }
        public List<String> getToolsUsed() { return toolsUsed; }
        public void setToolsUsed(List<String> toolsUsed) { this.toolsUsed = toolsUsed; }
        public Integer getResponseLength() { return responseLength; }
        public void setResponseLength(Integer responseLength) { this.responseLength = responseLength; }
    }

    public static class DataAccessAuditData {
        private String userId;
        private String sessionId;
        private String clientIp;
        private String userAgent;
        private String action;
        private String resourceType;
        private String resourceId;
        private String description;
        private String dataSource;
        private boolean sensitiveData;
        private Map<String, Object> queryParameters = new HashMap<>();
        private Integer recordsAccessed;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }
        public boolean isSensitiveData() { return sensitiveData; }
        public void setSensitiveData(boolean sensitiveData) { this.sensitiveData = sensitiveData; }
        public Map<String, Object> getQueryParameters() { return queryParameters; }
        public void setQueryParameters(Map<String, Object> queryParameters) { this.queryParameters = queryParameters; }
        public Integer getRecordsAccessed() { return recordsAccessed; }
        public void setRecordsAccessed(Integer recordsAccessed) { this.recordsAccessed = recordsAccessed; }
    }

    public static class SecurityAuditData {
        private String userId;
        private String sessionId;
        private String clientIp;
        private String userAgent;
        private String action;
        private String description;
        private AuditEvent.RiskLevel riskLevel;
        private List<String> complianceFlags = new ArrayList<>();
        private String threatType;
        private String securityRule;
        private String mitigationAction;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public AuditEvent.RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(AuditEvent.RiskLevel riskLevel) { this.riskLevel = riskLevel; }
        public List<String> getComplianceFlags() { return complianceFlags; }
        public void setComplianceFlags(List<String> complianceFlags) { this.complianceFlags = complianceFlags; }
        public String getThreatType() { return threatType; }
        public void setThreatType(String threatType) { this.threatType = threatType; }
        public String getSecurityRule() { return securityRule; }
        public void setSecurityRule(String securityRule) { this.securityRule = securityRule; }
        public String getMitigationAction() { return mitigationAction; }
        public void setMitigationAction(String mitigationAction) { this.mitigationAction = mitigationAction; }
    }

    public static class AuthenticationAuditData {
        private String userId;
        private String sessionId;
        private String clientIp;
        private String userAgent;
        private String action;
        private String description;
        private Integer responseStatus;
        private String authMethod;
        private boolean successful;
        private String failureReason;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getResponseStatus() { return responseStatus; }
        public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
        public String getAuthMethod() { return authMethod; }
        public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }

    public static class ComplianceReport {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Map<String, Object> aiUsageStatistics = new HashMap<>();
        private Map<String, Long> dataAccessPatterns = new HashMap<>();
        private Map<String, Object> securityEventsSummary = new HashMap<>();
        private List<AuditEvent> highRiskEvents = new ArrayList<>();
        private long sensitiveDataAccessCount;

        public void setReportPeriod(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        // Getters and setters
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public Map<String, Object> getAiUsageStatistics() { return aiUsageStatistics; }
        public void setAiUsageStatistics(Map<String, Object> aiUsageStatistics) { this.aiUsageStatistics = aiUsageStatistics; }
        public Map<String, Long> getDataAccessPatterns() { return dataAccessPatterns; }
        public void setDataAccessPatterns(Map<String, Long> dataAccessPatterns) { this.dataAccessPatterns = dataAccessPatterns; }
        public Map<String, Object> getSecurityEventsSummary() { return securityEventsSummary; }
        public void setSecurityEventsSummary(Map<String, Object> securityEventsSummary) { this.securityEventsSummary = securityEventsSummary; }
        public List<AuditEvent> getHighRiskEvents() { return highRiskEvents; }
        public void setHighRiskEvents(List<AuditEvent> highRiskEvents) { this.highRiskEvents = highRiskEvents; }
        public long getSensitiveDataAccessCount() { return sensitiveDataAccessCount; }
        public void setSensitiveDataAccessCount(long sensitiveDataAccessCount) { this.sensitiveDataAccessCount = sensitiveDataAccessCount; }
    }

    public static class AuditSearchCriteria {
        private String userId;
        private AuditEvent.AuditEventType eventType;
        private List<AuditEvent.RiskLevel> riskLevels;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public AuditEvent.AuditEventType getEventType() { return eventType; }
        public void setEventType(AuditEvent.AuditEventType eventType) { this.eventType = eventType; }
        public List<AuditEvent.RiskLevel> getRiskLevels() { return riskLevels; }
        public void setRiskLevels(List<AuditEvent.RiskLevel> riskLevels) { this.riskLevels = riskLevels; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}