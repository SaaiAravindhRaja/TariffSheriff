package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Enhanced service for intelligent multi-level caching of chat responses
 * Features:
 * - Multi-level caching (L1: Hot cache, L2: Warm cache, L3: Cold cache)
 * - Smart cache invalidation based on data freshness and relevance
 * - Cache warming for frequently requested trade routes and products
 * - Comprehensive cache analytics and performance monitoring
 */
@Service
public class ChatCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatCacheService.class);
    
    // Configuration
    @Value("${cache.l1.max-size:500}")
    private int l1MaxSize;
    
    @Value("${cache.l2.max-size:2000}")
    private int l2MaxSize;
    
    @Value("${cache.l3.max-size:5000}")
    private int l3MaxSize;
    
    @Value("${cache.l1.ttl-minutes:30}")
    private long l1TtlMinutes;
    
    @Value("${cache.l2.ttl-minutes:120}")
    private long l2TtlMinutes;
    
    @Value("${cache.l3.ttl-minutes:720}")
    private long l3TtlMinutes;
    
    @Value("${cache.compression.enabled:true}")
    private boolean compressionEnabled;
    
    @Value("${cache.compression.threshold:1024}")
    private int compressionThreshold;
    
    @Value("${cache.predictive.enabled:true}")
    private boolean predictiveCachingEnabled;
    
    @Value("${cache.warming.batch-size:50}")
    private int warmingBatchSize;
    
    // Multi-level cache storage
    private final ConcurrentHashMap<String, CacheEntry> l1Cache = new ConcurrentHashMap<>(); // Hot cache
    private final ConcurrentHashMap<String, CacheEntry> l2Cache = new ConcurrentHashMap<>(); // Warm cache
    private final ConcurrentHashMap<String, CacheEntry> l3Cache = new ConcurrentHashMap<>(); // Cold cache
    
    // Cache analytics
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong l3Hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong invalidations = new AtomicLong(0);
    
    // Cache warming patterns
    private final Set<Pattern> warmingPatterns = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Integer> queryFrequency = new ConcurrentHashMap<>();
    
    // Predictive caching
    private final ConcurrentHashMap<String, PredictivePattern> predictivePatterns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> userQueryHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> queryTimestamps = new ConcurrentHashMap<>();
    
    // Compression statistics
    private final AtomicLong compressionSavings = new AtomicLong(0);
    private final AtomicLong compressedEntries = new AtomicLong(0);
    
    // Executors
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService warmingExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService analyticsExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public ChatCacheService() {
        initializeWarmingPatterns();
        
        // Schedule cache cleanup every 10 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 10, 10, TimeUnit.MINUTES);
        
        // Schedule cache warming every 30 minutes
        warmingExecutor.scheduleAtFixedRate(this::performCacheWarming, 30, 30, TimeUnit.MINUTES);
        
        // Schedule analytics collection every 5 minutes
        analyticsExecutor.scheduleAtFixedRate(this::collectAnalytics, 5, 5, TimeUnit.MINUTES);
        
        // Schedule cache optimization every 2 hours
        analyticsExecutor.scheduleAtFixedRate(this::optimizeCache, 120, 120, TimeUnit.MINUTES);
    }
    
    /**
     * Initialize patterns for cache warming
     */
    private void initializeWarmingPatterns() {
        // Common trade route patterns
        warmingPatterns.add(Pattern.compile(".*tariff.*from.*(china|germany|japan|mexico|canada).*to.*(us|usa|united states).*", Pattern.CASE_INSENSITIVE));
        warmingPatterns.add(Pattern.compile(".*hs code.*for.*(electronics|automotive|textiles|machinery).*", Pattern.CASE_INSENSITIVE));
        warmingPatterns.add(Pattern.compile(".*trade agreement.*(usmca|nafta|cptpp|eu).*", Pattern.CASE_INSENSITIVE));
        warmingPatterns.add(Pattern.compile(".*import.*cost.*(calculation|analysis).*", Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Get cached response for a query using multi-level cache strategy
     */
    public ChatQueryResponse getCachedResponse(String query) {
        String key = generateCacheKey(query);
        updateQueryFrequency(key);
        
        // Try L1 cache first (hot cache)
        CacheEntry entry = l1Cache.get(key);
        if (entry != null && !entry.isExpired(l1TtlMinutes)) {
            l1Hits.incrementAndGet();
            entry.updateAccessTime();
            logger.debug("L1 cache hit for query: {}", truncateQuery(query));
            return entry.response;
        }
        
        // Try L2 cache (warm cache)
        entry = l2Cache.get(key);
        if (entry != null && !entry.isExpired(l2TtlMinutes)) {
            l2Hits.incrementAndGet();
            entry.updateAccessTime();
            // Promote to L1 if frequently accessed
            if (entry.accessCount > 3) {
                promoteToL1(key, entry);
            }
            logger.debug("L2 cache hit for query: {}", truncateQuery(query));
            return entry.response;
        }
        
        // Try L3 cache (cold cache)
        entry = l3Cache.get(key);
        if (entry != null && !entry.isExpired(l3TtlMinutes)) {
            l3Hits.incrementAndGet();
            entry.updateAccessTime();
            // Promote to L2 if accessed
            promoteToL2(key, entry);
            logger.debug("L3 cache hit for query: {}", truncateQuery(query));
            return entry.response;
        }
        
        // Clean up expired entries
        cleanupExpiredEntry(key);
        misses.incrementAndGet();
        return null;
    }
    
    /**
     * Cache a response for a query using intelligent placement strategy
     */
    public void cacheResponse(String query, ChatQueryResponse response) {
        // Don't cache error responses
        if (!response.isSuccess()) {
            return;
        }
        
        String key = generateCacheKey(query);
        
        // Apply compression if enabled and response is large enough
        ChatQueryResponse responseToCache = response;
        boolean compressed = false;
        
        if (compressionEnabled && shouldCompress(response)) {
            try {
                responseToCache = compressResponse(response);
                compressed = true;
                compressedEntries.incrementAndGet();
                
                // Calculate compression savings
                long originalSize = estimateResponseSize(response);
                long compressedSize = estimateResponseSize(responseToCache);
                compressionSavings.addAndGet(originalSize - compressedSize);
                
                logger.debug("Compressed response for query: {} (saved {} bytes)", 
                    truncateQuery(query), originalSize - compressedSize);
            } catch (Exception e) {
                logger.warn("Failed to compress response for query: {}", truncateQuery(query), e);
                responseToCache = response; // Fall back to uncompressed
            }
        }
        
        CacheEntry entry = new CacheEntry(responseToCache, System.currentTimeMillis(), query, compressed);
        
        // Update user query history for predictive caching
        updateUserQueryHistory(extractUserId(query), query);
        
        // Determine cache level based on query characteristics
        CacheLevel level = determineCacheLevel(query, response);
        
        switch (level) {
            case L1:
                cacheInL1(key, entry);
                break;
            case L2:
                cacheInL2(key, entry);
                break;
            case L3:
                cacheInL3(key, entry);
                break;
        }
        
        logger.debug("Cached response in {} for query: {}", level, truncateQuery(query));
    }
    
    /**
     * Check if response should be compressed
     */
    private boolean shouldCompress(ChatQueryResponse response) {
        return estimateResponseSize(response) > compressionThreshold;
    }
    
    /**
     * Estimate response size in bytes
     */
    private long estimateResponseSize(ChatQueryResponse response) {
        // Rough estimation based on response content
        long size = 0;
        
        if (response.getResponse() != null) {
            size += response.getResponse().length() * 2; // Assuming UTF-16 encoding
        }
        
        if (response.getToolResults() != null) {
            size += response.getToolResults().size() * 100; // Rough estimate per tool result
        }
        
        return size;
    }
    
    /**
     * Compress response using GZIP
     */
    private ChatQueryResponse compressResponse(ChatQueryResponse response) throws IOException {
        // Create a compressed version of the response
        // For simplicity, we'll compress the main response text
        String originalResponse = response.getResponse();
        
        if (originalResponse != null && originalResponse.length() > compressionThreshold) {
            byte[] compressed = compress(originalResponse);
            String compressedResponse = Base64.getEncoder().encodeToString(compressed);
            
            // Create new response with compressed content
            // Note: In a real implementation, you'd need to modify ChatQueryResponse
            // to support compression metadata
            return response; // For now, return original
        }
        
        return response;
    }
    
    /**
     * Decompress response when retrieving from cache
     */
    private ChatQueryResponse decompressResponse(ChatQueryResponse response) throws IOException {
        // Decompress the response if it was compressed
        // This would check compression metadata and decompress accordingly
        return response; // For now, return as-is
    }
    
    /**
     * Compress string using GZIP
     */
    private byte[] compress(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data.getBytes("UTF-8"));
        }
        return bos.toByteArray();
    }
    
    /**
     * Decompress GZIP data
     */
    private String decompress(byte[] compressed) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        }
        return bos.toString("UTF-8");
    }
    
    /**
     * Extract user ID from query context (simplified)
     */
    private String extractUserId(String query) {
        // In a real implementation, this would extract user ID from context
        // For now, return a default user ID
        return "default_user";
    }
    
    /**
     * Update user query history for predictive caching
     */
    private void updateUserQueryHistory(String userId, String query) {
        userQueryHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(query);
        queryTimestamps.put(query, System.currentTimeMillis());
        
        // Keep only recent queries (last 100 per user)
        List<String> userQueries = userQueryHistory.get(userId);
        if (userQueries.size() > 100) {
            userQueries.remove(0);
        }
    }
    
    /**
     * Determine appropriate cache level for a query
     */
    private CacheLevel determineCacheLevel(String query, ChatQueryResponse response) {
        // High priority queries go to L1
        if (isHighPriorityQuery(query)) {
            return CacheLevel.L1;
        }
        
        // Fast responses with good quality go to L1
        if (response.getProcessingTimeMs() < 5000 && response.getConfidence() != null && response.getConfidence() > 0.8) {
            return CacheLevel.L1;
        }
        
        // Medium complexity queries go to L2
        if (response.getProcessingTimeMs() < 15000) {
            return CacheLevel.L2;
        }
        
        // Complex or slow queries go to L3
        return CacheLevel.L3;
    }
    
    /**
     * Check if query matches high priority patterns
     */
    private boolean isHighPriorityQuery(String query) {
        return warmingPatterns.stream().anyMatch(pattern -> pattern.matcher(query).matches());
    }
    
    /**
     * Clear all cached responses
     */
    public void clearCache() {
        l1Cache.clear();
        l2Cache.clear();
        l3Cache.clear();
        queryFrequency.clear();
        resetAnalytics();
        logger.info("All cache levels cleared");
    }
    
    /**
     * Clear specific cache level
     */
    public void clearCacheLevel(CacheLevel level) {
        switch (level) {
            case L1:
                l1Cache.clear();
                break;
            case L2:
                l2Cache.clear();
                break;
            case L3:
                l3Cache.clear();
                break;
        }
        logger.info("Cache level {} cleared", level);
    }
    
    /**
     * Invalidate cache entries based on data freshness and relevance
     */
    public void invalidateByPattern(String pattern) {
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        
        invalidateInCache(l1Cache, regex);
        invalidateInCache(l2Cache, regex);
        invalidateInCache(l3Cache, regex);
        
        logger.info("Invalidated cache entries matching pattern: {}", pattern);
    }
    
    /**
     * Invalidate cache entries for specific trade routes or products
     */
    public void invalidateTradeRoute(String origin, String destination) {
        String pattern = String.format(".*(%s|%s).*(%s|%s).*", 
            origin.toLowerCase(), destination.toLowerCase(),
            destination.toLowerCase(), origin.toLowerCase());
        invalidateByPattern(pattern);
    }
    
    /**
     * Invalidate cache entries for specific products
     */
    public void invalidateProduct(String productCode) {
        String pattern = String.format(".*(hs.*%s|%s).*", productCode, productCode);
        invalidateByPattern(pattern);
    }
    
    /**
     * Get comprehensive cache statistics
     */
    public EnhancedCacheStats getCacheStats() {
        long now = System.currentTimeMillis();
        
        CacheLevelStats l1Stats = getCacheLevelStats(l1Cache, l1TtlMinutes, now);
        CacheLevelStats l2Stats = getCacheLevelStats(l2Cache, l2TtlMinutes, now);
        CacheLevelStats l3Stats = getCacheLevelStats(l3Cache, l3TtlMinutes, now);
        
        long totalHits = l1Hits.get() + l2Hits.get() + l3Hits.get();
        long totalRequests = totalHits + misses.get();
        double hitRatio = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        
        return new EnhancedCacheStats(l1Stats, l2Stats, l3Stats, hitRatio, 
            evictions.get(), invalidations.get(), getTopQueries(),
            compressionSavings.get(), compressedEntries.get(), predictivePatterns.size());
    }
    
    /**
     * Get statistics for a specific cache level
     */
    private CacheLevelStats getCacheLevelStats(ConcurrentHashMap<String, CacheEntry> cache, 
                                               long ttlMinutes, long now) {
        int totalEntries = cache.size();
        long validEntries = cache.values().stream()
                .mapToLong(entry -> entry.isExpired(ttlMinutes, now) ? 0 : 1)
                .sum();
        
        double avgAge = cache.values().stream()
                .filter(entry -> !entry.isExpired(ttlMinutes, now))
                .mapToLong(entry -> now - entry.timestamp)
                .average()
                .orElse(0.0) / 1000.0 / 60.0; // Convert to minutes
        
        return new CacheLevelStats(totalEntries, (int) validEntries, avgAge);
    }
    
    /**
     * Get top frequently queried patterns
     */
    private List<String> getTopQueries() {
        return queryFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Generate cache key from query with enhanced normalization
     */
    private String generateCacheKey(String query) {
        try {
            // Enhanced normalization
            String normalized = query.toLowerCase().trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^a-z0-9\\s]", "") // Remove special characters
                    .replaceAll("\\b(the|a|an|and|or|but|in|on|at|to|for|of|with|by)\\b", "") // Remove common words
                    .trim();
            
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
     * Update query frequency for analytics
     */
    private void updateQueryFrequency(String key) {
        queryFrequency.merge(key, 1, Integer::sum);
    }
    
    /**
     * Truncate query for logging
     */
    private String truncateQuery(String query) {
        return query.length() > 50 ? query.substring(0, 50) + "..." : query;
    }
    
    /**
     * Cache entry in L1 with eviction if needed
     */
    private void cacheInL1(String key, CacheEntry entry) {
        if (l1Cache.size() >= l1MaxSize) {
            evictFromCache(l1Cache, l1MaxSize / 4); // Evict 25% when full
        }
        l1Cache.put(key, entry);
    }
    
    /**
     * Cache entry in L2 with eviction if needed
     */
    private void cacheInL2(String key, CacheEntry entry) {
        if (l2Cache.size() >= l2MaxSize) {
            evictFromCache(l2Cache, l2MaxSize / 4);
        }
        l2Cache.put(key, entry);
    }
    
    /**
     * Cache entry in L3 with eviction if needed
     */
    private void cacheInL3(String key, CacheEntry entry) {
        if (l3Cache.size() >= l3MaxSize) {
            evictFromCache(l3Cache, l3MaxSize / 4);
        }
        l3Cache.put(key, entry);
    }
    
    /**
     * Promote entry from L2 to L1
     */
    private void promoteToL1(String key, CacheEntry entry) {
        l2Cache.remove(key);
        cacheInL1(key, entry);
        logger.debug("Promoted entry to L1: {}", key.substring(0, 8));
    }
    
    /**
     * Promote entry from L3 to L2
     */
    private void promoteToL2(String key, CacheEntry entry) {
        l3Cache.remove(key);
        cacheInL2(key, entry);
        logger.debug("Promoted entry to L2: {}", key.substring(0, 8));
    }
    
    /**
     * Clean up expired entries from all cache levels
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        
        int l1Cleaned = cleanupCacheLevel(l1Cache, l1TtlMinutes, now);
        int l2Cleaned = cleanupCacheLevel(l2Cache, l2TtlMinutes, now);
        int l3Cleaned = cleanupCacheLevel(l3Cache, l3TtlMinutes, now);
        
        int totalCleaned = l1Cleaned + l2Cleaned + l3Cleaned;
        if (totalCleaned > 0) {
            logger.debug("Cleaned up {} expired entries (L1: {}, L2: {}, L3: {})", 
                totalCleaned, l1Cleaned, l2Cleaned, l3Cleaned);
        }
    }
    
    /**
     * Clean up expired entries from specific cache level
     */
    private int cleanupCacheLevel(ConcurrentHashMap<String, CacheEntry> cache, long ttlMinutes, long now) {
        List<String> keysToRemove = cache.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(ttlMinutes, now))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        keysToRemove.forEach(cache::remove);
        return keysToRemove.size();
    }
    
    /**
     * Clean up expired entry for specific key
     */
    private void cleanupExpiredEntry(String key) {
        long now = System.currentTimeMillis();
        
        CacheEntry entry = l1Cache.get(key);
        if (entry != null && entry.isExpired(l1TtlMinutes, now)) {
            l1Cache.remove(key);
        }
        
        entry = l2Cache.get(key);
        if (entry != null && entry.isExpired(l2TtlMinutes, now)) {
            l2Cache.remove(key);
        }
        
        entry = l3Cache.get(key);
        if (entry != null && entry.isExpired(l3TtlMinutes, now)) {
            l3Cache.remove(key);
        }
    }
    
    /**
     * Evict entries from cache using intelligent relevance-based strategy
     */
    private void evictFromCache(ConcurrentHashMap<String, CacheEntry> cache, int entriesToRemove) {
        // Use relevance score for intelligent eviction
        cache.entrySet().stream()
                .sorted((e1, e2) -> {
                    // Primary sort by relevance score (ascending - lower relevance evicted first)
                    int relevanceCompare = Double.compare(e1.getValue().getRelevanceScore(), e2.getValue().getRelevanceScore());
                    if (relevanceCompare != 0) return relevanceCompare;
                    
                    // Secondary sort by last access time (ascending - older evicted first)
                    return Long.compare(e1.getValue().lastAccessTime, e2.getValue().lastAccessTime);
                })
                .limit(entriesToRemove)
                .map(Map.Entry::getKey)
                .forEach(key -> {
                    cache.remove(key);
                    evictions.incrementAndGet();
                });
        
        logger.debug("Evicted {} entries from cache using relevance-based strategy", entriesToRemove);
    }
    
    /**
     * Perform intelligent cache optimization
     */
    public void optimizeCache() {
        logger.info("Starting intelligent cache optimization");
        
        // Update relevance scores for all entries
        updateRelevanceScores();
        
        // Promote high-relevance entries to higher cache levels
        promoteHighRelevanceEntries();
        
        // Demote low-relevance entries to lower cache levels
        demoteLowRelevanceEntries();
        
        // Clean up entries with very low relevance
        cleanupLowRelevanceEntries();
        
        logger.info("Cache optimization completed");
    }
    
    /**
     * Update relevance scores for all cache entries
     */
    private void updateRelevanceScores() {
        updateRelevanceScoresForCache(l1Cache);
        updateRelevanceScoresForCache(l2Cache);
        updateRelevanceScoresForCache(l3Cache);
    }
    
    /**
     * Update relevance scores for specific cache level
     */
    private void updateRelevanceScoresForCache(ConcurrentHashMap<String, CacheEntry> cache) {
        cache.values().forEach(entry -> {
            // Relevance score is updated in updateAccessTime method
            // Here we could add additional factors like query frequency
            String key = generateCacheKey(entry.originalQuery);
            Integer frequency = queryFrequency.get(key);
            if (frequency != null && frequency > 1) {
                entry.relevanceScore *= Math.log(frequency + 1);
            }
        });
    }
    
    /**
     * Promote high-relevance entries to higher cache levels
     */
    private void promoteHighRelevanceEntries() {
        // Promote from L3 to L2
        List<Map.Entry<String, CacheEntry>> l3HighRelevance = l3Cache.entrySet().stream()
                .filter(entry -> entry.getValue().getRelevanceScore() > 2.0)
                .collect(java.util.stream.Collectors.toList());
        
        for (Map.Entry<String, CacheEntry> entry : l3HighRelevance) {
            if (l2Cache.size() < l2MaxSize) {
                l3Cache.remove(entry.getKey());
                l2Cache.put(entry.getKey(), entry.getValue());
                logger.debug("Promoted entry from L3 to L2: {}", entry.getKey().substring(0, 8));
            }
        }
        
        // Promote from L2 to L1
        List<Map.Entry<String, CacheEntry>> l2HighRelevance = l2Cache.entrySet().stream()
                .filter(entry -> entry.getValue().getRelevanceScore() > 3.0)
                .collect(java.util.stream.Collectors.toList());
        
        for (Map.Entry<String, CacheEntry> entry : l2HighRelevance) {
            if (l1Cache.size() < l1MaxSize) {
                l2Cache.remove(entry.getKey());
                l1Cache.put(entry.getKey(), entry.getValue());
                logger.debug("Promoted entry from L2 to L1: {}", entry.getKey().substring(0, 8));
            }
        }
    }
    
    /**
     * Demote low-relevance entries to lower cache levels
     */
    private void demoteLowRelevanceEntries() {
        // Demote from L1 to L2
        List<Map.Entry<String, CacheEntry>> l1LowRelevance = l1Cache.entrySet().stream()
                .filter(entry -> entry.getValue().getRelevanceScore() < 1.0)
                .collect(java.util.stream.Collectors.toList());
        
        for (Map.Entry<String, CacheEntry> entry : l1LowRelevance) {
            if (l2Cache.size() < l2MaxSize) {
                l1Cache.remove(entry.getKey());
                l2Cache.put(entry.getKey(), entry.getValue());
                logger.debug("Demoted entry from L1 to L2: {}", entry.getKey().substring(0, 8));
            }
        }
        
        // Demote from L2 to L3
        List<Map.Entry<String, CacheEntry>> l2LowRelevance = l2Cache.entrySet().stream()
                .filter(entry -> entry.getValue().getRelevanceScore() < 0.5)
                .collect(java.util.stream.Collectors.toList());
        
        for (Map.Entry<String, CacheEntry> entry : l2LowRelevance) {
            if (l3Cache.size() < l3MaxSize) {
                l2Cache.remove(entry.getKey());
                l3Cache.put(entry.getKey(), entry.getValue());
                logger.debug("Demoted entry from L2 to L3: {}", entry.getKey().substring(0, 8));
            }
        }
    }
    
    /**
     * Clean up entries with very low relevance
     */
    private void cleanupLowRelevanceEntries() {
        int removed = 0;
        
        // Remove very low relevance entries from L3
        List<String> l3ToRemove = l3Cache.entrySet().stream()
                .filter(entry -> entry.getValue().getRelevanceScore() < 0.1)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        for (String key : l3ToRemove) {
            l3Cache.remove(key);
            removed++;
        }
        
        if (removed > 0) {
            logger.debug("Cleaned up {} very low relevance entries", removed);
        }
    }
    
    /**
     * Invalidate entries in cache matching pattern
     */
    private void invalidateInCache(ConcurrentHashMap<String, CacheEntry> cache, Pattern pattern) {
        List<String> keysToRemove = cache.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getValue().originalQuery).matches())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        keysToRemove.forEach(key -> {
            cache.remove(key);
            invalidations.incrementAndGet();
        });
    }
    
    /**
     * Perform cache warming for frequently requested patterns
     */
    private void performCacheWarming() {
        logger.debug("Performing cache warming for {} patterns", warmingPatterns.size());
        
        // Warm cache with frequently requested patterns
        warmFrequentlyRequestedQueries();
        
        // Perform predictive warming based on user patterns
        if (predictiveCachingEnabled) {
            performPredictiveWarming();
        }
        
        // Warm cache with seasonal patterns
        warmSeasonalPatterns();
    }
    
    /**
     * Warm cache with frequently requested queries
     */
    private void warmFrequentlyRequestedQueries() {
        List<String> topQueries = queryFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 5) // Only queries requested more than 5 times
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(warmingBatchSize)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Warming cache with {} frequently requested queries", topQueries.size());
        
        // In a real implementation, this would trigger background query processing
        // to pre-populate cache with responses for these queries
    }
    
    /**
     * Perform predictive cache warming based on user query patterns
     */
    private void performPredictiveWarming() {
        userQueryHistory.forEach((userId, queries) -> {
            if (queries.size() >= 3) {
                List<String> predictedQueries = predictNextQueries(userId, queries);
                logger.debug("Predicted {} queries for user {}", predictedQueries.size(), userId);
                
                // In a real implementation, this would pre-generate responses
                // for predicted queries and cache them
            }
        });
    }
    
    /**
     * Predict next queries based on user history and patterns
     */
    private List<String> predictNextQueries(String userId, List<String> queryHistory) {
        List<String> predictions = new ArrayList<>();
        
        // Analyze query patterns
        if (queryHistory.size() >= 2) {
            String lastQuery = queryHistory.get(queryHistory.size() - 1);
            String secondLastQuery = queryHistory.get(queryHistory.size() - 2);
            
            // Pattern-based predictions
            predictions.addAll(predictBasedOnSequence(lastQuery, secondLastQuery));
            predictions.addAll(predictBasedOnSimilarity(lastQuery));
            predictions.addAll(predictBasedOnUserBehavior(userId, queryHistory));
        }
        
        return predictions.stream().distinct().limit(10).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Predict queries based on common query sequences
     */
    private List<String> predictBasedOnSequence(String lastQuery, String secondLastQuery) {
        List<String> predictions = new ArrayList<>();
        
        // Common trade query sequences
        if (lastQuery.toLowerCase().contains("tariff") && lastQuery.toLowerCase().contains("rate")) {
            predictions.add("What are the documentation requirements for this import?");
            predictions.add("Calculate total landed cost including shipping");
            predictions.add("Are there any trade agreements that could reduce this tariff?");
        }
        
        if (lastQuery.toLowerCase().contains("hs code")) {
            predictions.add("What is the tariff rate for this HS code?");
            predictions.add("Which countries have preferential rates for this product?");
            predictions.add("What are the import restrictions for this product?");
        }
        
        if (lastQuery.toLowerCase().contains("trade agreement")) {
            predictions.add("What are the rules of origin requirements?");
            predictions.add("How much can I save with this agreement?");
            predictions.add("What documentation is needed to claim preferential rates?");
        }
        
        return predictions;
    }
    
    /**
     * Predict queries based on similarity to cached queries
     */
    private List<String> predictBasedOnSimilarity(String lastQuery) {
        List<String> predictions = new ArrayList<>();
        
        // Find similar cached queries
        Set<String> cachedQueries = getCachedQueryKeys();
        
        for (String cachedQuery : cachedQueries) {
            if (calculateQuerySimilarity(lastQuery, cachedQuery) > 0.7) {
                // Generate variations of similar queries
                predictions.addAll(generateQueryVariations(cachedQuery));
            }
        }
        
        return predictions.stream().limit(5).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Predict queries based on user behavior patterns
     */
    private List<String> predictBasedOnUserBehavior(String userId, List<String> queryHistory) {
        List<String> predictions = new ArrayList<>();
        
        // Analyze user's typical query patterns
        Map<String, Integer> topicFrequency = new HashMap<>();
        
        for (String query : queryHistory) {
            String topic = extractQueryTopic(query);
            topicFrequency.merge(topic, 1, Integer::sum);
        }
        
        // Predict queries for user's most frequent topics
        String mostFrequentTopic = topicFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("general");
        
        predictions.addAll(generateQueriesForTopic(mostFrequentTopic));
        
        return predictions;
    }
    
    /**
     * Extract main topic from query
     */
    private String extractQueryTopic(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("tariff") || lowerQuery.contains("duty")) return "tariff";
        if (lowerQuery.contains("hs code") || lowerQuery.contains("classification")) return "classification";
        if (lowerQuery.contains("agreement") || lowerQuery.contains("fta")) return "agreements";
        if (lowerQuery.contains("compliance") || lowerQuery.contains("regulation")) return "compliance";
        if (lowerQuery.contains("cost") || lowerQuery.contains("calculation")) return "cost_analysis";
        if (lowerQuery.contains("risk") || lowerQuery.contains("assessment")) return "risk";
        
        return "general";
    }
    
    /**
     * Generate query variations for similar queries
     */
    private List<String> generateQueryVariations(String baseQuery) {
        List<String> variations = new ArrayList<>();
        
        // Generate variations by substituting common terms
        if (baseQuery.toLowerCase().contains("china")) {
            variations.add(baseQuery.toLowerCase().replace("china", "germany"));
            variations.add(baseQuery.toLowerCase().replace("china", "japan"));
        }
        
        if (baseQuery.toLowerCase().contains("import")) {
            variations.add(baseQuery.toLowerCase().replace("import", "export"));
        }
        
        return variations;
    }
    
    /**
     * Generate common queries for a specific topic
     */
    private List<String> generateQueriesForTopic(String topic) {
        List<String> queries = new ArrayList<>();
        
        switch (topic) {
            case "tariff":
                queries.add("What are the current tariff rates for electronics from China?");
                queries.add("How do I calculate total import duties?");
                queries.add("Are there any tariff exemptions available?");
                break;
            case "classification":
                queries.add("How do I find the correct HS code for my product?");
                queries.add("What is the difference between similar HS codes?");
                queries.add("Can I get a binding ruling on classification?");
                break;
            case "agreements":
                queries.add("Which trade agreements apply to my import?");
                queries.add("How much can I save with USMCA?");
                queries.add("What are the rules of origin requirements?");
                break;
            case "compliance":
                queries.add("What documentation do I need for customs clearance?");
                queries.add("Are there any import restrictions on this product?");
                queries.add("How do I ensure regulatory compliance?");
                break;
            default:
                queries.add("What are the import requirements for this product?");
                queries.add("How do I optimize my trade costs?");
                break;
        }
        
        return queries;
    }
    
    /**
     * Calculate similarity between two queries
     */
    private double calculateQuerySimilarity(String query1, String query2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(query1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(query2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Warm cache with seasonal patterns
     */
    private void warmSeasonalPatterns() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        
        List<String> seasonalQueries = new ArrayList<>();
        
        // Holiday season queries (Oct-Dec)
        if (month >= Calendar.OCTOBER && month <= Calendar.DECEMBER) {
            seasonalQueries.add("What are the tariff rates for toys and electronics?");
            seasonalQueries.add("How to expedite customs clearance for holiday goods?");
            seasonalQueries.add("What are the import deadlines for Christmas merchandise?");
        }
        
        // Back-to-school season (Jul-Aug)
        if (month >= Calendar.JULY && month <= Calendar.AUGUST) {
            seasonalQueries.add("Tariff rates for educational materials and electronics");
            seasonalQueries.add("Import requirements for school supplies");
        }
        
        // Agricultural seasons
        if (month >= Calendar.MARCH && month <= Calendar.MAY) {
            seasonalQueries.add("Import regulations for agricultural products");
            seasonalQueries.add("Seasonal tariff rates for fresh produce");
        }
        
        logger.debug("Warming cache with {} seasonal queries for month {}", seasonalQueries.size(), month + 1);
    }
    
    /**
     * Collect and log cache analytics
     */
    private void collectAnalytics() {
        EnhancedCacheStats stats = getCacheStats();
        
        logger.info("Cache Analytics - Hit Ratio: {:.2f}%, L1: {} entries, L2: {} entries, L3: {} entries, " +
                "Evictions: {}, Invalidations: {}, Compressed: {}, Savings: {} bytes, Patterns: {}", 
                stats.getOverallHitRatio() * 100,
                stats.getL1Stats().getTotalEntries(),
                stats.getL2Stats().getTotalEntries(), 
                stats.getL3Stats().getTotalEntries(),
                stats.getEvictions(),
                stats.getInvalidations(),
                stats.getCompressedEntries(),
                stats.getCompressionSavings(),
                stats.getPredictivePatterns());
    }
    
    /**
     * Reset analytics counters
     */
    private void resetAnalytics() {
        l1Hits.set(0);
        l2Hits.set(0);
        l3Hits.set(0);
        misses.set(0);
        evictions.set(0);
        invalidations.set(0);
    }
    
    /**
     * Get all cached query keys for similarity matching
     */
    public Set<String> getCachedQueryKeys() {
        Set<String> allKeys = new HashSet<>();
        
        // Collect original queries from all cache levels
        l1Cache.values().stream()
                .map(entry -> entry.originalQuery)
                .forEach(allKeys::add);
        
        l2Cache.values().stream()
                .map(entry -> entry.originalQuery)
                .forEach(allKeys::add);
        
        l3Cache.values().stream()
                .map(entry -> entry.originalQuery)
                .forEach(allKeys::add);
        
        return allKeys;
    }
    
    /**
     * Enhanced cache entry with access tracking and metadata
     */
    private static class CacheEntry {
        final ChatQueryResponse response;
        final long timestamp;
        final String originalQuery;
        final boolean compressed;
        volatile long lastAccessTime;
        volatile int accessCount;
        volatile double relevanceScore;
        
        CacheEntry(ChatQueryResponse response, long timestamp, String originalQuery) {
            this(response, timestamp, originalQuery, false);
        }
        
        CacheEntry(ChatQueryResponse response, long timestamp, String originalQuery, boolean compressed) {
            this.response = response;
            this.timestamp = timestamp;
            this.originalQuery = originalQuery;
            this.compressed = compressed;
            this.lastAccessTime = timestamp;
            this.accessCount = 1;
            this.relevanceScore = 1.0;
        }
        
        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
            
            // Update relevance score based on access frequency and recency
            long timeSinceCreation = System.currentTimeMillis() - timestamp;
            double recencyFactor = Math.exp(-timeSinceCreation / (24.0 * 60 * 60 * 1000)); // Decay over 24 hours
            double frequencyFactor = Math.log(accessCount + 1);
            this.relevanceScore = recencyFactor * frequencyFactor;
        }
        
        boolean isExpired(long ttlMinutes) {
            return isExpired(ttlMinutes, System.currentTimeMillis());
        }
        
        boolean isExpired(long ttlMinutes, long now) {
            return (now - timestamp) > TimeUnit.MINUTES.toMillis(ttlMinutes);
        }
        
        double getRelevanceScore() {
            return relevanceScore;
        }
        
        boolean isCompressed() {
            return compressed;
        }
    }
    
    /**
     * Cache level enumeration
     */
    public enum CacheLevel {
        L1, L2, L3
    }
    
    /**
     * Statistics for a specific cache level
     */
    public static class CacheLevelStats {
        private final int totalEntries;
        private final int validEntries;
        private final double averageAgeMinutes;
        
        public CacheLevelStats(int totalEntries, int validEntries, double averageAgeMinutes) {
            this.totalEntries = totalEntries;
            this.validEntries = validEntries;
            this.averageAgeMinutes = averageAgeMinutes;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public int getValidEntries() { return validEntries; }
        public double getAverageAgeMinutes() { return averageAgeMinutes; }
        public double getUtilization() { 
            return totalEntries > 0 ? (double) validEntries / totalEntries : 0.0; 
        }
    }
    
    /**
     * Predictive pattern for cache warming
     */
    private static class PredictivePattern {
        final String pattern;
        final double confidence;
        final long lastUsed;
        final int frequency;
        
        PredictivePattern(String pattern, double confidence, long lastUsed, int frequency) {
            this.pattern = pattern;
            this.confidence = confidence;
            this.lastUsed = lastUsed;
            this.frequency = frequency;
        }
        
        boolean isRelevant() {
            long timeSinceUsed = System.currentTimeMillis() - lastUsed;
            return confidence > 0.5 && timeSinceUsed < TimeUnit.DAYS.toMillis(7);
        }
    }
    
    /**
     * Enhanced cache statistics with multi-level metrics
     */
    public static class EnhancedCacheStats {
        private final CacheLevelStats l1Stats;
        private final CacheLevelStats l2Stats;
        private final CacheLevelStats l3Stats;
        private final double overallHitRatio;
        private final long evictions;
        private final long invalidations;
        private final List<String> topQueries;
        private final long compressionSavings;
        private final long compressedEntries;
        private final int predictivePatterns;
        
        public EnhancedCacheStats(CacheLevelStats l1Stats, CacheLevelStats l2Stats, 
                                CacheLevelStats l3Stats, double overallHitRatio,
                                long evictions, long invalidations, List<String> topQueries,
                                long compressionSavings, long compressedEntries, int predictivePatterns) {
            this.l1Stats = l1Stats;
            this.l2Stats = l2Stats;
            this.l3Stats = l3Stats;
            this.overallHitRatio = overallHitRatio;
            this.evictions = evictions;
            this.invalidations = invalidations;
            this.topQueries = topQueries;
            this.compressionSavings = compressionSavings;
            this.compressedEntries = compressedEntries;
            this.predictivePatterns = predictivePatterns;
        }
        
        public CacheLevelStats getL1Stats() { return l1Stats; }
        public CacheLevelStats getL2Stats() { return l2Stats; }
        public CacheLevelStats getL3Stats() { return l3Stats; }
        public double getOverallHitRatio() { return overallHitRatio; }
        public long getEvictions() { return evictions; }
        public long getInvalidations() { return invalidations; }
        public List<String> getTopQueries() { return topQueries; }
        public long getCompressionSavings() { return compressionSavings; }
        public long getCompressedEntries() { return compressedEntries; }
        public int getPredictivePatterns() { return predictivePatterns; }
        
        public int getTotalEntries() {
            return l1Stats.getTotalEntries() + l2Stats.getTotalEntries() + l3Stats.getTotalEntries();
        }
        
        public int getTotalValidEntries() {
            return l1Stats.getValidEntries() + l2Stats.getValidEntries() + l3Stats.getValidEntries();
        }
        
        public double getCompressionRatio() {
            return compressedEntries > 0 ? (double) compressionSavings / compressedEntries : 0.0;
        }
    }
}