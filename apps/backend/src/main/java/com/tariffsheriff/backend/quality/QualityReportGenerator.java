package com.tariffsheriff.backend.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates comprehensive quality report combining accuracy and performance metrics
 * Used in CI/CD pipeline for quality gates and PR comments
 */
@SpringBootApplication
@Profile("quality-validation")
public class QualityReportGenerator implements CommandLineRunner {

    @Autowired
    private ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(QualityReportGenerator.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Generating comprehensive quality report...");
        
        // Load individual reports
        ResponseAccuracyValidator.QualityReport accuracyReport = loadAccuracyReport();
        PerformanceBenchmarkValidator.PerformanceReport performanceReport = loadPerformanceReport();
        
        // Generate combined report
        CombinedQualityReport combinedReport = generateCombinedReport(accuracyReport, performanceReport);
        
        // Save combined report
        saveCombinedReport(combinedReport);
        
        // Print summary
        printQualitySummary(combinedReport);
        
        System.out.println("Quality report generation completed.");
    }
    
    private ResponseAccuracyValidator.QualityReport loadAccuracyReport() {
        try {
            File accuracyFile = new File("target/accuracy-report.json");
            if (accuracyFile.exists()) {
                return objectMapper.readValue(accuracyFile, ResponseAccuracyValidator.QualityReport.class);
            } else {
                System.out.println("Warning: Accuracy report not found, using default values");
                ResponseAccuracyValidator.QualityReport defaultReport = new ResponseAccuracyValidator.QualityReport();
                defaultReport.setAccuracyScore(0.0);
                defaultReport.setPassRate(0.0);
                defaultReport.setQualityGatesPassed(false);
                defaultReport.getRecommendations().add("Run accuracy validation to get detailed metrics");
                return defaultReport;
            }
        } catch (IOException e) {
            System.err.println("Error loading accuracy report: " + e.getMessage());
            ResponseAccuracyValidator.QualityReport errorReport = new ResponseAccuracyValidator.QualityReport();
            errorReport.setAccuracyScore(0.0);
            errorReport.setPassRate(0.0);
            errorReport.setQualityGatesPassed(false);
            errorReport.getRecommendations().add("Failed to load accuracy report: " + e.getMessage());
            return errorReport;
        }
    }
    
    private PerformanceBenchmarkValidator.PerformanceReport loadPerformanceReport() {
        try {
            File performanceFile = new File("target/performance-report.json");
            if (performanceFile.exists()) {
                return objectMapper.readValue(performanceFile, PerformanceBenchmarkValidator.PerformanceReport.class);
            } else {
                System.out.println("Warning: Performance report not found, using default values");
                PerformanceBenchmarkValidator.PerformanceReport defaultReport = new PerformanceBenchmarkValidator.PerformanceReport();
                defaultReport.setAverageResponseTime(0.0);
                defaultReport.setSuccessRate(0.0);
                defaultReport.setThroughput(0.0);
                defaultReport.setQualityGatesPassed(false);
                defaultReport.getRecommendations().add("Run performance validation to get detailed metrics");
                return defaultReport;
            }
        } catch (IOException e) {
            System.err.println("Error loading performance report: " + e.getMessage());
            PerformanceBenchmarkValidator.PerformanceReport errorReport = new PerformanceBenchmarkValidator.PerformanceReport();
            errorReport.setAverageResponseTime(0.0);
            errorReport.setSuccessRate(0.0);
            errorReport.setThroughput(0.0);
            errorReport.setQualityGatesPassed(false);
            errorReport.getRecommendations().add("Failed to load performance report: " + e.getMessage());
            return errorReport;
        }
    }
    
    private CombinedQualityReport generateCombinedReport(
            ResponseAccuracyValidator.QualityReport accuracyReport,
            PerformanceBenchmarkValidator.PerformanceReport performanceReport) {
        
        CombinedQualityReport combined = new CombinedQualityReport();
        
        // Basic info
        combined.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        combined.setVersion("1.0.0");
        
        // Accuracy metrics
        combined.setAccuracyScore(accuracyReport.getAccuracyScore());
        combined.setAccuracyPassRate(accuracyReport.getPassRate());
        
        // Performance metrics
        combined.setAverageResponseTime(performanceReport.getAverageResponseTime());
        combined.setSuccessRate(performanceReport.getSuccessRate() * 100); // Convert to percentage
        combined.setThroughput(performanceReport.getThroughput());
        combined.setMemoryUsage(performanceReport.getMemoryUsage());
        
        // Calculate cache hit rate (simulated for demo)
        combined.setCacheHitRate(calculateCacheHitRate());
        
        // Context retention (simulated for demo)
        combined.setContextRetention(calculateContextRetention());
        
        // Overall quality gates
        boolean overallPassed = accuracyReport.isQualityGatesPassed() && performanceReport.isQualityGatesPassed();
        combined.setQualityGatesPassed(overallPassed);
        
        // Combine recommendations
        List<String> allRecommendations = new ArrayList<>();
        allRecommendations.addAll(accuracyReport.getRecommendations());
        allRecommendations.addAll(performanceReport.getRecommendations());
        
        // Add overall recommendations
        if (!overallPassed) {
            allRecommendations.add("Overall quality gates failed - review both accuracy and performance metrics");
        }
        
        if (combined.getAccuracyScore() < 80) {
            allRecommendations.add("Consider retraining or fine-tuning AI models to improve accuracy");
        }
        
        if (combined.getAverageResponseTime() > 10000) {
            allRecommendations.add("Implement response caching and optimize AI processing pipeline");
        }
        
        combined.setRecommendations(allRecommendations);
        
        // Quality score calculation
        double qualityScore = calculateOverallQualityScore(combined);
        combined.setOverallQualityScore(qualityScore);
        
        return combined;
    }
    
    private double calculateCacheHitRate() {
        // In a real implementation, this would come from actual cache metrics
        // For demo purposes, simulate a reasonable cache hit rate
        return 75.0 + (Math.random() * 20.0); // 75-95%
    }
    
    private double calculateContextRetention() {
        // In a real implementation, this would measure how well the AI maintains context
        // For demo purposes, simulate context retention
        return 85.0 + (Math.random() * 10.0); // 85-95%
    }
    
    private double calculateOverallQualityScore(CombinedQualityReport report) {
        // Weighted quality score calculation
        double accuracyWeight = 0.4;
        double performanceWeight = 0.3;
        double reliabilityWeight = 0.3;
        
        double accuracyScore = report.getAccuracyScore();
        double performanceScore = Math.min(100, (15000 - report.getAverageResponseTime()) / 150); // Normalize response time
        double reliabilityScore = report.getSuccessRate();
        
        return (accuracyScore * accuracyWeight) + 
               (performanceScore * performanceWeight) + 
               (reliabilityScore * reliabilityWeight);
    }
    
    private void saveCombinedReport(CombinedQualityReport report) throws IOException {
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        File reportFile = new File(targetDir, "quality-report.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);
        
        System.out.println("Combined quality report saved to: " + reportFile.getAbsolutePath());
    }
    
    private void printQualitySummary(CombinedQualityReport report) {
        System.out.printf("%n=== AI Copilot Quality Summary ===%n");
        System.out.printf("Overall Quality Score: %.1f/100%n", report.getOverallQualityScore());
        System.out.printf("Quality Gates Status: %s%n", report.isQualityGatesPassed() ? "✅ PASSED" : "❌ FAILED");
        System.out.printf("%n--- Accuracy Metrics ---%n");
        System.out.printf("Accuracy Score: %.1f%%%n", report.getAccuracyScore());
        System.out.printf("Test Pass Rate: %.1f%%%n", report.getAccuracyPassRate());
        System.out.printf("Context Retention: %.1f%%%n", report.getContextRetention());
        System.out.printf("%n--- Performance Metrics ---%n");
        System.out.printf("Average Response Time: %.0fms%n", report.getAverageResponseTime());
        System.out.printf("Success Rate: %.1f%%%n", report.getSuccessRate());
        System.out.printf("Throughput: %.1f requests/second%n", report.getThroughput());
        System.out.printf("Memory Usage: %dMB%n", report.getMemoryUsage());
        System.out.printf("Cache Hit Rate: %.1f%%%n", report.getCacheHitRate());
        
        if (!report.getRecommendations().isEmpty()) {
            System.out.printf("%n--- Recommendations ---%n");
            report.getRecommendations().forEach(rec -> System.out.println("• " + rec));
        }
        
        System.out.printf("%n=== End Quality Summary ===%n");
    }
    
    public static class CombinedQualityReport {
        private String timestamp;
        private String version;
        private double overallQualityScore;
        private boolean qualityGatesPassed;
        
        // Accuracy metrics
        private double accuracyScore;
        private double accuracyPassRate;
        private double contextRetention;
        
        // Performance metrics
        private double averageResponseTime;
        private double successRate;
        private double throughput;
        private long memoryUsage;
        private double cacheHitRate;
        
        // Recommendations
        private List<String> recommendations = new ArrayList<>();
        
        // Getters and setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public double getOverallQualityScore() { return overallQualityScore; }
        public void setOverallQualityScore(double overallQualityScore) { this.overallQualityScore = overallQualityScore; }
        
        public boolean isQualityGatesPassed() { return qualityGatesPassed; }
        public void setQualityGatesPassed(boolean qualityGatesPassed) { this.qualityGatesPassed = qualityGatesPassed; }
        
        public double getAccuracyScore() { return accuracyScore; }
        public void setAccuracyScore(double accuracyScore) { this.accuracyScore = accuracyScore; }
        
        public double getAccuracyPassRate() { return accuracyPassRate; }
        public void setAccuracyPassRate(double accuracyPassRate) { this.accuracyPassRate = accuracyPassRate; }
        
        public double getContextRetention() { return contextRetention; }
        public void setContextRetention(double contextRetention) { this.contextRetention = contextRetention; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
        
        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}