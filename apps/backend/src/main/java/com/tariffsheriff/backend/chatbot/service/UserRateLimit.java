package com.tariffsheriff.backend.chatbot.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks rate limit data for a single user using sliding window approach
 */
class UserRateLimit {
    
    private final ConcurrentLinkedQueue<LocalDateTime> requestTimestamps = new ConcurrentLinkedQueue<>();
    
    /**
     * Record a new request timestamp
     */
    public void recordRequest(LocalDateTime timestamp) {
        requestTimestamps.offer(timestamp);
    }
    
    /**
     * Get number of requests in the last minute
     */
    public long getRequestsInLastMinute(LocalDateTime now) {
        LocalDateTime oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
        return requestTimestamps.stream()
                .filter(timestamp -> timestamp.isAfter(oneMinuteAgo))
                .count();
    }
    
    /**
     * Get number of requests in the last hour
     */
    public long getRequestsInLastHour(LocalDateTime now) {
        LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        return requestTimestamps.stream()
                .filter(timestamp -> timestamp.isAfter(oneHourAgo))
                .count();
    }
    
    /**
     * Remove timestamps older than 1 hour to prevent memory leaks
     */
    public void cleanupOldEntries(LocalDateTime now) {
        LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        requestTimestamps.removeIf(timestamp -> timestamp.isBefore(oneHourAgo));
    }
    
    /**
     * Check if this user has no recent activity (for cleanup)
     */
    public boolean isEmpty() {
        return requestTimestamps.isEmpty();
    }
}