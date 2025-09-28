package com.tariffsheriff.backend.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Interceptor for applying rate limiting to authentication endpoints.
 * Checks rate limits before allowing requests to proceed to controllers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Record global IP request
        rateLimitService.recordIpRequest(ipAddress);

        // Check global IP rate limit
        if (!rateLimitService.isIpAllowed(ipAddress)) {
            return handleRateLimit(response, "Too many requests from this IP address", 
                    rateLimitService.getRateLimitRemainingTime("rate_limit:ip:" + ipAddress));
        }

        // Apply specific rate limits based on endpoint
        if ("POST".equals(method)) {
            if (requestUri.contains("/api/auth/login")) {
                return handleLoginRateLimit(request, response, ipAddress);
            } else if (requestUri.contains("/api/auth/register")) {
                return handleRegistrationRateLimit(request, response, ipAddress);
            } else if (requestUri.contains("/api/auth/forgot-password")) {
                return handlePasswordResetRateLimit(request, response, ipAddress);
            }
        }

        // Check for suspicious activity
        if (rateLimitService.isSuspiciousActivity(ipAddress, null)) {
            log.warn("Suspicious activity detected from IP: {} for request: {}", ipAddress, requestUri);
            // Could implement additional security measures here (CAPTCHA, temporary ban, etc.)
        }

        return true; // Allow request to proceed
    }

    /**
     * Handle rate limiting for login attempts.
     */
    private boolean handleLoginRateLimit(HttpServletRequest request, HttpServletResponse response, String ipAddress) throws IOException {
        if (!rateLimitService.isLoginAllowed(ipAddress)) {
            long remainingTime = rateLimitService.getRateLimitRemainingTime("rate_limit:login:" + ipAddress);
            return handleRateLimit(response, "Too many login attempts. Please try again later.", remainingTime);
        }
        
        // Record the attempt (will be incremented regardless of success/failure)
        rateLimitService.recordLoginAttempt(ipAddress);
        return true;
    }

    /**
     * Handle rate limiting for registration attempts.
     */
    private boolean handleRegistrationRateLimit(HttpServletRequest request, HttpServletResponse response, String ipAddress) throws IOException {
        if (!rateLimitService.isRegistrationAllowed(ipAddress)) {
            long remainingTime = rateLimitService.getRateLimitRemainingTime("rate_limit:register:" + ipAddress);
            return handleRateLimit(response, "Too many registration attempts. Please try again later.", remainingTime);
        }
        
        rateLimitService.recordRegistrationAttempt(ipAddress);
        return true;
    }

    /**
     * Handle rate limiting for password reset attempts.
     */
    private boolean handlePasswordResetRateLimit(HttpServletRequest request, HttpServletResponse response, String ipAddress) throws IOException {
        if (!rateLimitService.isPasswordResetAllowed(ipAddress)) {
            long remainingTime = rateLimitService.getRateLimitRemainingTime("rate_limit:password_reset:" + ipAddress);
            return handleRateLimit(response, "Too many password reset attempts. Please try again later.", remainingTime);
        }
        
        rateLimitService.recordPasswordResetAttempt(ipAddress);
        return true;
    }

    /**
     * Handle rate limit exceeded response.
     */
    private boolean handleRateLimit(HttpServletResponse response, String message, long remainingTimeSeconds) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Add rate limit headers
        response.setHeader("X-Rate-Limit-Remaining", "0");
        response.setHeader("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + remainingTimeSeconds));
        response.setHeader("Retry-After", String.valueOf(remainingTimeSeconds));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("RATE_LIMIT_EXCEEDED")
                .message(message)
                .status(429)
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();

        return false; // Block the request
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}