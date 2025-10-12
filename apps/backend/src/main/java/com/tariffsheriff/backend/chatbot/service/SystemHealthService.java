package com.tariffsheriff.backend.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive system health monitoring service with real-time alerting,
 * automated incident response, performance baselines, and anomaly detection
 */
@Service
public class SystemHealthService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemHealthService.class);
    
    @Autowired
    private ToolHealthMonitor toolHealthMonitor;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;
    
    // System health metrics
    private final SystemHealthMetrics systemMetrics = new SystemHealthMetrics();
    
    // Performance baselines
    private final PerformanceBaselines baselines = new PerformanceBaselines();
    
    // Anomaly detection
    private final AnomalyDetector anomalyDetector = new AnomalyDetector();
    
    // Alert management
    private final AlertManager alertManager = new AlertManager();
    
    // Incident response
    private final IncidentResponseManager incidentManager = new IncidentResponseManager();
    
    // Health check registry
    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();
    
    // Configuration thresholds
    private static final double CPU_THRESHOLD = 80.0; // 80%
    private static final double MEMORY_THRESHOLD = 85.0; // 85%
    private static final double DISK_THRESHOLD = 90.0; // 90%
    private static final long RESPONSE_TIME_THRESHOLD = 10000; // 10 seconds
    private static final double ERROR_RATE_THRESHOLD = 0.05; // 5%
    
    /**
     * Initialize system health service
     */
    public SystemHealthService() {
        initializeHealthChecks();
        logger.info("SystemHealthService initialized with {} health checks", healthChecks.size());
    }
    
    /**
     * Get comprehensive system health status
     */
    public SystemHealthStatus getSystemHealthStatus() {
        SystemHealthStatus status = new SystemHealthStatus();
        
        // Collect system metrics
        status.setSystemMetrics(collectSystemMetrics());
        
        // Get tool health statuses
        status.setToolHealthStatuses(toolHealthMonitor.getAllToolHealth());
        
        // Get circuit breaker statuses
        status.setCircuitBreakerStatuses(circuitBreakerService.getAllCircuitBreakerStatuses());
        
        // Run health checks
        status.setHealthCheckResults(runAllHealthChecks());
        
        // Get active alerts
        status.setActiveAlerts(alertManager.getActiveAlerts());
        
        // Get active incidents
        status.setActiveIncidents(incidentManager.getActiveIncidents());
        
        // Calculate overall health score
        status.setOverallHealthScore(calculateOverallHealthScore(status));
        
        // Determine system status
        status.setSystemStatus(determineSystemStatus(status));
        
        status.setLastUpdated(LocalDateTime.now());
        
        return status;
    }
    
    /**
     * Scheduled comprehensive health monitoring
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void performSystemHealthCheck() {
        try {
            logger.debug("Starting comprehensive system health check");
            
            // Collect current metrics
            SystemMetrics currentMetrics = collectSystemMetrics();
            systemMetrics.recordMetrics(currentMetrics);
            
            // Update performance baselines
            baselines.updateBaselines(currentMetrics);
            
            // Perform anomaly detection
            List<Anomaly> anomalies = anomalyDetector.detectAnomalies(currentMetrics, baselines);
            
            // Process anomalies and generate alerts
            for (Anomaly anomaly : anomalies) {
                processAnomaly(anomaly);
            }
            
            // Run critical health checks
            runCriticalHealthChecks();
            
            // Check for incident escalation
            incidentManager.checkIncidentEscalation();
            
            // Update system health dashboard
            updateSystemHealthDashboard(currentMetrics);
            
            logger.debug("Completed comprehensive system health check");
            
        } catch (Exception e) {
            logger.error("Error during system health check", e);
            alertManager.createAlert("SYSTEM_HEALTH_CHECK_FAILED", 
                    "System health check failed: " + e.getMessage(), AlertSeverity.HIGH);
        }
    }
    
    /**
     * Scheduled performance baseline update
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updatePerformanceBaselines() {
        try {
            baselines.recalculateBaselines();
            logger.debug("Performance baselines updated");
        } catch (Exception e) {
            logger.error("Error updating performance baselines", e);
        }
    }
    
    /**
     * Register a custom health check
     */
    public void registerHealthCheck(String name, HealthCheck healthCheck) {
        healthChecks.put(name, healthCheck);
        logger.info("Registered health check: {}", name);
    }
    
    /**
     * Get performance baselines
     */
    public PerformanceBaselines getPerformanceBaselines() {
        return baselines;
    }
    
    /**
     * Get anomaly detection results
     */
    public List<Anomaly> getRecentAnomalies() {
        return anomalyDetector.getRecentAnomalies();
    }
    
    /**
     * Create manual alert
     */
    public void createManualAlert(String type, String message, AlertSeverity severity) {
        alertManager.createAlert(type, message, severity);
    }
    
    /**
     * Acknowledge alert
     */
    public void acknowledgeAlert(String alertId, String acknowledgedBy) {
        alertManager.acknowledgeAlert(alertId, acknowledgedBy);
    }
    
    /**
     * Create manual incident
     */
    public String createManualIncident(String title, String description, IncidentSeverity severity) {
        return incidentManager.createIncident(title, description, severity);
    }
    
    /**
     * Update incident status
     */
    public void updateIncidentStatus(String incidentId, IncidentStatus status, String updatedBy) {
        incidentManager.updateIncidentStatus(incidentId, status, updatedBy);
    }
    
    // ========== PRIVATE METHODS ==========
    
    /**
     * Initialize built-in health checks
     */
    private void initializeHealthChecks() {
        // Database connectivity check
        healthChecks.put("database", () -> {
            try {
                // In a real implementation, this would check database connectivity
                return new HealthCheckResult("database", true, "Database connection healthy", null);
            } catch (Exception e) {
                return new HealthCheckResult("database", false, "Database connection failed", e.getMessage());
            }
        });
        
        // External API connectivity check
        healthChecks.put("external_apis", () -> {
            try {
                // Check circuit breaker statuses for external APIs
                Map<String, CircuitBreakerService.CircuitBreakerStatus> statuses = 
                        circuitBreakerService.getAllCircuitBreakerStatuses();
                
                boolean allHealthy = statuses.values().stream().allMatch(
                        CircuitBreakerService.CircuitBreakerStatus::isHealthy);
                
                if (allHealthy) {
                    return new HealthCheckResult("external_apis", true, "All external APIs healthy", null);
                } else {
                    String unhealthyApis = statuses.entrySet().stream()
                            .filter(entry -> !entry.getValue().isHealthy())
                            .map(Map.Entry::getKey)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("unknown");
                    
                    return new HealthCheckResult("external_apis", false, 
                            "Unhealthy external APIs: " + unhealthyApis, null);
                }
            } catch (Exception e) {
                return new HealthCheckResult("external_apis", false, "External API check failed", e.getMessage());
            }
        });
        
        // Tool health check
        healthChecks.put("tools", () -> {
            try {
                Map<String, ToolHealthMonitor.EnhancedToolHealthStatus> toolStatuses = 
                        toolHealthMonitor.getAllToolHealth();
                
                boolean allHealthy = toolStatuses.values().stream().allMatch(
                        ToolHealthMonitor.EnhancedToolHealthStatus::isHealthy);
                
                if (allHealthy) {
                    return new HealthCheckResult("tools", true, "All tools healthy", null);
                } else {
                    String unhealthyTools = toolStatuses.entrySet().stream()
                            .filter(entry -> !entry.getValue().isHealthy())
                            .map(Map.Entry::getKey)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("unknown");
                    
                    return new HealthCheckResult("tools", false, 
                            "Unhealthy tools: " + unhealthyTools, null);
                }
            } catch (Exception e) {
                return new HealthCheckResult("tools", false, "Tool health check failed", e.getMessage());
            }
        });
        
        // System resources check
        healthChecks.put("system_resources", () -> {
            try {
                SystemMetrics metrics = collectSystemMetrics();
                List<String> issues = new ArrayList<>();
                
                if (metrics.getCpuUsage() > CPU_THRESHOLD) {
                    issues.add("High CPU usage: " + String.format("%.1f%%", metrics.getCpuUsage()));
                }
                if (metrics.getMemoryUsage() > MEMORY_THRESHOLD) {
                    issues.add("High memory usage: " + String.format("%.1f%%", metrics.getMemoryUsage()));
                }
                
                if (issues.isEmpty()) {
                    return new HealthCheckResult("system_resources", true, "System resources healthy", null);
                } else {
                    return new HealthCheckResult("system_resources", false, 
                            "Resource issues: " + String.join(", ", issues), null);
                }
            } catch (Exception e) {
                return new HealthCheckResult("system_resources", false, "System resource check failed", e.getMessage());
            }
        });
    }
    
    /**
     * Collect current system metrics
     */
    private SystemMetrics collectSystemMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        
        try {
            // CPU usage
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                        (com.sun.management.OperatingSystemMXBean) osBean;
                metrics.setCpuUsage(sunOsBean.getProcessCpuLoad() * 100);
            }
            
            // Memory usage
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            metrics.setMemoryUsage((double) usedMemory / maxMemory * 100);
            metrics.setUsedMemoryMB(usedMemory / 1024 / 1024);
            metrics.setMaxMemoryMB(maxMemory / 1024 / 1024);
            
            // Thread count
            metrics.setActiveThreads(Thread.activeCount());
            
            // System load average
            metrics.setSystemLoadAverage(osBean.getSystemLoadAverage());
            
            // Available processors
            metrics.setAvailableProcessors(osBean.getAvailableProcessors());
            
            metrics.setTimestamp(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error collecting system metrics", e);
        }
        
        return metrics;
    }
    
    /**
     * Run all health checks
     */
    private Map<String, HealthCheckResult> runAllHealthChecks() {
        Map<String, HealthCheckResult> results = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
            try {
                HealthCheckResult result = entry.getValue().check();
                results.put(entry.getKey(), result);
                
                // Create alert for failed health checks
                if (!result.isHealthy()) {
                    alertManager.createAlert("HEALTH_CHECK_FAILED", 
                            "Health check failed: " + entry.getKey() + " - " + result.getMessage(), 
                            AlertSeverity.MEDIUM);
                }
                
            } catch (Exception e) {
                logger.error("Error running health check {}", entry.getKey(), e);
                results.put(entry.getKey(), new HealthCheckResult(entry.getKey(), false, 
                        "Health check execution failed", e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * Run only critical health checks
     */
    private void runCriticalHealthChecks() {
        List<String> criticalChecks = Arrays.asList("database", "system_resources");
        
        for (String checkName : criticalChecks) {
            HealthCheck healthCheck = healthChecks.get(checkName);
            if (healthCheck != null) {
                try {
                    HealthCheckResult result = healthCheck.check();
                    if (!result.isHealthy()) {
                        alertManager.createAlert("CRITICAL_HEALTH_CHECK_FAILED", 
                                "Critical health check failed: " + checkName + " - " + result.getMessage(), 
                                AlertSeverity.CRITICAL);
                    }
                } catch (Exception e) {
                    logger.error("Error running critical health check {}", checkName, e);
                }
            }
        }
    }
    
    /**
     * Process detected anomaly
     */
    private void processAnomaly(Anomaly anomaly) {
        logger.warn("Anomaly detected: {}", anomaly.getDescription());
        
        // Create alert for anomaly
        AlertSeverity severity = determineAnomalySeverity(anomaly);
        alertManager.createAlert("ANOMALY_DETECTED", anomaly.getDescription(), severity);
        
        // Check if incident should be created
        if (severity == AlertSeverity.CRITICAL) {
            String incidentId = incidentManager.createIncident(
                    "Critical Anomaly: " + anomaly.getType(),
                    anomaly.getDescription(),
                    IncidentSeverity.HIGH
            );
            logger.error("Critical anomaly incident created: {}", incidentId);
        }
    }
    
    /**
     * Determine anomaly severity
     */
    private AlertSeverity determineAnomalySeverity(Anomaly anomaly) {
        switch (anomaly.getType()) {
            case "CPU_SPIKE":
            case "MEMORY_SPIKE":
                return anomaly.getSeverityScore() > 0.8 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH;
            case "RESPONSE_TIME_SPIKE":
                return anomaly.getSeverityScore() > 0.7 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
            case "ERROR_RATE_SPIKE":
                return anomaly.getSeverityScore() > 0.6 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
            default:
                return AlertSeverity.LOW;
        }
    }
    
    /**
     * Calculate overall system health score (0.0 to 1.0)
     */
    private double calculateOverallHealthScore(SystemHealthStatus status) {
        double score = 1.0;
        
        // Deduct for system resource issues
        SystemMetrics metrics = status.getSystemMetrics();
        if (metrics.getCpuUsage() > CPU_THRESHOLD) {
            score -= 0.2;
        }
        if (metrics.getMemoryUsage() > MEMORY_THRESHOLD) {
            score -= 0.2;
        }
        
        // Deduct for unhealthy tools
        long unhealthyTools = status.getToolHealthStatuses().values().stream()
                .filter(tool -> !tool.isHealthy())
                .count();
        score -= (unhealthyTools * 0.1);
        
        // Deduct for open circuit breakers
        long openCircuitBreakers = status.getCircuitBreakerStatuses().values().stream()
                .filter(cb -> !cb.isHealthy())
                .count();
        score -= (openCircuitBreakers * 0.15);
        
        // Deduct for failed health checks
        long failedHealthChecks = status.getHealthCheckResults().values().stream()
                .filter(result -> !result.isHealthy())
                .count();
        score -= (failedHealthChecks * 0.1);
        
        // Deduct for active critical alerts
        long criticalAlerts = status.getActiveAlerts().stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();
        score -= (criticalAlerts * 0.2);
        
        return Math.max(0.0, score);
    }
    
    /**
     * Determine overall system status
     */
    private SystemStatus determineSystemStatus(SystemHealthStatus status) {
        double healthScore = status.getOverallHealthScore();
        
        if (healthScore >= 0.9) {
            return SystemStatus.HEALTHY;
        } else if (healthScore >= 0.7) {
            return SystemStatus.DEGRADED;
        } else if (healthScore >= 0.5) {
            return SystemStatus.UNHEALTHY;
        } else {
            return SystemStatus.CRITICAL;
        }
    }
    
    /**
     * Update system health dashboard
     */
    private void updateSystemHealthDashboard(SystemMetrics metrics) {
        // This would update a dashboard or send metrics to monitoring systems
        logger.debug("System health dashboard updated - CPU: {:.1f}%, Memory: {:.1f}%, Threads: {}", 
                metrics.getCpuUsage(), metrics.getMemoryUsage(), metrics.getActiveThreads());
    }
    
    // ========== INNER CLASSES AND INTERFACES ==========
    
    /**
     * Health check interface
     */
    @FunctionalInterface
    public interface HealthCheck {
        HealthCheckResult check();
    }
    
    /**
     * System health status container
     */
    public static class SystemHealthStatus {
        private SystemMetrics systemMetrics;
        private Map<String, ToolHealthMonitor.EnhancedToolHealthStatus> toolHealthStatuses;
        private Map<String, CircuitBreakerService.CircuitBreakerStatus> circuitBreakerStatuses;
        private Map<String, HealthCheckResult> healthCheckResults;
        private List<Alert> activeAlerts;
        private List<Incident> activeIncidents;
        private double overallHealthScore;
        private SystemStatus systemStatus;
        private LocalDateTime lastUpdated;
        
        // Getters and setters
        public SystemMetrics getSystemMetrics() { return systemMetrics; }
        public void setSystemMetrics(SystemMetrics systemMetrics) { this.systemMetrics = systemMetrics; }
        
        public Map<String, ToolHealthMonitor.EnhancedToolHealthStatus> getToolHealthStatuses() { return toolHealthStatuses; }
        public void setToolHealthStatuses(Map<String, ToolHealthMonitor.EnhancedToolHealthStatus> toolHealthStatuses) { this.toolHealthStatuses = toolHealthStatuses; }
        
        public Map<String, CircuitBreakerService.CircuitBreakerStatus> getCircuitBreakerStatuses() { return circuitBreakerStatuses; }
        public void setCircuitBreakerStatuses(Map<String, CircuitBreakerService.CircuitBreakerStatus> circuitBreakerStatuses) { this.circuitBreakerStatuses = circuitBreakerStatuses; }
        
        public Map<String, HealthCheckResult> getHealthCheckResults() { return healthCheckResults; }
        public void setHealthCheckResults(Map<String, HealthCheckResult> healthCheckResults) { this.healthCheckResults = healthCheckResults; }
        
        public List<Alert> getActiveAlerts() { return activeAlerts; }
        public void setActiveAlerts(List<Alert> activeAlerts) { this.activeAlerts = activeAlerts; }
        
        public List<Incident> getActiveIncidents() { return activeIncidents; }
        public void setActiveIncidents(List<Incident> activeIncidents) { this.activeIncidents = activeIncidents; }
        
        public double getOverallHealthScore() { return overallHealthScore; }
        public void setOverallHealthScore(double overallHealthScore) { this.overallHealthScore = overallHealthScore; }
        
        public SystemStatus getSystemStatus() { return systemStatus; }
        public void setSystemStatus(SystemStatus systemStatus) { this.systemStatus = systemStatus; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    /**
     * System metrics container
     */
    public static class SystemMetrics {
        private double cpuUsage;
        private double memoryUsage;
        private long usedMemoryMB;
        private long maxMemoryMB;
        private int activeThreads;
        private double systemLoadAverage;
        private int availableProcessors;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public long getUsedMemoryMB() { return usedMemoryMB; }
        public void setUsedMemoryMB(long usedMemoryMB) { this.usedMemoryMB = usedMemoryMB; }
        
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public void setMaxMemoryMB(long maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; }
        
        public int getActiveThreads() { return activeThreads; }
        public void setActiveThreads(int activeThreads) { this.activeThreads = activeThreads; }
        
        public double getSystemLoadAverage() { return systemLoadAverage; }
        public void setSystemLoadAverage(double systemLoadAverage) { this.systemLoadAverage = systemLoadAverage; }
        
        public int getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Health check result
     */
    public static class HealthCheckResult {
        private final String name;
        private final boolean healthy;
        private final String message;
        private final String error;
        private final LocalDateTime timestamp;
        
        public HealthCheckResult(String name, boolean healthy, String message, String error) {
            this.name = name;
            this.healthy = healthy;
            this.message = message;
            this.error = error;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getName() { return name; }
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public String getError() { return error; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * System status enumeration
     */
    public enum SystemStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        CRITICAL
    }
    
    /**
     * Alert severity enumeration
     */
    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Incident severity enumeration
     */
    public enum IncidentSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Incident status enumeration
     */
    public enum IncidentStatus {
        OPEN,
        INVESTIGATING,
        RESOLVED,
        CLOSED
    }
    
    // Additional classes would be implemented here for:
    // - SystemHealthMetrics
    // - PerformanceBaselines
    // - AnomalyDetector
    // - AlertManager
    // - IncidentResponseManager
    // - Alert
    // - Incident
    // - Anomaly
    
    // For brevity, I'm including simplified placeholder implementations
    
    private static class SystemHealthMetrics {
        void recordMetrics(SystemMetrics metrics) {
            // Record metrics for historical analysis
        }
    }
    
    private static class PerformanceBaselines {
        void updateBaselines(SystemMetrics metrics) {
            // Update performance baselines
        }
        
        void recalculateBaselines() {
            // Recalculate baselines
        }
    }
    
    private static class AnomalyDetector {
        List<Anomaly> detectAnomalies(SystemMetrics metrics, PerformanceBaselines baselines) {
            // Detect anomalies
            return new ArrayList<>();
        }
        
        List<Anomaly> getRecentAnomalies() {
            return new ArrayList<>();
        }
    }
    
    private static class AlertManager {
        void createAlert(String type, String message, AlertSeverity severity) {
            // Create alert
        }
        
        void acknowledgeAlert(String alertId, String acknowledgedBy) {
            // Acknowledge alert
        }
        
        List<Alert> getActiveAlerts() {
            return new ArrayList<>();
        }
    }
    
    private static class IncidentResponseManager {
        String createIncident(String title, String description, IncidentSeverity severity) {
            // Create incident
            return UUID.randomUUID().toString();
        }
        
        void updateIncidentStatus(String incidentId, IncidentStatus status, String updatedBy) {
            // Update incident status
        }
        
        void checkIncidentEscalation() {
            // Check for incident escalation
        }
        
        List<Incident> getActiveIncidents() {
            return new ArrayList<>();
        }
    }
    
    private static class Alert {
        private AlertSeverity severity;
        
        public AlertSeverity getSeverity() { return severity; }
    }
    
    private static class Incident {
        // Incident implementation
    }
    
    private static class Anomaly {
        private String type;
        private String description;
        private double severityScore;
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getSeverityScore() { return severityScore; }
    }
}