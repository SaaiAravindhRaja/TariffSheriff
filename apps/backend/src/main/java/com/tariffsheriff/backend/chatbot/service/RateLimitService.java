package com.tariffsheriff.backend.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory rate limiting service for chatbot queries
 * Implements sliding window rate limiting per user
 */
@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    // Rate limit configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int MAX_REQUESTS_PER_HOUR = 100;
    
    // In-memory storage for rate limit tracking
    private final ConcurrentMap<String, UserRateLimit> userRateLimits = new ConcurrentHashMap<>();
    
    /**
     * Check if user is within rate limits
     * 
     * @param userEmail The user's email address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            logger.warn("Rate limit check called with null or empty user email");
            return false;
        }
        
        UserRateLimit userLimit = userRateLimits.computeIfAbsent(userEmail, k -> new UserRateLimit());
        
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up old entries
        userLimit.cleanupOldEntries(now);
        
        // Check minute limit
        long requestsInLastMinute = userLimit.getRequestsInLastMinute(now);
        if (requestsInLastMinute >= MAX_REQUESTS_PER_MINUTE) {
            logger.warn("Rate limit exceeded for user {} - {} requests in last minute (limit: {})", 
                    userEmail, requestsInLastMinute, MAX_REQUESTS_PER_MINUTE);
            return false;
        }
        
        // Check hour limit
        long requestsInLastHour = userLimit.getRequestsInLastHour(now);
        if (requestsInLastHour >= MAX_REQUESTS_PER_HOUR) {
            logger.warn("Rate limit exceeded for user {} - {} requests in last hour (limit: {})", 
                    userEmail, requestsInLastHour, MAX_REQUESTS_PER_HOUR);
            return false;
        }
        
        // Record the request
        userLimit.recordRequest(now);
        
        logger.debug("Rate limit check passed for user {} - Minute: {}/{}, Hour: {}/{}", 
                userEmail, requestsInLastMinute + 1, MAX_REQUESTS_PER_MINUTE, 
                requestsInLastHour + 1, MAX_REQUESTS_PER_HOUR);
        
        return true;
    }
    
    /**
     * Get current rate limit status for a user
     * 
     * @param userEmail The user's email address
     * @return RateLimitStatus with current usage
     */
    public RateLimitStatus getRateLimitStatus(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            return new RateLimitStatus(0, 0, MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
        }
        
        UserRateLimit userLimit = userRateLimits.get(userEmail);
        if (userLimit == null) {
            return new RateLimitStatus(0, 0, MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
        }
        
        LocalDateTime now = LocalDateTime.now();
        userLimit.cleanupOldEntries(now);
        
        long requestsInLastMinute = userLimit.getRequestsInLastMinute(now);
        long requestsInLastHour = userLimit.getRequestsInLastHour(now);
        
        return new RateLimitStatus(requestsInLastMinute, requestsInLastHour, 
                MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
    }
    
    /**
     * Clear rate limit data for a user (for testing or admin purposes)
     */
    public void clearUserRateLimit(String userEmail) {
        userRateLimits.remove(userEmail);
        logger.info("Cleared rate limit data for user: {}", userEmail);
    }
    
    /**
     * Get total number of users being tracked
     */
    public int getTrackedUsersCount() {
        return userRateLimits.size();
    }
    
    /**
     * Clean up old entries for all users (maintenance task)
     */
    public void cleanupOldEntries() {
        LocalDateTime now = LocalDateTime.now();
        userRateLimits.values().forEach(userLimit -> userLimit.cleanupOldEntries(now));
        
        // Remove users with no recent activity
        userRateLimits.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        logger.debug("Cleaned up old rate limit entries. Currently tracking {} users", userRateLimits.size());
    }
    
    /**
     * Rate limit status information
     */
    public static class RateLimitStatus {
        private final long requestsInLastMinute;
        private final long requestsInLastHour;
        private final int maxRequestsPerMinute;
        private final int maxRequestsPerHour;
        
        public RateLimitStatus(long requestsInLastMinute, long requestsInLastHour, 
                              int maxRequestsPerMinute, int maxRequestsPerHour) {
            this.requestsInLastMinute = requestsInLastMinute;
            this.requestsInLastHour = requestsInLastHour;
            this.maxRequestsPerMinute = maxRequestsPerMinute;
            this.maxRequestsPerHour = maxRequestsPerHour;
        }
        
        public long getRequestsInLastMinute() { return requestsInLastMinute; }
        public long getRequestsInLastHour() { return requestsInLastHour; }
        public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
        public int getMaxRequestsPerHour() { return maxRequestsPerHour; }
        
        public boolean isMinuteLimitExceeded() { 
            return requestsInLastMinute >= maxRequestsPerMinute; 
        }
        
        public boolean isHourLimitExceeded() { 
            return requestsInLastHour >= maxRequestsPerHour; 
        }
        
        public long getMinuteRemainingRequests() {
            return Math.max(0, maxRequestsPerMinute - requestsInLastMinute);
        }
        
        public long getHourRemainingRequests() {
            return Math.max(0, maxRequestsPerHour - requestsInLastHour);
        }
    }
}