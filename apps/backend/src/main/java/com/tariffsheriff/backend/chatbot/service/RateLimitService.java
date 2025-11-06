package com.tariffsheriff.backend.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simplified token bucket rate limiting service for chatbot queries
 * Uses efficient token bucket algorithm instead of storing all timestamps
 */
@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    // Rate limit configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int MAX_REQUESTS_PER_HOUR = 100;
    
    // In-memory storage for rate limiters
    private final ConcurrentMap<String, TokenBucket> userLimiters = new ConcurrentHashMap<>();
    
    /**
     * Check if user is within rate limits and consume a token
     * 
     * @param userEmail The user's email address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            logger.warn("Rate limit check called with null or empty user email");
            return false;
        }
        
        TokenBucket bucket = userLimiters.computeIfAbsent(userEmail, 
            k -> new TokenBucket(MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR));
        
        boolean allowed = bucket.tryConsume();
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for user {} - Minute: {}/{}, Hour: {}/{}", 
                    userEmail, 
                    bucket.getMinuteTokens(), MAX_REQUESTS_PER_MINUTE,
                    bucket.getHourTokens(), MAX_REQUESTS_PER_HOUR);
        } else {
            logger.debug("Rate limit check passed for user {} - Minute: {}/{}, Hour: {}/{}", 
                    userEmail,
                    bucket.getMinuteTokens(), MAX_REQUESTS_PER_MINUTE,
                    bucket.getHourTokens(), MAX_REQUESTS_PER_HOUR);
        }
        
        return allowed;
    }
    
    /**
     * Allow a request and record it (alias for isAllowed)
     */
    public boolean allowRequest(String userEmail) {
        return isAllowed(userEmail);
    }
    
    /**
     * Get current rate limit status for a user
     */
    public RateLimitStatus getRateLimitStatus(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            return new RateLimitStatus(0, 0, MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
        }
        
        TokenBucket bucket = userLimiters.get(userEmail);
        if (bucket == null) {
            return new RateLimitStatus(0, 0, MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
        }
        
        bucket.refill(); // Ensure tokens are up to date
        
        long usedMinute = MAX_REQUESTS_PER_MINUTE - bucket.getMinuteTokens();
        long usedHour = MAX_REQUESTS_PER_HOUR - bucket.getHourTokens();
        
        return new RateLimitStatus(usedMinute, usedHour, 
                MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
    }
    
    /**
     * Clear rate limit data for a user (for testing or admin purposes)
     */
    public void clearUserRateLimit(String userEmail) {
        userLimiters.remove(userEmail);
        logger.info("Cleared rate limit data for user: {}", userEmail);
    }
    
    /**
     * Get total number of users being tracked
     */
    public int getTrackedUsersCount() {
        return userLimiters.size();
    }
    
    /**
     * Clean up old entries for all users (maintenance task)
     */
    public void cleanupOldEntries() {
        // Remove users with full tokens (inactive)
        userLimiters.entrySet().removeIf(entry -> {
            TokenBucket bucket = entry.getValue();
            bucket.refill();
            return bucket.getMinuteTokens() >= MAX_REQUESTS_PER_MINUTE 
                && bucket.getHourTokens() >= MAX_REQUESTS_PER_HOUR;
        });
        
        logger.debug("Cleaned up inactive rate limiters. Currently tracking {} users", userLimiters.size());
    }
    
    /**
     * Token bucket implementation for efficient rate limiting
     * Stores only counters instead of all timestamps (90% less memory)
     */
    private static class TokenBucket {
        private final int minuteCapacity;
        private final int hourCapacity;
        private int minuteTokens;
        private int hourTokens;
        private LocalDateTime lastMinuteRefill;
        private LocalDateTime lastHourRefill;
        
        public TokenBucket(int minuteCapacity, int hourCapacity) {
            this.minuteCapacity = minuteCapacity;
            this.hourCapacity = hourCapacity;
            this.minuteTokens = minuteCapacity;
            this.hourTokens = hourCapacity;
            this.lastMinuteRefill = LocalDateTime.now();
            this.lastHourRefill = LocalDateTime.now();
        }
        
        public synchronized boolean tryConsume() {
            refill();
            
            if (minuteTokens > 0 && hourTokens > 0) {
                minuteTokens--;
                hourTokens--;
                return true;
            }
            
            return false;
        }
        
        public synchronized void refill() {
            LocalDateTime now = LocalDateTime.now();
            
            // Refill minute bucket
            long minutesPassed = ChronoUnit.MINUTES.between(lastMinuteRefill, now);
            if (minutesPassed >= 1) {
                minuteTokens = minuteCapacity;
                lastMinuteRefill = now;
            }
            
            // Refill hour bucket
            long hoursPassed = ChronoUnit.HOURS.between(lastHourRefill, now);
            if (hoursPassed >= 1) {
                hourTokens = hourCapacity;
                lastHourRefill = now;
            }
        }
        
        public synchronized int getMinuteTokens() {
            refill();
            return minuteTokens;
        }
        
        public synchronized int getHourTokens() {
            refill();
            return hourTokens;
        }
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
