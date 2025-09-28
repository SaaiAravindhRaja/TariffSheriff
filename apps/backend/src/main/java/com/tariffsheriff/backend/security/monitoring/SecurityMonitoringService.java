package com.tariffsheriff.backend.security.monitoring;

import com.tariffsheriff.backend.security.ratelimit.RateLimitService;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.service.AuditService;
import com.tariffsheriff.backend.user.service.EmailService;
import com.tariffsheriff.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Security monitoring service for detecting and responding to suspicious activities.
 * Implements account lockout mechanisms and security alerting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitoringService {

    private final RateLimitService rateLimitService;
    private final UserService userService;
    private final AuditService auditService;
    private final EmailService emailService;

    // Security thresholds
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCKOUT_MINUTES = 30;
    private static final int PROGRESSIVE_LOCKOUT_MULTIPLIER = 2;

    /**
     * Handle failed login attempt and implement progressive account lockout.
     *
     * @param user User who failed to login
     * @param ipAddress IP address of the attempt
     * @param userAgent User agent string
     */
    public void handleFailedLoginAttempt(User user, String ipAddress, String userAgent) {
        // Increment failed login attempts
        user.incrementFailedLoginAttempts();
        
        // Log the failed attempt
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        details.put("userAgent", userAgent);
        details.put("failedAttempts", user.getFailedLoginAttempts());
        
        // TODO: Implement when AuditAction enum is updated with LOGIN_FAILED
        // auditService.logSecurityEvent(
        //     user.getId(),
        //     AuditAction.LOGIN_FAILED,
        //     "Failed login attempt",
        //     ipAddress,
        //     userAgent,
        //     details
        // );

        // Check if account should be locked
        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
            lockUserAccount(user, ipAddress, userAgent);
        } else {
            // TODO: Save the incremented failed attempts when UserService is updated
            // userService.updateUser(user);
            
            // Send warning email if approaching lockout
            if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS - 2) {
                sendSecurityWarningEmail(user, ipAddress);
            }
        }

        log.warn("Failed login attempt for user: {} from IP: {} (attempt {}/{})", 
                user.getEmail(), ipAddress, user.getFailedLoginAttempts(), MAX_FAILED_LOGIN_ATTEMPTS);
    }

    /**
     * Handle successful login and reset security counters.
     *
     * @param user User who successfully logged in
     * @param ipAddress IP address of the login
     * @param userAgent User agent string
     */
    public void handleSuccessfulLogin(User user, String ipAddress, String userAgent) {
        // TODO: Reset failed login attempts when UserService is updated
        // if (user.getFailedLoginAttempts() > 0) {
        //     user.resetFailedLoginAttempts();
        //     userService.updateUser(user);
        // }

        // TODO: Update last login information when UserService is updated
        // user.setLastLogin(LocalDateTime.now());
        // user.setLastLoginIp(ipAddress);
        // userService.updateUser(user);

        // Log successful login
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        details.put("userAgent", userAgent);
        
        // TODO: Implement when AuditAction enum is updated with LOGIN_SUCCESS
        // auditService.logSecurityEvent(
        //     user.getId(),
        //     AuditAction.LOGIN_SUCCESS,
        //     "Successful login",
        //     ipAddress,
        //     userAgent,
        //     details
        // );

        // Check for suspicious login patterns
        checkSuspiciousLoginPattern(user, ipAddress);

        log.info("Successful login for user: {} from IP: {}", user.getEmail(), ipAddress);
    }

    /**
     * Lock user account due to failed login attempts.
     *
     * @param user User to lock
     * @param ipAddress IP address of the attempts
     * @param userAgent User agent string
     */
    private void lockUserAccount(User user, String ipAddress, String userAgent) {
        // Calculate lockout duration (progressive lockout)
        int lockoutMinutes = ACCOUNT_LOCKOUT_MINUTES;
        if (user.getFailedLoginAttempts() > MAX_FAILED_LOGIN_ATTEMPTS) {
            // Progressive lockout: double the time for each additional failure
            int extraAttempts = user.getFailedLoginAttempts() - MAX_FAILED_LOGIN_ATTEMPTS;
            lockoutMinutes *= Math.pow(PROGRESSIVE_LOCKOUT_MULTIPLIER, extraAttempts);
            lockoutMinutes = Math.min(lockoutMinutes, 24 * 60); // Max 24 hours
        }

        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
        user.lockAccount(lockUntil);
        // TODO: Update user when UserService is updated
        // userService.updateUser(user);

        // Also lock in Redis for immediate effect
        rateLimitService.lockUserAccount(user.getId(), lockoutMinutes);

        // Log the account lockout
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        details.put("userAgent", userAgent);
        details.put("lockoutMinutes", lockoutMinutes);
        details.put("lockUntil", lockUntil.toString());
        details.put("failedAttempts", user.getFailedLoginAttempts());
        
        // TODO: Implement when AuditAction enum is updated with ACCOUNT_LOCKED
        // auditService.logSecurityEvent(
        //     user.getId(),
        //     AuditAction.ACCOUNT_LOCKED,
        //     "Account locked due to failed login attempts",
        //     ipAddress,
        //     userAgent,
        //     details
        // );

        // Send security alert email
        sendAccountLockedEmail(user, ipAddress, lockoutMinutes);

        log.warn("Account locked for user: {} from IP: {} for {} minutes", 
                user.getEmail(), ipAddress, lockoutMinutes);
    }

    /**
     * Check for suspicious login patterns (different IP, unusual time, etc.).
     *
     * @param user User who logged in
     * @param currentIpAddress Current IP address
     */
    private void checkSuspiciousLoginPattern(User user, String currentIpAddress) {
        String lastIpAddress = user.getLastLoginIp();
        
        // Check for IP address change
        if (lastIpAddress != null && !lastIpAddress.equals(currentIpAddress)) {
            // Different IP address - could be suspicious
            Map<String, Object> details = new HashMap<>();
            details.put("currentIp", currentIpAddress);
            details.put("previousIp", lastIpAddress);
            
            // TODO: Implement when AuditAction enum is updated with SUSPICIOUS_LOGIN
            // auditService.logSecurityEvent(
            //     user.getId(),
            //     AuditAction.SUSPICIOUS_LOGIN,
            //     "Login from different IP address",
            //     currentIpAddress,
            //     null,
            //     details
            // );

            // Send security notification
            sendIpChangeNotificationEmail(user, currentIpAddress, lastIpAddress);
            
            log.info("IP address change detected for user: {} (previous: {}, current: {})", 
                    user.getEmail(), lastIpAddress, currentIpAddress);
        }
    }

    /**
     * Unlock user account manually (admin function).
     *
     * @param user User to unlock
     * @param adminUserId ID of admin performing the unlock
     * @param reason Reason for unlocking
     */
    public void unlockUserAccount(User user, Long adminUserId, String reason) {
        user.unlockAccount();
        // TODO: Update user when UserService is updated
        // userService.updateUser(user);

        // Remove Redis lock as well
        rateLimitService.unlockUserAccount(user.getId());

        // Log the unlock action
        Map<String, Object> details = new HashMap<>();
        details.put("adminUserId", adminUserId);
        details.put("reason", reason);
        
        // TODO: Implement when AuditAction enum is updated with ACCOUNT_UNLOCKED
        // auditService.logSecurityEvent(
        //     user.getId(),
        //     AuditAction.ACCOUNT_UNLOCKED,
        //     "Account unlocked by administrator",
        //     null,
        //     null,
        //     details
        // );

        log.info("Account unlocked for user: {} by admin: {} (reason: {})", 
                user.getEmail(), adminUserId, reason);
    }

    /**
     * Check if user can login based on account status and lockout.
     *
     * @param user User to check
     * @return true if user can login, false otherwise
     */
    public boolean canUserLogin(User user) {
        // Check database lockout
        if (user.isAccountLocked()) {
            return false;
        }

        // Check Redis lockout (for immediate effect)
        if (rateLimitService.isUserAccountLocked(user.getId())) {
            return false;
        }

        // Check if user can login based on status
        return user.canLogin();
    }

    /**
     * Send security warning email when approaching account lockout.
     */
    private void sendSecurityWarningEmail(User user, String ipAddress) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", user.getName());
            templateData.put("ipAddress", ipAddress);
            templateData.put("remainingAttempts", MAX_FAILED_LOGIN_ATTEMPTS - user.getFailedLoginAttempts());
            
            // TODO: Update when EmailService signature is updated
            // emailService.sendSecurityAlert(user, "Multiple failed login attempts detected", templateData);
        } catch (Exception e) {
            log.error("Failed to send security warning email to user: {}", user.getEmail(), e);
        }
    }

    /**
     * Send account locked notification email.
     */
    private void sendAccountLockedEmail(User user, String ipAddress, int lockoutMinutes) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", user.getName());
            templateData.put("ipAddress", ipAddress);
            templateData.put("lockoutMinutes", lockoutMinutes);
            templateData.put("unlockTime", LocalDateTime.now().plusMinutes(lockoutMinutes));
            
            // TODO: Update when EmailService signature is updated
            // emailService.sendSecurityAlert(user, "Account temporarily locked", templateData);
        } catch (Exception e) {
            log.error("Failed to send account locked email to user: {}", user.getEmail(), e);
        }
    }

    /**
     * Send IP address change notification email.
     */
    private void sendIpChangeNotificationEmail(User user, String currentIp, String previousIp) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", user.getName());
            templateData.put("currentIp", currentIp);
            templateData.put("previousIp", previousIp);
            templateData.put("loginTime", LocalDateTime.now());
            
            // TODO: Update when EmailService signature is updated
            // emailService.sendSecurityAlert(user, "Login from new IP address", templateData);
        } catch (Exception e) {
            log.error("Failed to send IP change notification email to user: {}", user.getEmail(), e);
        }
    }

    /**
     * Get security status for a user (for admin monitoring).
     *
     * @param user User to check
     * @return SecurityStatus object with current security information
     */
    public SecurityStatus getUserSecurityStatus(User user) {
        return SecurityStatus.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .accountLocked(user.isAccountLocked())
                .accountLockedUntil(user.getAccountLockedUntil())
                .redisLocked(rateLimitService.isUserAccountLocked(user.getId()))
                .redisLockRemainingTime(rateLimitService.getUserLockRemainingTime(user.getId()))
                .lastLogin(user.getLastLogin())
                .lastLoginIp(user.getLastLoginIp())
                .canLogin(canUserLogin(user))
                .build();
    }
}