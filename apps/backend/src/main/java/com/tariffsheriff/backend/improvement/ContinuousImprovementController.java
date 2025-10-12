package com.tariffsheriff.backend.improvement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * REST controller for continuous improvement operations
 */
@RestController
@RequestMapping("/api/improvement")
public class ContinuousImprovementController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContinuousImprovementController.class);
    
    @Autowired
    private ContinuousImprovementService improvementService;
    
    /**
     * Get improvement dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> getImprovementDashboard() {
        try {
            Map<String, Object> dashboard = improvementService.getImprovementDashboard();
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            logger.error("Error getting improvement dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get improvement dashboard"));
        }
    }
    
    /**
     * Create A/B test
     */
    @PostMapping("/ab-tests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAbTest(@RequestBody AbTestRequest request) {
        try {
            ContinuousImprovementService.AbTest test = improvementService.createAbTest(
                request.getName(), request.getFeature(), request.getDescription());
            
            // Configure test parameters
            if (request.getTrafficSplit() != null) {
                test.setTrafficSplit(request.getTrafficSplit());
            }
            if (request.getSuccessMetric() != null) {
                test.setSuccessMetric(request.getSuccessMetric());
            }
            if (request.getMinimumSampleSize() != null) {
                test.setMinimumSampleSize(request.getMinimumSampleSize());
            }
            
            // Set variant configurations
            if (request.getVariantA() != null) {
                test.getVariantA().putAll(request.getVariantA());
            }
            if (request.getVariantB() != null) {
                test.getVariantB().putAll(request.getVariantB());
            }
            
            return ResponseEntity.ok(test);
            
        } catch (Exception e) {
            logger.error("Error creating A/B test: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create A/B test"));
        }
    }
    
    /**
     * Start A/B test
     */
    @PostMapping("/ab-tests/{testId}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> startAbTest(@PathVariable String testId) {
        try {
            boolean success = improvementService.startAbTest(testId);
            if (success) {
                return ResponseEntity.ok(Map.of("status", "A/B test started successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to start A/B test"));
            }
            
        } catch (Exception e) {
            logger.error("Error starting A/B test {}: {}", testId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start A/B test"));
        }
    }
    
    /**
     * Stop A/B test
     */
    @PostMapping("/ab-tests/{testId}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> stopAbTest(@PathVariable String testId) {
        try {
            boolean success = improvementService.stopAbTest(testId);
            if (success) {
                return ResponseEntity.ok(Map.of("status", "A/B test stopped successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to stop A/B test"));
            }
            
        } catch (Exception e) {
            logger.error("Error stopping A/B test {}: {}", testId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to stop A/B test"));
        }
    }
    
    /**
     * Get A/B test results
     */
    @GetMapping("/ab-tests/{testId}/results")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> getAbTestResults(@PathVariable String testId) {
        try {
            Map<String, Object> results = improvementService.analyzeAbTestResults(testId);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error getting A/B test results for {}: {}", testId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get A/B test results"));
        }
    }
    
    /**
     * Get all A/B tests
     */
    @GetMapping("/ab-tests")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> getAllAbTests() {
        try {
            Map<String, ContinuousImprovementService.AbTest> tests = improvementService.getAllAbTests();
            return ResponseEntity.ok(tests);
            
        } catch (Exception e) {
            logger.error("Error getting A/B tests: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get A/B tests"));
        }
    }
    
    /**
     * Assign user to A/B test variant
     */
    @GetMapping("/ab-tests/{testId}/variant")
    public ResponseEntity<?> getUserVariant(@PathVariable String testId, 
                                           @RequestParam String userId) {
        try {
            String variant = improvementService.assignUserToVariant(testId, userId);
            return ResponseEntity.ok(Map.of("variant", variant));
            
        } catch (Exception e) {
            logger.error("Error assigning user {} to test {}: {}", userId, testId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to assign user to variant"));
        }
    }
    
    /**
     * Record A/B test result
     */
    @PostMapping("/ab-tests/{testId}/results")
    public ResponseEntity<?> recordAbTestResult(@PathVariable String testId,
                                               @RequestBody AbTestResultRequest request) {
        try {
            improvementService.recordAbTestResult(
                testId, request.getUserId(), request.getVariant(), 
                request.getMetric(), request.getValue());
            
            return ResponseEntity.ok(Map.of("status", "Result recorded successfully"));
            
        } catch (Exception e) {
            logger.error("Error recording A/B test result: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to record A/B test result"));
        }
    }
    
    /**
     * Submit user feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
        try {
            improvementService.recordUserFeedback(
                request.getUserId(), request.getFeature(), request.getQueryId(),
                request.getRating(), request.getComment(), request.getCategory());
            
            return ResponseEntity.ok(Map.of("status", "Feedback recorded successfully"));
            
        } catch (Exception e) {
            logger.error("Error recording feedback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to record feedback"));
        }
    }
    
    /**
     * Get feedback for feature
     */
    @GetMapping("/feedback/{feature}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> getFeedbackForFeature(@PathVariable String feature) {
        try {
            var feedback = improvementService.getFeedbackForFeature(feature);
            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            logger.error("Error getting feedback for feature {}: {}", feature, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get feedback"));
        }
    }
    
    /**
     * Get optimization recommendations
     */
    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> getOptimizationRecommendations() {
        try {
            var recommendations = improvementService.getOptimizationRecommendations();
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            logger.error("Error getting optimization recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get optimization recommendations"));
        }
    }
    
    /**
     * Apply optimization recommendation
     */
    @PostMapping("/recommendations/{recommendationId}/apply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> applyOptimizationRecommendation(@PathVariable String recommendationId) {
        try {
            boolean success = improvementService.applyOptimizationRecommendation(recommendationId);
            if (success) {
                return ResponseEntity.ok(Map.of("status", "Optimization applied successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to apply optimization"));
            }
            
        } catch (Exception e) {
            logger.error("Error applying optimization {}: {}", recommendationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to apply optimization"));
        }
    }
    
    // Request DTOs
    public static class AbTestRequest {
        private String name;
        private String feature;
        private String description;
        private Double trafficSplit;
        private String successMetric;
        private Double minimumSampleSize;
        private Map<String, Object> variantA;
        private Map<String, Object> variantB;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getFeature() { return feature; }
        public void setFeature(String feature) { this.feature = feature; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getTrafficSplit() { return trafficSplit; }
        public void setTrafficSplit(Double trafficSplit) { this.trafficSplit = trafficSplit; }
        public String getSuccessMetric() { return successMetric; }
        public void setSuccessMetric(String successMetric) { this.successMetric = successMetric; }
        public Double getMinimumSampleSize() { return minimumSampleSize; }
        public void setMinimumSampleSize(Double minimumSampleSize) { this.minimumSampleSize = minimumSampleSize; }
        public Map<String, Object> getVariantA() { return variantA; }
        public void setVariantA(Map<String, Object> variantA) { this.variantA = variantA; }
        public Map<String, Object> getVariantB() { return variantB; }
        public void setVariantB(Map<String, Object> variantB) { this.variantB = variantB; }
    }
    
    public static class AbTestResultRequest {
        private String userId;
        private String variant;
        private String metric;
        private double value;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getVariant() { return variant; }
        public void setVariant(String variant) { this.variant = variant; }
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }
    
    public static class FeedbackRequest {
        private String userId;
        private String feature;
        private String queryId;
        private int rating;
        private String comment;
        private String category;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFeature() { return feature; }
        public void setFeature(String feature) { this.feature = feature; }
        public String getQueryId() { return queryId; }
        public void setQueryId(String queryId) { this.queryId = queryId; }
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}