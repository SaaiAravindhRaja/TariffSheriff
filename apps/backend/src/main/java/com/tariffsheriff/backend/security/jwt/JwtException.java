package com.tariffsheriff.backend.security.jwt;

/**
 * Custom JWT exception for handling JWT-related errors.
 * Wraps various JWT parsing and validation exceptions into a single type.
 */
public class JwtException extends RuntimeException {

    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}