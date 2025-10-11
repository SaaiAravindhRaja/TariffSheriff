package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching chat responses to improve performance
 */
@Service
public class ChatCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatCacheService.class);
    
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MINUTES = 60; // 1 hour
    
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public ChatCacheService() {
        // Schedule cache cleanup every 15 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 15, 15, TimeUnit.MINUTES);
    }
    
    /**
     * Get cached response for a query
     */
    public ChatQueryResponse getCachedResponse(String query) {
        String key = generateCacheKey(query);
        CacheEntry entry = cache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            logger.debug("Cache hit for query: {}", query.substring(0, Math.min(50, query.length())));
            return entry.response;
        }
        
        if (entry != null && entry.isExpired()) {
            cache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache a response for a query
     */
    public void cacheResponse(String query, ChatQueryResponse response) {
        // Don't cache error responses
        if (!response.isSuccess()) {
            return;
        }
        
        // Don't cache responses that took too long (likely complex queries)
        if (response.getProcessingTimeMs() > 15000) {
            return;
        }
        
        String key = generateCacheKey(query);
        
        // Evict oldest entries if cache is full
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        cache.put(key, new CacheEntry(response, System.currentTimeMillis()));
        logger.debug("Cached response for query: {}", query.substring(0, Math.min(50, query.length())));
    }
    
    /**
     * Clear all cached responses
     */
    public void clearCache() {
        cache.clear();
        logger.info("Cache cleared");
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        long now = System.currentTimeMillis();
        long validEntries = cache.values().stream()
                .mapToLong(entry -> entry.isExpired(now) ? 0 : 1)
                .sum();
        
        return new CacheStats(cache.size(), (int) validEntries, MAX_CACHE_SIZE);
    }
    
    /**
     * Generate cache key from query
     */
    private String generateCacheKey(String query) {
        try {
            // Normalize query (lowercase, trim, remove extra spaces)
            String normalized = query.toLowerCase().trim().replaceAll("\\s+", " ");
            
            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating cache key", e);
            return String.valueOf(query.hashCode());
        }
    }
    
    /**
     * Clean up expired entries
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        
        // Collect keys to remove
        List<String> keysToRemove = cache.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(now))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        // Remove expired entries
        keysToRemove.forEach(cache::remove);
        
        if (!keysToRemove.isEmpty()) {
            logger.debug("Cleaned up {} expired cache entries", keysToRemove.size());
        }
    }
    
    /**
     * Evict oldest entries when cache is full
     */
    private void evictOldestEntries() {
        final int entriesToRemove = cache.size() - MAX_CACHE_SIZE + 100; // Remove extra to avoid frequent evictions
        
        cache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                .limit(entriesToRemove)
                .map(entry -> entry.getKey())
                .forEach(cache::remove);
        
        logger.debug("Evicted {} oldest cache entries", entriesToRemove);
    }
    
    /**
     * Cache entry wrapper
     */
    private static class CacheEntry {
        final ChatQueryResponse response;
        final long timestamp;
        
        CacheEntry(ChatQueryResponse response, long timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        boolean isExpired(long now) {
            return (now - timestamp) > TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES);
        }
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStats {
        private final int totalEntries;
        private final int validEntries;
        private final int maxSize;
        
        public CacheStats(int totalEntries, int validEntries, int maxSize) {
            this.totalEntries = totalEntries;
            this.validEntries = validEntries;
            this.maxSize = maxSize;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public int getValidEntries() { return validEntries; }
        public int getMaxSize() { return maxSize; }
        public double getHitRatio() { 
            return validEntries > 0 ? (double) validEntries / totalEntries : 0.0; 
        }
    }
}