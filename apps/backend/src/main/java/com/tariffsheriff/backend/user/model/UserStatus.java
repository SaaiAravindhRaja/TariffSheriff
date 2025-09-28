package com.tariffsheriff.backend.user.model;

/**
 * Enumeration of user account statuses
 */
public enum UserStatus {
    /**
     * User account is pending email verification
     */
    PENDING,
    
    /**
     * User account is active and can access the system
     */
    ACTIVE,
    
    /**
     * User account is suspended by an administrator
     */
    SUSPENDED,
    
    /**
     * User account is locked due to security reasons (e.g., too many failed login attempts)
     */
    LOCKED;
    
    /**
     * Check if the user can log in with this status
     * @return true if the user can log in
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }
    
    /**
     * Check if the account is in a locked state
     * @return true if the account is locked or suspended
     */
    public boolean isLocked() {
        return this == LOCKED || this == SUSPENDED;
    }
    
    /**
     * Check if the account needs email verification
     * @return true if the account is pending verification
     */
    public boolean needsVerification() {
        return this == PENDING;
    }
}