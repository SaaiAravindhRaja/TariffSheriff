package com.tariffsheriff.backend.security.ratelimit;

import lombok.Builder;
import lombok.Data;

/**
 * Data class representing the current rate limit status for an IP address.
 * Used for monitoring and debugging rate limiting behavior.
 */
@Data
@Builder
public class RateLimitStatus {
    private String ipAddress;
    private boolean allowed;
    private int currentCount;
    private int limit;
    private int remainingAttempts;
    private long resetTimeSeconds;
    
    // Login rate limiting
    private int loginAttempts;
    private int loginAttemptsLimit;
    private long loginRemainingTime; // seconds
    
    // Registration rate limiting
    private int registrationAttempts;
    private int registrationLimit;
    private long registrationRemainingTime; // seconds
    
    // Password reset rate limiting
    private int passwordResetAttempts;
    private int passwordResetLimit;
    private long passwordResetRemainingTime; // seconds
    
    // Global IP rate limiting
    private int globalRequests;
    private int globalLimit;
    private long globalRemainingTime; // seconds
    
    /**
     * Check if any rate limit is currently active.
     */
    public boolean hasActiveRateLimit() {
        return loginAttempts >= loginAttemptsLimit ||
               registrationAttempts >= registrationLimit ||
               passwordResetAttempts >= passwordResetLimit ||
               globalRequests >= globalLimit;
    }
    
    /**
     * Get the most restrictive remaining time.
     */
    public long getMinRemainingTime() {
        long min = Long.MAX_VALUE;
        
        if (loginAttempts >= loginAttemptsLimit && loginRemainingTime > 0) {
            min = Math.min(min, loginRemainingTime);
        }
        if (registrationAttempts >= registrationLimit && registrationRemainingTime > 0) {
            min = Math.min(min, registrationRemainingTime);
        }
        if (passwordResetAttempts >= passwordResetLimit && passwordResetRemainingTime > 0) {
            min = Math.min(min, passwordResetRemainingTime);
        }
        if (globalRequests >= globalLimit && globalRemainingTime > 0) {
            min = Math.min(min, globalRemainingTime);
        }
        
        return min == Long.MAX_VALUE ? 0 : min;
    }
}