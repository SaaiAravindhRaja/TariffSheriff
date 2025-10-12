package com.tariffsheriff.backend.monitoring;

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
 * Service for establishing and monitoring performance baselines for AI features
 */
@Service
public class PerformanceBaselineService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBaselineService.class);
    
    @Autowired
    private AiMetricsService aiMetricsService;
    
    @Value("${performance.baselines.enabled:true}")
    private boolean baselinesEnabled;
    
    @Value("${performance.baselines.collection-period-days:30}")
    private int collectionPeriodDays;
    
    @Value("${performance.baselines.update-frequency-hours:24}")
    private int updateFrequencyHours;
    
    // Performance baselines storage
    private final Map<String, PerformanceBaseline> baselines = new ConcurrentHashMap<>();
    private final Map<String, List<PerformanceDataPoint>> historicalData = new ConcurrentHashMap<>();
    
    // SLA definitions
    private final Map<String, SlaDefinition> slaDefinitions = new ConcurrentHashMap<>();
    
    public static class PerformanceBaseline {
        private String metricName;
        private double mean;
        private double median;
        private double p95;
        private double p99;
        private double standardDeviation;
        private double minValue;
        private double maxValue;
        private int sampleCount;
        private Instant calculatedAt;
        private Instant validFrom;
        private Instant validUntil;
        
        public PerformanceBaseline(String metricName) {
            this.metricName = metricName;
            this.calculatedAt = Instant.now();
            this.validFrom = Instant.now();
            this.validUntil = Instant.now().plusSeconds(30 * 24 * 3600); // 30 days
        }
        
        // Getters and setters
        public String getMetricName() { return metricName; }
        public double getMean() { return mean; }
        public void setMean(double mean) { this.mean = mean; }
        public double getMedian() { return median; }
        public void setMedian(double median) { this.median = median; }
        public double getP95() { return p95; }
        public void setP95(double p95) { this.p95 = p95; }
        public double getP99() { return p99; }
        public void setP99(double p99) { this.p99 = p99; }
        public double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(double standardDeviation) { this.standardDeviation = standardDeviation; }
        public double getMinValue() { return minValue; }
        public void setMinValue(double minValue) { this.minValue = minValue; }
        public double getMaxValue() { return maxValue; }
        public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
        public int getSampleCount() { return sampleCount; }
        public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }
        public Instant getCalculatedAt() { return calculatedAt; }
        public Instant getValidFrom() { return validFrom; }
        public Instant getValidUntil() { return validUntil; }
    }
    
    public static class PerformanceDataPoint {
        private Instant timestamp;
        private String metricName;
        private double value;
        private Map<String, String> tags;
        
        public PerformanceDataPoint(String metricName, double value) {
            this.timestamp = Instant.now();
            this.metricName = metricName;
            this.value = value;
            this.tags = new HashMap<>();
        }
        
        // Getters and setters
        public Instant getTimestamp() { return timestamp; }
        public String getMetricName() { return metricName; }
        public double getValue() { return value; }
        public Map<String, String> getTags() { return tags; }
        public void addTag(String key, String value) { this.tags.put(key, value); }
    }
    
    public static class SlaDefinition {
        private String metricName;
        private String description;
        private double targetValue;
        private String operator; // "less_than", "greater_than", "equals"
        private double warningThreshold;
        private double criticalThreshold;
        private String unit;
        private boolean enabled;
        
        public SlaDefinition(String metricName, String description, double targetValue, String operator) {
            this.metricName = metricName;
            this.description = description;
            this.targetValue = targetValue;
            this.operator = operator;
            this.enabled = true;
        }
        
        // Getters and setters
        public String getMetricName() { return metricName; }
        public String getDescription() { return description; }
        public double getTargetValue() { return targetValue; }
        public String getOperator() { return operator; }
        public double getWarningThreshold() { return warningThreshold; }
        public void setWarningThreshold(double warningThreshold) { this.warningThreshold = warningThreshold; }
        public double getCriticalThreshold() { return criticalThreshold; }
        public void setCriticalThreshold(double criticalThreshold) { this.criticalThreshold = criticalThreshold; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class SlaViolation {
        private String metricName;
        private double currentValue;
        private double targetValue;
        private String severity; // "warning", "critical"
        private String description;
        private Instant detectedAt;
        
        public SlaViolation(String metricName, double currentValue, double targetValue, String severity, String description) {
            this.metricName = metricName;
            this.currentValue = currentValue;
            this.targetValue = targetValue;
            this.severity = severity;
            this.description = description;
            this.detectedAt = Instant.now();
        }
        
        // Getters
        public String getMetricName() { return metricName; }
        public double getCurrentValue() { return currentValue; }
        public double getTargetValue() { return targetValue; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public Instant getDetectedAt() { return detectedAt; }
    }
    
    /**
     * Initialize default SLA definitions
     */
    public void initializeDefaultSlas() {
        // Response time SLAs
        SlaDefinition responseTimeSla = new SlaDefinition(
            "query.processing.time.mean", 
            "Average query processing time should be under 10 seconds", 
            10000.0, 
            "less_than"
        );
        responseTimeSla.setWarningThreshold(8000.0);
        responseTimeSla.setCriticalThreshold(15000.0);
        responseTimeSla.setUnit("milliseconds");
        slaDefinitions.put("query.processing.time.mean", responseTimeSla);
        
        // Error rate SLA
        SlaDefinition errorRateSla = new SlaDefinition(
            "error.rate", 
            "Error rate should be under 5%", 
            0.05, 
            "less_than"
        );
        errorRateSla.setWarningThreshold(0.03);
        errorRateSla.setCriticalThreshold(0.10);
        errorRateSla.setUnit("percentage");
        slaDefinitions.put("error.rate", errorRateSla);
        
        // Throughput SLA
        SlaDefinition throughputSla = new SlaDefinition(
            "queries.per.minute", 
            "System should handle at least 100 queries per minute", 
            100.0, 
            "greater_than"
        );
        throughputSla.setWarningThreshold(80.0);
        throughputSla.setCriticalThreshold(50.0);
        throughputSla.setUnit("queries/minute");
        slaDefinitions.put("queries.per.minute", throughputSla);
        
        // Cache hit ratio SLA
        SlaDefinition cacheHitSla = new SlaDefinition(
            "cache.hit.ratio", 
            "Cache hit ratio should be above 70%", 
            0.70, 
            "greater_than"
        );
        cacheHitSla.setWarningThreshold(0.60);
        cacheHitSla.setCriticalThreshold(0.40);
        cacheHitSla.setUnit("percentage");
        slaDefinitions.put("cache.hit.ratio", cacheHitSla);
        
        // AI orchestration time SLA
        SlaDefinition orchestrationSla = new SlaDefinition(
            "orchestration.time.mean", 
            "AI orchestration should complete within 5 seconds", 
            5000.0, 
            "less_than"
        );
        orchestrationSla.setWarningThreshold(4000.0);
        orchestrationSla.setCriticalThreshold(8000.0);
        orchestrationSla.setUnit("milliseconds");
        slaDefinitions.put("orchestration.time.mean", orchestrationSla);
        
        logger.info("Initialized {} default SLA definitions", slaDefinitions.size());
    }
    
    /**
     * Record performance data point
     */
    public void recordPerformanceData(String metricName, double value, Map<String, String> tags) {
        if (!baselinesEnabled) {
            return;
        }
        
        PerformanceDataPoint dataPoint = new PerformanceDataPoint(metricName, value);
        if (tags != null) {
            dataPoint.getTags().putAll(tags);
        }
        
        historicalData.computeIfAbsent(metricName, k -> new ArrayList<>()).add(dataPoint);
        
        // Keep only recent data points (last 30 days)
        Instant cutoff = Instant.now().minusSeconds(collectionPeriodDays * 24 * 3600L);
        historicalData.get(metricName).removeIf(point -> point.getTimestamp().isBefore(cutoff));
        
        logger.debug("Recorded performance data: {} = {}", metricName, value);
    }
    
    /**
     * Calculate baseline for a metric
     */
    public PerformanceBaseline calculateBaseline(String metricName) {
        List<PerformanceDataPoint> data = historicalData.get(metricName);
        if (data == null || data.isEmpty()) {
            logger.warn("No historical data available for metric: {}", metricName);
            return null;
        }
        
        // Extract values and sort
        List<Double> values = data.stream()
            .map(PerformanceDataPoint::getValue)
            .sorted()
            .collect(Collectors.toList());
        
        if (values.size() < 10) {
            logger.warn("Insufficient data points ({}) for baseline calculation: {}", values.size(), metricName);
            return null;
        }
        
        PerformanceBaseline baseline = new PerformanceBaseline(metricName);
        baseline.setSampleCount(values.size());
        baseline.setMinValue(values.get(0));
        baseline.setMaxValue(values.get(values.size() - 1));
        
        // Calculate mean
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / values.size();
        baseline.setMean(mean);
        
        // Calculate median
        double median;
        int size = values.size();
        if (size % 2 == 0) {
            median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            median = values.get(size / 2);
        }
        baseline.setMedian(median);
        
        // Calculate percentiles
        baseline.setP95(calculatePercentile(values, 95));
        baseline.setP99(calculatePercentile(values, 99));
        
        // Calculate standard deviation
        double variance = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        baseline.setStandardDeviation(Math.sqrt(variance));
        
        logger.info("Calculated baseline for {}: mean={}, median={}, p95={}, p99={}, samples={}", 
            metricName, mean, median, baseline.getP95(), baseline.getP99(), values.size());
        
        return baseline;
    }
    
    /**
     * Calculate percentile value
     */
    private double calculatePercentile(List<Double> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        
        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);
        
        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }
        
        double lowerValue = sortedValues.get(lowerIndex);
        double upperValue = sortedValues.get(upperIndex);
        double weight = index - lowerIndex;
        
        return lowerValue + weight * (upperValue - lowerValue);
    }
    
    /**
     * Update all baselines
     */
    @Scheduled(fixedRateString = "#{${performance.baselines.update-frequency-hours:24} * 3600000}")
    public void updateBaselines() {
        if (!baselinesEnabled) {
            return;
        }
        
        logger.info("Starting baseline update process");
        
        int updatedCount = 0;
        for (String metricName : historicalData.keySet()) {
            PerformanceBaseline baseline = calculateBaseline(metricName);
            if (baseline != null) {
                baselines.put(metricName, baseline);
                updatedCount++;
            }
        }
        
        logger.info("Updated {} performance baselines", updatedCount);
    }
    
    /**
     * Check SLA compliance
     */
    public List<SlaViolation> checkSlaCompliance() {
        List<SlaViolation> violations = new ArrayList<>();
        
        // Get current metrics
        Map<String, Object> currentMetrics = aiMetricsService.getMetricsSummary();
        
        for (SlaDefinition sla : slaDefinitions.values()) {
            if (!sla.isEnabled()) {
                continue;
            }
            
            Object metricValue = currentMetrics.get(sla.getMetricName());
            if (metricValue == null) {
                continue;
            }
            
            double currentValue = ((Number) metricValue).doubleValue();
            
            // Check for violations
            SlaViolation violation = checkSlaViolation(sla, currentValue);
            if (violation != null) {
                violations.add(violation);
            }
        }
        
        return violations;
    }
    
    /**
     * Check individual SLA violation
     */
    private SlaViolation checkSlaViolation(SlaDefinition sla, double currentValue) {
        boolean isViolation = false;
        String severity = null;
        
        switch (sla.getOperator()) {
            case "less_than":
                if (currentValue > sla.getCriticalThreshold()) {
                    isViolation = true;
                    severity = "critical";
                } else if (currentValue > sla.getWarningThreshold()) {
                    isViolation = true;
                    severity = "warning";
                }
                break;
                
            case "greater_than":
                if (currentValue < sla.getCriticalThreshold()) {
                    isViolation = true;
                    severity = "critical";
                } else if (currentValue < sla.getWarningThreshold()) {
                    isViolation = true;
                    severity = "warning";
                }
                break;
                
            case "equals":
                double tolerance = sla.getTargetValue() * 0.1; // 10% tolerance
                if (Math.abs(currentValue - sla.getTargetValue()) > tolerance) {
                    isViolation = true;
                    severity = "warning";
                }
                break;
        }
        
        if (isViolation) {
            return new SlaViolation(
                sla.getMetricName(),
                currentValue,
                sla.getTargetValue(),
                severity,
                sla.getDescription()
            );
        }
        
        return null;
    }
    
    /**
     * Get performance baseline
     */
    public PerformanceBaseline getBaseline(String metricName) {
        return baselines.get(metricName);
    }
    
    /**
     * Get all baselines
     */
    public Map<String, PerformanceBaseline> getAllBaselines() {
        return new HashMap<>(baselines);
    }
    
    /**
     * Get SLA definitions
     */
    public Map<String, SlaDefinition> getSlaDefinitions() {
        return new HashMap<>(slaDefinitions);
    }
    
    /**
     * Update SLA definition
     */
    public void updateSlaDefinition(String metricName, SlaDefinition sla) {
        slaDefinitions.put(metricName, sla);
        logger.info("Updated SLA definition for metric: {}", metricName);
    }
    
    /**
     * Get performance report
     */
    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Current metrics
        Map<String, Object> currentMetrics = aiMetricsService.getMetricsSummary();
        report.put("currentMetrics", currentMetrics);
        
        // Baselines
        report.put("baselines", getAllBaselines());
        
        // SLA compliance
        List<SlaViolation> violations = checkSlaCompliance();
        report.put("slaViolations", violations);
        
        // Performance trends
        Map<String, Object> trends = new HashMap<>();
        for (String metricName : historicalData.keySet()) {
            List<PerformanceDataPoint> data = historicalData.get(metricName);
            if (data.size() >= 2) {
                // Calculate trend (simple linear regression slope)
                double trend = calculateTrend(data);
                trends.put(metricName, trend);
            }
        }
        report.put("trends", trends);
        
        // Summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMetrics", baselines.size());
        summary.put("slaViolations", violations.size());
        summary.put("criticalViolations", violations.stream().mapToLong(v -> "critical".equals(v.getSeverity()) ? 1 : 0).sum());
        summary.put("warningViolations", violations.stream().mapToLong(v -> "warning".equals(v.getSeverity()) ? 1 : 0).sum());
        summary.put("lastUpdated", Instant.now());
        
        report.put("summary", summary);
        
        return report;
    }
    
    /**
     * Calculate trend for a metric (simple linear regression slope)
     */
    private double calculateTrend(List<PerformanceDataPoint> data) {
        if (data.size() < 2) {
            return 0.0;
        }
        
        // Use last 100 data points for trend calculation
        List<PerformanceDataPoint> recentData = data.stream()
            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .skip(Math.max(0, data.size() - 100))
            .collect(Collectors.toList());
        
        int n = recentData.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i; // Time index
            double y = recentData.get(i).getValue();
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // Calculate slope (trend)
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * Generate capacity planning recommendations
     */
    public Map<String, Object> getCapacityPlanningRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();
        
        // Analyze trends and predict future capacity needs
        for (String metricName : historicalData.keySet()) {
            List<PerformanceDataPoint> data = historicalData.get(metricName);
            if (data.size() < 10) {
                continue;
            }
            
            double trend = calculateTrend(data);
            PerformanceBaseline baseline = baselines.get(metricName);
            
            if (baseline != null && Math.abs(trend) > baseline.getStandardDeviation() * 0.1) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("metric", metricName);
                recommendation.put("trend", trend);
                recommendation.put("currentMean", baseline.getMean());
                recommendation.put("projectedValue30Days", baseline.getMean() + (trend * 30));
                
                // Generate recommendation text
                String recommendationText;
                if (trend > 0 && metricName.contains("time")) {
                    recommendationText = "Response times are trending upward. Consider scaling resources or optimizing performance.";
                } else if (trend < 0 && metricName.contains("ratio")) {
                    recommendationText = "Performance ratio is declining. Review system efficiency and consider improvements.";
                } else if (trend > 0 && metricName.contains("queries")) {
                    recommendationText = "Query volume is increasing. Plan for additional capacity to handle growth.";
                } else {
                    recommendationText = "Monitor this metric closely as it shows significant trend changes.";
                }
                
                recommendation.put("recommendation", recommendationText);
                recommendations.put(metricName, recommendation);
            }
        }
        
        return recommendations;
    }
    
    /**
     * Initialize service with default SLAs
     */
    public void initialize() {
        if (slaDefinitions.isEmpty()) {
            initializeDefaultSlas();
        }
        logger.info("Performance baseline service initialized with {} SLA definitions", slaDefinitions.size());
    }
}