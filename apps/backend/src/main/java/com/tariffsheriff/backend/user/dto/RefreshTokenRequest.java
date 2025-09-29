package com.tariffsheriff.backend.user.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for token refresh requests
 * Requirement 2.3: Token refresh functionality
 */
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
    
    // Default constructor
    public RefreshTokenRequest() {}
    
    // Constructor
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    // Getters and setters
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}