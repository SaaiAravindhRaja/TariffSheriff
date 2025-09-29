package com.tariffsheriff.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for handling unauthorized access.
 * Returns proper JSON error responses for authentication failures with detailed context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.warn("Unauthorized access attempt to: {} from IP: {} - {}", 
                request.getRequestURI(), ipAddress, authException.getMessage());

        // Set response status and content type
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Determine specific error message and code
        String errorCode = "UNAUTHORIZED";
        String errorMessage = "Authentication required to access this resource";
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            errorCode = "MISSING_TOKEN";
            errorMessage = "Missing Authorization header. Please provide a valid JWT token.";
        } else if (!authHeader.startsWith("Bearer ")) {
            errorCode = "INVALID_TOKEN_FORMAT";
            errorMessage = "Invalid Authorization header format. Expected 'Bearer <token>'.";
        } else {
            // Token is present but invalid/expired
            String token = authHeader.substring(7);
            if (token.isEmpty()) {
                errorCode = "EMPTY_TOKEN";
                errorMessage = "Empty JWT token provided.";
            } else {
                errorCode = "INVALID_TOKEN";
                errorMessage = "Invalid or expired JWT token. Please login again.";
            }
        }

        // Create error response with additional context
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("endpoint", request.getRequestURI());
        additionalInfo.put("method", request.getMethod());
        additionalInfo.put("requiresAuthentication", true);
        
        // Add helpful hints for common endpoints
        if (request.getRequestURI().startsWith("/api/auth/")) {
            additionalInfo.put("hint", "Authentication endpoints may not require tokens for some operations");
        } else if (request.getRequestURI().startsWith("/api/admin/")) {
            additionalInfo.put("hint", "Admin endpoints require ADMIN role");
        } else if (request.getRequestURI().startsWith("/api/user/")) {
            additionalInfo.put("hint", "User endpoints require valid authentication");
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorCode)
                .message(errorMessage)
                .status(401)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .details(additionalInfo)
                .build();

        // Add security headers
        response.setHeader("WWW-Authenticate", "Bearer realm=\"TariffSheriff API\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        // Write JSON response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (common with load balancers/proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header (Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Check for X-Forwarded header
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }
        
        // Check for Forwarded header (RFC 7239)
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty() && !"unknown".equalsIgnoreCase(forwarded)) {
            // Parse Forwarded header: for=192.0.2.60;proto=http;by=203.0.113.43
            if (forwarded.contains("for=")) {
                String forPart = forwarded.substring(forwarded.indexOf("for=") + 4);
                if (forPart.contains(";")) {
                    forPart = forPart.substring(0, forPart.indexOf(";"));
                }
                return forPart.trim();
            }
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }
}