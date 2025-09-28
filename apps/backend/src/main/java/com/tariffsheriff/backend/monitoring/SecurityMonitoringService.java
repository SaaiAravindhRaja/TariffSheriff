package com.tariffsheriff.backend.monitoring;

import com.tariffsheriff.backend.user.service.AuditService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for monitoring security events and generating alerts
 */
@Service
public class SecurityMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityMonitoringService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_EVENTS");

    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter loginAttemptsCounter;
    private final Counter failedLoginAttemptsCounter;
    private final Counter accountLockoutCounter;
    private final Counter passwordResetCounter;
    private final Counter suspiciousActivityCounter;
    private final Timer authenticationTimer;

    // In-memory tracking for alerting
    private final Map<String, AtomicInteger> failedLoginsByIp = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailedLoginByIp = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failedLoginsByUser = new ConcurrentHashMap<>();

    public SecurityMonitoringService(AuditService auditService, MeterRegistry meterRegistry) {
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.loginAttemptsCounter = Counter.builder("auth.login.attempts")
            .description("Total number of login attempts")
            .register(meterRegistry);

        this.failedLoginAttemptsCounter = Counter.builder("auth.login.failed")
            .description("Number of failed login attempts")
            .register(meterRegistry);

        this.accountLockoutCounter = Counter.builder("auth.account.lockout")
            .description("Number of account lockouts")
            .register(meterRegistry);

        this.passwordResetCounter = Counter.builder("auth.password.reset")
            .description("Number of password reset requests")
            .register(meterRegistry);

        this.suspiciousActivityCounter = Counter.builder("security.suspicious.activity")
            .description("Number of suspicious security events")
            .register(meterRegistry);

        this.authenticationTimer = Timer.builder("auth.authentication.duration")
            .description("Time taken for authentication operations")
            .register(meterRegistry);
    }

    /**
     * Record a successful login event
     */
    public void recordSuccessfulLogin(String email, String ipAddress, String userAgent) {
        loginAttemptsCounter.increment();
        
        // Clear failed login tracking for this IP and user
        failedLoginsByIp.remove(ipAddress);
        lastFailedLoginByIp.remove(ipAddress);
        failedLoginsByUser.remove(email);

        securityLogger.info("Successful login - User: {}, IP: {}, UserAgent: {}", 
            email, ipAddress, userAgent);

        auditService.logSecurityEvent(email, "LOGIN_SUCCESS", ipAddress, userAgent, null);
    }

    /**
     * Record a failed login event and check for suspicious activity
     */
    public void recordFailedLogin(String email, String ipAddress, String userAgent, String reason) {
        loginAttemptsCounter.increment();
        failedLoginAttemptsCounter.increment();

        // Track failed logins by IP
        AtomicInteger ipFailures = failedLoginsByIp.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        int ipFailureCount = ipFailures.incrementAndGet();
        lastFailedLoginByIp.put(ipAddress, LocalDateTime.now());

        // Track failed logins by user
        AtomicInteger userFailures = failedLoginsByUser.computeIfAbsent(email, k -> new AtomicInteger(0));
        int userFailureCount = userFailures.incrementAndGet();

        securityLogger.warn("Failed login attempt - User: {}, IP: {}, Reason: {}, UserAgent: {}", 
            email, ipAddress, reason, userAgent);

        auditService.logSecurityEvent(email, "LOGIN_FAILED", ipAddress, userAgent, 
            Map.of("reason", reason, "ipFailureCount", String.valueOf(ipFailureCount)));

        // Check for suspicious activity
        checkForSuspiciousActivity(email, ipAddress, ipFailureCount, userFailureCount);
    }

    /**
     * Record an account lockout event
     */
    public void recordAccountLockout(String email, String ipAddress, String reason) {
        accountLockoutCounter.increment();
        
        securityLogger.error("Account locked - User: {}, IP: {}, Reason: {}", 
            email, ipAddress, reason);

        auditService.logSecurityEvent(email, "ACCOUNT_LOCKED", ipAddress, null, 
            Map.of("reason", reason));

        // This is always suspicious
        recordSuspiciousActivity("ACCOUNT_LOCKOUT", email, ipAddress, 
            "Account locked due to: " + reason);
    }

    /**
     * Record a password reset request
     */
    public void recordPasswordReset(String email, String ipAddress, String userAgent) {
        passwordResetCounter.increment();
        
        securityLogger.info("Password reset requested - User: {}, IP: {}", email, ipAddress);

        auditService.logSecurityEvent(email, "PASSWORD_RESET_REQUESTED", ipAddress, userAgent, null);
    }

    /**
     * Record a password change event
     */
    public void recordPasswordChange(String email, String ipAddress, String userAgent) {
        securityLogger.info("Password changed - User: {}, IP: {}", email, ipAddress);

        auditService.logSecurityEvent(email, "PASSWORD_CHANGED", ipAddress, userAgent, null);
    }

    /**
     * Record token-related security events
     */
    public void recordTokenEvent(String email, String event, String ipAddress, String tokenId) {
        securityLogger.info("Token event - User: {}, Event: {}, IP: {}, TokenId: {}", 
            email, event, ipAddress, tokenId);

        auditService.logSecurityEvent(email, event, ipAddress, null, 
            Map.of("tokenId", tokenId));
    }

    /**
     * Time an authentication operation
     */
    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop the authentication timer
     */
    public void stopAuthenticationTimer(Timer.Sample sample) {
        sample.stop(authenticationTimer);
    }

    /**
     * Check for suspicious activity patterns
     */
    private void checkForSuspiciousActivity(String email, String ipAddress, int ipFailureCount, int userFailureCount) {
        // Multiple failed logins from same IP
        if (ipFailureCount >= 10) {
            recordSuspiciousActivity("BRUTE_FORCE_IP", email, ipAddress, 
                "Multiple failed logins from IP: " + ipFailureCount);
        }

        // Multiple failed logins for same user
        if (userFailureCount >= 5) {
            recordSuspiciousActivity("BRUTE_FORCE_USER", email, ipAddress, 
                "Multiple failed logins for user: " + userFailureCount);
        }

        // Rapid failed login attempts (more than 5 in 1 minute)
        LocalDateTime lastFailure = lastFailedLoginByIp.get(ipAddress);
        if (lastFailure != null && Duration.between(lastFailure, LocalDateTime.now()).toMinutes() < 1 && ipFailureCount >= 5) {
            recordSuspiciousActivity("RAPID_FAILED_LOGINS", email, ipAddress, 
                "Rapid failed login attempts: " + ipFailureCount + " in < 1 minute");
        }
    }

    /**
     * Record suspicious activity and generate alerts
     */
    private void recordSuspiciousActivity(String activityType, String email, String ipAddress, String details) {
        suspiciousActivityCounter.increment();
        
        securityLogger.error("SUSPICIOUS ACTIVITY DETECTED - Type: {}, User: {}, IP: {}, Details: {}", 
            activityType, email, ipAddress, details);

        auditService.logSecurityEvent(email, "SUSPICIOUS_ACTIVITY", ipAddress, null, 
            Map.of("activityType", activityType, "details", details));

        // In a real implementation, this would trigger alerts via:
        // - Email notifications
        // - Slack/Teams webhooks
        // - PagerDuty/OpsGenie alerts
        // - SIEM system integration
        generateAlert(activityType, email, ipAddress, details);
    }

    /**
     * Generate security alerts (placeholder for actual alerting implementation)
     */
    private void generateAlert(String activityType, String email, String ipAddress, String details) {
        // Log the alert for now - in production this would integrate with alerting systems
        logger.error("SECURITY ALERT: {} - User: {}, IP: {}, Details: {}", 
            activityType, email, ipAddress, details);

        // TODO: Implement actual alerting mechanisms:
        // - Send email to security team
        // - Post to Slack security channel
        // - Create incident in PagerDuty
        // - Update security dashboard
    }

    /**
     * Get current security metrics
     */
    public SecurityMetrics getSecurityMetrics() {
        return SecurityMetrics.builder()
            .totalLoginAttempts((long) loginAttemptsCounter.count())
            .failedLoginAttempts((long) failedLoginAttemptsCounter.count())
            .accountLockouts((long) accountLockoutCounter.count())
            .passwordResets((long) passwordResetCounter.count())
            .suspiciousActivities((long) suspiciousActivityCounter.count())
            .averageAuthenticationTime(authenticationTimer.mean())
            .activeFailedIpAddresses(failedLoginsByIp.size())
            .build();
    }

    /**
     * Clean up old tracking data (should be called periodically)
     */
    public void cleanupOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        lastFailedLoginByIp.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        // Remove IP addresses that haven't had failures in the last 24 hours
        failedLoginsByIp.entrySet().removeIf(entry -> {
            String ip = entry.getKey();
            LocalDateTime lastFailure = lastFailedLoginByIp.get(ip);
            return lastFailure == null || lastFailure.isBefore(cutoff);
        });
        
        logger.debug("Cleaned up old security monitoring data");
    }

    /**
     * Security metrics data class
     */
    public static class SecurityMetrics {
        private final long totalLoginAttempts;
        private final long failedLoginAttempts;
        private final long accountLockouts;
        private final long passwordResets;
        private final long suspiciousActivities;
        private final double averageAuthenticationTime;
        private final int activeFailedIpAddresses;

        private SecurityMetrics(Builder builder) {
            this.totalLoginAttempts = builder.totalLoginAttempts;
            this.failedLoginAttempts = builder.failedLoginAttempts;
            this.accountLockouts = builder.accountLockouts;
            this.passwordResets = builder.passwordResets;
            this.suspiciousActivities = builder.suspiciousActivities;
            this.averageAuthenticationTime = builder.averageAuthenticationTime;
            this.activeFailedIpAddresses = builder.activeFailedIpAddresses;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public long getTotalLoginAttempts() { return totalLoginAttempts; }
        public long getFailedLoginAttempts() { return failedLoginAttempts; }
        public long getAccountLockouts() { return accountLockouts; }
        public long getPasswordResets() { return passwordResets; }
        public long getSuspiciousActivities() { return suspiciousActivities; }
        public double getAverageAuthenticationTime() { return averageAuthenticationTime; }
        public int getActiveFailedIpAddresses() { return activeFailedIpAddresses; }

        public static class Builder {
            private long totalLoginAttempts;
            private long failedLoginAttempts;
            private long accountLockouts;
            private long passwordResets;
            private long suspiciousActivities;
            private double averageAuthenticationTime;
            private int activeFailedIpAddresses;

            public Builder totalLoginAttempts(long totalLoginAttempts) {
                this.totalLoginAttempts = totalLoginAttempts;
                return this;
            }

            public Builder failedLoginAttempts(long failedLoginAttempts) {
                this.failedLoginAttempts = failedLoginAttempts;
                return this;
            }

            public Builder accountLockouts(long accountLockouts) {
                this.accountLockouts = accountLockouts;
                return this;
            }

            public Builder passwordResets(long passwordResets) {
                this.passwordResets = passwordResets;
                return this;
            }

            public Builder suspiciousActivities(long suspiciousActivities) {
                this.suspiciousActivities = suspiciousActivities;
                return this;
            }

            public Builder averageAuthenticationTime(double averageAuthenticationTime) {
                this.averageAuthenticationTime = averageAuthenticationTime;
                return this;
            }

            public Builder activeFailedIpAddresses(int activeFailedIpAddresses) {
                this.activeFailedIpAddresses = activeFailedIpAddresses;
                return this;
            }

            public SecurityMetrics build() {
                return new SecurityMetrics(this);
            }
        }
    }
}