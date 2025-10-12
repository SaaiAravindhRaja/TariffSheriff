package com.tariffsheriff.backend.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Validates AI performance benchmarks for CI/CD quality gates
 * Tests response time, throughput, and resource utilization
 */
@SpringBootApplication
@Profile("quality-validation")
public class PerformanceBenchmarkValidator implements CommandLineRunner {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ObjectMapper objectMapper;

    // Performance thresholds
    private static final long MAX_RESPONSE_TIME_MS = 15000; // 15 seconds
    private static final double MIN_SUCCESS_RATE = 0.95; // 95%
    private static final double MIN_THROUGHPUT_RPS = 2.0; // 2 requests per second
    private static final long MAX_MEMORY_INCREASE_MB = 500; // 500 MB

    private static final String[] BENCHMARK_QUERIES = {
        "What is the tariff rate for importing cars from Germany?",
        "Compare importing steel from China vs India",
        "What documentation is required for importing pharmaceuticals?",
        "What trade agreements does the US have with Mexico?",
        "What HS code is used for smartphones?",
        "Analyze the risk of importing semiconductors from Taiwan",
        "What are the compliance requirements for importing medical devices?",
        "Compare shipping costs for importing electronics from Japan vs South Korea",
        "What are the anti-dumping duties on solar panels from China?",
        "What seasonal patterns affect importing fruits from Chile?"
    };

    public static void main(String[] args) {
        SpringApplication.run(PerformanceBenchmarkValidator.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting AI Performance Benchmark Validation...");
        
        PerformanceReport report = new PerformanceReport();
        
        // Measure initial memory
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Run performance tests
        ResponseTimeTest responseTimeTest = runResponseTimeTest();
        ThroughputTest throughputTest = runThroughputTest();
        ConcurrencyTest concurrencyTest = runConcurrencyTest();
        
        // Measure final memory
        System.gc(); // Suggest garbage collection
        Thread.sleep(1000); // Wait for GC
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024); // Convert to MB
        
        // Populate report
        report.setAverageResponseTime(responseTimeTest.getAverageResponseTime());
        report.setMaxResponseTime(responseTimeTest.getMaxResponseTime());
        report.setMinResponseTime(responseTimeTest.getMinResponseTime());
        report.setSuccessRate(responseTimeTest.getSuccessRate());
        report.setThroughput(throughputTest.getThroughput());
        report.setConcurrentUsers(concurrencyTest.getConcurrentUsers());
        report.setConcurrencySuccessRate(concurrencyTest.getSuccessRate());
        report.setMemoryUsage(memoryIncrease);
        
        // Validate against benchmarks
        boolean performancePassed = validatePerformanceBenchmarks(report);
        report.setQualityGatesPassed(performancePassed);
        
        // Generate recommendations
        generateRecommendations(report);
        
        // Save report
        saveReport(report);
        
        // Print results
        printResults(report);
        
        if (!performancePassed) {
            System.err.println("Performance benchmarks failed! Please optimize the system.");
            System.exit(1);
        }
    }
    
    private ResponseTimeTest runResponseTimeTest() throws Exception {
        System.out.println("Running response time test...");
        
        List<Long> responseTimes = new ArrayList<>();
        int successCount = 0;
        int totalRequests = BENCHMARK_QUERIES.length * 2; // Run each query twice
        
        for (int i = 0; i < 2; i++) {
            for (String query : BENCHMARK_QUERIES) {
                try {
                    ChatQueryRequest request = new ChatQueryRequest();
                    request.setQuery(query);
                    request.setUserId("perf-test-user");
                    request.setConversationId("perf-test-" + i + "-" + UUID.randomUUID().toString());
                    
                    long startTime = System.currentTimeMillis();
                    ChatQueryResponse response = chatbotService.processQuery(request);
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    responseTimes.add(responseTime);
                    
                    if (response != null && response.getResponse() != null && !response.getResponse().isEmpty()) {
                        successCount++;
                    }
                    
                    // Small delay between requests
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    System.err.println("Error in response time test: " + e.getMessage());
                    responseTimes.add(MAX_RESPONSE_TIME_MS + 1000); // Add penalty time
                }
            }
        }
        
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        
        long minResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0L);
        
        double successRate = (double) successCount / totalRequests;
        
        return new ResponseTimeTest(averageResponseTime, maxResponseTime, minResponseTime, successRate);
    }
    
    private ThroughputTest runThroughputTest() throws Exception {
        System.out.println("Running throughput test...");
        
        int duration = 30; // 30 seconds
        int requestCount = 0;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (duration * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            try {
                String query = BENCHMARK_QUERIES[requestCount % BENCHMARK_QUERIES.length];
                
                ChatQueryRequest request = new ChatQueryRequest();
                request.setQuery(query);
                request.setUserId("throughput-test-user");
                request.setConversationId("throughput-test-" + requestCount);
                
                ChatQueryResponse response = chatbotService.processQuery(request);
                
                if (response != null && response.getResponse() != null) {
                    requestCount++;
                }
                
                // Small delay to prevent overwhelming the system
                Thread.sleep(50);
                
            } catch (Exception e) {
                System.err.println("Error in throughput test: " + e.getMessage());
            }
        }
        
        double actualDuration = (System.currentTimeMillis() - startTime) / 1000.0;
        double throughput = requestCount / actualDuration;
        
        return new ThroughputTest(throughput, requestCount, actualDuration);
    }
    
    private ConcurrencyTest runConcurrencyTest() throws Exception {
        System.out.println("Running concurrency test...");
        
        int concurrentUsers = 5;
        int requestsPerUser = 3;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    for (int j = 0; j < requestsPerUser; j++) {
                        String query = BENCHMARK_QUERIES[(userId + j) % BENCHMARK_QUERIES.length];
                        
                        ChatQueryRequest request = new ChatQueryRequest();
                        request.setQuery(query);
                        request.setUserId("concurrent-test-user-" + userId);
                        request.setConversationId("concurrent-test-" + userId + "-" + j);
                        
                        ChatQueryResponse response = chatbotService.processQuery(request);
                        
                        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                } catch (Exception e) {
                    System.err.println("Error in concurrency test for user " + userId + ": " + e.getMessage());
                    return false;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all futures to complete
        int successCount = 0;
        for (CompletableFuture<Boolean> future : futures) {
            try {
                if (future.get(60, TimeUnit.SECONDS)) {
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("Concurrency test future failed: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        
        double successRate = (double) successCount / concurrentUsers;
        
        return new ConcurrencyTest(concurrentUsers, successRate);
    }
    
    private boolean validatePerformanceBenchmarks(PerformanceReport report) {
        boolean passed = true;
        
        if (report.getAverageResponseTime() > MAX_RESPONSE_TIME_MS) {
            System.err.printf("FAIL: Average response time %.2fms exceeds threshold %dms%n", 
                report.getAverageResponseTime(), MAX_RESPONSE_TIME_MS);
            passed = false;
        }
        
        if (report.getSuccessRate() < MIN_SUCCESS_RATE) {
            System.err.printf("FAIL: Success rate %.2f%% below threshold %.2f%%%n", 
                report.getSuccessRate() * 100, MIN_SUCCESS_RATE * 100);
            passed = false;
        }
        
        if (report.getThroughput() < MIN_THROUGHPUT_RPS) {
            System.err.printf("FAIL: Throughput %.2f RPS below threshold %.2f RPS%n", 
                report.getThroughput(), MIN_THROUGHPUT_RPS);
            passed = false;
        }
        
        if (report.getMemoryUsage() > MAX_MEMORY_INCREASE_MB) {
            System.err.printf("FAIL: Memory usage %dMB exceeds threshold %dMB%n", 
                report.getMemoryUsage(), MAX_MEMORY_INCREASE_MB);
            passed = false;
        }
        
        return passed;
    }
    
    private void generateRecommendations(PerformanceReport report) {
        List<String> recommendations = new ArrayList<>();
        
        if (report.getAverageResponseTime() > MAX_RESPONSE_TIME_MS * 0.8) {
            recommendations.add("Consider optimizing AI processing pipeline to reduce response times");
        }
        
        if (report.getSuccessRate() < 0.98) {
            recommendations.add("Investigate and fix causes of request failures");
        }
        
        if (report.getThroughput() < MIN_THROUGHPUT_RPS * 1.5) {
            recommendations.add("Consider implementing connection pooling and caching optimizations");
        }
        
        if (report.getMemoryUsage() > MAX_MEMORY_INCREASE_MB * 0.7) {
            recommendations.add("Review memory usage patterns and implement garbage collection optimizations");
        }
        
        if (report.getConcurrencySuccessRate() < 0.9) {
            recommendations.add("Improve concurrent request handling and thread safety");
        }
        
        report.setRecommendations(recommendations);
    }
    
    private void saveReport(PerformanceReport report) throws IOException {
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        File reportFile = new File(targetDir, "performance-report.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);
        
        System.out.println("Performance report saved to: " + reportFile.getAbsolutePath());
    }
    
    private void printResults(PerformanceReport report) {
        System.out.printf("%nPerformance Benchmark Results:%n");
        System.out.printf("Average Response Time: %.2fms (threshold: %dms)%n", 
            report.getAverageResponseTime(), MAX_RESPONSE_TIME_MS);
        System.out.printf("Success Rate: %.2f%% (threshold: %.2f%%)%n", 
            report.getSuccessRate() * 100, MIN_SUCCESS_RATE * 100);
        System.out.printf("Throughput: %.2f RPS (threshold: %.2f RPS)%n", 
            report.getThroughput(), MIN_THROUGHPUT_RPS);
        System.out.printf("Memory Usage: %dMB (threshold: %dMB)%n", 
            report.getMemoryUsage(), MAX_MEMORY_INCREASE_MB);
        System.out.printf("Concurrency Success Rate: %.2f%%\n", 
            report.getConcurrencySuccessRate() * 100);
        System.out.printf("Quality Gates: %s%n", 
            report.isQualityGatesPassed() ? "PASSED" : "FAILED");
        
        if (!report.getRecommendations().isEmpty()) {
            System.out.println("\nRecommendations:");
            report.getRecommendations().forEach(rec -> System.out.println("- " + rec));
        }
    }
    
    // Inner classes for test results
    public static class ResponseTimeTest {
        private double averageResponseTime;
        private long maxResponseTime;
        private long minResponseTime;
        private double successRate;
        
        public ResponseTimeTest(double averageResponseTime, long maxResponseTime, 
                               long minResponseTime, double successRate) {
            this.averageResponseTime = averageResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.minResponseTime = minResponseTime;
            this.successRate = successRate;
        }
        
        // Getters
        public double getAverageResponseTime() { return averageResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public long getMinResponseTime() { return minResponseTime; }
        public double getSuccessRate() { return successRate; }
    }
    
    public static class ThroughputTest {
        private double throughput;
        private int requestCount;
        private double duration;
        
        public ThroughputTest(double throughput, int requestCount, double duration) {
            this.throughput = throughput;
            this.requestCount = requestCount;
            this.duration = duration;
        }
        
        // Getters
        public double getThroughput() { return throughput; }
        public int getRequestCount() { return requestCount; }
        public double getDuration() { return duration; }
    }
    
    public static class ConcurrencyTest {
        private int concurrentUsers;
        private double successRate;
        
        public ConcurrencyTest(int concurrentUsers, double successRate) {
            this.concurrentUsers = concurrentUsers;
            this.successRate = successRate;
        }
        
        // Getters
        public int getConcurrentUsers() { return concurrentUsers; }
        public double getSuccessRate() { return successRate; }
    }
    
    public static class PerformanceReport {
        private double averageResponseTime;
        private long maxResponseTime;
        private long minResponseTime;
        private double successRate;
        private double throughput;
        private int concurrentUsers;
        private double concurrencySuccessRate;
        private long memoryUsage;
        private boolean qualityGatesPassed;
        private List<String> recommendations = new ArrayList<>();
        
        // Getters and setters
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public long getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(long minResponseTime) { this.minResponseTime = minResponseTime; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
        
        public int getConcurrentUsers() { return concurrentUsers; }
        public void setConcurrentUsers(int concurrentUsers) { this.concurrentUsers = concurrentUsers; }
        
        public double getConcurrencySuccessRate() { return concurrencySuccessRate; }
        public void setConcurrencySuccessRate(double concurrencySuccessRate) { this.concurrencySuccessRate = concurrencySuccessRate; }
        
        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public boolean isQualityGatesPassed() { return qualityGatesPassed; }
        public void setQualityGatesPassed(boolean qualityGatesPassed) { this.qualityGatesPassed = qualityGatesPassed; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}