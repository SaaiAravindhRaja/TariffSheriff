package com.tariffsheriff.backend.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for monitoring AI system health and sending alerts
 */
@Service
public class AlertingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);
    
    @Autowired
    private AiMetricsService aiMetricsService;
    
    @Autowired
    private BusinessIntelligenceService businessIntelligenceService;
    
    @Value("${monitoring.alerts.enabled:true}")
    private boolean alertsEnabled;
    
    @Value("${monitoring.alerts.email.enabled:false}")
    private boolean emailAlertsEnabled;
    
    @Value("${monitoring.alerts.slack.enabled:false}")
    private boolean slackAlertsEnabled;
    
    @Value("${monitoring.alerts.webhook.url:}")
    private String webhookUrl;
    
    // Alert thresholds
    @Value("${monitoring.thresholds.error-rate:0.05}")
    private double errorRateThreshold;
    
    @Value("${monitoring.thresholds.response-time:10000}")
    private long responseTimeThreshold;
    
    @Value("${monitoring.thresholds.queue-size:100}")
    private long queueSizeThreshold;
    
    @Value("${monitoring.thresholds.memory-usage:0.85}")
    private double memoryUsageThreshold;
    
    @Value("${monitoring.thresholds.cache-hit-ratio:0.70}")
    private double cacheHitRatioThreshold;
    
    // Alert state tracking
    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastAlertTimes = new ConcurrentHashMap<>();
    private final Duration alertCooldown = Duration.ofMinutes(15);
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL, EMERGENCY
    }
    
    public enum AlertType {
        HIGH_ERROR_RATE,
        SLOW_RESPONSE_TIME,
        HIGH_QUEUE_SIZE,
        HIGH_MEMORY_USAGE,
        LOW_CACHE_HIT_RATIO,
        SERVICE_UNAVAILABLE,
        AGENT_FAILURE,
        DATA_QUALITY_ISSUE,
        SECURITY_THREAT,
        PERFORMANCE_DEGRADATION
    }
    
    public static class Alert {
        private String id;
        private AlertType type;
        private AlertSeverity severity;
        private String title;
        private String description;
        private Map<String, Object> metadata;
        private Instant timestamp;
        private boolean resolved;
        private Instant resolvedAt;
        
        public Alert(AlertType type, AlertSeverity severity, String title, String description) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.metadata = new HashMap<>();
            this.timestamp = Instant.now();
            this.resolved = false;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public AlertType getType() { return type; }
        public AlertSeverity getSeverity() { return severity; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
        public Instant getTimestamp() { return timestamp; }
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { 
            this.resolved = resolved; 
            if (resolved) {
                this.resolvedAt = Instant.now();
            }
        }
        public Instant getResolvedAt() { return resolvedAt; }
    }
    
    public static class AlertState {
        private AlertType type;
        private boolean active;
        private Instant firstTriggered;
        private Instant lastTriggered;
        private int triggerCount;
        private double currentValue;
        private double threshold;
        
        public AlertState(AlertType type) {
            this.type = type;
            this.active = false;
            this.triggerCount = 0;
        }
        
        // Getters and setters
        public AlertType getType() { return type; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { 
            this.active = active;
            if (active && firstTriggered == null) {
                this.firstTriggered = Instant.now();
            }
            if (active) {
                this.lastTriggered = Instant.now();
                this.triggerCount++;
            }
        }
        public Instant getFirstTriggered() { return firstTriggered; }
        public Instant getLastTriggered() { return lastTriggered; }
        public int getTriggerCount() { return triggerCount; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
    }
    
    /**
     * Check all system metrics and trigger alerts if necessary
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkSystemHealth() {
        if (!alertsEnabled) {
            return;
        }
        
        try {
            Map<String, Object> metrics = aiMetricsService.getMetricsSummary();
            
            // Check error rate
            checkErrorRate(metrics);
            
            // Check response time
            checkResponseTime(metrics);
            
            // Check queue size
            checkQueueSize(metrics);
            
            // Check memory usage
            checkMemoryUsage(metrics);
            
            // Check cache hit ratio
            checkCacheHitRatio(metrics);
            
            // Check service availability
            checkServiceAvailability(metrics);
            
        } catch (Exception e) {
            logger.error("Error during system health check: {}", e.getMessage(), e);
            triggerAlert(AlertType.SERVICE_UNAVAILABLE, AlertSeverity.CRITICAL,
                "Health Check Failed", "System health check encountered an error: " + e.getMessage());
        }
    }
    
    /**
     * Check error rate and trigger alert if threshold exceeded
     */
    private void checkErrorRate(Map<String, Object> metrics) {
        double totalQueries = (Double) metrics.getOrDefault("queries.total", 0.0);
        double failedQueries = (Double) metrics.getOrDefault("queries.failed", 0.0);
        
        if (totalQueries > 0) {
            double errorRate = failedQueries / totalQueries;
            
            AlertState state = alertStates.computeIfAbsent(AlertType.HIGH_ERROR_RATE.name(), 
                k -> new AlertState(AlertType.HIGH_ERROR_RATE));
            state.setCurrentValue(errorRate);
            state.setThreshold(errorRateThreshold);
            
            if (errorRate > errorRateThreshold) {
                if (!state.isActive()) {
                    state.setActive(true);
                    triggerAlert(AlertType.HIGH_ERROR_RATE, AlertSeverity.WARNING,
                        "High Error Rate Detected",
                        String.format("Error rate is %.2f%% (threshold: %.2f%%)", 
                            errorRate * 100, errorRateThreshold * 100));
                }
            } else if (state.isActive() && errorRate < errorRateThreshold * 0.8) {
                state.setActive(false);
                resolveAlert(AlertType.HIGH_ERROR_RATE, "Error rate returned to normal");
            }
        }
    }
    
    /**
     * Check response time and trigger alert if threshold exceeded
     */
    private void checkResponseTime(Map<String, Object> metrics) {
        double avgResponseTime = (Double) metrics.getOrDefault("query.processing.time.mean", 0.0);
        
        AlertState state = alertStates.computeIfAbsent(AlertType.SLOW_RESPONSE_TIME.name(),
            k -> new AlertState(AlertType.SLOW_RESPONSE_TIME));
        state.setCurrentValue(avgResponseTime);
        state.setThreshold(responseTimeThreshold);
        
        if (avgResponseTime > responseTimeThreshold) {
            if (!state.isActive()) {
                state.setActive(true);
                triggerAlert(AlertType.SLOW_RESPONSE_TIME, AlertSeverity.WARNING,
                    "Slow Response Time Detected",
                    String.format("Average response time is %.2fms (threshold: %dms)",
                        avgResponseTime, responseTimeThreshold));
            }
        } else if (state.isActive() && avgResponseTime < responseTimeThreshold * 0.8) {
            state.setActive(false);
            resolveAlert(AlertType.SLOW_RESPONSE_TIME, "Response time returned to normal");
        }
    }
    
    /**
     * Check queue size and trigger alert if threshold exceeded
     */
    private void checkQueueSize(Map<String, Object> metrics) {
        long queueSize = ((Number) metrics.getOrDefault("queries.queued", 0)).longValue();
        
        AlertState state = alertStates.computeIfAbsent(AlertType.HIGH_QUEUE_SIZE.name(),
            k -> new AlertState(AlertType.HIGH_QUEUE_SIZE));
        state.setCurrentValue(queueSize);
        state.setThreshold(queueSizeThreshold);
        
        if (queueSize > queueSizeThreshold) {
            AlertSeverity severity = queueSize > queueSizeThreshold * 2 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            
            if (!state.isActive()) {
                state.setActive(true);
                triggerAlert(AlertType.HIGH_QUEUE_SIZE, severity,
                    "High Queue Size Detected",
                    String.format("Queue size is %d (threshold: %d)", queueSize, queueSizeThreshold));
            }
        } else if (state.isActive() && queueSize < queueSizeThreshold * 0.8) {
            state.setActive(false);
            resolveAlert(AlertType.HIGH_QUEUE_SIZE, "Queue size returned to normal");
        }
    }
    
    /**
     * Check memory usage and trigger alert if threshold exceeded
     */
    private void checkMemoryUsage(Map<String, Object> metrics) {
        long memoryUsage = ((Number) metrics.getOrDefault("memory.usage", 0)).longValue();
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryRatio = (double) memoryUsage / maxMemory;
        
        AlertState state = alertStates.computeIfAbsent(AlertType.HIGH_MEMORY_USAGE.name(),
            k -> new AlertState(AlertType.HIGH_MEMORY_USAGE));
        state.setCurrentValue(memoryRatio);
        state.setThreshold(memoryUsageThreshold);
        
        if (memoryRatio > memoryUsageThreshold) {
            AlertSeverity severity = memoryRatio > 0.95 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            
            if (!state.isActive()) {
                state.setActive(true);
                triggerAlert(AlertType.HIGH_MEMORY_USAGE, severity,
                    "High Memory Usage Detected",
                    String.format("Memory usage is %.2f%% (threshold: %.2f%%)",
                        memoryRatio * 100, memoryUsageThreshold * 100));
            }
        } else if (state.isActive() && memoryRatio < memoryUsageThreshold * 0.9) {
            state.setActive(false);
            resolveAlert(AlertType.HIGH_MEMORY_USAGE, "Memory usage returned to normal");
        }
    }
    
    /**
     * Check cache hit ratio and trigger alert if below threshold
     */
    private void checkCacheHitRatio(Map<String, Object> metrics) {
        double cacheHitRatio = (Double) metrics.getOrDefault("cache.hit.ratio", 1.0);
        
        AlertState state = alertStates.computeIfAbsent(AlertType.LOW_CACHE_HIT_RATIO.name(),
            k -> new AlertState(AlertType.LOW_CACHE_HIT_RATIO));
        state.setCurrentValue(cacheHitRatio);
        state.setThreshold(cacheHitRatioThreshold);
        
        if (cacheHitRatio < cacheHitRatioThreshold) {
            if (!state.isActive()) {
                state.setActive(true);
                triggerAlert(AlertType.LOW_CACHE_HIT_RATIO, AlertSeverity.WARNING,
                    "Low Cache Hit Ratio Detected",
                    String.format("Cache hit ratio is %.2f%% (threshold: %.2f%%)",
                        cacheHitRatio * 100, cacheHitRatioThreshold * 100));
            }
        } else if (state.isActive() && cacheHitRatio > cacheHitRatioThreshold * 1.1) {
            state.setActive(false);
            resolveAlert(AlertType.LOW_CACHE_HIT_RATIO, "Cache hit ratio returned to normal");
        }
    }
    
    /**
     * Check service availability
     */
    private void checkServiceAvailability(Map<String, Object> metrics) {
        long activeQueries = ((Number) metrics.getOrDefault("queries.active", 0)).longValue();
        long totalQueries = ((Number) metrics.getOrDefault("queries.total", 0)).longValue();
        
        // If we have no queries in the last check interval, the service might be down
        // This is a simple check - in production you'd want more sophisticated health checks
        AlertState state = alertStates.computeIfAbsent(AlertType.SERVICE_UNAVAILABLE.name(),
            k -> new AlertState(AlertType.SERVICE_UNAVAILABLE));
        
        // For now, we'll just log that we're checking service availability
        logger.debug("Service availability check: active={}, total={}", activeQueries, totalQueries);
    }
    
    /**
     * Trigger an alert
     */
    public void triggerAlert(AlertType type, AlertSeverity severity, String title, String description) {
        // Check cooldown period
        String alertKey = type.name();
        Instant lastAlert = lastAlertTimes.get(alertKey);
        if (lastAlert != null && Duration.between(lastAlert, Instant.now()).compareTo(alertCooldown) < 0) {
            logger.debug("Alert {} is in cooldown period, skipping", type);
            return;
        }
        
        Alert alert = new Alert(type, severity, title, description);
        
        // Add system context
        Map<String, Object> metrics = aiMetricsService.getMetricsSummary();
        alert.addMetadata("system_metrics", metrics);
        alert.addMetadata("timestamp", Instant.now().toString());
        
        // Log the alert
        logger.warn("ALERT [{}] {}: {}", severity, title, description);
        
        // Send notifications
        sendAlertNotifications(alert);
        
        // Update last alert time
        lastAlertTimes.put(alertKey, Instant.now());
    }
    
    /**
     * Resolve an alert
     */
    public void resolveAlert(AlertType type, String resolution) {
        logger.info("RESOLVED [{}]: {}", type, resolution);
        
        // In a full implementation, you'd track resolved alerts and send notifications
        AlertState state = alertStates.get(type.name());
        if (state != null) {
            state.setActive(false);
        }
    }
    
    /**
     * Send alert notifications
     */
    private void sendAlertNotifications(Alert alert) {
        try {
            // Email notifications
            if (emailAlertsEnabled) {
                sendEmailAlert(alert);
            }
            
            // Slack notifications
            if (slackAlertsEnabled) {
                sendSlackAlert(alert);
            }
            
            // Webhook notifications
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                sendWebhookAlert(alert);
            }
            
        } catch (Exception e) {
            logger.error("Error sending alert notifications: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send email alert (placeholder implementation)
     */
    private void sendEmailAlert(Alert alert) {
        // In a real implementation, you'd integrate with an email service
        logger.info("Would send email alert: {} - {}", alert.getTitle(), alert.getDescription());
    }
    
    /**
     * Send Slack alert (placeholder implementation)
     */
    private void sendSlackAlert(Alert alert) {
        // In a real implementation, you'd integrate with Slack API
        logger.info("Would send Slack alert: {} - {}", alert.getTitle(), alert.getDescription());
    }
    
    /**
     * Send webhook alert (placeholder implementation)
     */
    private void sendWebhookAlert(Alert alert) {
        // In a real implementation, you'd make HTTP POST to webhook URL
        logger.info("Would send webhook alert to {}: {} - {}", 
            webhookUrl, alert.getTitle(), alert.getDescription());
    }
    
    /**
     * Get current alert states
     */
    public Map<String, AlertState> getAlertStates() {
        return new HashMap<>(alertStates);
    }
    
    /**
     * Get active alerts
     */
    public List<AlertState> getActiveAlerts() {
        return alertStates.values().stream()
            .filter(AlertState::isActive)
            .sorted((a, b) -> b.getLastTriggered().compareTo(a.getLastTriggered()))
            .toList();
    }
    
    /**
     * Manual alert trigger for testing
     */
    public void triggerTestAlert() {
        triggerAlert(AlertType.PERFORMANCE_DEGRADATION, AlertSeverity.INFO,
            "Test Alert", "This is a test alert to verify the alerting system is working");
    }
    
    /**
     * Clear all alert states (for testing)
     */
    public void clearAlertStates() {
        alertStates.clear();
        lastAlertTimes.clear();
        logger.info("All alert states cleared");
    }
}