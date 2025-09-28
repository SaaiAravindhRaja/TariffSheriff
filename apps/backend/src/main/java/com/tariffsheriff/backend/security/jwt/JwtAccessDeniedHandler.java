package com.tariffsheriff.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom access denied handler for role-based access violations.
 * Returns proper JSON error responses for authorization failures with detailed context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, 
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        String ipAddress = getClientIpAddress(request);
        
        log.warn("Access denied for user: {} attempting to access: {} from IP: {} - {}", 
                username, request.getRequestURI(), ipAddress, accessDeniedException.getMessage());

        // Set response status and content type
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Determine specific error details
        String errorCode = "ACCESS_DENIED";
        String errorMessage = "Insufficient privileges to access this resource";
        Map<String, Object> additionalInfo = new HashMap<>();
        
        // Add user context if authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            additionalInfo.put("authenticatedUser", username);
            
            if (authentication.getAuthorities() != null) {
                String userRoles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", "));
                additionalInfo.put("currentRoles", userRoles);
                
                log.debug("User {} with roles [{}] denied access to {}", username, userRoles, request.getRequestURI());
            }
        }

        // Provide specific error messages and required roles based on endpoint
        String uri = request.getRequestURI();
        if (uri.contains("/api/admin/")) {
            errorCode = "ADMIN_REQUIRED";
            errorMessage = "Administrator privileges required for this operation";
            additionalInfo.put("requiredRole", "ADMIN");
            additionalInfo.put("hint", "Contact your administrator to request elevated privileges");
        } else if (uri.contains("/api/user/admin/") || uri.contains("/api/user/*/role") || uri.contains("/api/user/*/status")) {
            errorCode = "ADMIN_REQUIRED";
            errorMessage = "Administrator privileges required for user management operations";
            additionalInfo.put("requiredRole", "ADMIN");
        } else if (uri.contains("/api/analytics/") || uri.contains("/api/tariff/admin/")) {
            errorCode = "ANALYST_OR_ADMIN_REQUIRED";
            errorMessage = "Analyst or Administrator privileges required for this operation";
            additionalInfo.put("requiredRoles", "ANALYST, ADMIN");
        } else if (uri.contains("/api/tariff/bulk/")) {
            errorCode = "ADMIN_REQUIRED";
            errorMessage = "Administrator privileges required for bulk operations";
            additionalInfo.put("requiredRole", "ADMIN");
        } else if (uri.contains("/actuator/")) {
            errorCode = "ADMIN_REQUIRED";
            errorMessage = "Administrator privileges required for system monitoring";
            additionalInfo.put("requiredRole", "ADMIN");
        } else {
            // Generic access denied
            additionalInfo.put("hint", "You may need elevated privileges or different role assignments");
        }

        // Add endpoint information
        additionalInfo.put("endpoint", request.getRequestURI());
        additionalInfo.put("method", request.getMethod());
        additionalInfo.put("timestamp", LocalDateTime.now());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorCode)
                .message(errorMessage)
                .status(403)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .details(additionalInfo)
                .build();

        // Add security headers
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