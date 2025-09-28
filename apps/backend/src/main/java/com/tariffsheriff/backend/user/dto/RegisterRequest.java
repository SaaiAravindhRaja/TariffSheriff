package com.tariffsheriff.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO for user registration requests
 * Requirement 1.4: User registration with comprehensive validation
 */
@Schema(description = "User registration request")
public class RegisterRequest {
    
    @Schema(description = "User's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Schema(description = "User's password (must contain uppercase, lowercase, digit, and special character)", 
            example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character"
    )
    private String password;
    
    @Schema(description = "Password confirmation (must match password)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    @Schema(description = "Optional user bio/description", example = "Software developer with 5 years of experience")
    @Size(max = 500, message = "About me must not exceed 500 characters")
    private String aboutMe;
    
    @Schema(description = "User role", example = "USER", allowableValues = {"USER", "ANALYST", "ADMIN"})
    @Pattern(regexp = "^(USER|ANALYST|ADMIN)$", message = "Role must be USER, ANALYST, or ADMIN")
    private String role = "USER";
    
    // Default constructor
    public RegisterRequest() {}
    
    // Constructor
    public RegisterRequest(String name, String email, String password, String confirmPassword, String aboutMe, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.aboutMe = aboutMe;
        this.role = role != null ? role : "USER";
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getAboutMe() {
        return aboutMe;
    }
    
    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * Validates that password and confirmPassword match
     */
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}