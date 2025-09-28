package com.tariffsheriff.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO for user information in responses
 * Requirement 2.3: User data transfer without sensitive information
 */
@Schema(description = "User information response")
public class UserDto {
    
    @Schema(description = "User ID", example = "1")
    private Long id;
    
    @Schema(description = "User's full name", example = "John Doe")
    private String name;
    
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;
    
    @Schema(description = "User's bio/description", example = "Software developer with 5 years of experience")
    @JsonProperty("about_me")
    private String aboutMe;
    
    @Schema(description = "User role", example = "USER")
    private UserRole role;
    
    @Schema(description = "User account status", example = "ACTIVE")
    private UserStatus status;
    
    @Schema(description = "Whether email is verified", example = "true")
    @JsonProperty("email_verified")
    private boolean emailVerified;
    
    @Schema(description = "Last login timestamp", example = "2024-01-15T10:30:00")
    @JsonProperty("last_login")
    private LocalDateTime lastLogin;
    
    @Schema(description = "Account creation timestamp", example = "2024-01-01T09:00:00")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public UserDto() {}
    
    // Constructor
    public UserDto(Long id, String name, String email, String aboutMe, UserRole role, 
                   UserStatus status, boolean emailVerified, LocalDateTime lastLogin, 
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.aboutMe = aboutMe;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public String getAboutMe() {
        return aboutMe;
    }
    
    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}