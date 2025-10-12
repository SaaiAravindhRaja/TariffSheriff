package com.tariffsheriff.backend.auth.filter;

import com.tariffsheriff.backend.auth.service.AiAuthenticationService;
import com.tariffsheriff.backend.auth.service.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Enhanced JWT authentication filter with AI-specific session monitoring
 */
@Component
@RequiredArgsConstructor
public class EnhancedJwtAuthenticationFilter extends OncePerRequestFilter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EnhancedJwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AiAuthenticationService aiAuthenticationService;
    
    // Track suspicious authentication patterns
    private final Map<String, AuthenticationAttemptTracker> authAttempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String clientIp = getClientIpAddress(request);
        final String userAgent = request.getHeader("User-Agent");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtTokenProvider.extractUsername(jwt);
            
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Check for suspicious authentication patterns
                if (isSuspiciousAuthenticationAttempt(userEmail, clientIp, userAgent)) {
                    log.warn("Suspicious authentication attempt detected for user: {} from IP: {}", userEmail, clientIp);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                
                if (jwtTokenProvider.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // Add enhanced authentication details
                    EnhancedWebAuthenticationDetails details = new EnhancedWebAuthenticationDetails(
                            request, clientIp, userAgent, LocalDateTime.now());
                    authToken.setDetails(details);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Track successful authentication
                    trackSuccessfulAuthentication(userEmail, clientIp, userAgent);
                    
                    // Update AI session monitoring
                    aiAuthenticationService.updateSessionActivity(userEmail, "authentication");
                    
                    log.debug("Successfully authenticated user: {} from IP: {}", userEmail, clientIp);
                } else {
                    // Track failed authentication
                    trackFailedAuthentication(userEmail, clientIp, userAgent);
                    log.warn("Invalid JWT token for user: {} from IP: {}", userEmail, clientIp);
                }
            }
        } catch (JwtException e) {
            log.error("JWT processing error from IP {}: {}", clientIp, e.getMessage());
            trackFailedAuthentication("unknown", clientIp, userAgent);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Detects suspicious authentication patterns
     */
    private boolean isSuspiciousAuthenticationAttempt(String userEmail, String clientIp, String userAgent) {
        AuthenticationAttemptTracker tracker = authAttempts.computeIfAbsent(
                userEmail + ":" + clientIp, k -> new AuthenticationAttemptTracker());
        
        // Check for too many failed attempts
        if (tracker.getFailedAttempts() > 5) {
            LocalDateTime lastFailure = tracker.getLastFailedAttempt();
            if (lastFailure != null && lastFailure.isAfter(LocalDateTime.now().minusMinutes(15))) {
                return true; // Too many recent failures
            }
        }
        
        // Check for rapid authentication attempts
        if (tracker.getRecentAttemptCount(LocalDateTime.now().minusMinutes(1)) > 10) {
            return true; // Too many attempts in short time
        }
        
        // Check for unusual user agent patterns
        if (userAgent != null && isUnusualUserAgent(userAgent)) {
            return true;
        }
        
        return false;
    }

    /**
     * Checks for unusual user agent patterns that might indicate automated attacks
     */
    private boolean isUnusualUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return true; // Missing user agent is suspicious
        }
        
        String lowerUserAgent = userAgent.toLowerCase();
        
        // Check for common bot/script indicators
        String[] suspiciousPatterns = {
            "bot", "crawler", "spider", "scraper", "curl", "wget", "python", "java", "go-http"
        };
        
        for (String pattern : suspiciousPatterns) {
            if (lowerUserAgent.contains(pattern)) {
                return true;
            }
        }
        
        // Check for very short user agents (likely custom/automated)
        if (userAgent.length() < 20) {
            return true;
        }
        
        return false;
    }

    /**
     * Tracks successful authentication for pattern analysis
     */
    private void trackSuccessfulAuthentication(String userEmail, String clientIp, String userAgent) {
        String key = userEmail + ":" + clientIp;
        AuthenticationAttemptTracker tracker = authAttempts.computeIfAbsent(key, 
                k -> new AuthenticationAttemptTracker());
        tracker.recordSuccessfulAttempt(LocalDateTime.now(), userAgent);
    }

    /**
     * Tracks failed authentication for security monitoring
     */
    private void trackFailedAuthentication(String userEmail, String clientIp, String userAgent) {
        String key = userEmail + ":" + clientIp;
        AuthenticationAttemptTracker tracker = authAttempts.computeIfAbsent(key, 
                k -> new AuthenticationAttemptTracker());
        tracker.recordFailedAttempt(LocalDateTime.now(), userAgent);
    }

    /**
     * Extracts client IP address from request
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

    /**
     * Enhanced authentication details with additional security information
     */
    public static class EnhancedWebAuthenticationDetails {
        private final String remoteAddress;
        private final String sessionId;
        private final String clientIp;
        private final String userAgent;
        private final LocalDateTime authenticationTime;

        public EnhancedWebAuthenticationDetails(HttpServletRequest request, String clientIp, 
                                              String userAgent, LocalDateTime authenticationTime) {
            this.remoteAddress = request.getRemoteAddr();
            this.sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.authenticationTime = authenticationTime;
        }

        public String getRemoteAddress() { return remoteAddress; }
        public String getSessionId() { return sessionId; }
        public String getClientIp() { return clientIp; }
        public String getUserAgent() { return userAgent; }
        public LocalDateTime getAuthenticationTime() { return authenticationTime; }
    }

    /**
     * Tracks authentication attempts for security analysis
     */
    private static class AuthenticationAttemptTracker {
        private int failedAttempts = 0;
        private int successfulAttempts = 0;
        private LocalDateTime lastFailedAttempt;
        private LocalDateTime lastSuccessfulAttempt;
        private final Map<LocalDateTime, String> attemptHistory = new ConcurrentHashMap<>();

        public void recordFailedAttempt(LocalDateTime timestamp, String userAgent) {
            failedAttempts++;
            lastFailedAttempt = timestamp;
            attemptHistory.put(timestamp, "FAILED:" + userAgent);
            cleanupOldAttempts();
        }

        public void recordSuccessfulAttempt(LocalDateTime timestamp, String userAgent) {
            successfulAttempts++;
            lastSuccessfulAttempt = timestamp;
            failedAttempts = 0; // Reset failed attempts on successful auth
            attemptHistory.put(timestamp, "SUCCESS:" + userAgent);
            cleanupOldAttempts();
        }

        public int getRecentAttemptCount(LocalDateTime since) {
            return (int) attemptHistory.keySet().stream()
                    .filter(timestamp -> timestamp.isAfter(since))
                    .count();
        }

        private void cleanupOldAttempts() {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            attemptHistory.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
        }

        public int getFailedAttempts() { return failedAttempts; }
        public LocalDateTime getLastFailedAttempt() { return lastFailedAttempt; }
        public LocalDateTime getLastSuccessfulAttempt() { return lastSuccessfulAttempt; }
    }
}