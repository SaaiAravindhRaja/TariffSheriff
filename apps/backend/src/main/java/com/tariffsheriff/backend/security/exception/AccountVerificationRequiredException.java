package com.tariffsheriff.backend.security.exception;

/**
 * Exception thrown when account email verification is required.
 */
public class AccountVerificationRequiredException extends RuntimeException {
    
    public AccountVerificationRequiredException(String message) {
        super(message);
    }
    
    public AccountVerificationRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}