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
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Validates AI response accuracy against known correct answers
 * Used in CI/CD pipeline for quality gates
 */
@SpringBootApplication
@Profile("quality-validation")
public class ResponseAccuracyValidator implements CommandLineRunner {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final List<QualityTestCase> TEST_CASES = Arrays.asList(
        new QualityTestCase(
            "What is the tariff rate for importing cars from Germany to the US?",
            Arrays.asList("2.5%", "tariff", "Germany", "car", "automotive"),
            Arrays.asList("wrong", "incorrect", "don't know", "not sure"),
            0.8
        ),
        new QualityTestCase(
            "What HS code is used for smartphones?",
            Arrays.asList("8517", "phone", "mobile", "cellular"),
            Arrays.asList("wrong", "incorrect", "don't know"),
            0.8
        ),
        new QualityTestCase(
            "What trade agreements does the US have with Mexico?",
            Arrays.asList("USMCA", "NAFTA", "Mexico", "trade agreement"),
            Arrays.asList("wrong", "incorrect", "don't know"),
            0.8
        ),
        new QualityTestCase(
            "Compare importing steel from China vs India",
            Arrays.asList("China", "India", "steel", "compare", "tariff", "anti-dumping"),
            Arrays.asList("wrong", "incorrect", "don't know", "same"),
            0.7
        ),
        new QualityTestCase(
            "What documentation is required for importing pharmaceuticals?",
            Arrays.asList("FDA", "documentation", "pharmaceutical", "certificate", "compliance"),
            Arrays.asList("wrong", "incorrect", "don't know"),
            0.8
        )
    );

    public static void main(String[] args) {
        SpringApplication.run(ResponseAccuracyValidator.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting AI Response Accuracy Validation...");
        
        QualityReport report = new QualityReport();
        List<TestResult> results = new ArrayList<>();
        
        for (QualityTestCase testCase : TEST_CASES) {
            TestResult result = validateTestCase(testCase);
            results.add(result);
            
            System.out.printf("Test: %s - Score: %.2f - %s%n", 
                testCase.getQuery().substring(0, Math.min(50, testCase.getQuery().length())) + "...",
                result.getScore(),
                result.isPassed() ? "PASSED" : "FAILED"
            );
        }
        
        // Calculate overall metrics
        double averageScore = results.stream()
            .mapToDouble(TestResult::getScore)
            .average()
            .orElse(0.0);
        
        long passedTests = results.stream()
            .mapToLong(r -> r.isPassed() ? 1 : 0)
            .sum();
        
        double passRate = (double) passedTests / results.size();
        
        report.setAccuracyScore(averageScore * 100);
        report.setPassRate(passRate * 100);
        report.setTotalTests(results.size());
        report.setPassedTests((int) passedTests);
        report.setFailedTests(results.size() - (int) passedTests);
        report.setTestResults(results);
        
        // Quality gate validation
        boolean qualityGatesPassed = averageScore >= 0.75 && passRate >= 0.8;
        report.setQualityGatesPassed(qualityGatesPassed);
        
        if (!qualityGatesPassed) {
            report.getRecommendations().add("Improve AI response accuracy - current score below threshold");
        }
        if (passRate < 0.9) {
            report.getRecommendations().add("Investigate failed test cases and improve response quality");
        }
        
        // Save report
        saveReport(report);
        
        System.out.printf("%nAccuracy Validation Results:%n");
        System.out.printf("Average Accuracy Score: %.2f%%%n", report.getAccuracyScore());
        System.out.printf("Pass Rate: %.2f%% (%d/%d)%n", report.getPassRate(), report.getPassedTests(), report.getTotalTests());
        System.out.printf("Quality Gates: %s%n", qualityGatesPassed ? "PASSED" : "FAILED");
        
        if (!qualityGatesPassed) {
            System.err.println("Quality gates failed! Please review and improve AI responses.");
            System.exit(1);
        }
    }
    
    private TestResult validateTestCase(QualityTestCase testCase) {
        try {
            ChatQueryRequest request = new ChatQueryRequest();
            request.setQuery(testCase.getQuery());
            request.setUserId("quality-validator");
            request.setConversationId("quality-test-" + UUID.randomUUID().toString());
            
            long startTime = System.currentTimeMillis();
            ChatQueryResponse response = chatbotService.processQuery(request);
            long responseTime = System.currentTimeMillis() - startTime;
            
            double score = calculateScore(response.getResponse(), testCase);
            boolean passed = score >= testCase.getMinScore();
            
            return new TestResult(
                testCase.getQuery(),
                response.getResponse(),
                score,
                passed,
                responseTime,
                extractIssues(response.getResponse(), testCase)
            );
            
        } catch (Exception e) {
            return new TestResult(
                testCase.getQuery(),
                "ERROR: " + e.getMessage(),
                0.0,
                false,
                0,
                Arrays.asList("Exception occurred: " + e.getMessage())
            );
        }
    }
    
    private double calculateScore(String response, QualityTestCase testCase) {
        if (response == null || response.trim().isEmpty()) {
            return 0.0;
        }
        
        String lowerResponse = response.toLowerCase();
        
        // Check for negative indicators
        for (String negative : testCase.getNegativeIndicators()) {
            if (lowerResponse.contains(negative.toLowerCase())) {
                return 0.0; // Immediate fail for negative indicators
            }
        }
        
        // Calculate positive score
        int positiveMatches = 0;
        for (String positive : testCase.getPositiveIndicators()) {
            if (lowerResponse.contains(positive.toLowerCase())) {
                positiveMatches++;
            }
        }
        
        double positiveScore = (double) positiveMatches / testCase.getPositiveIndicators().size();
        
        // Bonus for comprehensive responses
        double lengthBonus = Math.min(0.1, response.length() / 1000.0);
        
        return Math.min(1.0, positiveScore + lengthBonus);
    }
    
    private List<String> extractIssues(String response, QualityTestCase testCase) {
        List<String> issues = new ArrayList<>();
        String lowerResponse = response.toLowerCase();
        
        // Check for negative indicators
        for (String negative : testCase.getNegativeIndicators()) {
            if (lowerResponse.contains(negative.toLowerCase())) {
                issues.add("Contains negative indicator: " + negative);
            }
        }
        
        // Check for missing positive indicators
        for (String positive : testCase.getPositiveIndicators()) {
            if (!lowerResponse.contains(positive.toLowerCase())) {
                issues.add("Missing expected content: " + positive);
            }
        }
        
        if (response.length() < 50) {
            issues.add("Response too short - may lack detail");
        }
        
        return issues;
    }
    
    private void saveReport(QualityReport report) throws IOException {
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        File reportFile = new File(targetDir, "accuracy-report.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);
        
        System.out.println("Accuracy report saved to: " + reportFile.getAbsolutePath());
    }
    
    // Inner classes for data structures
    public static class QualityTestCase {
        private String query;
        private List<String> positiveIndicators;
        private List<String> negativeIndicators;
        private double minScore;
        
        public QualityTestCase(String query, List<String> positiveIndicators, 
                              List<String> negativeIndicators, double minScore) {
            this.query = query;
            this.positiveIndicators = positiveIndicators;
            this.negativeIndicators = negativeIndicators;
            this.minScore = minScore;
        }
        
        // Getters
        public String getQuery() { return query; }
        public List<String> getPositiveIndicators() { return positiveIndicators; }
        public List<String> getNegativeIndicators() { return negativeIndicators; }
        public double getMinScore() { return minScore; }
    }
    
    public static class TestResult {
        private String query;
        private String response;
        private double score;
        private boolean passed;
        private long responseTime;
        private List<String> issues;
        
        public TestResult(String query, String response, double score, boolean passed, 
                         long responseTime, List<String> issues) {
            this.query = query;
            this.response = response;
            this.score = score;
            this.passed = passed;
            this.responseTime = responseTime;
            this.issues = issues;
        }
        
        // Getters
        public String getQuery() { return query; }
        public String getResponse() { return response; }
        public double getScore() { return score; }
        public boolean isPassed() { return passed; }
        public long getResponseTime() { return responseTime; }
        public List<String> getIssues() { return issues; }
    }
    
    public static class QualityReport {
        private double accuracyScore;
        private double passRate;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private boolean qualityGatesPassed;
        private List<String> recommendations = new ArrayList<>();
        private List<TestResult> testResults;
        
        // Getters and setters
        public double getAccuracyScore() { return accuracyScore; }
        public void setAccuracyScore(double accuracyScore) { this.accuracyScore = accuracyScore; }
        
        public double getPassRate() { return passRate; }
        public void setPassRate(double passRate) { this.passRate = passRate; }
        
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        
        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
        
        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        
        public boolean isQualityGatesPassed() { return qualityGatesPassed; }
        public void setQualityGatesPassed(boolean qualityGatesPassed) { this.qualityGatesPassed = qualityGatesPassed; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public List<TestResult> getTestResults() { return testResults; }
        public void setTestResults(List<TestResult> testResults) { this.testResults = testResults; }
    }
}