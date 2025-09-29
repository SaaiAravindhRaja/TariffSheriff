package com.tariffsheriff.backend.user.model;

/**
 * Enumeration of audit actions that can be logged in the system.
 * Covers authentication events, user management, and security-related activities.
 */
public enum AuditAction {
    // User Registration and Verification
    USER_REGISTERED("User account registered", true, false),
    USER_EMAIL_VERIFIED("User email verified", false, false),
    
    // Authentication Events
    USER_LOGIN_SUCCESS("User login successful", true, true),
    USER_LOGIN_FAILED("User login failed", true, true),
    USER_LOGOUT("User logout", true, true),
    
    // Password Management
    USER_PASSWORD_CHANGED("User password changed", true, false),
    USER_PASSWORD_RESET_REQUESTED("User password reset requested", true, false),
    USER_PASSWORD_RESET_COMPLETED("User password reset completed", true, false),
    
    // Account Security
    USER_ACCOUNT_LOCKED("User account locked", true, true),
    USER_ACCOUNT_UNLOCKED("User account unlocked", true, true),
    SUSPICIOUS_ACTIVITY_DETECTED("Suspicious activity detected", true, true),
    
    // User Management
    USER_ROLE_CHANGED("User role changed", true, false),
    USER_STATUS_CHANGED("User status changed", true, false),
    
    // Token Management
    TOKEN_REFRESHED("Authentication token refreshed", true, true),
    TOKEN_BLACKLISTED("Authentication token blacklisted", true, true),
    
    // Administrative Actions
    ADMIN_ACTION_PERFORMED("Administrative action performed", true, false),
    
    // System Events
    SYSTEM_EVENT("System event occurred", true, false);

    private final String description;
    private final boolean securityEvent;
    private final boolean authenticationEvent;

    AuditAction(String description, boolean securityEvent, boolean authenticationEvent) {
        this.description = description;
        this.securityEvent = securityEvent;
        this.authenticationEvent = authenticationEvent;
    }

    /**
     * Get the human-readable description of this action
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this action is considered a security event
     * @return true if this is a security-related event
     */
    public boolean isSecurityEvent() {
        return securityEvent;
    }

    /**
     * Check if this action is an authentication event
     * @return true if this is an authentication-related event
     */
    public boolean isAuthenticationEvent() {
        return authenticationEvent;
    }

    /**
     * Check if this action indicates a failed operation
     * @return true if this represents a failure
     */
    public boolean isFailureEvent() {
        return this == USER_LOGIN_FAILED || this == SUSPICIOUS_ACTIVITY_DETECTED;
    }

    /**
     * Check if this action indicates a successful operation
     * @return true if this represents a success
     */
    public boolean isSuccessEvent() {
        return this == USER_LOGIN_SUCCESS || this == USER_EMAIL_VERIFIED || 
               this == USER_PASSWORD_RESET_COMPLETED;
    }

    /**
     * Get actions that are considered high-priority security events
     * @return array of high-priority security actions
     */
    public static AuditAction[] getHighPrioritySecurityEvents() {
        return new AuditAction[] {
            USER_LOGIN_FAILED,
            USER_ACCOUNT_LOCKED,
            SUSPICIOUS_ACTIVITY_DETECTED,
            USER_PASSWORD_RESET_REQUESTED,
            ADMIN_ACTION_PERFORMED
        };
    }

    /**
     * Get actions related to authentication
     * @return array of authentication-related actions
     */
    public static AuditAction[] getAuthenticationEvents() {
        return new AuditAction[] {
            USER_LOGIN_SUCCESS,
            USER_LOGIN_FAILED,
            USER_LOGOUT,
            TOKEN_REFRESHED,
            TOKEN_BLACKLISTED
        };
    }
}