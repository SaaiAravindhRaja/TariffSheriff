package com.tariffsheriff.backend.news.exception;

/**
 * Exception thrown when a news query is invalid or malformed
 */
public class InvalidQueryException extends RuntimeException {
    
    public InvalidQueryException(String message) {
        super(message);
    }
    
    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
