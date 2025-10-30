package com.tariffsheriff.backend.web.error;

import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TariffRateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTariffRateNotFound(TariffRateNotFoundException ex, HttpServletRequest req) {
        ErrorResponse body = build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Chatbot/AI-specific exception handlers removed for simplicity

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        log.error("Validation error on {}: {}", req.getRequestURI(), ex.getMessage());
        StringBuilder message = new StringBuilder("I had trouble with your request. Please check:\n");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            message.append("• ").append(error.getField()).append(": ").append(error.getDefaultMessage()).append("\n"));
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, message.toString(), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        log.error("Bad request on {}: {}", req.getRequestURI(), ex.getMessage());
        String userFriendlyMessage = makeUserFriendly(extractMessage(ex));
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, userFriendlyMessage, req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Convert generic error messages to user-friendly ones
     */
    private String makeUserFriendly(String technicalMessage) {
        if (technicalMessage == null) {
            return "I encountered an issue processing your request. Please try again.";
        }
        
        String lower = technicalMessage.toLowerCase();
        
        if (lower.contains("json") || lower.contains("parse") || lower.contains("deserialize")) {
            return "I had trouble understanding the format of your request. Please check your input and try again.";
        }
        
        if (lower.contains("required") || lower.contains("missing")) {
            return "Some required information is missing from your request. Please provide all necessary details.";
        }
        
        if (lower.contains("invalid") || lower.contains("illegal")) {
            return "The information provided doesn't match what I expected. Please check your input and try again.";
        }
        
        return "I had trouble processing your request. Please check your input and try again.";
    }

    private static ErrorResponse build(HttpStatus status, String message, String path) {
        ErrorResponse body = new ErrorResponse();
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setMessage(message);
        body.setPath(path);
        return body;
    }

    private static String extractMessage(Exception ex) {
        if (ex instanceof HttpMessageNotReadableException readable && readable.getMostSpecificCause() != null) {
            return readable.getMostSpecificCause().getMessage();
        }
        return ex.getMessage();
    }
}
