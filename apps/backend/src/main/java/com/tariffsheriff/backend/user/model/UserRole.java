package com.tariffsheriff.backend.user.model;

/**
 * Enumeration of user roles in the system.
 * Defines hierarchical permissions: ADMIN > ANALYST > USER
 */
public enum UserRole {
    /**
     * Regular user with basic access to tariff calculation features
     */
    USER,
    
    /**
     * Analyst with access to advanced analytics and reporting features
     */
    ANALYST,
    
    /**
     * Administrator with full system access and user management capabilities
     */
    ADMIN;
    
    /**
     * Check if this role has at least the specified minimum role level
     * @param minimumRole the minimum required role
     * @return true if this role meets or exceeds the minimum role
     */
    public boolean hasRole(UserRole minimumRole) {
        return this.ordinal() >= minimumRole.ordinal();
    }
    
    /**
     * Check if this role is an admin role
     * @return true if this is an ADMIN role
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Check if this role has analyst privileges or higher
     * @return true if this is ANALYST or ADMIN role
     */
    public boolean isAnalystOrHigher() {
        return this == ANALYST || this == ADMIN;
    }
}