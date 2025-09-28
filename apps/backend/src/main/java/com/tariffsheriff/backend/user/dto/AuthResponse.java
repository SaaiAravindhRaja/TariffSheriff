package com.tariffsheriff.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for authentication responses
 * Requirement 2.3: Authentication response with token information
 */
@Schema(description = "Authentication response with JWT tokens and user information")
public class AuthResponse {
    
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("access_token")
    private String accessToken;
    
    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @Schema(description = "Token type", example = "Bearer")
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    @Schema(description = "Token expiration time in seconds", example = "900")
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @Schema(description = "Authenticated user information")
    private UserDto user;
    
    // Default constructor
    public AuthResponse() {}
    
    // Constructor
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, UserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
    
    // Getters and setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
}