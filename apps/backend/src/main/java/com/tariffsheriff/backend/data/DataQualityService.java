package com.tariffsheriff.backend.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for data quality validation and monitoring
 * Features:
 * - Data quality metrics and scoring system
 * - Automated data validation and anomaly detection
 * - Data freshness monitoring and alerting
 * - Data lineage tracking for audit and compliance
 */
@Service
public class DataQualityService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataQualityService.class);
    
    // Configuration
    @Value("${data.quality.freshness.threshold.minutes:60}")
    private long freshnessThresholdMinutes;
    
    @Value("${data.quality.completeness.threshold:0.95}")
    private double completenessThreshold;
    
    @Value("${data.quality.accuracy.threshold:0.90}")
    private double accuracyThreshold;
    
    @Value("${data.quality.consistency.threshold:0.85}")
    private double consistencyThreshold;
    
    // Data quality metrics
    private final Map<String, DataQualityMetrics> qualityMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<DataValidationRule>> validationRules = new ConcurrentHashMap<>();
    private final Map<String, DataLineage> dataLineage = new ConcurrentHashMap<>();
    
    // Anomaly detection
    private final Map<String, AnomalyDetector> anomalyDetectors = new ConcurrentHashMap<>();
    private final List<DataQualityAlert> activeAlerts = new CopyOnWriteArrayList<>();
    
    // Statistics
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong failedValidations = new AtomicLong(0);
    private final AtomicLong anomaliesDetected = new AtomicLong(0);
    
    // Executors
    private final ScheduledExecutorService monitoringExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService validationExecutor = Executors.newFixedThreadPool(3);
    
    public DataQualityService() {
        initializeValidationRules();
        initializeAnomalyDetectors();
        
        // Schedule quality monitoring every 5 minutes
        monitoringExecutor.scheduleAtFixedRate(this::performQualityMonitoring, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Initialize validation rules for different data types
     */
    private void initializeValidationRules() {
        // Tariff data validation rules
        List<DataValidationRule> tariffRules = Arrays.asList(
            new DataValidationRule("tariff_rate_range", "Tariff rate must be between 0 and 100", 
                data -> {
                    if (data.containsKey("tariffRate")) {
                        double rate = (Double) data.get("tariffRate");
                        return rate >= 0 && rate <= 100;
                    }
                    return true;
                }),
            new DataValidationRule("country_code_format", "Country code must be 2 characters", 
                data -> {
                    if (data.containsKey("countryCode")) {
                        String code = (String) data.get("countryCode");
                        return code != null && code.length() == 2;
                    }
                    return true;
                }),
            new DataValidationRule("hs_code_format", "HS code must be 6-10 digits", 
                data -> {
                    if (data.containsKey("hsCode")) {
                        String code = (String) data.get("hsCode");
                        return code != null && Pattern.matches("\\d{6,10}", code);
                    }
                    return true;
                })
        );
        validationRules.put("tariff", tariffRules);
        
        // Market data validation rules
        List<DataValidationRule> marketRules = Arrays.asList(
            new DataValidationRule("price_positive", "Price must be positive", 
                data -> {
                    if (data.containsKey("price")) {
                        double price = (Double) data.get("price");
                        return price > 0;
                    }
                    return true;
                }),
            new DataValidationRule("volume_non_negative", "Volume must be non-negative", 
                data -> {
                    if (data.containsKey("volume")) {
                        long volume = (Long) data.get("volume");
                        return volume >= 0;
                    }
                    return true;
                })
        );
        validationRules.put("market", marketRules);
        
        // News data validation rules
        List<DataValidationRule> newsRules = Arrays.asList(
            new DataValidationRule("title_not_empty", "Title must not be empty", 
                data -> {
                    if (data.containsKey("title")) {
                        String title = (String) data.get("title");
                        return title != null && !title.trim().isEmpty();
                    }
                    return true;
                }),
            new DataValidationRule("url_format", "URL must be valid", 
                data -> {
                    if (data.containsKey("url")) {
                        String url = (String) data.get("url");
                        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
                    }
                    return true;
                })
        );
        validationRules.put("news", newsRules);
    }
    
    /**
     * Initialize anomaly detectors for different data types
     */
    private void initializeAnomalyDetectors() {
        // Tariff rate anomaly detector
        anomalyDetectors.put("tariff_rates", new AnomalyDetector("tariff_rates", 
            values -> {
                if (values.size() < 10) return false;
                
                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double stdDev = Math.sqrt(values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average().orElse(0));
                
                double latest = values.get(values.size() - 1);
                return Math.abs(latest - mean) > 3 * stdDev; // 3-sigma rule
            }));
        
        // Market price anomaly detector
        anomalyDetectors.put("market_prices", new AnomalyDetector("market_prices",
            values -> {
                if (values.size() < 5) return false;
                
                // Check for sudden price spikes (>50% change)
                double current = values.get(values.size() - 1);
                double previous = values.get(values.size() - 2);
                
                return Math.abs((current - previous) / previous) > 0.5;
            }));
        
        // Data volume anomaly detector
        anomalyDetectors.put("data_volume", new AnomalyDetector("data_volume",
            values -> {
                if (values.size() < 7) return false;
                
                // Check for unusual data volume patterns
                double recent = values.subList(values.size() - 3, values.size())
                    .stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double historical = values.subList(0, values.size() - 3)
                    .stream().mapToDouble(Double::doubleValue).average().orElse(0);
                
                return Math.abs(recent - historical) / historical > 0.3; // 30% deviation
            }));
    }
    
    /**
     * Validate data quality for a dataset
     */
    public CompletableFuture<DataQualityResult> validateData(String dataType, List<Map<String, Object>> dataset) {
        return CompletableFuture.supplyAsync(() -> {
            totalValidations.incrementAndGet();
            
            try {
                DataQualityResult result = performValidation(dataType, dataset);
                
                // Update metrics
                updateQualityMetrics(dataType, result);
                
                // Check for anomalies
                detectAnomalies(dataType, dataset);
                
                // Update data lineage
                updateDataLineage(dataType, dataset.size(), Instant.now());
                
                if (result.getOverallScore() < accuracyThreshold) {
                    failedValidations.incrementAndGet();
                    createQualityAlert(dataType, "Low data quality score: " + result.getOverallScore());
                }
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error validating data for type: {}", dataType, e);
                failedValidations.incrementAndGet();
                return new DataQualityResult(dataType, 0.0, Collections.emptyList(), Collections.emptyList());
            }
        }, validationExecutor);
    }
    
    /**
     * Perform actual data validation
     */
    private DataQualityResult performValidation(String dataType, List<Map<String, Object>> dataset) {
        List<DataValidationRule> rules = validationRules.getOrDefault(dataType, Collections.emptyList());
        List<ValidationViolation> violations = new ArrayList<>();
        List<QualityDimension> dimensions = new ArrayList<>();
        
        if (dataset.isEmpty()) {
            violations.add(new ValidationViolation("empty_dataset", "Dataset is empty", "CRITICAL"));
            return new DataQualityResult(dataType, 0.0, violations, dimensions);
        }
        
        // Completeness check
        double completeness = calculateCompleteness(dataset);
        dimensions.add(new QualityDimension("completeness", completeness));
        
        // Accuracy check (rule-based validation)
        double accuracy = calculateAccuracy(dataset, rules, violations);
        dimensions.add(new QualityDimension("accuracy", accuracy));
        
        // Consistency check
        double consistency = calculateConsistency(dataset);
        dimensions.add(new QualityDimension("consistency", consistency));
        
        // Freshness check
        double freshness = calculateFreshness(dataset);
        dimensions.add(new QualityDimension("freshness", freshness));
        
        // Calculate overall score
        double overallScore = (completeness * 0.3 + accuracy * 0.3 + consistency * 0.2 + freshness * 0.2);
        
        return new DataQualityResult(dataType, overallScore, violations, dimensions);
    }
    
    /**
     * Calculate data completeness
     */
    private double calculateCompleteness(List<Map<String, Object>> dataset) {
        if (dataset.isEmpty()) return 0.0;
        
        Set<String> allFields = dataset.stream()
            .flatMap(record -> record.keySet().stream())
            .collect(Collectors.toSet());
        
        int totalFields = allFields.size() * dataset.size();
        int nonNullFields = 0;
        
        for (Map<String, Object> record : dataset) {
            for (String field : allFields) {
                Object value = record.get(field);
                if (value != null && !value.toString().trim().isEmpty()) {
                    nonNullFields++;
                }
            }
        }
        
        return totalFields > 0 ? (double) nonNullFields / totalFields : 0.0;
    }
    
    /**
     * Calculate data accuracy based on validation rules
     */
    private double calculateAccuracy(List<Map<String, Object>> dataset, List<DataValidationRule> rules, 
                                   List<ValidationViolation> violations) {
        if (rules.isEmpty()) return 1.0;
        
        int totalChecks = dataset.size() * rules.size();
        int passedChecks = 0;
        
        for (Map<String, Object> record : dataset) {
            for (DataValidationRule rule : rules) {
                try {
                    if (rule.validate(record)) {
                        passedChecks++;
                    } else {
                        violations.add(new ValidationViolation(rule.getName(), rule.getDescription(), "ERROR"));
                    }
                } catch (Exception e) {
                    violations.add(new ValidationViolation(rule.getName(), 
                        "Validation error: " + e.getMessage(), "ERROR"));
                }
            }
        }
        
        return totalChecks > 0 ? (double) passedChecks / totalChecks : 1.0;
    }
    
    /**
     * Calculate data consistency
     */
    private double calculateConsistency(List<Map<String, Object>> dataset) {
        if (dataset.size() < 2) return 1.0;
        
        Map<String, Set<Object>> fieldValues = new HashMap<>();
        
        // Collect all values for each field
        for (Map<String, Object> record : dataset) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                fieldValues.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(entry.getValue());
            }
        }
        
        // Calculate consistency score based on value distribution
        double totalConsistency = 0.0;
        int fieldCount = 0;
        
        for (Map.Entry<String, Set<Object>> entry : fieldValues.entrySet()) {
            Set<Object> values = entry.getValue();
            
            // For categorical fields, consistency is higher with fewer unique values
            // For numerical fields, we check for reasonable distribution
            double fieldConsistency = 1.0 - (double) values.size() / dataset.size();
            totalConsistency += Math.max(0.0, fieldConsistency);
            fieldCount++;
        }
        
        return fieldCount > 0 ? totalConsistency / fieldCount : 1.0;
    }
    
    /**
     * Calculate data freshness
     */
    private double calculateFreshness(List<Map<String, Object>> dataset) {
        Instant now = Instant.now();
        Instant threshold = now.minus(Duration.ofMinutes(freshnessThresholdMinutes));
        
        long freshRecords = dataset.stream()
            .mapToLong(record -> {
                Object timestamp = record.get("timestamp");
                if (timestamp instanceof Instant) {
                    return ((Instant) timestamp).isAfter(threshold) ? 1 : 0;
                }
                return 0; // Assume fresh if no timestamp
            })
            .sum();
        
        return dataset.size() > 0 ? (double) freshRecords / dataset.size() : 1.0;
    }    /**

     * Detect anomalies in the dataset
     */
    private void detectAnomalies(String dataType, List<Map<String, Object>> dataset) {
        // Extract numerical values for anomaly detection
        Map<String, List<Double>> numericalFields = extractNumericalFields(dataset);
        
        for (Map.Entry<String, List<Double>> entry : numericalFields.entrySet()) {
            String fieldName = entry.getKey();
            List<Double> values = entry.getValue();
            
            String detectorKey = dataType + "_" + fieldName;
            AnomalyDetector detector = anomalyDetectors.get(detectorKey);
            
            if (detector == null) {
                // Create a generic anomaly detector for this field
                detector = new AnomalyDetector(detectorKey, this::detectStatisticalAnomaly);
                anomalyDetectors.put(detectorKey, detector);
            }
            
            if (detector.detectAnomaly(values)) {
                anomaliesDetected.incrementAndGet();
                createAnomalyAlert(dataType, fieldName, values.get(values.size() - 1));
            }
        }
    }
    
    /**
     * Extract numerical fields from dataset
     */
    private Map<String, List<Double>> extractNumericalFields(List<Map<String, Object>> dataset) {
        Map<String, List<Double>> numericalFields = new HashMap<>();
        
        for (Map<String, Object> record : dataset) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    numericalFields.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(((Number) value).doubleValue());
                }
            }
        }
        
        return numericalFields;
    }
    
    /**
     * Generic statistical anomaly detection
     */
    private boolean detectStatisticalAnomaly(List<Double> values) {
        if (values.size() < 10) return false;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = Math.sqrt(values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0));
        
        double latest = values.get(values.size() - 1);
        return Math.abs(latest - mean) > 2.5 * stdDev; // 2.5-sigma threshold
    }
    
    /**
     * Update quality metrics for a data type
     */
    private void updateQualityMetrics(String dataType, DataQualityResult result) {
        DataQualityMetrics metrics = qualityMetrics.computeIfAbsent(dataType, 
            k -> new DataQualityMetrics(dataType));
        
        metrics.updateMetrics(result);
    }
    
    /**
     * Update data lineage information
     */
    private void updateDataLineage(String dataType, int recordCount, Instant timestamp) {
        DataLineage lineage = dataLineage.computeIfAbsent(dataType, 
            k -> new DataLineage(dataType));
        
        lineage.addDataPoint(recordCount, timestamp);
    }
    
    /**
     * Create a data quality alert
     */
    private void createQualityAlert(String dataType, String message) {
        DataQualityAlert alert = new DataQualityAlert(
            UUID.randomUUID().toString(),
            dataType,
            "QUALITY_ISSUE",
            message,
            "MEDIUM",
            Instant.now()
        );
        
        activeAlerts.add(alert);
        logger.warn("Data quality alert for {}: {}", dataType, message);
    }
    
    /**
     * Create an anomaly alert
     */
    private void createAnomalyAlert(String dataType, String fieldName, double value) {
        String message = String.format("Anomaly detected in field '%s': value %f", fieldName, value);
        
        DataQualityAlert alert = new DataQualityAlert(
            UUID.randomUUID().toString(),
            dataType,
            "ANOMALY",
            message,
            "HIGH",
            Instant.now()
        );
        
        activeAlerts.add(alert);
        logger.warn("Anomaly alert for {}: {}", dataType, message);
    }
    
    /**
     * Perform periodic quality monitoring
     */
    private void performQualityMonitoring() {
        try {
            // Clean up old alerts (older than 24 hours)
            Instant cutoff = Instant.now().minus(Duration.ofHours(24));
            activeAlerts.removeIf(alert -> alert.getTimestamp().isBefore(cutoff));
            
            // Log quality metrics summary
            logQualityMetricsSummary();
            
            // Check for persistent quality issues
            checkForPersistentIssues();
            
        } catch (Exception e) {
            logger.error("Error during quality monitoring", e);
        }
    }
    
    /**
     * Log quality metrics summary
     */
    private void logQualityMetricsSummary() {
        if (qualityMetrics.isEmpty()) return;
        
        logger.info("Data Quality Summary:");
        qualityMetrics.forEach((dataType, metrics) -> {
            logger.info("  {}: Score={:.2f}, Validations={}, Failures={}", 
                dataType, metrics.getAverageScore(), 
                metrics.getTotalValidations(), metrics.getFailureCount());
        });
    }
    
    /**
     * Check for persistent quality issues
     */
    private void checkForPersistentIssues() {
        qualityMetrics.forEach((dataType, metrics) -> {
            if (metrics.getAverageScore() < accuracyThreshold && 
                metrics.getTotalValidations() > 10) {
                
                createQualityAlert(dataType, 
                    String.format("Persistent quality issues detected. Average score: %.2f", 
                        metrics.getAverageScore()));
            }
        });
    }
    
    /**
     * Get quality metrics for a specific data type
     */
    public DataQualityMetrics getQualityMetrics(String dataType) {
        return qualityMetrics.get(dataType);
    }
    
    /**
     * Get all quality metrics
     */
    public Map<String, DataQualityMetrics> getAllQualityMetrics() {
        return new HashMap<>(qualityMetrics);
    }
    
    /**
     * Get active alerts
     */
    public List<DataQualityAlert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }
    
    /**
     * Get alerts for specific data type
     */
    public List<DataQualityAlert> getAlertsForDataType(String dataType) {
        return activeAlerts.stream()
            .filter(alert -> alert.getDataType().equals(dataType))
            .collect(Collectors.toList());
    }
    
    /**
     * Get data lineage for a specific data type
     */
    public DataLineage getDataLineage(String dataType) {
        return dataLineage.get(dataType);
    }
    
    /**
     * Get overall quality statistics
     */
    public DataQualityStats getOverallStats() {
        double averageScore = qualityMetrics.values().stream()
            .mapToDouble(DataQualityMetrics::getAverageScore)
            .average().orElse(0.0);
        
        return new DataQualityStats(
            totalValidations.get(),
            failedValidations.get(),
            anomaliesDetected.get(),
            activeAlerts.size(),
            averageScore
        );
    }
    
    /**
     * Clear all quality data
     */
    public void clearQualityData() {
        qualityMetrics.clear();
        activeAlerts.clear();
        dataLineage.clear();
        totalValidations.set(0);
        failedValidations.set(0);
        anomaliesDetected.set(0);
        
        logger.info("Data quality data cleared");
    }
    
    /**
     * Data validation rule
     */
    public static class DataValidationRule {
        private final String name;
        private final String description;
        private final ValidationFunction validator;
        
        public DataValidationRule(String name, String description, ValidationFunction validator) {
            this.name = name;
            this.description = description;
            this.validator = validator;
        }
        
        public boolean validate(Map<String, Object> data) {
            return validator.validate(data);
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * Validation function interface
     */
    @FunctionalInterface
    public interface ValidationFunction {
        boolean validate(Map<String, Object> data);
    }
    
    /**
     * Anomaly detector
     */
    public static class AnomalyDetector {
        private final String name;
        private final AnomalyDetectionFunction detector;
        private final List<Double> historicalValues = new ArrayList<>();
        
        public AnomalyDetector(String name, AnomalyDetectionFunction detector) {
            this.name = name;
            this.detector = detector;
        }
        
        public boolean detectAnomaly(List<Double> values) {
            // Update historical values
            historicalValues.addAll(values);
            
            // Keep only recent values (last 100)
            if (historicalValues.size() > 100) {
                historicalValues.subList(0, historicalValues.size() - 100).clear();
            }
            
            return detector.detect(new ArrayList<>(historicalValues));
        }
        
        public String getName() { return name; }
    }
    
    /**
     * Anomaly detection function interface
     */
    @FunctionalInterface
    public interface AnomalyDetectionFunction {
        boolean detect(List<Double> values);
    }
    
    /**
     * Data quality result
     */
    public static class DataQualityResult {
        private final String dataType;
        private final double overallScore;
        private final List<ValidationViolation> violations;
        private final List<QualityDimension> dimensions;
        
        public DataQualityResult(String dataType, double overallScore, 
                               List<ValidationViolation> violations, List<QualityDimension> dimensions) {
            this.dataType = dataType;
            this.overallScore = overallScore;
            this.violations = violations;
            this.dimensions = dimensions;
        }
        
        public String getDataType() { return dataType; }
        public double getOverallScore() { return overallScore; }
        public List<ValidationViolation> getViolations() { return violations; }
        public List<QualityDimension> getDimensions() { return dimensions; }
        
        public boolean isHighQuality() { return overallScore >= 0.8; }
        public boolean hasCriticalIssues() { 
            return violations.stream().anyMatch(v -> "CRITICAL".equals(v.getSeverity())); 
        }
    }
    
    /**
     * Validation violation
     */
    public static class ValidationViolation {
        private final String ruleName;
        private final String message;
        private final String severity;
        
        public ValidationViolation(String ruleName, String message, String severity) {
            this.ruleName = ruleName;
            this.message = message;
            this.severity = severity;
        }
        
        public String getRuleName() { return ruleName; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
    }
    
    /**
     * Quality dimension
     */
    public static class QualityDimension {
        private final String name;
        private final double score;
        
        public QualityDimension(String name, double score) {
            this.name = name;
            this.score = score;
        }
        
        public String getName() { return name; }
        public double getScore() { return score; }
    }
    
    /**
     * Data quality metrics
     */
    public static class DataQualityMetrics {
        private final String dataType;
        private final List<Double> scores = new ArrayList<>();
        private int totalValidations = 0;
        private int failureCount = 0;
        private Instant lastUpdate = Instant.now();
        
        public DataQualityMetrics(String dataType) {
            this.dataType = dataType;
        }
        
        public void updateMetrics(DataQualityResult result) {
            scores.add(result.getOverallScore());
            totalValidations++;
            
            if (!result.isHighQuality()) {
                failureCount++;
            }
            
            lastUpdate = Instant.now();
            
            // Keep only recent scores (last 100)
            if (scores.size() > 100) {
                scores.remove(0);
            }
        }
        
        public String getDataType() { return dataType; }
        public double getAverageScore() { 
            return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0); 
        }
        public int getTotalValidations() { return totalValidations; }
        public int getFailureCount() { return failureCount; }
        public double getFailureRate() { 
            return totalValidations > 0 ? (double) failureCount / totalValidations : 0.0; 
        }
        public Instant getLastUpdate() { return lastUpdate; }
    }
    
    /**
     * Data quality alert
     */
    public static class DataQualityAlert {
        private final String id;
        private final String dataType;
        private final String alertType;
        private final String message;
        private final String severity;
        private final Instant timestamp;
        
        public DataQualityAlert(String id, String dataType, String alertType, 
                              String message, String severity, Instant timestamp) {
            this.id = id;
            this.dataType = dataType;
            this.alertType = alertType;
            this.message = message;
            this.severity = severity;
            this.timestamp = timestamp;
        }
        
        public String getId() { return id; }
        public String getDataType() { return dataType; }
        public String getAlertType() { return alertType; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Data lineage tracking
     */
    public static class DataLineage {
        private final String dataType;
        private final List<DataPoint> dataPoints = new ArrayList<>();
        
        public DataLineage(String dataType) {
            this.dataType = dataType;
        }
        
        public void addDataPoint(int recordCount, Instant timestamp) {
            dataPoints.add(new DataPoint(recordCount, timestamp));
            
            // Keep only recent data points (last 1000)
            if (dataPoints.size() > 1000) {
                dataPoints.remove(0);
            }
        }
        
        public String getDataType() { return dataType; }
        public List<DataPoint> getDataPoints() { return new ArrayList<>(dataPoints); }
        
        public static class DataPoint {
            private final int recordCount;
            private final Instant timestamp;
            
            public DataPoint(int recordCount, Instant timestamp) {
                this.recordCount = recordCount;
                this.timestamp = timestamp;
            }
            
            public int getRecordCount() { return recordCount; }
            public Instant getTimestamp() { return timestamp; }
        }
    }
    
    /**
     * Overall data quality statistics
     */
    public static class DataQualityStats {
        private final long totalValidations;
        private final long failedValidations;
        private final long anomaliesDetected;
        private final int activeAlerts;
        private final double averageQualityScore;
        
        public DataQualityStats(long totalValidations, long failedValidations, 
                              long anomaliesDetected, int activeAlerts, double averageQualityScore) {
            this.totalValidations = totalValidations;
            this.failedValidations = failedValidations;
            this.anomaliesDetected = anomaliesDetected;
            this.activeAlerts = activeAlerts;
            this.averageQualityScore = averageQualityScore;
        }
        
        public long getTotalValidations() { return totalValidations; }
        public long getFailedValidations() { return failedValidations; }
        public long getAnomaliesDetected() { return anomaliesDetected; }
        public int getActiveAlerts() { return activeAlerts; }
        public double getAverageQualityScore() { return averageQualityScore; }
        public double getSuccessRate() { 
            return totalValidations > 0 ? 1.0 - (double) failedValidations / totalValidations : 1.0; 
        }
    }
}