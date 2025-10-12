package com.tariffsheriff.backend.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for collecting and analyzing business intelligence metrics
 * for enhanced AI capabilities usage
 */
@Service
public class BusinessIntelligenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessIntelligenceService.class);
    
    @Autowired
    private AiMetricsService aiMetricsService;
    
    // Usage tracking
    private final Map<String, UserUsageStats> userUsageStats = new ConcurrentHashMap<>();
    private final Map<String, FeatureUsageStats> featureUsageStats = new ConcurrentHashMap<>();
    private final Map<String, QueryPatternStats> queryPatternStats = new ConcurrentHashMap<>();
    
    // Time-based analytics
    private final Map<String, List<TimeSeriesDataPoint>> timeSeriesData = new ConcurrentHashMap<>();
    
    public static class UserUsageStats {
        private String userId;
        private long totalQueries;
        private long successfulQueries;
        private long failedQueries;
        private double averageComplexity;
        private Set<String> featuresUsed;
        private Map<String, Long> queryTypes;
        private Instant firstSeen;
        private Instant lastSeen;
        private double totalProcessingTime;
        
        public UserUsageStats(String userId) {
            this.userId = userId;
            this.totalQueries = 0;
            this.successfulQueries = 0;
            this.failedQueries = 0;
            this.averageComplexity = 0.0;
            this.featuresUsed = new HashSet<>();
            this.queryTypes = new ConcurrentHashMap<>();
            this.firstSeen = Instant.now();
            this.lastSeen = Instant.now();
            this.totalProcessingTime = 0.0;
        }
        
        // Getters and setters
        public String getUserId() { return userId; }
        public long getTotalQueries() { return totalQueries; }
        public void incrementTotalQueries() { this.totalQueries++; }
        public long getSuccessfulQueries() { return successfulQueries; }
        public void incrementSuccessfulQueries() { this.successfulQueries++; }
        public long getFailedQueries() { return failedQueries; }
        public void incrementFailedQueries() { this.failedQueries++; }
        public double getAverageComplexity() { return averageComplexity; }
        public void updateAverageComplexity(double complexity) {
            this.averageComplexity = (this.averageComplexity * (totalQueries - 1) + complexity) / totalQueries;
        }
        public Set<String> getFeaturesUsed() { return featuresUsed; }
        public void addFeatureUsed(String feature) { this.featuresUsed.add(feature); }
        public Map<String, Long> getQueryTypes() { return queryTypes; }
        public void incrementQueryType(String type) {
            this.queryTypes.merge(type, 1L, Long::sum);
        }
        public Instant getFirstSeen() { return firstSeen; }
        public Instant getLastSeen() { return lastSeen; }
        public void updateLastSeen() { this.lastSeen = Instant.now(); }
        public double getTotalProcessingTime() { return totalProcessingTime; }
        public void addProcessingTime(double time) { this.totalProcessingTime += time; }
        
        public double getSuccessRate() {
            return totalQueries > 0 ? (double) successfulQueries / totalQueries : 0.0;
        }
        
        public double getAverageProcessingTime() {
            return totalQueries > 0 ? totalProcessingTime / totalQueries : 0.0;
        }
    }
    
    public static class FeatureUsageStats {
        private String featureName;
        private long totalUsage;
        private long uniqueUsers;
        private Set<String> users;
        private double averageSuccessRate;
        private Map<String, Long> usageByHour;
        private Instant firstUsed;
        private Instant lastUsed;
        
        public FeatureUsageStats(String featureName) {
            this.featureName = featureName;
            this.totalUsage = 0;
            this.uniqueUsers = 0;
            this.users = new HashSet<>();
            this.averageSuccessRate = 0.0;
            this.usageByHour = new ConcurrentHashMap<>();
            this.firstUsed = Instant.now();
            this.lastUsed = Instant.now();
        }
        
        // Getters and setters
        public String getFeatureName() { return featureName; }
        public long getTotalUsage() { return totalUsage; }
        public void incrementTotalUsage() { this.totalUsage++; }
        public long getUniqueUsers() { return uniqueUsers; }
        public Set<String> getUsers() { return users; }
        public void addUser(String userId) {
            if (this.users.add(userId)) {
                this.uniqueUsers++;
            }
        }
        public double getAverageSuccessRate() { return averageSuccessRate; }
        public void updateSuccessRate(double rate) { this.averageSuccessRate = rate; }
        public Map<String, Long> getUsageByHour() { return usageByHour; }
        public void incrementUsageByHour(String hour) {
            this.usageByHour.merge(hour, 1L, Long::sum);
        }
        public Instant getFirstUsed() { return firstUsed; }
        public Instant getLastUsed() { return lastUsed; }
        public void updateLastUsed() { this.lastUsed = Instant.now(); }
    }
    
    public static class QueryPatternStats {
        private String pattern;
        private long frequency;
        private double averageComplexity;
        private double averageProcessingTime;
        private Set<String> users;
        private Map<String, Long> outcomes;
        
        public QueryPatternStats(String pattern) {
            this.pattern = pattern;
            this.frequency = 0;
            this.averageComplexity = 0.0;
            this.averageProcessingTime = 0.0;
            this.users = new HashSet<>();
            this.outcomes = new ConcurrentHashMap<>();
        }
        
        // Getters and setters
        public String getPattern() { return pattern; }
        public long getFrequency() { return frequency; }
        public void incrementFrequency() { this.frequency++; }
        public double getAverageComplexity() { return averageComplexity; }
        public void updateAverageComplexity(double complexity) {
            this.averageComplexity = (this.averageComplexity * (frequency - 1) + complexity) / frequency;
        }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public void updateAverageProcessingTime(double time) {
            this.averageProcessingTime = (this.averageProcessingTime * (frequency - 1) + time) / frequency;
        }
        public Set<String> getUsers() { return users; }
        public void addUser(String userId) { this.users.add(userId); }
        public Map<String, Long> getOutcomes() { return outcomes; }
        public void incrementOutcome(String outcome) {
            this.outcomes.merge(outcome, 1L, Long::sum);
        }
    }
    
    public static class TimeSeriesDataPoint {
        private Instant timestamp;
        private String metric;
        private double value;
        private Map<String, String> tags;
        
        public TimeSeriesDataPoint(String metric, double value) {
            this.timestamp = Instant.now();
            this.metric = metric;
            this.value = value;
            this.tags = new HashMap<>();
        }
        
        // Getters and setters
        public Instant getTimestamp() { return timestamp; }
        public String getMetric() { return metric; }
        public double getValue() { return value; }
        public Map<String, String> getTags() { return tags; }
        public void addTag(String key, String value) { this.tags.put(key, value); }
    }
    
    /**
     * Record user query activity
     */
    public void recordUserQuery(String userId, String queryType, double complexity, 
                               double processingTime, boolean success, Set<String> featuresUsed) {
        
        // Update user stats
        UserUsageStats userStats = userUsageStats.computeIfAbsent(userId, UserUsageStats::new);
        userStats.incrementTotalQueries();
        userStats.updateLastSeen();
        userStats.updateAverageComplexity(complexity);
        userStats.addProcessingTime(processingTime);
        userStats.incrementQueryType(queryType);
        
        if (success) {
            userStats.incrementSuccessfulQueries();
        } else {
            userStats.incrementFailedQueries();
        }
        
        // Update feature usage
        for (String feature : featuresUsed) {
            userStats.addFeatureUsed(feature);
            
            FeatureUsageStats featureStats = featureUsageStats.computeIfAbsent(feature, FeatureUsageStats::new);
            featureStats.incrementTotalUsage();
            featureStats.addUser(userId);
            featureStats.updateLastUsed();
            
            // Track usage by hour
            String hour = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).getHour() + ":00";
            featureStats.incrementUsageByHour(hour);
        }
        
        // Update query patterns
        String pattern = extractQueryPattern(queryType, complexity);
        QueryPatternStats patternStats = queryPatternStats.computeIfAbsent(pattern, QueryPatternStats::new);
        patternStats.incrementFrequency();
        patternStats.updateAverageComplexity(complexity);
        patternStats.updateAverageProcessingTime(processingTime);
        patternStats.addUser(userId);
        patternStats.incrementOutcome(success ? "success" : "failure");
        
        // Record time series data
        recordTimeSeriesData("user.queries", 1.0, Map.of("user_id", userId, "success", String.valueOf(success)));
        recordTimeSeriesData("query.complexity", complexity, Map.of("user_id", userId));
        recordTimeSeriesData("query.processing_time", processingTime, Map.of("user_id", userId));
        
        logger.debug("Recorded user query: userId={}, type={}, complexity={}, time={}, success={}", 
            userId, queryType, complexity, processingTime, success);
    }
    
    /**
     * Get user usage analytics
     */
    public Map<String, Object> getUserAnalytics(String userId) {
        UserUsageStats stats = userUsageStats.get(userId);
        if (stats == null) {
            return Map.of("error", "User not found");
        }
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("userId", stats.getUserId());
        analytics.put("totalQueries", stats.getTotalQueries());
        analytics.put("successfulQueries", stats.getSuccessfulQueries());
        analytics.put("failedQueries", stats.getFailedQueries());
        analytics.put("successRate", stats.getSuccessRate());
        analytics.put("averageComplexity", stats.getAverageComplexity());
        analytics.put("averageProcessingTime", stats.getAverageProcessingTime());
        analytics.put("featuresUsed", stats.getFeaturesUsed());
        analytics.put("queryTypes", stats.getQueryTypes());
        analytics.put("firstSeen", stats.getFirstSeen());
        analytics.put("lastSeen", stats.getLastSeen());
        
        return analytics;
    }
    
    /**
     * Get feature usage analytics
     */
    public Map<String, Object> getFeatureAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        for (FeatureUsageStats stats : featureUsageStats.values()) {
            Map<String, Object> featureData = new HashMap<>();
            featureData.put("totalUsage", stats.getTotalUsage());
            featureData.put("uniqueUsers", stats.getUniqueUsers());
            featureData.put("averageSuccessRate", stats.getAverageSuccessRate());
            featureData.put("usageByHour", stats.getUsageByHour());
            featureData.put("firstUsed", stats.getFirstUsed());
            featureData.put("lastUsed", stats.getLastUsed());
            
            analytics.put(stats.getFeatureName(), featureData);
        }
        
        return analytics;
    }
    
    /**
     * Get query pattern analytics
     */
    public Map<String, Object> getQueryPatternAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        // Sort patterns by frequency
        List<QueryPatternStats> sortedPatterns = queryPatternStats.values().stream()
            .sorted((a, b) -> Long.compare(b.getFrequency(), a.getFrequency()))
            .collect(Collectors.toList());
        
        for (QueryPatternStats stats : sortedPatterns) {
            Map<String, Object> patternData = new HashMap<>();
            patternData.put("frequency", stats.getFrequency());
            patternData.put("averageComplexity", stats.getAverageComplexity());
            patternData.put("averageProcessingTime", stats.getAverageProcessingTime());
            patternData.put("uniqueUsers", stats.getUsers().size());
            patternData.put("outcomes", stats.getOutcomes());
            
            analytics.put(stats.getPattern(), patternData);
        }
        
        return analytics;
    }
    
    /**
     * Get comprehensive business intelligence report
     */
    public Map<String, Object> getBusinessIntelligenceReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Overall statistics
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalUsers", userUsageStats.size());
        overview.put("totalQueries", userUsageStats.values().stream().mapToLong(UserUsageStats::getTotalQueries).sum());
        overview.put("totalFeatures", featureUsageStats.size());
        overview.put("totalPatterns", queryPatternStats.size());
        
        // Calculate overall success rate
        long totalSuccessful = userUsageStats.values().stream().mapToLong(UserUsageStats::getSuccessfulQueries).sum();
        long totalQueries = userUsageStats.values().stream().mapToLong(UserUsageStats::getTotalQueries).sum();
        overview.put("overallSuccessRate", totalQueries > 0 ? (double) totalSuccessful / totalQueries : 0.0);
        
        // Calculate average complexity
        double avgComplexity = userUsageStats.values().stream()
            .mapToDouble(UserUsageStats::getAverageComplexity)
            .average()
            .orElse(0.0);
        overview.put("averageComplexity", avgComplexity);
        
        report.put("overview", overview);
        
        // Top users by activity
        List<UserUsageStats> topUsers = userUsageStats.values().stream()
            .sorted((a, b) -> Long.compare(b.getTotalQueries(), a.getTotalQueries()))
            .limit(10)
            .collect(Collectors.toList());
        
        report.put("topUsers", topUsers.stream().map(stats -> Map.of(
            "userId", stats.getUserId(),
            "totalQueries", stats.getTotalQueries(),
            "successRate", stats.getSuccessRate()
        )).collect(Collectors.toList()));
        
        // Most used features
        List<FeatureUsageStats> topFeatures = featureUsageStats.values().stream()
            .sorted((a, b) -> Long.compare(b.getTotalUsage(), a.getTotalUsage()))
            .limit(10)
            .collect(Collectors.toList());
        
        report.put("topFeatures", topFeatures.stream().map(stats -> Map.of(
            "feature", stats.getFeatureName(),
            "totalUsage", stats.getTotalUsage(),
            "uniqueUsers", stats.getUniqueUsers()
        )).collect(Collectors.toList()));
        
        // Include detailed analytics
        report.put("featureAnalytics", getFeatureAnalytics());
        report.put("queryPatternAnalytics", getQueryPatternAnalytics());
        
        return report;
    }
    
    /**
     * Record time series data point
     */
    private void recordTimeSeriesData(String metric, double value, Map<String, String> tags) {
        TimeSeriesDataPoint dataPoint = new TimeSeriesDataPoint(metric, value);
        dataPoint.getTags().putAll(tags);
        
        timeSeriesData.computeIfAbsent(metric, k -> new ArrayList<>()).add(dataPoint);
        
        // Keep only last 1000 data points per metric to prevent memory issues
        List<TimeSeriesDataPoint> dataPoints = timeSeriesData.get(metric);
        if (dataPoints.size() > 1000) {
            dataPoints.remove(0);
        }
    }
    
    /**
     * Extract query pattern from query type and complexity
     */
    private String extractQueryPattern(String queryType, double complexity) {
        String complexityLevel;
        if (complexity < 0.3) {
            complexityLevel = "simple";
        } else if (complexity < 0.7) {
            complexityLevel = "medium";
        } else {
            complexityLevel = "complex";
        }
        
        return queryType + "_" + complexityLevel;
    }
    
    /**
     * Scheduled task to update feature success rates
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updateFeatureSuccessRates() {
        for (FeatureUsageStats featureStats : featureUsageStats.values()) {
            // Calculate success rate for this feature based on user stats
            double totalQueries = 0;
            double successfulQueries = 0;
            
            for (String userId : featureStats.getUsers()) {
                UserUsageStats userStats = userUsageStats.get(userId);
                if (userStats != null && userStats.getFeaturesUsed().contains(featureStats.getFeatureName())) {
                    totalQueries += userStats.getTotalQueries();
                    successfulQueries += userStats.getSuccessfulQueries();
                }
            }
            
            double successRate = totalQueries > 0 ? successfulQueries / totalQueries : 0.0;
            featureStats.updateSuccessRate(successRate);
        }
        
        logger.debug("Updated feature success rates for {} features", featureUsageStats.size());
    }
    
    /**
     * Get time series data for a metric
     */
    public List<TimeSeriesDataPoint> getTimeSeriesData(String metric, Instant since) {
        List<TimeSeriesDataPoint> allData = timeSeriesData.get(metric);
        if (allData == null) {
            return new ArrayList<>();
        }
        
        return allData.stream()
            .filter(point -> point.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
    }
    
    /**
     * Clear old analytics data
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldData() {
        Instant cutoff = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 days ago
        
        // Clean up time series data
        for (List<TimeSeriesDataPoint> dataPoints : timeSeriesData.values()) {
            dataPoints.removeIf(point -> point.getTimestamp().isBefore(cutoff));
        }
        
        logger.info("Cleaned up old analytics data older than 30 days");
    }
}