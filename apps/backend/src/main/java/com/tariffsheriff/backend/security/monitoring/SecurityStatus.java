package com.tariffsheriff.backend.security.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data class representing the security status of a user account.
 * Used for monitoring and administrative purposes.
 */
@Data
@Builder
public class SecurityStatus {
    private Long userId;
    private String email;
    
    // Failed login tracking
    private int failedLoginAttempts;
    private boolean accountLocked;
    private LocalDateTime accountLockedUntil;
    
    // Redis-based temporary locks
    private boolean redisLocked;
    private long redisLockRemainingTime; // seconds
    
    // Login history
    private LocalDateTime lastLogin;
    private String lastLoginIp;
    
    // Overall status
    private boolean canLogin;
    
    /**
     * Check if account is currently locked (either in database or Redis).
     */
    public boolean isCurrentlyLocked() {
        return accountLocked || redisLocked;
    }
    
    /**
     * Get the most restrictive lock remaining time.
     */
    public long getEffectiveLockRemainingTime() {
        if (redisLocked && redisLockRemainingTime > 0) {
            return redisLockRemainingTime;
        }
        
        if (accountLocked && accountLockedUntil != null) {
            LocalDateTime now = LocalDateTime.now();
            if (accountLockedUntil.isAfter(now)) {
                return java.time.Duration.between(now, accountLockedUntil).getSeconds();
            }
        }
        
        return 0;
    }
    
    /**
     * Check if user is approaching account lockout threshold.
     */
    public boolean isApproachingLockout() {
        return failedLoginAttempts >= 3 && !isCurrentlyLocked();
    }
}