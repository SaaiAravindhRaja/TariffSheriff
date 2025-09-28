package com.tariffsheriff.backend.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for monitoring and maintenance
 */
@Component
public class MonitoringScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final SecurityMonitoringService securityMonitoringService;

    public MonitoringScheduler(SecurityMonitoringService securityMonitoringService) {
        this.securityMonitoringService = securityMonitoringService;
    }

    /**
     * Clean up old security monitoring data every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupSecurityData() {
        try {
            logger.debug("Starting security data cleanup");
            securityMonitoringService.cleanupOldData();
            logger.debug("Security data cleanup completed");
        } catch (Exception e) {
            logger.error("Error during security data cleanup", e);
        }
    }

    /**
     * Log security metrics every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void logSecurityMetrics() {
        try {
            SecurityMonitoringService.SecurityMetrics metrics = securityMonitoringService.getSecurityMetrics();
            
            logger.info("Security Metrics - Login Attempts: {}, Failed Logins: {}, Account Lockouts: {}, " +
                       "Password Resets: {}, Suspicious Activities: {}, Avg Auth Time: {}ms, Active Failed IPs: {}",
                metrics.getTotalLoginAttempts(),
                metrics.getFailedLoginAttempts(),
                metrics.getAccountLockouts(),
                metrics.getPasswordResets(),
                metrics.getSuspiciousActivities(),
                String.format("%.2f", metrics.getAverageAuthenticationTime()),
                metrics.getActiveFailedIpAddresses());
        } catch (Exception e) {
            logger.error("Error logging security metrics", e);
        }
    }
}