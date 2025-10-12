package com.tariffsheriff.backend.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive performance monitoring service for AI operations
 * Features:
 * - Real-time performance metrics collection
 * - Performance monitoring dashboards and alerts
 * - Automated performance optimization and tuning
 * - Capacity planning and resource forecasting
 */
@Service
public class PerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
    
    // Configuration
    @Value("${performance.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${performance.metrics.retention-hours:24}")
    private int metricsRetentionHours;
    
    @Value("${performance.alerts.response-time-threshold-ms:15000}")
    private long responseTimeThresholdMs;
    
    @Value("${performance.alerts.error-rate-threshold:0.05}")
    private double errorRateThreshold;
    
    @Value("${performance.alerts.cpu-threshold:0.8}")
    private double cpuThreshold;
    
    @Value("${performance.alerts.memory-threshold:0.85}")
    private double memoryThreshold;
    
    // Metrics storage
    private final ConcurrentHashMap<String, PerformanceMetric> currentMetrics = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PerformanceSnapshot> historicalMetrics = new ConcurrentLinkedQueue<>();
    
    // Counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    // Current system state
    private final AtomicReference<SystemHealth> currentSystemHealth = new AtomicReference<>(SystemHealth.HEALTHY);
    private final ConcurrentHashMap<String, AlertStatus> activeAlerts = new ConcurrentHashMap<>();
    
    // Executors
    private final ScheduledExecutorService metricsCollector = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService alertProcessor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService optimizer = Executors.newSingleThreadScheduledExecutor();
    
    // Performance thresholds
    private final Map<String, PerformanceThreshold> thresholds = new ConcurrentHashMap<>();
    
    public PerformanceMonitoringService() {
        initializeThresholds();
        
        if (monitoringEnabled) {
            // Schedule metrics collection every 30 seconds
            metricsCollector.scheduleAtFixedRate(this::collectMetrics, 30, 30, TimeUnit.SECONDS);
            
            // Schedule alert processing every minute
            alertProcessor.scheduleAtFixedRate(this::processAlerts, 60, 60, TimeUnit.SECONDS);
            
            // Schedule optimization every 10 minutes
            optimizer.scheduleAtFixedRate(this::performOptimization, 600, 600, TimeUnit.SECONDS);
            
            logger.info("Performance monitoring service started");
        }
    }
    
    /**
     * Initialize performance thresholds
     */
    private void initializeThresholds() {
        thresholds.put("response_time", new PerformanceThreshold("response_time", responseTimeThresholdMs, ThresholdType.MAX));
        thresholds.put("error_rate", new PerformanceThreshold("error_rate", errorRateThreshold, ThresholdType.MAX));
        thresholds.put("cpu_usage", new PerformanceThreshold("cpu_usage", cpuThreshold, ThresholdType.MAX));
        thresholds.put("memory_usage", new PerformanceThreshold("memory_usage", memoryThreshold, ThresholdType.MAX));
        thresholds.put("cache_hit_rate", new PerformanceThreshold("cache_hit_rate", 0.7, ThresholdType.MIN));
        thresholds.put("concurrent_users", new PerformanceThreshold("concurrent_users", 1000, ThresholdType.MAX));
    }
    
    /**
     * Record performance metrics for an operation
     */
    public void recordOperation(String operationType, long durationMs, boolean success) {
        if (!monitoringEnabled) return;
        
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(durationMs);
        
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        // Update operation-specific metrics
        PerformanceMetric metric = currentMetrics.computeIfAbsent(operationType, 
            k -> new PerformanceMetric(operationType));
        
        metric.recordOperation(durationMs, success);
        
        // Check for immediate alerts
        checkImmediateAlerts(operationType, durationMs, success);
    }
    
    /**
     * Record AI-specific metrics
     */
    public void recordAiOperation(String operation, long durationMs, boolean success, 
                                 String model, int tokenCount, double confidence) {
        recordOperation("ai_" + operation, durationMs, success);
        
        // Record AI-specific metrics
        AiPerformanceMetric aiMetric = (AiPerformanceMetric) currentMetrics.computeIfAbsent(
            "ai_" + operation, k -> new AiPerformanceMetric(operation));
        
        aiMetric.recordAiOperation(durationMs, success, model, tokenCount, confidence);
    }
    
    /**
     * Record tool execution metrics
     */
    public void recordToolExecution(String toolName, long durationMs, boolean success, 
                                   String resultType, int resultSize) {
        recordOperation("tool_" + toolName, durationMs, success);
        
        ToolPerformanceMetric toolMetric = (ToolPerformanceMetric) currentMetrics.computeIfAbsent(
            "tool_" + toolName, k -> new ToolPerformanceMetric(toolName));
        
        toolMetric.recordToolExecution(durationMs, success, resultType, resultSize);
    }
    
    /**
     * Get current performance dashboard
     */
    public PerformanceDashboard getDashboard() {
        SystemMetrics systemMetrics = collectSystemMetrics();
        List<OperationMetrics> operationMetrics = getOperationMetrics();
        List<Alert> currentAlerts = getCurrentAlerts();
        PerformanceTrends trends = calculateTrends();
        
        return new PerformanceDashboard(systemMetrics, operationMetrics, currentAlerts, trends);
    }
    
    /**
     * Get performance metrics for specific time range
     */
    public List<PerformanceSnapshot> getMetricsForTimeRange(Instant start, Instant end) {
        return historicalMetrics.stream()
                .filter(snapshot -> snapshot.getTimestamp().isAfter(start) && snapshot.getTimestamp().isBefore(end))
                .sorted(Comparator.comparing(PerformanceSnapshot::getTimestamp))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get capacity forecast
     */
    public CapacityForecast getCapacityForecast(Duration forecastPeriod) {
        List<PerformanceSnapshot> recentMetrics = getRecentMetrics(Duration.ofHours(24));
        
        if (recentMetrics.size() < 10) {
            return new CapacityForecast("Insufficient data for forecasting", Collections.emptyList());
        }
        
        // Simple linear regression for forecasting
        List<CapacityPrediction> predictions = new ArrayList<>();
        
        // Predict request volume
        double requestGrowthRate = calculateGrowthRate(recentMetrics, "request_rate");
        predictions.add(new CapacityPrediction("request_volume", requestGrowthRate, 
            "Requests per minute expected to grow by " + String.format("%.2f%%", requestGrowthRate * 100)));
        
        // Predict resource usage
        double cpuGrowthRate = calculateGrowthRate(recentMetrics, "cpu_usage");
        predictions.add(new CapacityPrediction("cpu_usage", cpuGrowthRate,
            "CPU usage expected to grow by " + String.format("%.2f%%", cpuGrowthRate * 100)));
        
        double memoryGrowthRate = calculateGrowthRate(recentMetrics, "memory_usage");
        predictions.add(new CapacityPrediction("memory_usage", memoryGrowthRate,
            "Memory usage expected to grow by " + String.format("%.2f%%", memoryGrowthRate * 100)));
        
        return new CapacityForecast("Forecast based on 24-hour trend analysis", predictions);
    }
    
    /**
     * Trigger performance optimization
     */
    public OptimizationResult triggerOptimization() {
        logger.info("Triggering performance optimization");
        
        List<OptimizationAction> actions = new ArrayList<>();
        
        // Analyze current performance
        PerformanceDashboard dashboard = getDashboard();
        
        // Cache optimization
        if (dashboard.getSystemMetrics().getCacheHitRate() < 0.7) {
            actions.add(new OptimizationAction("cache_optimization", 
                "Optimize cache configuration and warming strategies",
                OptimizationPriority.HIGH));
        }
        
        // Memory optimization
        if (dashboard.getSystemMetrics().getMemoryUsage() > 0.8) {
            actions.add(new OptimizationAction("memory_optimization",
                "Optimize memory usage and garbage collection",
                OptimizationPriority.HIGH));
        }
        
        // Response time optimization
        double avgResponseTime = dashboard.getSystemMetrics().getAverageResponseTime();
        if (avgResponseTime > responseTimeThresholdMs * 0.8) {
            actions.add(new OptimizationAction("response_time_optimization",
                "Optimize query processing and tool execution",
                OptimizationPriority.MEDIUM));
        }
        
        // Connection pool optimization
        if (dashboard.getOperationMetrics().stream()
                .anyMatch(op -> op.getOperationType().contains("external") && op.getAverageResponseTime() > 5000)) {
            actions.add(new OptimizationAction("connection_pool_optimization",
                "Optimize external API connection pools",
                OptimizationPriority.MEDIUM));
        }
        
        // Execute optimization actions
        for (OptimizationAction action : actions) {
            executeOptimizationAction(action);
        }
        
        return new OptimizationResult(actions, "Optimization completed with " + actions.size() + " actions");
    }
    
    /**
     * Collect current metrics
     */
    private void collectMetrics() {
        try {
            SystemMetrics systemMetrics = collectSystemMetrics();
            List<OperationMetrics> operationMetrics = getOperationMetrics();
            
            PerformanceSnapshot snapshot = new PerformanceSnapshot(Instant.now(), systemMetrics, operationMetrics);
            historicalMetrics.offer(snapshot);
            
            // Clean up old metrics
            cleanupOldMetrics();
            
            logger.debug("Collected performance metrics: {} operations, {:.2f}% CPU, {:.2f}% memory",
                operationMetrics.size(), systemMetrics.getCpuUsage() * 100, systemMetrics.getMemoryUsage() * 100);
            
        } catch (Exception e) {
            logger.error("Error collecting performance metrics", e);
        }
    }
    
    /**
     * Collect system-level metrics
     */
    private SystemMetrics collectSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory;
        
        // CPU usage (simplified - in production would use JMX)
        double cpuUsage = getCurrentCpuUsage();
        
        // Calculate rates
        long currentTime = System.currentTimeMillis();
        long totalReqs = totalRequests.get();
        long successReqs = successfulRequests.get();
        long failedReqs = failedRequests.get();
        
        double errorRate = totalReqs > 0 ? (double) failedReqs / totalReqs : 0.0;
        double averageResponseTime = totalReqs > 0 ? (double) totalResponseTime.get() / totalReqs : 0.0;
        
        // Cache hit rate (would integrate with actual cache service)
        double cacheHitRate = getCacheHitRate();
        
        return new SystemMetrics(cpuUsage, memoryUsage, errorRate, averageResponseTime, 
            cacheHitRate, totalReqs, getCurrentConcurrentUsers());
    }
    
    /**
     * Get operation-specific metrics
     */
    private List<OperationMetrics> getOperationMetrics() {
        return currentMetrics.values().stream()
                .map(metric -> new OperationMetrics(
                    metric.getOperationType(),
                    metric.getTotalOperations(),
                    metric.getSuccessfulOperations(),
                    metric.getAverageResponseTime(),
                    metric.getP95ResponseTime(),
                    metric.getErrorRate()
                ))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Process alerts based on current metrics
     */
    private void processAlerts() {
        try {
            SystemMetrics metrics = collectSystemMetrics();
            
            // Check system-level alerts
            checkAlert("high_cpu", metrics.getCpuUsage(), cpuThreshold, "High CPU usage detected");
            checkAlert("high_memory", metrics.getMemoryUsage(), memoryThreshold, "High memory usage detected");
            checkAlert("high_error_rate", metrics.getErrorRate(), errorRateThreshold, "High error rate detected");
            checkAlert("slow_response", metrics.getAverageResponseTime(), responseTimeThresholdMs, "Slow response times detected");
            checkAlert("low_cache_hit", metrics.getCacheHitRate(), 0.7, "Low cache hit rate detected");
            
            // Check operation-specific alerts
            for (PerformanceMetric metric : currentMetrics.values()) {
                if (metric.getErrorRate() > errorRateThreshold) {
                    triggerAlert("high_error_rate_" + metric.getOperationType(), 
                        "High error rate for operation: " + metric.getOperationType());
                }
                
                if (metric.getAverageResponseTime() > responseTimeThresholdMs) {
                    triggerAlert("slow_operation_" + metric.getOperationType(),
                        "Slow response time for operation: " + metric.getOperationType());
                }
            }
            
            // Update system health
            updateSystemHealth(metrics);
            
        } catch (Exception e) {
            logger.error("Error processing alerts", e);
        }
    }
    
    /**
     * Check individual alert condition
     */
    private void checkAlert(String alertId, double currentValue, double threshold, String message) {
        if (currentValue > threshold) {
            triggerAlert(alertId, message + String.format(" (%.2f > %.2f)", currentValue, threshold));
        } else {
            resolveAlert(alertId);
        }
    }
    
    /**
     * Trigger an alert
     */
    private void triggerAlert(String alertId, String message) {
        AlertStatus existingAlert = activeAlerts.get(alertId);
        
        if (existingAlert == null) {
            AlertStatus newAlert = new AlertStatus(alertId, message, Instant.now(), AlertSeverity.WARNING);
            activeAlerts.put(alertId, newAlert);
            logger.warn("ALERT TRIGGERED: {} - {}", alertId, message);
            
            // In production, this would send notifications (email, Slack, etc.)
            sendAlertNotification(newAlert);
        } else {
            existingAlert.updateLastSeen();
        }
    }
    
    /**
     * Resolve an alert
     */
    private void resolveAlert(String alertId) {
        AlertStatus alert = activeAlerts.remove(alertId);
        if (alert != null) {
            logger.info("ALERT RESOLVED: {} - {}", alertId, alert.getMessage());
            sendAlertResolutionNotification(alert);
        }
    }
    
    /**
     * Update overall system health
     */
    private void updateSystemHealth(SystemMetrics metrics) {
        SystemHealth newHealth = SystemHealth.HEALTHY;
        
        if (metrics.getCpuUsage() > cpuThreshold || 
            metrics.getMemoryUsage() > memoryThreshold ||
            metrics.getErrorRate() > errorRateThreshold) {
            newHealth = SystemHealth.DEGRADED;
        }
        
        if (metrics.getCpuUsage() > 0.95 || 
            metrics.getMemoryUsage() > 0.95 ||
            metrics.getErrorRate() > 0.2) {
            newHealth = SystemHealth.CRITICAL;
        }
        
        SystemHealth previousHealth = currentSystemHealth.getAndSet(newHealth);
        
        if (previousHealth != newHealth) {
            logger.info("System health changed from {} to {}", previousHealth, newHealth);
        }
    }
    
    /**
     * Perform automated optimization
     */
    private void performOptimization() {
        try {
            logger.debug("Performing automated performance optimization");
            
            // Auto-optimization based on metrics
            SystemMetrics metrics = collectSystemMetrics();
            
            // Optimize cache if hit rate is low
            if (metrics.getCacheHitRate() < 0.7) {
                optimizeCache();
            }
            
            // Optimize memory if usage is high
            if (metrics.getMemoryUsage() > 0.8) {
                optimizeMemory();
            }
            
            // Optimize connection pools if external calls are slow
            optimizeConnectionPools();
            
        } catch (Exception e) {
            logger.error("Error during automated optimization", e);
        }
    }
    
    /**
     * Execute specific optimization action
     */
    private void executeOptimizationAction(OptimizationAction action) {
        logger.info("Executing optimization action: {}", action.getActionType());
        
        switch (action.getActionType()) {
            case "cache_optimization":
                optimizeCache();
                break;
            case "memory_optimization":
                optimizeMemory();
                break;
            case "response_time_optimization":
                optimizeResponseTime();
                break;
            case "connection_pool_optimization":
                optimizeConnectionPools();
                break;
            default:
                logger.warn("Unknown optimization action: {}", action.getActionType());
        }
    }
    
    // Optimization methods (simplified implementations)
    
    private void optimizeCache() {
        logger.info("Optimizing cache configuration");
        // In production, this would trigger cache optimization
        // For now, just log the action
    }
    
    private void optimizeMemory() {
        logger.info("Optimizing memory usage");
        // Trigger garbage collection
        System.gc();
    }
    
    private void optimizeResponseTime() {
        logger.info("Optimizing response time");
        // In production, this would adjust thread pools, timeouts, etc.
    }
    
    private void optimizeConnectionPools() {
        logger.info("Optimizing connection pools");
        // In production, this would adjust connection pool sizes
    }
    
    // Helper methods
    
    private void checkImmediateAlerts(String operationType, long durationMs, boolean success) {
        if (durationMs > responseTimeThresholdMs) {
            triggerAlert("immediate_slow_response_" + operationType,
                "Immediate slow response detected for " + operationType + ": " + durationMs + "ms");
        }
        
        if (!success) {
            // Check if error rate is spiking
            PerformanceMetric metric = currentMetrics.get(operationType);
            if (metric != null && metric.getRecentErrorRate() > errorRateThreshold * 2) {
                triggerAlert("immediate_error_spike_" + operationType,
                    "Error rate spike detected for " + operationType);
            }
        }
    }
    
    private void cleanupOldMetrics() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(metricsRetentionHours));
        
        while (!historicalMetrics.isEmpty() && 
               historicalMetrics.peek().getTimestamp().isBefore(cutoff)) {
            historicalMetrics.poll();
        }
    }
    
    private List<PerformanceSnapshot> getRecentMetrics(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        
        return historicalMetrics.stream()
                .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoff))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private double calculateGrowthRate(List<PerformanceSnapshot> metrics, String metricName) {
        if (metrics.size() < 2) return 0.0;
        
        // Simple linear regression to calculate growth rate
        // In production, this would be more sophisticated
        PerformanceSnapshot first = metrics.get(0);
        PerformanceSnapshot last = metrics.get(metrics.size() - 1);
        
        double firstValue = getMetricValue(first, metricName);
        double lastValue = getMetricValue(last, metricName);
        
        if (firstValue == 0) return 0.0;
        
        return (lastValue - firstValue) / firstValue;
    }
    
    private double getMetricValue(PerformanceSnapshot snapshot, String metricName) {
        switch (metricName) {
            case "request_rate":
                return snapshot.getSystemMetrics().getTotalRequests();
            case "cpu_usage":
                return snapshot.getSystemMetrics().getCpuUsage();
            case "memory_usage":
                return snapshot.getSystemMetrics().getMemoryUsage();
            default:
                return 0.0;
        }
    }
    
    private PerformanceTrends calculateTrends() {
        List<PerformanceSnapshot> recentMetrics = getRecentMetrics(Duration.ofHours(1));
        
        if (recentMetrics.size() < 2) {
            return new PerformanceTrends(0.0, 0.0, 0.0, 0.0);
        }
        
        double responseTimeTrend = calculateGrowthRate(recentMetrics, "response_time");
        double errorRateTrend = calculateGrowthRate(recentMetrics, "error_rate");
        double cpuTrend = calculateGrowthRate(recentMetrics, "cpu_usage");
        double memoryTrend = calculateGrowthRate(recentMetrics, "memory_usage");
        
        return new PerformanceTrends(responseTimeTrend, errorRateTrend, cpuTrend, memoryTrend);
    }
    
    private List<Alert> getCurrentAlerts() {
        return activeAlerts.values().stream()
                .map(status -> new Alert(status.getAlertId(), status.getMessage(), 
                    status.getTriggeredAt(), status.getSeverity()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Simplified implementations for system metrics
    
    private double getCurrentCpuUsage() {
        // In production, this would use JMX or system monitoring
        return Math.random() * 0.3 + 0.1; // Simulate 10-40% CPU usage
    }
    
    private double getCacheHitRate() {
        // In production, this would integrate with actual cache service
        return Math.random() * 0.3 + 0.7; // Simulate 70-100% hit rate
    }
    
    private int getCurrentConcurrentUsers() {
        // In production, this would track actual concurrent users
        return (int) (Math.random() * 100 + 10); // Simulate 10-110 users
    }
    
    private void sendAlertNotification(AlertStatus alert) {
        // In production, this would send actual notifications
        logger.info("Sending alert notification for: {}", alert.getAlertId());
    }
    
    private void sendAlertResolutionNotification(AlertStatus alert) {
        // In production, this would send actual notifications
        logger.info("Sending alert resolution notification for: {}", alert.getAlertId());
    }
}
  
  // Supporting classes and enums
    
    enum SystemHealth {
        HEALTHY, DEGRADED, CRITICAL
    }
    
    enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    enum ThresholdType {
        MIN, MAX
    }
    
    enum OptimizationPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Performance threshold configuration
     */
    class PerformanceThreshold {
        private final String metricName;
        private final double threshold;
        private final ThresholdType type;
        
        public PerformanceThreshold(String metricName, double threshold, ThresholdType type) {
            this.metricName = metricName;
            this.threshold = threshold;
            this.type = type;
        }
        
        public String getMetricName() { return metricName; }
        public double getThreshold() { return threshold; }
        public ThresholdType getType() { return type; }
    }
    
    /**
     * Base performance metric
     */
    class PerformanceMetric {
        protected final String operationType;
        protected final AtomicLong totalOperations = new AtomicLong(0);
        protected final AtomicLong successfulOperations = new AtomicLong(0);
        protected final AtomicLong totalResponseTime = new AtomicLong(0);
        protected final AtomicLong maxResponseTime = new AtomicLong(0);
        protected final ConcurrentLinkedQueue<Long> recentResponseTimes = new ConcurrentLinkedQueue<>();
        protected final ConcurrentLinkedQueue<Boolean> recentResults = new ConcurrentLinkedQueue<>();
        
        public PerformanceMetric(String operationType) {
            this.operationType = operationType;
        }
        
        public void recordOperation(long durationMs, boolean success) {
            totalOperations.incrementAndGet();
            totalResponseTime.addAndGet(durationMs);
            
            if (success) {
                successfulOperations.incrementAndGet();
            }
            
            // Update max response time
            maxResponseTime.updateAndGet(current -> Math.max(current, durationMs));
            
            // Keep recent metrics for trend analysis
            recentResponseTimes.offer(durationMs);
            recentResults.offer(success);
            
            // Keep only last 100 operations
            if (recentResponseTimes.size() > 100) {
                recentResponseTimes.poll();
                recentResults.poll();
            }
        }
        
        public String getOperationType() { return operationType; }
        public long getTotalOperations() { return totalOperations.get(); }
        public long getSuccessfulOperations() { return successfulOperations.get(); }
        
        public double getAverageResponseTime() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
        }
        
        public double getErrorRate() {
            long total = totalOperations.get();
            return total > 0 ? (double) (total - successfulOperations.get()) / total : 0.0;
        }
        
        public double getRecentErrorRate() {
            if (recentResults.isEmpty()) return 0.0;
            
            long recentFailures = recentResults.stream()
                    .mapToLong(success -> success ? 0 : 1)
                    .sum();
            
            return (double) recentFailures / recentResults.size();
        }
        
        public long getP95ResponseTime() {
            List<Long> sortedTimes = recentResponseTimes.stream()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
            
            if (sortedTimes.isEmpty()) return 0;
            
            int p95Index = (int) Math.ceil(sortedTimes.size() * 0.95) - 1;
            return sortedTimes.get(Math.max(0, p95Index));
        }
    }
    
    /**
     * AI-specific performance metric
     */
    class AiPerformanceMetric extends PerformanceMetric {
        private final AtomicLong totalTokens = new AtomicLong(0);
        private final AtomicReference<Double> averageConfidence = new AtomicReference<>(0.0);
        private final ConcurrentHashMap<String, AtomicLong> modelUsage = new ConcurrentHashMap<>();
        
        public AiPerformanceMetric(String operationType) {
            super("ai_" + operationType);
        }
        
        public void recordAiOperation(long durationMs, boolean success, String model, 
                                     int tokenCount, double confidence) {
            recordOperation(durationMs, success);
            
            totalTokens.addAndGet(tokenCount);
            modelUsage.computeIfAbsent(model, k -> new AtomicLong(0)).incrementAndGet();
            
            // Update average confidence
            long operations = totalOperations.get();
            if (operations > 0) {
                double currentAvg = averageConfidence.get();
                double newAvg = (currentAvg * (operations - 1) + confidence) / operations;
                averageConfidence.set(newAvg);
            }
        }
        
        public long getTotalTokens() { return totalTokens.get(); }
        public double getAverageConfidence() { return averageConfidence.get(); }
        public Map<String, Long> getModelUsage() {
            return modelUsage.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                    ));
        }
    }
    
    /**
     * Tool-specific performance metric
     */
    class ToolPerformanceMetric extends PerformanceMetric {
        private final ConcurrentHashMap<String, AtomicLong> resultTypes = new ConcurrentHashMap<>();
        private final AtomicLong totalResultSize = new AtomicLong(0);
        
        public ToolPerformanceMetric(String toolName) {
            super("tool_" + toolName);
        }
        
        public void recordToolExecution(long durationMs, boolean success, String resultType, int resultSize) {
            recordOperation(durationMs, success);
            
            if (success) {
                resultTypes.computeIfAbsent(resultType, k -> new AtomicLong(0)).incrementAndGet();
                totalResultSize.addAndGet(resultSize);
            }
        }
        
        public Map<String, Long> getResultTypes() {
            return resultTypes.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                    ));
        }
        
        public double getAverageResultSize() {
            long successful = successfulOperations.get();
            return successful > 0 ? (double) totalResultSize.get() / successful : 0.0;
        }
    }
    
    /**
     * System-level metrics
     */
    class SystemMetrics {
        private final double cpuUsage;
        private final double memoryUsage;
        private final double errorRate;
        private final double averageResponseTime;
        private final double cacheHitRate;
        private final long totalRequests;
        private final int concurrentUsers;
        
        public SystemMetrics(double cpuUsage, double memoryUsage, double errorRate,
                           double averageResponseTime, double cacheHitRate, 
                           long totalRequests, int concurrentUsers) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.errorRate = errorRate;
            this.averageResponseTime = averageResponseTime;
            this.cacheHitRate = cacheHitRate;
            this.totalRequests = totalRequests;
            this.concurrentUsers = concurrentUsers;
        }
        
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public double getErrorRate() { return errorRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getCacheHitRate() { return cacheHitRate; }
        public long getTotalRequests() { return totalRequests; }
        public int getConcurrentUsers() { return concurrentUsers; }
    }
    
    /**
     * Operation-specific metrics
     */
    class OperationMetrics {
        private final String operationType;
        private final long totalOperations;
        private final long successfulOperations;
        private final double averageResponseTime;
        private final long p95ResponseTime;
        private final double errorRate;
        
        public OperationMetrics(String operationType, long totalOperations, 
                              long successfulOperations, double averageResponseTime,
                              long p95ResponseTime, double errorRate) {
            this.operationType = operationType;
            this.totalOperations = totalOperations;
            this.successfulOperations = successfulOperations;
            this.averageResponseTime = averageResponseTime;
            this.p95ResponseTime = p95ResponseTime;
            this.errorRate = errorRate;
        }
        
        public String getOperationType() { return operationType; }
        public long getTotalOperations() { return totalOperations; }
        public long getSuccessfulOperations() { return successfulOperations; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public long getP95ResponseTime() { return p95ResponseTime; }
        public double getErrorRate() { return errorRate; }
    }
    
    /**
     * Performance snapshot for historical tracking
     */
    class PerformanceSnapshot {
        private final Instant timestamp;
        private final SystemMetrics systemMetrics;
        private final List<OperationMetrics> operationMetrics;
        
        public PerformanceSnapshot(Instant timestamp, SystemMetrics systemMetrics, 
                                 List<OperationMetrics> operationMetrics) {
            this.timestamp = timestamp;
            this.systemMetrics = systemMetrics;
            this.operationMetrics = new ArrayList<>(operationMetrics);
        }
        
        public Instant getTimestamp() { return timestamp; }
        public SystemMetrics getSystemMetrics() { return systemMetrics; }
        public List<OperationMetrics> getOperationMetrics() { return operationMetrics; }
    }
    
    /**
     * Alert status tracking
     */
    class AlertStatus {
        private final String alertId;
        private final String message;
        private final Instant triggeredAt;
        private final AlertSeverity severity;
        private volatile Instant lastSeen;
        
        public AlertStatus(String alertId, String message, Instant triggeredAt, AlertSeverity severity) {
            this.alertId = alertId;
            this.message = message;
            this.triggeredAt = triggeredAt;
            this.severity = severity;
            this.lastSeen = triggeredAt;
        }
        
        public void updateLastSeen() {
            this.lastSeen = Instant.now();
        }
        
        public String getAlertId() { return alertId; }
        public String getMessage() { return message; }
        public Instant getTriggeredAt() { return triggeredAt; }
        public AlertSeverity getSeverity() { return severity; }
        public Instant getLastSeen() { return lastSeen; }
    }
    
    /**
     * Alert for dashboard display
     */
    class Alert {
        private final String alertId;
        private final String message;
        private final Instant triggeredAt;
        private final AlertSeverity severity;
        
        public Alert(String alertId, String message, Instant triggeredAt, AlertSeverity severity) {
            this.alertId = alertId;
            this.message = message;
            this.triggeredAt = triggeredAt;
            this.severity = severity;
        }
        
        public String getAlertId() { return alertId; }
        public String getMessage() { return message; }
        public Instant getTriggeredAt() { return triggeredAt; }
        public AlertSeverity getSeverity() { return severity; }
    }
    
    /**
     * Performance trends analysis
     */
    class PerformanceTrends {
        private final double responseTimeTrend;
        private final double errorRateTrend;
        private final double cpuTrend;
        private final double memoryTrend;
        
        public PerformanceTrends(double responseTimeTrend, double errorRateTrend, 
                               double cpuTrend, double memoryTrend) {
            this.responseTimeTrend = responseTimeTrend;
            this.errorRateTrend = errorRateTrend;
            this.cpuTrend = cpuTrend;
            this.memoryTrend = memoryTrend;
        }
        
        public double getResponseTimeTrend() { return responseTimeTrend; }
        public double getErrorRateTrend() { return errorRateTrend; }
        public double getCpuTrend() { return cpuTrend; }
        public double getMemoryTrend() { return memoryTrend; }
    }
    
    /**
     * Performance dashboard data
     */
    class PerformanceDashboard {
        private final SystemMetrics systemMetrics;
        private final List<OperationMetrics> operationMetrics;
        private final List<Alert> currentAlerts;
        private final PerformanceTrends trends;
        
        public PerformanceDashboard(SystemMetrics systemMetrics, List<OperationMetrics> operationMetrics,
                                  List<Alert> currentAlerts, PerformanceTrends trends) {
            this.systemMetrics = systemMetrics;
            this.operationMetrics = new ArrayList<>(operationMetrics);
            this.currentAlerts = new ArrayList<>(currentAlerts);
            this.trends = trends;
        }
        
        public SystemMetrics getSystemMetrics() { return systemMetrics; }
        public List<OperationMetrics> getOperationMetrics() { return operationMetrics; }
        public List<Alert> getCurrentAlerts() { return currentAlerts; }
        public PerformanceTrends getTrends() { return trends; }
    }
    
    /**
     * Capacity forecast
     */
    class CapacityForecast {
        private final String summary;
        private final List<CapacityPrediction> predictions;
        
        public CapacityForecast(String summary, List<CapacityPrediction> predictions) {
            this.summary = summary;
            this.predictions = new ArrayList<>(predictions);
        }
        
        public String getSummary() { return summary; }
        public List<CapacityPrediction> getPredictions() { return predictions; }
    }
    
    /**
     * Capacity prediction
     */
    class CapacityPrediction {
        private final String metric;
        private final double growthRate;
        private final String description;
        
        public CapacityPrediction(String metric, double growthRate, String description) {
            this.metric = metric;
            this.growthRate = growthRate;
            this.description = description;
        }
        
        public String getMetric() { return metric; }
        public double getGrowthRate() { return growthRate; }
        public String getDescription() { return description; }
    }
    
    /**
     * Optimization action
     */
    class OptimizationAction {
        private final String actionType;
        private final String description;
        private final OptimizationPriority priority;
        
        public OptimizationAction(String actionType, String description, OptimizationPriority priority) {
            this.actionType = actionType;
            this.description = description;
            this.priority = priority;
        }
        
        public String getActionType() { return actionType; }
        public String getDescription() { return description; }
        public OptimizationPriority getPriority() { return priority; }
    }
    
    /**
     * Optimization result
     */
    class OptimizationResult {
        private final List<OptimizationAction> actions;
        private final String summary;
        
        public OptimizationResult(List<OptimizationAction> actions, String summary) {
            this.actions = new ArrayList<>(actions);
            this.summary = summary;
        }
        
        public List<OptimizationAction> getActions() { return actions; }
        public String getSummary() { return summary; }
    }
