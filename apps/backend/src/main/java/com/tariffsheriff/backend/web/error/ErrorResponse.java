package com.tariffsheriff.backend.web.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Error response returned when an API request fails")
public class ErrorResponse {
    
    @Schema(
        description = "HTTP status code",
        example = "400"
    )
    private int status;
    
    @Schema(
        description = "HTTP status reason phrase",
        example = "Bad Request"
    )
    private String error;
    
    @Schema(
        description = "User-friendly error message explaining what went wrong",
        example = "Your query appears to be invalid or incomplete. Please provide a clear question about tariff news."
    )
    private String message;
    
    @Schema(
        description = "API endpoint path where the error occurred",
        example = "/api/news/query"
    )
    private String path;
    
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2024-01-15 10:30:45"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse() {}

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Getters and setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

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
}