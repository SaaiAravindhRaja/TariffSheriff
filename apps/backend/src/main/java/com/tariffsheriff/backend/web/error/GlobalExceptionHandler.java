package com.tariffsheriff.backend.web.error;

import com.tariffsheriff.backend.chatbot.exception.RateLimitExceededException;
import com.tariffsheriff.backend.service.exception.NotFoundException;
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

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        ErrorResponse body = build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(TariffRateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTariffRateNotFound(TariffRateNotFoundException ex, HttpServletRequest req) {
        ErrorResponse body = build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest req) {
        log.warn("Rate limit exceeded on {}: {}", req.getRequestURI(), ex.getMessage());
        ErrorResponse body = build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        log.error("Validation error on {}: {}", req.getRequestURI(), ex.getMessage());
        StringBuilder message = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            message.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; "));
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, message.toString(), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        log.error("Bad request on {}: {}", req.getRequestURI(), ex.getMessage());
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, extractMessage(ex), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
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

