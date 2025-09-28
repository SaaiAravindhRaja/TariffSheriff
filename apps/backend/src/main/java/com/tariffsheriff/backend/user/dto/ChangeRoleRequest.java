package com.tariffsheriff.backend.user.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for user role change requests (Admin only)
 * Requirement 5.2: Role-based access control management
 */
public class ChangeRoleRequest {
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(USER|ANALYST|ADMIN)$", message = "Role must be USER, ANALYST, or ADMIN")
    private String role;
    
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
    
    // Default constructor
    public ChangeRoleRequest() {}
    
    // Constructor
    public ChangeRoleRequest(String role, String reason) {
        this.role = role;
        this.reason = reason;
    }
    
    // Getters and setters
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}