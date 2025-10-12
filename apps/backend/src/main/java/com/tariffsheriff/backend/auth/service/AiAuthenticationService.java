package com.tariffsheriff.backend.auth.service;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.model.AiPermission;
import com.tariffsheriff.backend.auth.model.AiRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced authentication service with AI-specific access controls
 */
@Service
public class AiAuthenticationService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiAuthenticationService.class);

    // Session monitoring for AI usage patterns
    private final Map<String, AiUserSession> activeSessions = new ConcurrentHashMap<>();
    
    // Rate limiting per user and feature
    private final Map<String, UserRateLimit> userRateLimits = new ConcurrentHashMap<>();

    /**
     * Checks if the current user has the specified AI permission
     */
    public boolean hasPermission(AiPermission permission) {
        User user = getCurrentUser();
        if (user == null) {
            log.warn("No authenticated user found when checking permission: {}", permission);
            return false;
        }
        
        return hasPermission(user, permission);
    }

    /**
     * Checks if the specified user has the given AI permission
     */
    public boolean hasPermission(User user, AiPermission permission) {
        if (user == null) return false;
        
        AiRole aiRole = getUserAiRole(user);
        boolean hasPermission = aiRole.hasPermission(permission);
        
        log.debug("Permission check for user {}: {} = {}", user.getEmail(), permission, hasPermission);
        return hasPermission;
    }

    /**
     * Gets the AI role for the user
     */
    public AiRole getUserAiRole(User user) {
        if (user == null) return AiRole.BASIC_USER;
        
        // Map traditional roles to AI roles
        if (user.getIsAdmin()) {
            return AiRole.ADMIN;
        }
        
        // Check user's role field for AI-specific roles
        String userRole = user.getRole();
        if (userRole != null) {
            switch (userRole.toUpperCase()) {
                case "ENTERPRISE":
                case "ENTERPRISE_USER":
                    return AiRole.ENTERPRISE_USER;
                case "ANALYST":
                case "TRADE_ANALYST":
                    return AiRole.TRADE_ANALYST;
                case "PROFESSIONAL":
                case "TRADE_PROFESSIONAL":
                    return AiRole.TRADE_PROFESSIONAL;
                case "ADMIN":
                case "ADMINISTRATOR":
                    return AiRole.ADMIN;
                default:
                    return AiRole.BASIC_USER;
            }
        }
        
        return AiRole.BASIC_USER;
    }

    /**
     * Gets all permissions for the current user
     */
    public Set<AiPermission> getUserPermissions() {
        User user = getCurrentUser();
        if (user == null) return Set.of();
        
        return getUserAiRole(user).getPermissions();
    }

    /**
     * Validates access to a specific AI feature with rate limiting
     */
    public AccessValidationResult validateFeatureAccess(String feature, AiPermission requiredPermission) {
        User user = getCurrentUser();
        if (user == null) {
            return AccessValidationResult.denied("User not authenticated");
        }
        
        // Check permission
        if (!hasPermission(user, requiredPermission)) {
            log.warn("User {} denied access to feature {} - insufficient permissions", user.getEmail(), feature);
            return AccessValidationResult.denied("Insufficient permissions for feature: " + feature);
        }
        
        // Check rate limits
        RateLimitResult rateLimitResult = checkRateLimit(user.getEmail(), feature);
        if (!rateLimitResult.isAllowed()) {
            log.warn("User {} denied access to feature {} - rate limit exceeded", user.getEmail(), feature);
            return AccessValidationResult.rateLimited(rateLimitResult.getMessage());
        }
        
        // Update session monitoring
        updateSessionActivity(user.getEmail(), feature);
        
        log.debug("User {} granted access to feature {}", user.getEmail(), feature);
        return AccessValidationResult.allowed();
    }

    /**
     * Monitors AI usage patterns for anomaly detection
     */
    public void updateSessionActivity(String userId, String feature) {
        AiUserSession session = activeSessions.computeIfAbsent(userId, AiUserSession::new);
        session.recordActivity(feature, LocalDateTime.now());
        
        // Detect anomalous patterns
        if (session.isAnomalousActivity()) {
            log.warn("Anomalous AI usage pattern detected for user: {}", userId);
            // Could trigger additional security measures
        }
    }

    /**
     * Checks rate limits for user and feature
     */
    private RateLimitResult checkRateLimit(String userId, String feature) {
        UserRateLimit rateLimit = userRateLimits.computeIfAbsent(userId, UserRateLimit::new);
        
        // Get rate limits based on user role
        User user = getCurrentUser();
        AiRole role = getUserAiRole(user);
        
        return rateLimit.checkLimit(feature, role);
    }

    /**
     * Gets the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        
        return null;
    }

    /**
     * Gets session information for monitoring
     */
    public AiUserSession getUserSession(String userId) {
        return activeSessions.get(userId);
    }

    /**
     * Cleans up inactive sessions
     */
    public void cleanupInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        activeSessions.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity().isBefore(cutoff));
        
        userRateLimits.entrySet().removeIf(entry ->
            entry.getValue().getLastAccess().isBefore(cutoff));
    }

    // Result classes
    
    public static class AccessValidationResult {
        private final boolean allowed;
        private final String reason;
        private final AccessDenialType denialType;

        private AccessValidationResult(boolean allowed, String reason, AccessDenialType denialType) {
            this.allowed = allowed;
            this.reason = reason;
            this.denialType = denialType;
        }

        public static AccessValidationResult allowed() {
            return new AccessValidationResult(true, null, null);
        }

        public static AccessValidationResult denied(String reason) {
            return new AccessValidationResult(false, reason, AccessDenialType.PERMISSION_DENIED);
        }

        public static AccessValidationResult rateLimited(String reason) {
            return new AccessValidationResult(false, reason, AccessDenialType.RATE_LIMITED);
        }

        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        public AccessDenialType getDenialType() { return denialType; }
    }

    public enum AccessDenialType {
        PERMISSION_DENIED,
        RATE_LIMITED,
        ANOMALOUS_BEHAVIOR
    }

    private static class RateLimitResult {
        private final boolean allowed;
        private final String message;

        public RateLimitResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
    }

    // Session and rate limiting classes
    
    public static class AiUserSession {
        private final String userId;
        private final Map<String, Integer> featureUsage = new ConcurrentHashMap<>();
        private LocalDateTime lastActivity;
        private LocalDateTime sessionStart;

        public AiUserSession(String userId) {
            this.userId = userId;
            this.sessionStart = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
        }

        public void recordActivity(String feature, LocalDateTime timestamp) {
            featureUsage.merge(feature, 1, Integer::sum);
            lastActivity = timestamp;
        }

        public boolean isAnomalousActivity() {
            // Simple anomaly detection based on usage patterns
            int totalRequests = featureUsage.values().stream().mapToInt(Integer::intValue).sum();
            
            // Check for excessive usage in short time
            if (totalRequests > 100 && sessionStart.isAfter(LocalDateTime.now().minusMinutes(10))) {
                return true;
            }
            
            // Check for unusual feature distribution
            long uniqueFeatures = featureUsage.keySet().size();
            if (uniqueFeatures > 10 && totalRequests > 50) {
                return true;
            }
            
            return false;
        }

        public String getUserId() { return userId; }
        public Map<String, Integer> getFeatureUsage() { return featureUsage; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public LocalDateTime getSessionStart() { return sessionStart; }
    }

    private static class UserRateLimit {
        private final String userId;
        private final Map<String, FeatureRateLimit> featureLimits = new ConcurrentHashMap<>();
        private LocalDateTime lastAccess;

        public UserRateLimit(String userId) {
            this.userId = userId;
            this.lastAccess = LocalDateTime.now();
        }

        public RateLimitResult checkLimit(String feature, AiRole role) {
            lastAccess = LocalDateTime.now();
            
            FeatureRateLimit featureLimit = featureLimits.computeIfAbsent(feature, 
                f -> new FeatureRateLimit(getRateLimitForRole(role, feature)));
            
            if (featureLimit.isAllowed()) {
                featureLimit.recordUsage();
                return new RateLimitResult(true, null);
            } else {
                return new RateLimitResult(false, 
                    String.format("Rate limit exceeded for feature %s. Try again in %d seconds.", 
                        feature, featureLimit.getSecondsUntilReset()));
            }
        }

        private int getRateLimitForRole(AiRole role, String feature) {
            // Define rate limits based on role and feature
            switch (role) {
                case BASIC_USER:
                    return 10; // 10 requests per minute
                case TRADE_PROFESSIONAL:
                    return 30; // 30 requests per minute
                case TRADE_ANALYST:
                    return 60; // 60 requests per minute
                case ENTERPRISE_USER:
                    return 120; // 120 requests per minute
                case ADMIN:
                    return 1000; // 1000 requests per minute
                default:
                    return 5; // Conservative default
            }
        }

        public LocalDateTime getLastAccess() { return lastAccess; }
    }

    private static class FeatureRateLimit {
        private final int maxRequests;
        private int currentRequests = 0;
        private LocalDateTime windowStart;

        public FeatureRateLimit(int maxRequests) {
            this.maxRequests = maxRequests;
            this.windowStart = LocalDateTime.now();
        }

        public boolean isAllowed() {
            resetIfNeeded();
            return currentRequests < maxRequests;
        }

        public void recordUsage() {
            resetIfNeeded();
            currentRequests++;
        }

        public long getSecondsUntilReset() {
            return 60 - (LocalDateTime.now().getSecond() - windowStart.getSecond());
        }

        private void resetIfNeeded() {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(windowStart.plusMinutes(1))) {
                currentRequests = 0;
                windowStart = now;
            }
        }
    }
}