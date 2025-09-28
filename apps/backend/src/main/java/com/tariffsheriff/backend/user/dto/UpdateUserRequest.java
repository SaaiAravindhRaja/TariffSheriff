package com.tariffsheriff.backend.user.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for user profile update requests
 * Requirement 5.3: User profile management with validation
 */
public class UpdateUserRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "About me must not exceed 500 characters")
    private String aboutMe;
    
    // Only admins can change email and role
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Pattern(regexp = "^(USER|ANALYST|ADMIN)$", message = "Role must be USER, ANALYST, or ADMIN")
    private String role;
    
    // Default constructor
    public UpdateUserRequest() {}
    
    // Constructor
    public UpdateUserRequest(String name, String aboutMe, String email, String role) {
        this.name = name;
        this.aboutMe = aboutMe;
        this.email = email;
        this.role = role;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAboutMe() {
        return aboutMe;
    }
    
    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}