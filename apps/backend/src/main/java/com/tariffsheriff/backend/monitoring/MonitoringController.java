package com.tariffsheriff.backend.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * REST controller for monitoring and metrics endpoints
 */
@RestController
@RequestMapping("/api/monitoring")
@PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
public class MonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    @Autowired
    private AiMetricsService aiMetricsService;
    
    @Autowired
    private BusinessIntelligenceService businessIntelligenceService;
    
    @Autowired
    private AlertingService alertingService;
    
    @Autowired
    private LogAggregationService logAggregationService;
    
    /**
     * Get AI metrics summary
     */
    @GetMapping("/metrics/ai")
    public ResponseEntity<?> getAiMetrics() {
        try {
            Map<String, Object> metrics = aiMetricsService.getMetricsSummary();
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting AI metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get AI metrics"));
        }
    }
    
    /**
     * Get business intelligence report
     */
    @GetMapping("/analytics/business-intelligence")
    public ResponseEntity<?> getBusinessIntelligence() {
        try {
            Map<String, Object> report = businessIntelligenceService.getBusinessIntelligenceReport();
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            logger.error("Error getting business intelligence: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get business intelligence"));
        }
    }
    
    /**
     * Get user analytics
     */
    @GetMapping("/analytics/user/{userId}")
    public ResponseEntity<?> getUserAnalytics(@PathVariable String userId) {
        try {
            Map<String, Object> analytics = businessIntelligenceService.getUserAnalytics(userId);
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            logger.error("Error getting user analytics for {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get user analytics"));
        }
    }
    
    /**
     * Get feature usage analytics
     */
    @GetMapping("/analytics/features")
    public ResponseEntity<?> getFeatureAnalytics() {
        try {
            Map<String, Object> analytics = businessIntelligenceService.getFeatureAnalytics();
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            logger.error("Error getting feature analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get feature analytics"));
        }
    }
    
    /**
     * Get query pattern analytics
     */
    @GetMapping("/analytics/query-patterns")
    public ResponseEntity<?> getQueryPatternAnalytics() {
        try {
            Map<String, Object> analytics = businessIntelligenceService.getQueryPatternAnalytics();
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            logger.error("Error getting query pattern analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get query pattern analytics"));
        }
    }
    
    /**
     * Get current alert states
     */
    @GetMapping("/alerts/states")
    public ResponseEntity<?> getAlertStates() {
        try {
            Map<String, AlertingService.AlertState> states = alertingService.getAlertStates();
            return ResponseEntity.ok(states);
            
        } catch (Exception e) {
            logger.error("Error getting alert states: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get alert states"));
        }
    }
    
    /**
     * Get active alerts
     */
    @GetMapping("/alerts/active")
    public ResponseEntity<?> getActiveAlerts() {
        try {
            var activeAlerts = alertingService.getActiveAlerts();
            return ResponseEntity.ok(activeAlerts);
            
        } catch (Exception e) {
            logger.error("Error getting active alerts: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get active alerts"));
        }
    }
    
    /**
     * Trigger test alert
     */
    @PostMapping("/alerts/test")
    public ResponseEntity<?> triggerTestAlert() {
        try {
            alertingService.triggerTestAlert();
            return ResponseEntity.ok(Map.of("status", "Test alert triggered"));
            
        } catch (Exception e) {
            logger.error("Error triggering test alert: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to trigger test alert"));
        }
    }
    
    /**
     * Get log entries
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getLogEntries(
            @RequestParam(defaultValue = "INFO") String level,
            @RequestParam(required = false) String loggerName,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            LogAggregationService.LogLevel logLevel = LogAggregationService.LogLevel.valueOf(level.toUpperCase());
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            
            var logEntries = logAggregationService.getLogEntries(logLevel, loggerName, since, limit);
            return ResponseEntity.ok(logEntries);
            
        } catch (Exception e) {
            logger.error("Error getting log entries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get log entries"));
        }
    }
    
    /**
     * Get AI-specific log entries
     */
    @GetMapping("/logs/ai")
    public ResponseEntity<?> getAiLogEntries(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            var logEntries = logAggregationService.getAiLogEntries(since, limit);
            return ResponseEntity.ok(logEntries);
            
        } catch (Exception e) {
            logger.error("Error getting AI log entries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get AI log entries"));
        }
    }
    
    /**
     * Get error log entries
     */
    @GetMapping("/logs/errors")
    public ResponseEntity<?> getErrorLogEntries(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            var logEntries = logAggregationService.getErrorLogEntries(since, limit);
            return ResponseEntity.ok(logEntries);
            
        } catch (Exception e) {
            logger.error("Error getting error log entries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get error log entries"));
        }
    }
    
    /**
     * Get log analysis
     */
    @GetMapping("/logs/analysis")
    public ResponseEntity<?> getLogAnalysis(@RequestParam(defaultValue = "24") int hours) {
        try {
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            Map<String, Object> analysis = logAggregationService.getLogAnalysis(since);
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("Error getting log analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get log analysis"));
        }
    }
    
    /**
     * Get log patterns
     */
    @GetMapping("/logs/patterns")
    public ResponseEntity<?> getLogPatterns() {
        try {
            Map<String, LogAggregationService.LogPattern> patterns = logAggregationService.getLogPatterns();
            return ResponseEntity.ok(patterns);
            
        } catch (Exception e) {
            logger.error("Error getting log patterns: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get log patterns"));
        }
    }
    
    /**
     * Get log statistics
     */
    @GetMapping("/logs/statistics")
    public ResponseEntity<?> getLogStatistics() {
        try {
            Map<String, LogAggregationService.LogStatistics> statistics = logAggregationService.getLogStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting log statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get log statistics"));
        }
    }
    
    /**
     * Get comprehensive monitoring dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getMonitoringDashboard() {
        try {
            Map<String, Object> dashboard = Map.of(
                "metrics", aiMetricsService.getMetricsSummary(),
                "businessIntelligence", businessIntelligenceService.getBusinessIntelligenceReport(),
                "activeAlerts", alertingService.getActiveAlerts(),
                "logAnalysis", logAggregationService.getLogAnalysis(Instant.now().minus(24, ChronoUnit.HOURS)),
                "timestamp", Instant.now()
            );
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            logger.error("Error getting monitoring dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get monitoring dashboard"));
        }
    }
    
    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        try {
            Map<String, Object> metrics = aiMetricsService.getMetricsSummary();
            var activeAlerts = alertingService.getActiveAlerts();
            
            // Determine overall health status
            String status = "healthy";
            if (!activeAlerts.isEmpty()) {
                boolean hasCritical = activeAlerts.stream()
                    .anyMatch(alert -> alert.getType().name().contains("CRITICAL"));
                status = hasCritical ? "critical" : "warning";
            }
            
            Map<String, Object> health = Map.of(
                "status", status,
                "timestamp", Instant.now(),
                "activeAlerts", activeAlerts.size(),
                "metrics", Map.of(
                    "totalQueries", metrics.getOrDefault("queries.total", 0),
                    "errorRate", calculateErrorRate(metrics),
                    "averageResponseTime", metrics.getOrDefault("query.processing.time.mean", 0),
                    "activeQueries", metrics.getOrDefault("queries.active", 0),
                    "queuedQueries", metrics.getOrDefault("queries.queued", 0)
                )
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error getting system health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "error", "Failed to get system health",
                    "timestamp", Instant.now()
                ));
        }
    }
    
    /**
     * Reset metrics (for testing)
     */
    @PostMapping("/metrics/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetMetrics() {
        try {
            aiMetricsService.resetMetrics();
            alertingService.clearAlertStates();
            logAggregationService.clearLogEntries();
            
            return ResponseEntity.ok(Map.of("status", "Metrics reset successfully"));
            
        } catch (Exception e) {
            logger.error("Error resetting metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to reset metrics"));
        }
    }
    
    private double calculateErrorRate(Map<String, Object> metrics) {
        double totalQueries = ((Number) metrics.getOrDefault("queries.total", 0)).doubleValue();
        double failedQueries = ((Number) metrics.getOrDefault("queries.failed", 0)).doubleValue();
        return totalQueries > 0 ? failedQueries / totalQueries : 0.0;
    }
}