package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.AuditAction;
import com.tariffsheriff.backend.user.model.AuditLog;
import com.tariffsheriff.backend.user.repository.AuditLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log authentication events
     * Requirements: 9.1, 9.2
     */
    public void logAuthenticationEvent(Long userId, String action, String details, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(AuditAction.valueOf(action));
            auditLog.setResourceType("AUTHENTICATION");
            auditLog.setResourceId(userId != null ? userId.toString() : null);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            // Also log to application logs for immediate monitoring
            logger.info("AUDIT: {} - User: {}, IP: {}, Details: {}", action, userId, ipAddress, details);
            
        } catch (Exception e) {
            // Don't let audit logging failures break the main flow
            logger.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    /**
     * Log user management events
     * Requirements: 9.1, 9.4
     */
    public void logUserManagementEvent(Long adminUserId, Long targetUserId, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(adminUserId);
            auditLog.setAction(AuditAction.valueOf(action));
            auditLog.setResourceType("USER_MANAGEMENT");
            auditLog.setResourceId(targetUserId != null ? targetUserId.toString() : null);
            auditLog.setIpAddress(ipAddress);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            logger.info("AUDIT: {} - Admin: {}, Target: {}, IP: {}, Details: {}", 
                action, adminUserId, targetUserId, ipAddress, details);
            
        } catch (Exception e) {
            logger.error("Failed to save user management audit log: {}", e.getMessage());
        }
    }

    /**
     * Log security events
     * Requirements: 9.2, 9.5
     */
    public void logSecurityEvent(Long userId, String action, String details, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(AuditAction.valueOf(action));
            auditLog.setResourceType("SECURITY");
            auditLog.setResourceId(userId != null ? userId.toString() : null);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            // Security events get special attention in logs
            logger.warn("SECURITY AUDIT: {} - User: {}, IP: {}, Details: {}", action, userId, ipAddress, details);
            
        } catch (Exception e) {
            logger.error("Failed to save security audit log: {}", e.getMessage());
        }
    }

    /**
     * Log data access events
     * Requirements: 9.1, 9.4
     */
    public void logDataAccessEvent(Long userId, String action, String resourceType, String resourceId, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(AuditAction.valueOf(action));
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            logger.debug("AUDIT: {} - User: {}, Resource: {}/{}, IP: {}", 
                action, userId, resourceType, resourceId, ipAddress);
            
        } catch (Exception e) {
            logger.error("Failed to save data access audit log: {}", e.getMessage());
        }
    }

    /**
     * Log system events
     * Requirements: 9.1, 9.5
     */
    public void logSystemEvent(String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(null); // System events don't have a specific user
            auditLog.setAction(AuditAction.valueOf(action));
            auditLog.setResourceType("SYSTEM");
            auditLog.setResourceId(null);
            auditLog.setIpAddress(ipAddress);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            logger.info("SYSTEM AUDIT: {} - IP: {}, Details: {}", action, ipAddress, details);
            
        } catch (Exception e) {
            logger.error("Failed to save system audit log: {}", e.getMessage());
        }
    }

    /**
     * Get audit logs for a specific user
     * Requirements: 9.6, 9.7
     */
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get audit logs by action type
     * Requirements: 9.6, 9.7
     */
    public Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }

    /**
     * Get audit logs by IP address (for security analysis)
     * Requirements: 9.5, 9.6
     */
    public Page<AuditLog> getAuditLogsByIpAddress(String ipAddress, Pageable pageable) {
        return auditLogRepository.findByIpAddressOrderByCreatedAtDesc(ipAddress, pageable);
    }

    /**
     * Get audit logs within date range
     * Requirements: 9.6, 9.7
     */
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
    }

    /**
     * Get recent failed login attempts for security monitoring
     * Requirements: 9.2, 9.5
     */
    public List<AuditLog> getRecentFailedLogins(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditLogRepository.findByActionAndCreatedAtAfterOrderByCreatedAtDesc(
            AuditAction.LOGIN_FAILED, since);
    }

    /**
     * Get suspicious activity patterns
     * Requirements: 9.5
     */
    public List<AuditLog> getSuspiciousActivity(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditLogRepository.findSuspiciousActivity(since);
    }

    /**
     * Count failed login attempts for an IP address
     * Requirements: 9.2, 9.5
     */
    public long countFailedLoginAttempts(String ipAddress, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(
            AuditAction.LOGIN_FAILED, ipAddress, since);
    }

    /**
     * Count failed login attempts for a user
     * Requirements: 9.2, 9.5
     */
    public long countFailedLoginAttempts(Long userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditLogRepository.countByActionAndUserIdAndCreatedAtAfter(
            AuditAction.LOGIN_FAILED, userId, since);
    }

    /**
     * Get audit statistics for dashboard
     * Requirements: 9.6
     */
    public Map<String, Long> getAuditStatistics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        return Map.of(
            "totalEvents", auditLogRepository.countByCreatedAtAfter(since),
            "loginAttempts", auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.LOGIN_SUCCESS, since) +
                           auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.LOGIN_FAILED, since),
            "failedLogins", auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.LOGIN_FAILED, since),
            "registrations", auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.USER_REGISTERED, since),
            "securityEvents", auditLogRepository.countSecurityEvents(since)
        );
    }

    /**
     * Clean up old audit logs (for data retention)
     * Requirements: 9.6
     */
    @Transactional
    public int cleanupOldAuditLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        int deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        
        logger.info("Cleaned up {} audit log entries older than {} days", deletedCount, retentionDays);
        return deletedCount;
    }

    /**
     * Export audit logs for compliance
     * Requirements: 9.7
     */
    public List<AuditLog> exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate, Long userId) {
        if (userId != null) {
            return auditLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDate, endDate);
        } else {
            return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
        }
    }

    /**
     * Check for anomalous behavior patterns
     * Requirements: 9.5
     */
    public boolean detectAnomalousActivity(Long userId, String ipAddress) {
        try {
            // Check for multiple failed logins in short time
            long recentFailures = countFailedLoginAttempts(ipAddress, 1);
            if (recentFailures >= 5) {
                logSecurityEvent(userId, "ANOMALY_DETECTED", 
                    "Multiple failed logins from IP: " + ipAddress, ipAddress, null);
                return true;
            }

            // Check for logins from multiple IPs in short time
            if (userId != null) {
                LocalDateTime since = LocalDateTime.now().minusHours(1);
                long uniqueIPs = auditLogRepository.countUniqueIPsForUser(userId, since);
                if (uniqueIPs >= 3) {
                    logSecurityEvent(userId, "ANOMALY_DETECTED", 
                        "Logins from multiple IPs in short time", ipAddress, null);
                    return true;
                }
            }

            return false;
            
        } catch (Exception e) {
            logger.error("Error detecting anomalous activity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate security report
     * Requirements: 9.5, 9.6
     */
    public SecurityReport generateSecurityReport(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        SecurityReport report = new SecurityReport();
        report.setPeriodDays(days);
        report.setGeneratedAt(LocalDateTime.now());
        
        // Basic statistics
        report.setTotalEvents(auditLogRepository.countByCreatedAtAfter(since));
        report.setFailedLogins(auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.LOGIN_FAILED, since));
        report.setSuccessfulLogins(auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.LOGIN_SUCCESS, since));
        
        // Security events
        report.setSecurityEvents(auditLogRepository.countSecurityEvents(since));
        report.setAccountLockouts(auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.ACCOUNT_LOCKED, since));
        
        // Top IPs with failed logins
        report.setTopFailedLoginIPs(auditLogRepository.findTopFailedLoginIPs(since, 10));
        
        // Recent suspicious activities
        report.setSuspiciousActivities(getSuspiciousActivity(days * 24));
        
        return report;
    }

    /**
     * Security report data structure
     */
    public static class SecurityReport {
        private int periodDays;
        private LocalDateTime generatedAt;
        private long totalEvents;
        private long failedLogins;
        private long successfulLogins;
        private long securityEvents;
        private long accountLockouts;
        private List<Object[]> topFailedLoginIPs;
        private List<AuditLog> suspiciousActivities;

        // Getters and setters
        public int getPeriodDays() { return periodDays; }
        public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

        public long getFailedLogins() { return failedLogins; }
        public void setFailedLogins(long failedLogins) { this.failedLogins = failedLogins; }

        public long getSuccessfulLogins() { return successfulLogins; }
        public void setSuccessfulLogins(long successfulLogins) { this.successfulLogins = successfulLogins; }

        public long getSecurityEvents() { return securityEvents; }
        public void setSecurityEvents(long securityEvents) { this.securityEvents = securityEvents; }

        public long getAccountLockouts() { return accountLockouts; }
        public void setAccountLockouts(long accountLockouts) { this.accountLockouts = accountLockouts; }

        public List<Object[]> getTopFailedLoginIPs() { return topFailedLoginIPs; }
        public void setTopFailedLoginIPs(List<Object[]> topFailedLoginIPs) { this.topFailedLoginIPs = topFailedLoginIPs; }

        public List<AuditLog> getSuspiciousActivities() { return suspiciousActivities; }
        public void setSuspiciousActivities(List<AuditLog> suspiciousActivities) { this.suspiciousActivities = suspiciousActivities; }
    }
}