package com.tariffsheriff.backend.improvement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * Service for continuous improvement through A/B testing, user feedback analysis,
 * and automated optimization
 */
@Service
public class ContinuousImprovementService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContinuousImprovementService.class);
    
    @Value("${improvement.enabled:true}")
    private boolean improvementEnabled;
    
    @Value("${improvement.ab-testing.enabled:true}")
    private boolean abTestingEnabled;
    
    @Value("${improvement.feedback.enabled:true}")
    private boolean feedbackEnabled;
    
    @Value("${improvement.auto-optimization.enabled:true}")
    private boolean autoOptimizationEnabled;
    
    // A/B Testing
    private final Map<String, AbTest> activeTests = new ConcurrentHashMap<>();
    private final Map<String, List<AbTestResult>> testResults = new ConcurrentHashMap<>();
    
    // User Feedback
    private final Map<String, UserFeedback> feedbackData = new ConcurrentHashMap<>();
    private final Map<String, List<FeedbackAnalysis>> feedbackAnalyses = new ConcurrentHashMap<>();
    
    // Optimization
    private final Map<String, OptimizationRecommendation> optimizationRecommendations = new ConcurrentHashMap<>();
    private final Map<String, OptimizationResult> appliedOptimizations = new ConcurrentHashMap<>();
    
    public static class AbTest {
        private String testId;
        private String name;
        private String description;
        private String feature;
        private Map<String, Object> variantA; // Control
        private Map<String, Object> variantB; // Treatment
        private double trafficSplit; // Percentage for variant B (0.0 to 1.0)
        private String successMetric;
        private double minimumSampleSize;
        private double confidenceLevel;
        private Instant startTime;
        private Instant endTime;
        private boolean active;
        private String status; // "running", "completed", "paused", "cancelled"
        
        public AbTest(String testId, String name, String feature) {
            this.testId = testId;
            this.name = name;
            this.feature = feature;
            this.variantA = new HashMap<>();
            this.variantB = new HashMap<>();
            this.trafficSplit = 0.5; // 50/50 split by default
            this.minimumSampleSize = 1000;
            this.confidenceLevel = 0.95;
            this.startTime = Instant.now();
            this.active = false;
            this.status = "created";
        }
        
        // Getters and setters
        public String getTestId() { return testId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getFeature() { return feature; }
        public Map<String, Object> getVariantA() { return variantA; }
        public Map<String, Object> getVariantB() { return variantB; }
        public double getTrafficSplit() { return trafficSplit; }
        public void setTrafficSplit(double trafficSplit) { this.trafficSplit = trafficSplit; }
        public String getSuccessMetric() { return successMetric; }
        public void setSuccessMetric(String successMetric) { this.successMetric = successMetric; }
        public double getMinimumSampleSize() { return minimumSampleSize; }
        public void setMinimumSampleSize(double minimumSampleSize) { this.minimumSampleSize = minimumSampleSize; }
        public double getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class AbTestResult {
        private String testId;
        private String userId;
        private String variant; // "A" or "B"
        private String metric;
        private double value;
        private Instant timestamp;
        private Map<String, Object> metadata;
        
        public AbTestResult(String testId, String userId, String variant, String metric, double value) {
            this.testId = testId;
            this.userId = userId;
            this.variant = variant;
            this.metric = metric;
            this.value = value;
            this.timestamp = Instant.now();
            this.metadata = new HashMap<>();
        }
        
        // Getters and setters
        public String getTestId() { return testId; }
        public String getUserId() { return userId; }
        public String getVariant() { return variant; }
        public String getMetric() { return metric; }
        public double getValue() { return value; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    }
    
    public static class UserFeedback {
        private String feedbackId;
        private String userId;
        private String feature;
        private String queryId;
        private int rating; // 1-5 scale
        private String comment;
        private String category; // "accuracy", "speed", "usability", "other"
        private Instant timestamp;
        private Map<String, Object> context;
        
        public UserFeedback(String feedbackId, String userId, String feature) {
            this.feedbackId = feedbackId;
            this.userId = userId;
            this.feature = feature;
            this.timestamp = Instant.now();
            this.context = new HashMap<>();
        }
        
        // Getters and setters
        public String getFeedbackId() { return feedbackId; }
        public String getUserId() { return userId; }
        public String getFeature() { return feature; }
        public String getQueryId() { return queryId; }
        public void setQueryId(String queryId) { this.queryId = queryId; }
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getContext() { return context; }
        public void addContext(String key, Object value) { this.context.put(key, value); }
    }
    
    public static class FeedbackAnalysis {
        private String feature;
        private double averageRating;
        private int totalFeedback;
        private Map<String, Integer> ratingDistribution;
        private Map<String, Integer> categoryDistribution;
        private List<String> commonIssues;
        private List<String> positiveComments;
        private double sentimentScore;
        private Instant analyzedAt;
        
        public FeedbackAnalysis(String feature) {
            this.feature = feature;
            this.ratingDistribution = new HashMap<>();
            this.categoryDistribution = new HashMap<>();
            this.commonIssues = new ArrayList<>();
            this.positiveComments = new ArrayList<>();
            this.analyzedAt = Instant.now();
        }
        
        // Getters and setters
        public String getFeature() { return feature; }
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        public int getTotalFeedback() { return totalFeedback; }
        public void setTotalFeedback(int totalFeedback) { this.totalFeedback = totalFeedback; }
        public Map<String, Integer> getRatingDistribution() { return ratingDistribution; }
        public Map<String, Integer> getCategoryDistribution() { return categoryDistribution; }
        public List<String> getCommonIssues() { return commonIssues; }
        public List<String> getPositiveComments() { return positiveComments; }
        public double getSentimentScore() { return sentimentScore; }
        public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }
        public Instant getAnalyzedAt() { return analyzedAt; }
    }
    
    public static class OptimizationRecommendation {
        private String recommendationId;
        private String feature;
        private String type; // "performance", "accuracy", "usability"
        private String description;
        private Map<String, Object> currentConfig;
        private Map<String, Object> recommendedConfig;
        private double expectedImprovement;
        private String impactLevel; // "low", "medium", "high"
        private String effort; // "low", "medium", "high"
        private Instant createdAt;
        private boolean applied;
        private String status; // "pending", "approved", "applied", "rejected"
        
        public OptimizationRecommendation(String recommendationId, String feature, String type) {
            this.recommendationId = recommendationId;
            this.feature = feature;
            this.type = type;
            this.currentConfig = new HashMap<>();
            this.recommendedConfig = new HashMap<>();
            this.createdAt = Instant.now();
            this.applied = false;
            this.status = "pending";
        }
        
        // Getters and setters
        public String getRecommendationId() { return recommendationId; }
        public String getFeature() { return feature; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getCurrentConfig() { return currentConfig; }
        public Map<String, Object> getRecommendedConfig() { return recommendedConfig; }
        public double getExpectedImprovement() { return expectedImprovement; }
        public void setExpectedImprovement(double expectedImprovement) { this.expectedImprovement = expectedImprovement; }
        public String getImpactLevel() { return impactLevel; }
        public void setImpactLevel(String impactLevel) { this.impactLevel = impactLevel; }
        public String getEffort() { return effort; }
        public void setEffort(String effort) { this.effort = effort; }
        public Instant getCreatedAt() { return createdAt; }
        public boolean isApplied() { return applied; }
        public void setApplied(boolean applied) { this.applied = applied; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class OptimizationResult {
        private String recommendationId;
        private String feature;
        private Map<String, Object> appliedConfig;
        private double actualImprovement;
        private String outcome; // "success", "failure", "partial"
        private Instant appliedAt;
        private Map<String, Double> beforeMetrics;
        private Map<String, Double> afterMetrics;
        
        public OptimizationResult(String recommendationId, String feature) {
            this.recommendationId = recommendationId;
            this.feature = feature;
            this.appliedConfig = new HashMap<>();
            this.appliedAt = Instant.now();
            this.beforeMetrics = new HashMap<>();
            this.afterMetrics = new HashMap<>();
        }
        
        // Getters and setters
        public String getRecommendationId() { return recommendationId; }
        public String getFeature() { return feature; }
        public Map<String, Object> getAppliedConfig() { return appliedConfig; }
        public double getActualImprovement() { return actualImprovement; }
        public void setActualImprovement(double actualImprovement) { this.actualImprovement = actualImprovement; }
        public String getOutcome() { return outcome; }
        public void setOutcome(String outcome) { this.outcome = outcome; }
        public Instant getAppliedAt() { return appliedAt; }
        public Map<String, Double> getBeforeMetrics() { return beforeMetrics; }
        public Map<String, Double> getAfterMetrics() { return afterMetrics; }
    }
    
    /**
     * Create A/B test
     */
    public AbTest createAbTest(String name, String feature, String description) {
        String testId = "test-" + System.currentTimeMillis();
        AbTest test = new AbTest(testId, name, feature);
        test.setDescription(description);
        
        activeTests.put(testId, test);
        testResults.put(testId, new ArrayList<>());
        
        logger.info("Created A/B test: {} for feature: {}", testId, feature);
        return test;
    }
    
    /**
     * Start A/B test
     */
    public boolean startAbTest(String testId) {
        AbTest test = activeTests.get(testId);
        if (test == null) {
            logger.error("A/B test not found: {}", testId);
            return false;
        }
        
        test.setActive(true);
        test.setStatus("running");
        test.setStartTime(Instant.now());
        
        logger.info("Started A/B test: {}", testId);
        return true;
    }
    
    /**
     * Stop A/B test
     */
    public boolean stopAbTest(String testId) {
        AbTest test = activeTests.get(testId);
        if (test == null) {
            logger.error("A/B test not found: {}", testId);
            return false;
        }
        
        test.setActive(false);
        test.setStatus("completed");
        test.setEndTime(Instant.now());
        
        logger.info("Stopped A/B test: {}", testId);
        return true;
    }
    
    /**
     * Assign user to A/B test variant
     */
    public String assignUserToVariant(String testId, String userId) {
        AbTest test = activeTests.get(testId);
        if (test == null || !test.isActive()) {
            return "A"; // Default to control
        }
        
        // Use consistent hashing to ensure same user always gets same variant
        int hash = Math.abs(userId.hashCode());
        double userPercentile = (hash % 1000) / 1000.0;
        
        String variant = userPercentile < test.getTrafficSplit() ? "B" : "A";
        
        logger.debug("Assigned user {} to variant {} for test {}", userId, variant, testId);
        return variant;
    }
    
    /**
     * Record A/B test result
     */
    public void recordAbTestResult(String testId, String userId, String variant, String metric, double value) {
        if (!abTestingEnabled) {
            return;
        }
        
        AbTestResult result = new AbTestResult(testId, userId, variant, metric, value);
        testResults.computeIfAbsent(testId, k -> new ArrayList<>()).add(result);
        
        logger.debug("Recorded A/B test result: test={}, user={}, variant={}, metric={}, value={}", 
            testId, userId, variant, metric, value);
    }
    
    /**
     * Analyze A/B test results
     */
    public Map<String, Object> analyzeAbTestResults(String testId) {
        AbTest test = activeTests.get(testId);
        List<AbTestResult> results = testResults.get(testId);
        
        if (test == null || results == null || results.isEmpty()) {
            return Map.of("error", "No data available for test: " + testId);
        }
        
        // Group results by variant
        Map<String, List<AbTestResult>> variantResults = results.stream()
            .collect(Collectors.groupingBy(AbTestResult::getVariant));
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("testId", testId);
        analysis.put("testName", test.getName());
        analysis.put("feature", test.getFeature());
        analysis.put("status", test.getStatus());
        analysis.put("totalSamples", results.size());
        
        // Calculate statistics for each variant
        Map<String, Object> variantStats = new HashMap<>();
        for (String variant : variantResults.keySet()) {
            List<AbTestResult> variantData = variantResults.get(variant);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("sampleSize", variantData.size());
            
            // Calculate mean for success metric
            if (test.getSuccessMetric() != null) {
                List<Double> metricValues = variantData.stream()
                    .filter(r -> test.getSuccessMetric().equals(r.getMetric()))
                    .map(AbTestResult::getValue)
                    .collect(Collectors.toList());
                
                if (!metricValues.isEmpty()) {
                    double mean = metricValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double variance = metricValues.stream()
                        .mapToDouble(v -> Math.pow(v - mean, 2))
                        .average().orElse(0.0);
                    double stdDev = Math.sqrt(variance);
                    
                    stats.put("mean", mean);
                    stats.put("standardDeviation", stdDev);
                    stats.put("sampleCount", metricValues.size());
                }
            }
            
            variantStats.put("variant" + variant, stats);
        }
        
        analysis.put("variantStats", variantStats);
        
        // Calculate statistical significance if we have both variants
        if (variantResults.containsKey("A") && variantResults.containsKey("B")) {
            Map<String, Object> significance = calculateStatisticalSignificance(
                variantResults.get("A"), variantResults.get("B"), test.getSuccessMetric());
            analysis.put("significance", significance);
        }
        
        // Recommendations
        List<String> recommendations = generateAbTestRecommendations(analysis);
        analysis.put("recommendations", recommendations);
        
        return analysis;
    }
    
    /**
     * Calculate statistical significance between two variants
     */
    private Map<String, Object> calculateStatisticalSignificance(
            List<AbTestResult> variantA, List<AbTestResult> variantB, String metric) {
        
        Map<String, Object> significance = new HashMap<>();
        
        if (metric == null) {
            significance.put("error", "No success metric defined");
            return significance;
        }
        
        // Get metric values for each variant
        List<Double> valuesA = variantA.stream()
            .filter(r -> metric.equals(r.getMetric()))
            .map(AbTestResult::getValue)
            .collect(Collectors.toList());
        
        List<Double> valuesB = variantB.stream()
            .filter(r -> metric.equals(r.getMetric()))
            .map(AbTestResult::getValue)
            .collect(Collectors.toList());
        
        if (valuesA.isEmpty() || valuesB.isEmpty()) {
            significance.put("error", "Insufficient data for significance testing");
            return significance;
        }
        
        // Calculate means and standard deviations
        double meanA = valuesA.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanB = valuesB.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double varianceA = valuesA.stream().mapToDouble(v -> Math.pow(v - meanA, 2)).average().orElse(0.0);
        double varianceB = valuesB.stream().mapToDouble(v -> Math.pow(v - meanB, 2)).average().orElse(0.0);
        
        double stdDevA = Math.sqrt(varianceA);
        double stdDevB = Math.sqrt(varianceB);
        
        // Calculate t-statistic (simplified two-sample t-test)
        double pooledStdDev = Math.sqrt(((valuesA.size() - 1) * varianceA + (valuesB.size() - 1) * varianceB) / 
                                       (valuesA.size() + valuesB.size() - 2));
        double standardError = pooledStdDev * Math.sqrt(1.0 / valuesA.size() + 1.0 / valuesB.size());
        double tStatistic = (meanB - meanA) / standardError;
        
        // Simple p-value approximation (for demonstration - use proper statistical library in production)
        double pValue = 2 * (1 - Math.abs(tStatistic) / (Math.abs(tStatistic) + Math.sqrt(valuesA.size() + valuesB.size() - 2)));
        
        significance.put("meanA", meanA);
        significance.put("meanB", meanB);
        significance.put("difference", meanB - meanA);
        significance.put("percentageChange", meanA != 0 ? ((meanB - meanA) / meanA) * 100 : 0);
        significance.put("tStatistic", tStatistic);
        significance.put("pValue", pValue);
        significance.put("isSignificant", pValue < 0.05);
        significance.put("confidenceLevel", pValue < 0.01 ? "99%" : pValue < 0.05 ? "95%" : "Not significant");
        
        return significance;
    }
    
    /**
     * Generate A/B test recommendations
     */
    private List<String> generateAbTestRecommendations(Map<String, Object> analysis) {
        List<String> recommendations = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> significance = (Map<String, Object>) analysis.get("significance");
        
        if (significance != null && significance.containsKey("isSignificant")) {
            boolean isSignificant = (Boolean) significance.get("isSignificant");
            double percentageChange = (Double) significance.getOrDefault("percentageChange", 0.0);
            
            if (isSignificant) {
                if (percentageChange > 0) {
                    recommendations.add("Variant B shows statistically significant improvement. Consider implementing variant B.");
                } else {
                    recommendations.add("Variant A performs significantly better. Keep current implementation.");
                }
            } else {
                recommendations.add("No statistically significant difference found. Consider running test longer or with larger sample size.");
            }
            
            if (Math.abs(percentageChange) > 10) {
                recommendations.add("Large effect size detected (" + String.format("%.1f", percentageChange) + "%). High impact change.");
            }
        }
        
        int totalSamples = (Integer) analysis.getOrDefault("totalSamples", 0);
        if (totalSamples < 1000) {
            recommendations.add("Sample size is small (" + totalSamples + "). Consider collecting more data for reliable results.");
        }
        
        return recommendations;
    }
    
    /**
     * Record user feedback
     */
    public void recordUserFeedback(String userId, String feature, String queryId, 
                                  int rating, String comment, String category) {
        if (!feedbackEnabled) {
            return;
        }
        
        String feedbackId = "feedback-" + System.currentTimeMillis();
        UserFeedback feedback = new UserFeedback(feedbackId, userId, feature);
        feedback.setQueryId(queryId);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setCategory(category);
        
        feedbackData.put(feedbackId, feedback);
        
        logger.info("Recorded user feedback: user={}, feature={}, rating={}, category={}", 
            userId, feature, rating, category);
    }
    
    /**
     * Analyze user feedback
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void analyzeFeedback() {
        if (!feedbackEnabled) {
            return;
        }
        
        // Group feedback by feature
        Map<String, List<UserFeedback>> feedbackByFeature = feedbackData.values().stream()
            .collect(Collectors.groupingBy(UserFeedback::getFeature));
        
        for (String feature : feedbackByFeature.keySet()) {
            List<UserFeedback> featureFeedback = feedbackByFeature.get(feature);
            FeedbackAnalysis analysis = analyzeFeedbackForFeature(feature, featureFeedback);
            
            feedbackAnalyses.computeIfAbsent(feature, k -> new ArrayList<>()).add(analysis);
            
            // Generate optimization recommendations based on feedback
            generateFeedbackBasedRecommendations(feature, analysis);
        }
        
        logger.info("Analyzed feedback for {} features", feedbackByFeature.size());
    }
    
    /**
     * Analyze feedback for a specific feature
     */
    private FeedbackAnalysis analyzeFeedbackForFeature(String feature, List<UserFeedback> feedback) {
        FeedbackAnalysis analysis = new FeedbackAnalysis(feature);
        analysis.setTotalFeedback(feedback.size());
        
        // Calculate average rating
        double avgRating = feedback.stream()
            .mapToInt(UserFeedback::getRating)
            .average()
            .orElse(0.0);
        analysis.setAverageRating(avgRating);
        
        // Rating distribution
        Map<String, Integer> ratingDist = feedback.stream()
            .collect(Collectors.groupingBy(
                f -> String.valueOf(f.getRating()),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        analysis.getRatingDistribution().putAll(ratingDist);
        
        // Category distribution
        Map<String, Integer> categoryDist = feedback.stream()
            .filter(f -> f.getCategory() != null)
            .collect(Collectors.groupingBy(
                UserFeedback::getCategory,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        analysis.getCategoryDistribution().putAll(categoryDist);
        
        // Extract common issues and positive comments
        List<String> comments = feedback.stream()
            .map(UserFeedback::getComment)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // Simple keyword analysis for common issues
        Map<String, Long> issueKeywords = comments.stream()
            .filter(comment -> feedback.stream()
                .filter(f -> comment.equals(f.getComment()))
                .anyMatch(f -> f.getRating() <= 2))
            .flatMap(comment -> Arrays.stream(comment.toLowerCase().split("\\s+")))
            .filter(word -> word.length() > 3)
            .collect(Collectors.groupingBy(word -> word, Collectors.counting()));
        
        analysis.getCommonIssues().addAll(
            issueKeywords.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
        );
        
        // Positive comments (rating >= 4)
        analysis.getPositiveComments().addAll(
            feedback.stream()
                .filter(f -> f.getRating() >= 4 && f.getComment() != null)
                .map(UserFeedback::getComment)
                .limit(5)
                .collect(Collectors.toList())
        );
        
        // Simple sentiment score (based on rating distribution)
        double sentimentScore = (avgRating - 1) / 4.0; // Normalize to 0-1 scale
        analysis.setSentimentScore(sentimentScore);
        
        return analysis;
    }
    
    /**
     * Generate optimization recommendations based on feedback
     */
    private void generateFeedbackBasedRecommendations(String feature, FeedbackAnalysis analysis) {
        if (analysis.getAverageRating() < 3.0) {
            String recommendationId = "feedback-rec-" + System.currentTimeMillis();
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                recommendationId, feature, "usability");
            
            recommendation.setDescription("Low user satisfaction detected (avg rating: " + 
                String.format("%.1f", analysis.getAverageRating()) + "). Review user feedback and improve feature.");
            recommendation.setImpactLevel("high");
            recommendation.setEffort("medium");
            recommendation.setExpectedImprovement(0.3); // 30% improvement expected
            
            optimizationRecommendations.put(recommendationId, recommendation);
            
            logger.info("Generated feedback-based recommendation for feature: {}", feature);
        }
        
        // Check for specific issues
        if (analysis.getCategoryDistribution().getOrDefault("speed", 0) > 
            analysis.getTotalFeedback() * 0.3) {
            
            String recommendationId = "speed-rec-" + System.currentTimeMillis();
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                recommendationId, feature, "performance");
            
            recommendation.setDescription("Performance issues reported by users. Optimize response times for " + feature);
            recommendation.setImpactLevel("medium");
            recommendation.setEffort("high");
            recommendation.setExpectedImprovement(0.2);
            
            optimizationRecommendations.put(recommendationId, recommendation);
        }
    }
    
    /**
     * Get continuous improvement dashboard data
     */
    public Map<String, Object> getImprovementDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // A/B Testing summary
        Map<String, Object> abTestingSummary = new HashMap<>();
        abTestingSummary.put("activeTests", activeTests.values().stream().filter(AbTest::isActive).count());
        abTestingSummary.put("completedTests", activeTests.values().stream().filter(t -> "completed".equals(t.getStatus())).count());
        abTestingSummary.put("totalTests", activeTests.size());
        
        dashboard.put("abTesting", abTestingSummary);
        
        // Feedback summary
        Map<String, Object> feedbackSummary = new HashMap<>();
        feedbackSummary.put("totalFeedback", feedbackData.size());
        
        double avgRating = feedbackData.values().stream()
            .mapToInt(UserFeedback::getRating)
            .average()
            .orElse(0.0);
        feedbackSummary.put("averageRating", avgRating);
        
        // Recent feedback (last 24 hours)
        Instant yesterday = Instant.now().minusSeconds(24 * 3600);
        long recentFeedback = feedbackData.values().stream()
            .filter(f -> f.getTimestamp().isAfter(yesterday))
            .count();
        feedbackSummary.put("recentFeedback", recentFeedback);
        
        dashboard.put("feedback", feedbackSummary);
        
        // Optimization summary
        Map<String, Object> optimizationSummary = new HashMap<>();
        optimizationSummary.put("pendingRecommendations", 
            optimizationRecommendations.values().stream().filter(r -> "pending".equals(r.getStatus())).count());
        optimizationSummary.put("appliedOptimizations", appliedOptimizations.size());
        optimizationSummary.put("totalRecommendations", optimizationRecommendations.size());
        
        dashboard.put("optimization", optimizationSummary);
        
        // Recent improvements
        List<Map<String, Object>> recentImprovements = appliedOptimizations.values().stream()
            .sorted((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()))
            .limit(5)
            .map(result -> {
                Map<String, Object> map = new HashMap<>();
                map.put("feature", result.getFeature());
                map.put("improvement", result.getActualImprovement());
                map.put("outcome", result.getOutcome());
                map.put("appliedAt", result.getAppliedAt());
                return map;
            })
            .collect(Collectors.toList());
        
        dashboard.put("recentImprovements", recentImprovements);
        
        dashboard.put("lastUpdated", Instant.now());
        
        return dashboard;
    }
    
    /**
     * Get all A/B tests
     */
    public Map<String, AbTest> getAllAbTests() {
        return new HashMap<>(activeTests);
    }
    
    /**
     * Get user feedback for a feature
     */
    public List<UserFeedback> getFeedbackForFeature(String feature) {
        return feedbackData.values().stream()
            .filter(f -> feature.equals(f.getFeature()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get optimization recommendations
     */
    public List<OptimizationRecommendation> getOptimizationRecommendations() {
        return optimizationRecommendations.values().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());
    }
    
    /**
     * Apply optimization recommendation
     */
    public boolean applyOptimizationRecommendation(String recommendationId) {
        OptimizationRecommendation recommendation = optimizationRecommendations.get(recommendationId);
        if (recommendation == null) {
            logger.error("Optimization recommendation not found: {}", recommendationId);
            return false;
        }
        
        // Record current metrics before applying optimization
        OptimizationResult result = new OptimizationResult(recommendationId, recommendation.getFeature());
        result.getAppliedConfig().putAll(recommendation.getRecommendedConfig());
        
        // In a real implementation, this would apply the actual configuration changes
        // For now, we'll simulate the application
        recommendation.setStatus("applied");
        recommendation.setApplied(true);
        
        result.setOutcome("success");
        result.setActualImprovement(recommendation.getExpectedImprovement() * 0.8); // Simulate 80% of expected improvement
        
        appliedOptimizations.put(recommendationId, result);
        
        logger.info("Applied optimization recommendation: {} for feature: {}", 
            recommendationId, recommendation.getFeature());
        
        return true;
    }
}