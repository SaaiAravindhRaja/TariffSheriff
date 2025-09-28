package com.tariffsheriff.backend.security.exception;

import com.tariffsheriff.backend.security.jwt.JwtException;
import com.tariffsheriff.backend.user.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Global exception handler for security-related exceptions.
 * Provides consistent error responses for authentication and authorization failures.
 */
@RestControllerAdvice
@Slf4j
public class SecurityExceptionHandler {

    /**
     * Handle JWT-related exceptions.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex, HttpServletRequest request) {
        log.debug("JWT exception: {} for request: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("JWT_ERROR")
                .message(ex.getMessage())
                .status(401)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle bad credentials (wrong username/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.debug("Bad credentials for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INVALID_CREDENTIALS")
                .message("Invalid email or password")
                .status(401)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle locked account exceptions.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(LockedException ex, HttpServletRequest request) {
        log.debug("Account locked for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCOUNT_LOCKED")
                .message("Account is temporarily locked due to multiple failed login attempts")
                .status(423) // HTTP 423 Locked
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    /**
     * Handle disabled account exceptions.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex, HttpServletRequest request) {
        log.debug("Account disabled for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCOUNT_DISABLED")
                .message("Account is disabled. Please contact support.")
                .status(403)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle expired account exceptions.
     */
    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ErrorResponse> handleAccountExpiredException(AccountExpiredException ex, HttpServletRequest request) {
        log.debug("Account expired for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCOUNT_EXPIRED")
                .message("Account has expired. Please contact support.")
                .status(403)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle general authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.debug("Authentication exception: {} for request: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("AUTHENTICATION_FAILED")
                .message("Authentication failed")
                .status(401)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.debug("Access denied for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCESS_DENIED")
                .message("Insufficient privileges to access this resource")
                .status(403)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle rate limit exceeded exceptions.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex, HttpServletRequest request) {
        log.debug("Rate limit exceeded for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("RATE_LIMIT_EXCEEDED")
                .message(ex.getMessage())
                .status(429)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    /**
     * Handle account verification required exceptions.
     */
    @ExceptionHandler(AccountVerificationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleAccountVerificationRequiredException(
            AccountVerificationRequiredException ex, HttpServletRequest request) {
        log.debug("Account verification required for request: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCOUNT_VERIFICATION_REQUIRED")
                .message("Email verification required. Please check your email and verify your account.")
                .status(403)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}