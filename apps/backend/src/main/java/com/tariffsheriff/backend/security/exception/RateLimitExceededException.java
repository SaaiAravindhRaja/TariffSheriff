package com.tariffsheriff.backend.security.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limit is exceeded.
 */
@Getter
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitExceededException(String message, long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}