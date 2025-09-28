package com.tariffsheriff.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response DTO
 * Requirement 12.4: Proper error response structures with detailed validation messages
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String error;
    private String message;
    private int status;
    private String path;
    private LocalDateTime timestamp;
    
    @JsonProperty("validation_errors")
    private Map<String, String> validationErrors;
    
    // Default constructor
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for basic errors
    public ErrorResponse(String error, String message, int status, String path) {
        this();
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
    }
    
    // Constructor with validation errors
    public ErrorResponse(String error, String message, int status, String path, Map<String, String> validationErrors) {
        this(error, message, status, path);
        this.validationErrors = validationErrors;
    }
    
    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ErrorResponse errorResponse = new ErrorResponse();
        
        public Builder error(String error) {
            errorResponse.error = error;
            return this;
        }
        
        public Builder message(String message) {
            errorResponse.message = message;
            return this;
        }
        
        public Builder status(int status) {
            errorResponse.status = status;
            return this;
        }
        
        public Builder path(String path) {
            errorResponse.path = path;
            return this;
        }
        
        public Builder validationErrors(Map<String, String> validationErrors) {
            errorResponse.validationErrors = validationErrors;
            return this;
        }
        
        public ErrorResponse build() {
            return errorResponse;
        }
    }
    
    // Getters and setters
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}