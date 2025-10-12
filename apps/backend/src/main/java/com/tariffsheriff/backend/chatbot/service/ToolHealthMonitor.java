package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.config.ToolProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Enhanced service for comprehensive monitoring of chatbot tools with predictive failure detection,
 * performance trend analysis, and automatic tool availability management
 */
@Service
public class ToolHealthMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolHealthMonitor.class);
    
    @Autowired
    private ToolProperties toolProperties;
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    private ToolRegistry toolRegistry;
    
    // Enhanced health metrics per tool
    private final Map<String, EnhancedToolHealthMetrics> healthMetrics = new ConcurrentHashMap<>();
    
    // Performance trend tracking
    private final Map<String, PerformanceTrendAnalyzer> trendAnalyzers = new ConcurrentHashMap<>();
    
    // Tool availability management
    private final Map<String, ToolAvailabilityManager> availabilityManagers = new ConcurrentHashMap<>();
    
    // Predictive failure detection
    private final Map<String, PredictiveFailureDetector> failureDetectors = new ConcurrentHashMap<>();
    
    // System-wide health dashboard data
    private final SystemHealthDashboard systemDashboard = new SystemHealthDashboard();
    
    // Configuration thresholds
    private static final double DEGRADATION_THRESHOLD = 0.7; // 70% success rate
    private static final double CRITICAL_THRESHOLD = 0.5; // 50% success rate
    private static final long PERFORMANCE_DEGRADATION_MS = 10000; // 10 seconds
    private static final int TREND_ANALYSIS_WINDOW = 50; // Last 50 executions
    private static final int FAILURE_PREDICTION_WINDOW = 20; // Last 20 executions
    
    /**
     * Record a successful tool execution with enhanced tracking
     */
    public void recordSuccess(String toolName, long executionTimeMs) {
        EnhancedToolHealthMetrics metrics = getOrCreateEnhancedMetrics(toolName);
        metrics.recordSuccess(executionTimeMs);
        
        // Update trend analysis
        PerformanceTrendAnalyzer trendAnalyzer = getOrCreateTrendAnalyzer(toolName);
        trendAnalyzer.recordExecution(executionTimeMs, true);
        
        // Update availability management
        ToolAvailabilityManager availabilityManager = getOrCreateAvailabilityManager(toolName);
        availabilityManager.recordSuccess();
        
        // Update predictive failure detection
        PredictiveFailureDetector failureDetector = getOrCreateFailureDetector(toolName);
        failureDetector.recordSuccess(executionTimeMs);
        
        // Update system dashboard
        systemDashboard.recordToolExecution(toolName, true, executionTimeMs);
        
        // Check for performance improvements
        checkPerformanceImprovement(toolName, metrics, trendAnalyzer);
    }
    
    /**
     * Record a failed tool execution with enhanced tracking
     */
    public void recordFailure(String toolName, String errorMessage) {
        EnhancedToolHealthMetrics metrics = getOrCreateEnhancedMetrics(toolName);
        metrics.recordFailure(errorMessage);
        
        // Update trend analysis
        PerformanceTrendAnalyzer trendAnalyzer = getOrCreateTrendAnalyzer(toolName);
        trendAnalyzer.recordExecution(0, false);
        
        // Update availability management
        ToolAvailabilityManager availabilityManager = getOrCreateAvailabilityManager(toolName);
        availabilityManager.recordFailure(errorMessage);
        
        // Update predictive failure detection
        PredictiveFailureDetector failureDetector = getOrCreateFailureDetector(toolName);
        failureDetector.recordFailure(errorMessage);
        
        // Update system dashboard
        systemDashboard.recordToolExecution(toolName, false, 0);
        
        // Check for degradation alerts
        checkDegradationAlerts(toolName, metrics, trendAnalyzer);
        
        // Check if tool should be automatically disabled
        checkAutoDisable(toolName, availabilityManager);
    }
    
    /**
     * Get enhanced health status for a specific tool
     */
    public EnhancedToolHealthStatus getToolHealth(String toolName) {
        EnhancedToolHealthMetrics metrics = healthMetrics.get(toolName);
        if (metrics == null) {
            return new EnhancedToolHealthStatus(toolName, true, "No metrics available", 
                    0, 0, 0, null, null, null, false);
        }
        
        PerformanceTrendAnalyzer trendAnalyzer = trendAnalyzers.get(toolName);
        ToolAvailabilityManager availabilityManager = availabilityManagers.get(toolName);
        PredictiveFailureDetector failureDetector = failureDetectors.get(toolName);
        
        return metrics.getEnhancedHealthStatus(trendAnalyzer, availabilityManager, failureDetector);
    }
    
    /**
     * Get enhanced health status for all tools
     */
    public Map<String, EnhancedToolHealthStatus> getAllToolHealth() {
        Map<String, EnhancedToolHealthStatus> healthStatuses = new HashMap<>();
        
        for (String toolName : healthMetrics.keySet()) {
            healthStatuses.put(toolName, getToolHealth(toolName));
        }
        
        return healthStatuses;
    }
    
    /**
     * Get system-wide health dashboard
     */
    public SystemHealthDashboard getSystemHealthDashboard() {
        return systemDashboard;
    }
    
    /**
     * Get performance trends for a specific tool
     */
    public PerformanceTrend getPerformanceTrend(String toolName) {
        PerformanceTrendAnalyzer analyzer = trendAnalyzers.get(toolName);
        return analyzer != null ? analyzer.getCurrentTrend() : null;
    }
    
    /**
     * Get predictive failure analysis for a specific tool
     */
    public FailurePrediction getFailurePrediction(String toolName) {
        PredictiveFailureDetector detector = failureDetectors.get(toolName);
        return detector != null ? detector.getCurrentPrediction() : null;
    }
    
    /**
     * Get tool availability score (0.0 to 1.0)
     */
    public double getToolAvailabilityScore(String toolName) {
        ToolAvailabilityManager manager = availabilityManagers.get(toolName);
        return manager != null ? manager.getAvailabilityScore() : 1.0;
    }
    
    /**
     * Check if tool should be automatically disabled
     */
    public boolean shouldDisableTool(String toolName) {
        ToolAvailabilityManager manager = availabilityManagers.get(toolName);
        return manager != null && manager.shouldDisable();
    }
    
    /**
     * Manually enable/disable a tool
     */
    public void setToolEnabled(String toolName, boolean enabled) {
        ToolAvailabilityManager manager = getOrCreateAvailabilityManager(toolName);
        manager.setManuallyDisabled(!enabled);
        
        logger.info("Tool {} manually {}", toolName, enabled ? "enabled" : "disabled");
    }
    
    /**
     * Enhanced scheduled health check for all tools with comprehensive monitoring
     */
    @Scheduled(fixedRateString = "#{@toolProperties.healthCheckIntervalMs}")
    public void performHealthChecks() {
        if (!toolProperties.isEnableHealthChecks()) {
            return;
        }
        
        logger.debug("Starting enhanced scheduled tool health checks");
        
        for (String toolName : toolRegistry.getAvailableTools().stream()
                .map(tool -> tool.getName()).toList()) {
            
            try {
                performEnhancedToolHealthCheck(toolName);
            } catch (Exception e) {
                logger.error("Error during health check for tool {}: {}", toolName, e.getMessage());
                recordFailure(toolName, "Health check failed: " + e.getMessage());
            }
        }
        
        // Perform system-wide analysis
        performSystemHealthAnalysis();
        
        // Generate alerts for degraded tools
        generateDegradationAlerts();
        
        // Update predictive models
        updatePredictiveModels();
        
        logger.debug("Completed enhanced scheduled tool health checks");
    }
    
    /**
     * Perform enhanced health check for a specific tool
     */
    private void performEnhancedToolHealthCheck(String toolName) {
        // Skip if tool is manually disabled
        ToolAvailabilityManager manager = availabilityManagers.get(toolName);
        if (manager != null && manager.isManuallyDisabled()) {
            logger.debug("Skipping health check for manually disabled tool: {}", toolName);
            return;
        }
        
        // Perform basic health check
        performToolHealthCheck(toolName);
        
        // Perform trend analysis
        PerformanceTrendAnalyzer analyzer = trendAnalyzers.get(toolName);
        if (analyzer != null) {
            analyzer.analyzeCurrentTrend();
        }
        
        // Update failure prediction
        PredictiveFailureDetector detector = failureDetectors.get(toolName);
        if (detector != null) {
            detector.updatePrediction();
        }
        
        // Check for auto-recovery
        if (manager != null) {
            manager.checkAutoRecovery();
        }
    }
    
    /**
     * Perform health check for a specific tool
     */
    private void performToolHealthCheck(String toolName) {
        // Create a simple health check call
        ToolCall healthCheck = createHealthCheckCall(toolName);
        
        long startTime = System.currentTimeMillis();
        ToolResult result = toolRegistry.executeToolCall(healthCheck);
        long executionTime = System.currentTimeMillis() - startTime;
        
        if (result.isSuccess()) {
            recordSuccess(toolName, executionTime);
            logger.debug("Health check passed for tool: {}", toolName);
        } else {
            recordFailure(toolName, "Health check failed: " + result.getError());
            logger.warn("Health check failed for tool {}: {}", toolName, result.getError());
        }
    }
    
    /**
     * Create a health check call for a tool
     */
    private ToolCall createHealthCheckCall(String toolName) {
        ToolCall healthCheck = new ToolCall();
        healthCheck.setName(toolName);
        
        // Create minimal parameters for health check
        Map<String, Object> parameters = new HashMap<>();
        
        switch (toolName) {
            case "getTariffRateLookup":
                parameters.put("hsCode", "0101.21");
                parameters.put("originCountry", "US");
                parameters.put("destinationCountry", "CA");
                break;
            case "findHsCodeForProduct":
                parameters.put("productDescription", "test");
                break;
            case "getAgreementsByCountry":
                parameters.put("countryCode", "US");
                break;
            default:
                // Generic health check
                break;
        }
        
        healthCheck.setArguments(parameters);
        return healthCheck;
    }
    
    // ========== HELPER METHODS ==========
    
    private EnhancedToolHealthMetrics getOrCreateEnhancedMetrics(String toolName) {
        return healthMetrics.computeIfAbsent(toolName, k -> new EnhancedToolHealthMetrics(toolName));
    }
    
    private PerformanceTrendAnalyzer getOrCreateTrendAnalyzer(String toolName) {
        return trendAnalyzers.computeIfAbsent(toolName, k -> new PerformanceTrendAnalyzer(toolName));
    }
    
    private ToolAvailabilityManager getOrCreateAvailabilityManager(String toolName) {
        return availabilityManagers.computeIfAbsent(toolName, k -> new ToolAvailabilityManager(toolName));
    }
    
    private PredictiveFailureDetector getOrCreateFailureDetector(String toolName) {
        return failureDetectors.computeIfAbsent(toolName, k -> new PredictiveFailureDetector(toolName));
    }
    
    /**
     * Check for performance improvement and log positive trends
     */
    private void checkPerformanceImprovement(String toolName, EnhancedToolHealthMetrics metrics, 
                                           PerformanceTrendAnalyzer analyzer) {
        if (analyzer.isImproving()) {
            logger.info("Performance improvement detected for tool {}: {}", 
                    toolName, analyzer.getCurrentTrend().getDescription());
        }
    }
    
    /**
     * Check for degradation and generate alerts
     */
    private void checkDegradationAlerts(String toolName, EnhancedToolHealthMetrics metrics, 
                                      PerformanceTrendAnalyzer analyzer) {
        double successRate = metrics.getSuccessRate();
        
        if (successRate < CRITICAL_THRESHOLD) {
            logger.error("CRITICAL: Tool {} success rate dropped to {:.1f}%", 
                    toolName, successRate * 100);
            systemDashboard.recordCriticalAlert(toolName, "Success rate below critical threshold");
        } else if (successRate < DEGRADATION_THRESHOLD) {
            logger.warn("WARNING: Tool {} success rate degraded to {:.1f}%", 
                    toolName, successRate * 100);
            systemDashboard.recordDegradationAlert(toolName, "Success rate below degradation threshold");
        }
        
        if (analyzer.isDegrading()) {
            logger.warn("Performance degradation trend detected for tool {}: {}", 
                    toolName, analyzer.getCurrentTrend().getDescription());
        }
    }
    
    /**
     * Check if tool should be automatically disabled
     */
    private void checkAutoDisable(String toolName, ToolAvailabilityManager manager) {
        if (manager.shouldDisable()) {
            logger.warn("Auto-disabling tool {} due to poor performance", toolName);
            systemDashboard.recordAutoDisable(toolName, manager.getDisableReason());
        }
    }
    
    /**
     * Perform system-wide health analysis
     */
    private void performSystemHealthAnalysis() {
        systemDashboard.updateSystemMetrics(healthMetrics, trendAnalyzers, availabilityManagers);
    }
    
    /**
     * Generate alerts for degraded tools
     */
    private void generateDegradationAlerts() {
        for (Map.Entry<String, EnhancedToolHealthMetrics> entry : healthMetrics.entrySet()) {
            String toolName = entry.getKey();
            EnhancedToolHealthMetrics metrics = entry.getValue();
            
            if (metrics.getSuccessRate() < DEGRADATION_THRESHOLD) {
                systemDashboard.recordDegradationAlert(toolName, 
                        String.format("Success rate: %.1f%%", metrics.getSuccessRate() * 100));
            }
            
            if (metrics.getAverageExecutionTime() > PERFORMANCE_DEGRADATION_MS) {
                systemDashboard.recordDegradationAlert(toolName, 
                        String.format("Slow response time: %dms", metrics.getAverageExecutionTime()));
            }
        }
    }
    
    /**
     * Update predictive models for all tools
     */
    private void updatePredictiveModels() {
        for (PredictiveFailureDetector detector : failureDetectors.values()) {
            detector.updatePrediction();
        }
    }
    
    // ========== ENHANCED HEALTH METRICS CLASS ==========
    
    /**
     * Enhanced class to track comprehensive health metrics for a tool
     */
    private static class EnhancedToolHealthMetrics {
        private final String toolName;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final List<ExecutionRecord> recentExecutions = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, Integer> errorPatterns = new ConcurrentHashMap<>();
        
        private volatile String lastError = null;
        private volatile LocalDateTime lastErrorTime = null;
        private volatile LocalDateTime lastSuccessTime = null;
        private volatile LocalDateTime createdAt = LocalDateTime.now();
        
        // Performance tracking
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = 0;
        private final AtomicLong slowExecutionCount = new AtomicLong(0);
        
        public EnhancedToolHealthMetrics(String toolName) {
            this.toolName = toolName;
        }
        
        public void recordSuccess(long executionTimeMs) {
            successCount.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);
            lastSuccessTime = LocalDateTime.now();
            
            // Update execution time bounds
            if (executionTimeMs < minExecutionTime) {
                minExecutionTime = executionTimeMs;
            }
            if (executionTimeMs > maxExecutionTime) {
                maxExecutionTime = executionTimeMs;
            }
            
            // Track slow executions
            if (executionTimeMs > PERFORMANCE_DEGRADATION_MS) {
                slowExecutionCount.incrementAndGet();
            }
            
            // Record execution
            recordExecution(executionTimeMs, true, null);
        }
        
        public void recordFailure(String errorMessage) {
            failureCount.incrementAndGet();
            lastError = errorMessage;
            lastErrorTime = LocalDateTime.now();
            
            // Track error patterns
            String errorType = categorizeError(errorMessage);
            errorPatterns.merge(errorType, 1, Integer::sum);
            
            // Record execution
            recordExecution(0, false, errorMessage);
        }
        
        private void recordExecution(long executionTime, boolean success, String error) {
            ExecutionRecord record = new ExecutionRecord(LocalDateTime.now(), executionTime, success, error);
            recentExecutions.add(record);
            
            // Keep only recent executions
            if (recentExecutions.size() > TREND_ANALYSIS_WINDOW) {
                recentExecutions.remove(0);
            }
        }
        
        private String categorizeError(String errorMessage) {
            if (errorMessage == null) return "UNKNOWN";
            
            String lowerError = errorMessage.toLowerCase();
            if (lowerError.contains("timeout")) return "TIMEOUT";
            if (lowerError.contains("connection")) return "CONNECTION";
            if (lowerError.contains("rate limit")) return "RATE_LIMIT";
            if (lowerError.contains("validation")) return "VALIDATION";
            if (lowerError.contains("not found")) return "NOT_FOUND";
            if (lowerError.contains("unauthorized")) return "UNAUTHORIZED";
            
            return "OTHER";
        }
        
        public EnhancedToolHealthStatus getEnhancedHealthStatus(PerformanceTrendAnalyzer trendAnalyzer,
                                                              ToolAvailabilityManager availabilityManager,
                                                              PredictiveFailureDetector failureDetector) {
            int successes = successCount.get();
            int failures = failureCount.get();
            int totalCalls = successes + failures;
            
            double successRate = totalCalls > 0 ? (double) successes / totalCalls : 1.0;
            long avgExecutionTime = successes > 0 ? totalExecutionTime.get() / successes : 0;
            
            boolean isHealthy = successRate >= 0.8 && (lastErrorTime == null || 
                (lastSuccessTime != null && lastSuccessTime.isAfter(lastErrorTime)));
            
            String status = determineDetailedStatus(isHealthy, successRate, avgExecutionTime);
            
            PerformanceTrend trend = trendAnalyzer != null ? trendAnalyzer.getCurrentTrend() : null;
            FailurePrediction prediction = failureDetector != null ? failureDetector.getCurrentPrediction() : null;
            double availabilityScore = availabilityManager != null ? availabilityManager.getAvailabilityScore() : 1.0;
            boolean autoDisabled = availabilityManager != null && availabilityManager.shouldDisable();
            
            return new EnhancedToolHealthStatus(toolName, isHealthy, status, successRate, 
                    avgExecutionTime, totalCalls, trend, prediction, 
                    createDetailedMetrics(), autoDisabled);
        }
        
        private String determineDetailedStatus(boolean isHealthy, double successRate, long avgExecutionTime) {
            if (!isHealthy) {
                if (successRate < CRITICAL_THRESHOLD) {
                    return "CRITICAL: Success rate " + String.format("%.1f%%", successRate * 100);
                } else if (successRate < DEGRADATION_THRESHOLD) {
                    return "DEGRADED: Success rate " + String.format("%.1f%%", successRate * 100);
                } else if (avgExecutionTime > PERFORMANCE_DEGRADATION_MS) {
                    return "SLOW: Average response time " + avgExecutionTime + "ms";
                } else if (lastError != null) {
                    return "ERROR: " + lastError;
                }
                return "UNHEALTHY";
            }
            
            if (avgExecutionTime > PERFORMANCE_DEGRADATION_MS / 2) {
                return "HEALTHY (Slow)";
            }
            
            return "HEALTHY";
        }
        
        private DetailedMetrics createDetailedMetrics() {
            return new DetailedMetrics(
                    minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime,
                    maxExecutionTime,
                    (int) slowExecutionCount.get(),
                    new HashMap<>(errorPatterns),
                    recentExecutions.size(),
                    createdAt
            );
        }
        
        // Getters for compatibility
        public double getSuccessRate() {
            int total = successCount.get() + failureCount.get();
            return total > 0 ? (double) successCount.get() / total : 1.0;
        }
        
        public long getAverageExecutionTime() {
            int successes = successCount.get();
            return successes > 0 ? totalExecutionTime.get() / successes : 0;
        }
    }
    
    // ========== SUPPORTING CLASSES ==========
    
    /**
     * Execution record for trend analysis
     */
    private static class ExecutionRecord {
        final LocalDateTime timestamp;
        final long executionTime;
        final boolean success;
        final String error;
        
        ExecutionRecord(LocalDateTime timestamp, long executionTime, boolean success, String error) {
            this.timestamp = timestamp;
            this.executionTime = executionTime;
            this.success = success;
            this.error = error;
        }
    }
    
    /**
     * Performance trend analyzer
     */
    private static class PerformanceTrendAnalyzer {
        private final String toolName;
        private final List<Double> successRateHistory = new ArrayList<>();
        private final List<Long> avgExecutionTimeHistory = new ArrayList<>();
        private volatile PerformanceTrend currentTrend;
        
        PerformanceTrendAnalyzer(String toolName) {
            this.toolName = toolName;
            this.currentTrend = new PerformanceTrend("STABLE", "No trend data available", 0.0);
        }
        
        void recordExecution(long executionTime, boolean success) {
            // This would be called from the main record methods
            // Implementation would track trends over time
        }
        
        void analyzeCurrentTrend() {
            // Analyze recent performance data to determine trends
            if (successRateHistory.size() < 5) {
                currentTrend = new PerformanceTrend("INSUFFICIENT_DATA", "Not enough data for trend analysis", 0.0);
                return;
            }
            
            // Simple trend analysis - in production, use more sophisticated algorithms
            double recentAvg = successRateHistory.subList(Math.max(0, successRateHistory.size() - 5), successRateHistory.size())
                    .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double olderAvg = successRateHistory.subList(0, Math.min(5, successRateHistory.size()))
                    .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            double trendValue = recentAvg - olderAvg;
            
            if (trendValue > 0.1) {
                currentTrend = new PerformanceTrend("IMPROVING", "Performance is improving", trendValue);
            } else if (trendValue < -0.1) {
                currentTrend = new PerformanceTrend("DEGRADING", "Performance is degrading", trendValue);
            } else {
                currentTrend = new PerformanceTrend("STABLE", "Performance is stable", trendValue);
            }
        }
        
        PerformanceTrend getCurrentTrend() {
            return currentTrend;
        }
        
        boolean isImproving() {
            return "IMPROVING".equals(currentTrend.getDirection());
        }
        
        boolean isDegrading() {
            return "DEGRADING".equals(currentTrend.getDirection());
        }
    }
    
    /**
     * Tool availability manager
     */
    private static class ToolAvailabilityManager {
        private final String toolName;
        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private volatile boolean manuallyDisabled = false;
        private volatile boolean autoDisabled = false;
        private volatile String disableReason = null;
        private volatile LocalDateTime lastRecoveryAttempt = null;
        
        private static final int MAX_CONSECUTIVE_FAILURES = 5;
        private static final long RECOVERY_ATTEMPT_INTERVAL_MINUTES = 10;
        
        ToolAvailabilityManager(String toolName) {
            this.toolName = toolName;
        }
        
        void recordSuccess() {
            consecutiveFailures.set(0);
            if (autoDisabled) {
                autoDisabled = false;
                disableReason = null;
                logger.info("Tool {} auto-recovered", toolName);
            }
        }
        
        void recordFailure(String errorMessage) {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= MAX_CONSECUTIVE_FAILURES && !autoDisabled) {
                autoDisabled = true;
                disableReason = "Too many consecutive failures: " + errorMessage;
                logger.warn("Tool {} auto-disabled after {} consecutive failures", toolName, failures);
            }
        }
        
        boolean shouldDisable() {
            return autoDisabled || manuallyDisabled;
        }
        
        double getAvailabilityScore() {
            if (manuallyDisabled) return 0.0;
            if (autoDisabled) return 0.1;
            
            int failures = consecutiveFailures.get();
            return Math.max(0.0, 1.0 - (failures * 0.2)); // Reduce score by 20% per failure
        }
        
        void setManuallyDisabled(boolean disabled) {
            this.manuallyDisabled = disabled;
            if (!disabled) {
                consecutiveFailures.set(0);
                autoDisabled = false;
                disableReason = null;
            }
        }
        
        boolean isManuallyDisabled() {
            return manuallyDisabled;
        }
        
        String getDisableReason() {
            return disableReason;
        }
        
        void checkAutoRecovery() {
            if (autoDisabled && (lastRecoveryAttempt == null || 
                    ChronoUnit.MINUTES.between(lastRecoveryAttempt, LocalDateTime.now()) >= RECOVERY_ATTEMPT_INTERVAL_MINUTES)) {
                lastRecoveryAttempt = LocalDateTime.now();
                // Reset for recovery attempt
                consecutiveFailures.set(0);
                logger.info("Attempting auto-recovery for tool {}", toolName);
            }
        }
    }
    
    /**
     * Predictive failure detector
     */
    private static class PredictiveFailureDetector {
        private final String toolName;
        private final List<ExecutionRecord> recentExecutions = new ArrayList<>();
        private volatile FailurePrediction currentPrediction;
        
        PredictiveFailureDetector(String toolName) {
            this.toolName = toolName;
            this.currentPrediction = new FailurePrediction("LOW", 0.0, "Insufficient data for prediction");
        }
        
        void recordSuccess(long executionTime) {
            recordExecution(new ExecutionRecord(LocalDateTime.now(), executionTime, true, null));
        }
        
        void recordFailure(String errorMessage) {
            recordExecution(new ExecutionRecord(LocalDateTime.now(), 0, false, errorMessage));
        }
        
        private void recordExecution(ExecutionRecord record) {
            recentExecutions.add(record);
            if (recentExecutions.size() > FAILURE_PREDICTION_WINDOW) {
                recentExecutions.remove(0);
            }
        }
        
        void updatePrediction() {
            if (recentExecutions.size() < 10) {
                currentPrediction = new FailurePrediction("LOW", 0.0, "Insufficient data for prediction");
                return;
            }
            
            // Simple failure prediction based on recent failure rate and trends
            long recentFailures = recentExecutions.stream()
                    .filter(r -> !r.success)
                    .count();
            
            double failureRate = (double) recentFailures / recentExecutions.size();
            
            // Check for increasing failure trend
            int halfSize = recentExecutions.size() / 2;
            long firstHalfFailures = recentExecutions.subList(0, halfSize).stream()
                    .filter(r -> !r.success).count();
            long secondHalfFailures = recentExecutions.subList(halfSize, recentExecutions.size()).stream()
                    .filter(r -> !r.success).count();
            
            boolean increasingFailures = secondHalfFailures > firstHalfFailures;
            
            String riskLevel;
            String description;
            
            if (failureRate > 0.5) {
                riskLevel = "HIGH";
                description = "High failure rate detected: " + String.format("%.1f%%", failureRate * 100);
            } else if (failureRate > 0.3 || increasingFailures) {
                riskLevel = "MEDIUM";
                description = increasingFailures ? "Increasing failure trend detected" : 
                             "Moderate failure rate: " + String.format("%.1f%%", failureRate * 100);
            } else {
                riskLevel = "LOW";
                description = "Tool performing normally";
            }
            
            currentPrediction = new FailurePrediction(riskLevel, failureRate, description);
        }
        
        FailurePrediction getCurrentPrediction() {
            return currentPrediction;
        }
    }
    
    /**
     * System health dashboard
     */
    private static class SystemHealthDashboard {
        private final Map<String, List<String>> toolAlerts = new ConcurrentHashMap<>();
        private final AtomicInteger totalExecutions = new AtomicInteger(0);
        private final AtomicInteger totalFailures = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile LocalDateTime lastUpdated = LocalDateTime.now();
        
        void recordToolExecution(String toolName, boolean success, long executionTime) {
            totalExecutions.incrementAndGet();
            if (!success) {
                totalFailures.incrementAndGet();
            }
            totalExecutionTime.addAndGet(executionTime);
            lastUpdated = LocalDateTime.now();
        }
        
        void recordDegradationAlert(String toolName, String message) {
            toolAlerts.computeIfAbsent(toolName, k -> new ArrayList<>()).add("DEGRADATION: " + message);
        }
        
        void recordCriticalAlert(String toolName, String message) {
            toolAlerts.computeIfAbsent(toolName, k -> new ArrayList<>()).add("CRITICAL: " + message);
        }
        
        void recordAutoDisable(String toolName, String reason) {
            toolAlerts.computeIfAbsent(toolName, k -> new ArrayList<>()).add("AUTO_DISABLED: " + reason);
        }
        
        void updateSystemMetrics(Map<String, EnhancedToolHealthMetrics> healthMetrics,
                               Map<String, PerformanceTrendAnalyzer> trendAnalyzers,
                               Map<String, ToolAvailabilityManager> availabilityManagers) {
            // Update system-wide metrics
            lastUpdated = LocalDateTime.now();
        }
        
        // Getters for dashboard data
        public double getSystemSuccessRate() {
            int total = totalExecutions.get();
            return total > 0 ? 1.0 - ((double) totalFailures.get() / total) : 1.0;
        }
        
        public long getSystemAverageExecutionTime() {
            int total = totalExecutions.get();
            return total > 0 ? totalExecutionTime.get() / total : 0;
        }
        
        public Map<String, List<String>> getToolAlerts() {
            return new HashMap<>(toolAlerts);
        }
        
        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
    
    // ========== DATA CLASSES ==========
    
    /**
     * Enhanced health status information for a tool
     */
    public static class EnhancedToolHealthStatus {
        private final String toolName;
        private final boolean healthy;
        private final String status;
        private final double successRate;
        private final long averageExecutionTimeMs;
        private final int totalExecutions;
        private final PerformanceTrend performanceTrend;
        private final FailurePrediction failurePrediction;
        private final DetailedMetrics detailedMetrics;
        private final boolean autoDisabled;
        
        public EnhancedToolHealthStatus(String toolName, boolean healthy, String status, 
                                      double successRate, long averageExecutionTimeMs, int totalExecutions,
                                      PerformanceTrend performanceTrend, FailurePrediction failurePrediction,
                                      DetailedMetrics detailedMetrics, boolean autoDisabled) {
            this.toolName = toolName;
            this.healthy = healthy;
            this.status = status;
            this.successRate = successRate;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
            this.totalExecutions = totalExecutions;
            this.performanceTrend = performanceTrend;
            this.failurePrediction = failurePrediction;
            this.detailedMetrics = detailedMetrics;
            this.autoDisabled = autoDisabled;
        }
        
        // Getters
        public String getToolName() { return toolName; }
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public double getSuccessRate() { return successRate; }
        public long getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
        public int getTotalExecutions() { return totalExecutions; }
        public PerformanceTrend getPerformanceTrend() { return performanceTrend; }
        public FailurePrediction getFailurePrediction() { return failurePrediction; }
        public DetailedMetrics getDetailedMetrics() { return detailedMetrics; }
        public boolean isAutoDisabled() { return autoDisabled; }
    }
    
    /**
     * Performance trend information
     */
    public static class PerformanceTrend {
        private final String direction;
        private final String description;
        private final double trendValue;
        
        public PerformanceTrend(String direction, String description, double trendValue) {
            this.direction = direction;
            this.description = description;
            this.trendValue = trendValue;
        }
        
        public String getDirection() { return direction; }
        public String getDescription() { return description; }
        public double getTrendValue() { return trendValue; }
    }
    
    /**
     * Failure prediction information
     */
    public static class FailurePrediction {
        private final String riskLevel;
        private final double failureProbability;
        private final String description;
        
        public FailurePrediction(String riskLevel, double failureProbability, String description) {
            this.riskLevel = riskLevel;
            this.failureProbability = failureProbability;
            this.description = description;
        }
        
        public String getRiskLevel() { return riskLevel; }
        public double getFailureProbability() { return failureProbability; }
        public String getDescription() { return description; }
    }
    
    /**
     * Detailed metrics for comprehensive monitoring
     */
    public static class DetailedMetrics {
        private final long minExecutionTime;
        private final long maxExecutionTime;
        private final int slowExecutionCount;
        private final Map<String, Integer> errorPatterns;
        private final int recentExecutionCount;
        private final LocalDateTime createdAt;
        
        public DetailedMetrics(long minExecutionTime, long maxExecutionTime, int slowExecutionCount,
                             Map<String, Integer> errorPatterns, int recentExecutionCount, LocalDateTime createdAt) {
            this.minExecutionTime = minExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.slowExecutionCount = slowExecutionCount;
            this.errorPatterns = errorPatterns;
            this.recentExecutionCount = recentExecutionCount;
            this.createdAt = createdAt;
        }
        
        public long getMinExecutionTime() { return minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public int getSlowExecutionCount() { return slowExecutionCount; }
        public Map<String, Integer> getErrorPatterns() { return errorPatterns; }
        public int getRecentExecutionCount() { return recentExecutionCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}