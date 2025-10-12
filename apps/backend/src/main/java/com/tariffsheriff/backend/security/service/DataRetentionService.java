package com.tariffsheriff.backend.security.service;

import com.tariffsheriff.backend.audit.model.AuditEvent;
import com.tariffsheriff.backend.audit.repository.AuditEventRepository;
import com.tariffsheriff.backend.security.model.SecurityThreat;
import com.tariffsheriff.backend.security.repository.SecurityThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data retention policy service for automatic cleanup and compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final AuditEventRepository auditEventRepository;
    private final SecurityThreatRepository securityThreatRepository;
    private final DataProtectionService dataProtectionService;

    // Retention policies in years
    private static final Map<String, Integer> RETENTION_POLICIES = new HashMap<>();
    static {
        RETENTION_POLICIES.put("AUDIT_EVENTS", 7);          // 7 years for audit events
        RETENTION_POLICIES.put("SECURITY_THREATS", 5);      // 5 years for security threats
        RETENTION_POLICIES.put("USER_CONVERSATIONS", 3);    // 3 years for conversations
        RETENTION_POLICIES.put("SYSTEM_LOGS", 2);           // 2 years for system logs
        RETENTION_POLICIES.put("TEMPORARY_DATA", 1);        // 1 year for temporary data
    }

    /**
     * Scheduled data retention cleanup - runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void performScheduledCleanup() {
        log.info("Starting scheduled data retention cleanup");
        
        DataRetentionReport report = new DataRetentionReport();
        report.setStartTime(LocalDateTime.now());
        
        try {
            // Clean up audit events
            int auditEventsDeleted = cleanupAuditEvents();
            report.setAuditEventsDeleted(auditEventsDeleted);
            
            // Clean up security threats
            int securityThreatsDeleted = cleanupSecurityThreats();
            report.setSecurityThreatsDeleted(securityThreatsDeleted);
            
            // Clean up other data types
            int otherDataDeleted = cleanupOtherData();
            report.setOtherDataDeleted(otherDataDeleted);
            
            report.setEndTime(LocalDateTime.now());
            report.setSuccess(true);
            
            log.info("Data retention cleanup completed successfully. Deleted: {} audit events, {} security threats, {} other records", 
                    auditEventsDeleted, securityThreatsDeleted, otherDataDeleted);
            
        } catch (Exception e) {
            report.setEndTime(LocalDateTime.now());
            report.setSuccess(false);
            report.setErrorMessage(e.getMessage());
            
            log.error("Data retention cleanup failed", e);
        }
    }

    /**
     * Cleans up expired audit events
     */
    @Transactional
    public int cleanupAuditEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(RETENTION_POLICIES.get("AUDIT_EVENTS"));
        
        List<AuditEvent> expiredEvents = auditEventRepository.findByRetentionUntilBefore(cutoffDate);
        
        if (!expiredEvents.isEmpty()) {
            // Securely delete sensitive data before removing records
            for (AuditEvent event : expiredEvents) {
                securelyDeleteAuditEventData(event);
            }
            
            auditEventRepository.deleteAll(expiredEvents);
            log.info("Deleted {} expired audit events", expiredEvents.size());
            return expiredEvents.size();
        }
        
        return 0;
    }

    /**
     * Cleans up expired security threats
     */
    @Transactional
    public int cleanupSecurityThreats() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(RETENTION_POLICIES.get("SECURITY_THREATS"));
        
        // Find resolved threats older than retention period
        List<SecurityThreat> expiredThreats = securityThreatRepository.findAll().stream()
                .filter(threat -> threat.getDetectedAt().isBefore(cutoffDate))
                .filter(threat -> threat.getStatus() == SecurityThreat.ThreatStatus.RESOLVED || 
                                threat.getStatus() == SecurityThreat.ThreatStatus.FALSE_POSITIVE)
                .toList();
        
        if (!expiredThreats.isEmpty()) {
            // Securely delete sensitive data before removing records
            for (SecurityThreat threat : expiredThreats) {
                securelyDeleteThreatData(threat);
            }
            
            securityThreatRepository.deleteAll(expiredThreats);
            log.info("Deleted {} expired security threats", expiredThreats.size());
            return expiredThreats.size();
        }
        
        return 0;
    }

    /**
     * Cleans up other types of expired data
     */
    @Transactional
    public int cleanupOtherData() {
        int deletedCount = 0;
        
        // Add cleanup for other data types as needed
        // For example: conversation history, temporary files, cached data, etc.
        
        return deletedCount;
    }

    /**
     * Validates data retention compliance
     */
    public DataRetentionComplianceReport validateRetentionCompliance() {
        DataRetentionComplianceReport report = new DataRetentionComplianceReport();
        report.setValidationTime(LocalDateTime.now());
        
        // Check audit events compliance
        long expiredAuditEvents = countExpiredAuditEvents();
        report.setExpiredAuditEventsCount(expiredAuditEvents);
        
        // Check security threats compliance
        long expiredSecurityThreats = countExpiredSecurityThreats();
        report.setExpiredSecurityThreatsCount(expiredSecurityThreats);
        
        // Overall compliance status
        boolean compliant = expiredAuditEvents == 0 && expiredSecurityThreats == 0;
        report.setCompliant(compliant);
        
        if (!compliant) {
            report.addRecommendation("Run data retention cleanup to remove expired records");
        }
        
        return report;
    }

    /**
     * Gets retention policy for data type
     */
    public int getRetentionPeriodYears(String dataType) {
        return RETENTION_POLICIES.getOrDefault(dataType.toUpperCase(), 7); // Default 7 years
    }

    /**
     * Checks if data should be retained based on legal hold
     */
    public boolean isUnderLegalHold(String dataId, String dataType) {
        // In a real implementation, this would check against a legal hold database
        // For now, return false (no legal holds)
        return false;
    }

    /**
     * Applies legal hold to data
     */
    public void applyLegalHold(String dataId, String dataType, String reason, LocalDateTime holdUntil) {
        // In a real implementation, this would add the data to a legal hold registry
        log.info("Legal hold applied to {} (type: {}) until {}: {}", dataId, dataType, holdUntil, reason);
    }

    /**
     * Removes legal hold from data
     */
    public void removeLegalHold(String dataId, String dataType, String reason) {
        // In a real implementation, this would remove the data from legal hold registry
        log.info("Legal hold removed from {} (type: {}): {}", dataId, dataType, reason);
    }

    /**
     * Securely deletes sensitive data from audit event
     */
    private void securelyDeleteAuditEventData(AuditEvent event) {
        // Create array of sensitive fields to securely delete
        String[] sensitiveData = {
            event.getAdditionalData(),
            event.getDataSourcesAccessed(),
            event.getUserAgent()
        };
        
        // Overwrite sensitive data
        dataProtectionService.secureDelete(sensitiveData);
        
        // Clear sensitive fields
        event.setAdditionalData(null);
        event.setDataSourcesAccessed(null);
        event.setUserAgent("[DELETED]");
    }

    /**
     * Securely deletes sensitive data from security threat
     */
    private void securelyDeleteThreatData(SecurityThreat threat) {
        // Create array of sensitive fields to securely delete
        String[] sensitiveData = {
            threat.getEvidence(),
            threat.getMitigationActions(),
            threat.getUserAgent()
        };
        
        // Overwrite sensitive data
        dataProtectionService.secureDelete(sensitiveData);
        
        // Clear sensitive fields
        threat.setEvidence(null);
        threat.setMitigationActions(null);
        threat.setUserAgent("[DELETED]");
    }

    /**
     * Counts expired audit events
     */
    private long countExpiredAuditEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(RETENTION_POLICIES.get("AUDIT_EVENTS"));
        return auditEventRepository.findByRetentionUntilBefore(cutoffDate).size();
    }

    /**
     * Counts expired security threats
     */
    private long countExpiredSecurityThreats() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(RETENTION_POLICIES.get("SECURITY_THREATS"));
        
        return securityThreatRepository.findAll().stream()
                .filter(threat -> threat.getDetectedAt().isBefore(cutoffDate))
                .filter(threat -> threat.getStatus() == SecurityThreat.ThreatStatus.RESOLVED || 
                                threat.getStatus() == SecurityThreat.ThreatStatus.FALSE_POSITIVE)
                .count();
    }

    // Report classes
    
    public static class DataRetentionReport {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean success;
        private String errorMessage;
        private int auditEventsDeleted;
        private int securityThreatsDeleted;
        private int otherDataDeleted;

        // Getters and setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getAuditEventsDeleted() { return auditEventsDeleted; }
        public void setAuditEventsDeleted(int auditEventsDeleted) { this.auditEventsDeleted = auditEventsDeleted; }
        public int getSecurityThreatsDeleted() { return securityThreatsDeleted; }
        public void setSecurityThreatsDeleted(int securityThreatsDeleted) { this.securityThreatsDeleted = securityThreatsDeleted; }
        public int getOtherDataDeleted() { return otherDataDeleted; }
        public void setOtherDataDeleted(int otherDataDeleted) { this.otherDataDeleted = otherDataDeleted; }
        
        public int getTotalDeleted() {
            return auditEventsDeleted + securityThreatsDeleted + otherDataDeleted;
        }
    }

    public static class DataRetentionComplianceReport {
        private LocalDateTime validationTime;
        private boolean compliant;
        private long expiredAuditEventsCount;
        private long expiredSecurityThreatsCount;
        private List<String> recommendations = new java.util.ArrayList<>();

        // Getters and setters
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        public boolean isCompliant() { return compliant; }
        public void setCompliant(boolean compliant) { this.compliant = compliant; }
        public long getExpiredAuditEventsCount() { return expiredAuditEventsCount; }
        public void setExpiredAuditEventsCount(long expiredAuditEventsCount) { this.expiredAuditEventsCount = expiredAuditEventsCount; }
        public long getExpiredSecurityThreatsCount() { return expiredSecurityThreatsCount; }
        public void setExpiredSecurityThreatsCount(long expiredSecurityThreatsCount) { this.expiredSecurityThreatsCount = expiredSecurityThreatsCount; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public void addRecommendation(String recommendation) {
            this.recommendations.add(recommendation);
        }
        
        public long getTotalExpiredRecords() {
            return expiredAuditEventsCount + expiredSecurityThreatsCount;
        }
    }
}