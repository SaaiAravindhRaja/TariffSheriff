package com.tariffsheriff.backend.news.exception;

/**
 * Exception thrown when news processing fails
 * This includes failures in article fetching, embedding generation, or answer synthesis
 */
public class NewsProcessingException extends RuntimeException {
    
    public NewsProcessingException(String message) {
        super(message);
    }
    
    public NewsProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
