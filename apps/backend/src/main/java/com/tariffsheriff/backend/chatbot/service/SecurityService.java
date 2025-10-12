package com.tariffsheriff.backend.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Enhanced SecurityService with advanced input validation and prompt injection prevention
 */
@Service
@Slf4j
public class SecurityService {

    // Prompt injection patterns
    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)ignore\\s+(previous|all)\\s+(instructions?|prompts?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)forget\\s+(everything|all)\\s+(above|before)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)system\\s*:\\s*you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)\\[\\s*system\\s*\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)act\\s+as\\s+(if\\s+you\\s+are|a)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)pretend\\s+(to\\s+be|you\\s+are)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)roleplay\\s+as", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)\\{\\{.*\\}\\}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)\\$\\{.*\\}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<\\s*script\\s*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)javascript\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)data\\s*:\\s*text/html", Pattern.CASE_INSENSITIVE)
    );

    // Malicious content patterns
    private static final List<Pattern> MALICIOUS_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)\\b(sql|union|select|insert|update|delete|drop|create|alter)\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<\\s*iframe\\s*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<\\s*object\\s*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<\\s*embed\\s*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)function\\s*\\(", Pattern.CASE_INSENSITIVE)
    );

    // Behavioral analysis tracking
    private final Map<String, UserBehaviorProfile> userBehaviorProfiles = new ConcurrentHashMap<>();
    
    // Input complexity tracking
    private final Map<String, List<InputComplexityMetric>> complexityHistory = new ConcurrentHashMap<>();

    /**
     * Validates input query for security threats
     */
    public ValidationResult validateInput(String userId, String input) {
        log.debug("Validating input for user: {}", userId);
        
        ValidationResult result = new ValidationResult();
        result.setUserId(userId);
        result.setInput(input);
        result.setTimestamp(LocalDateTime.now());
        
        // Check for prompt injection
        PromptInjectionResult injectionResult = detectPromptInjection(input);
        result.setPromptInjectionDetected(injectionResult.isDetected());
        result.setInjectionPatterns(injectionResult.getMatchedPatterns());
        
        // Check for malicious content
        MaliciousContentResult maliciousResult = detectMaliciousContent(input);
        result.setMaliciousContentDetected(maliciousResult.isDetected());
        result.setMaliciousPatterns(maliciousResult.getMatchedPatterns());
        
        // Analyze input complexity
        InputComplexityResult complexityResult = analyzeInputComplexity(userId, input);
        result.setComplexityScore(complexityResult.getScore());
        result.setResourceExhaustionRisk(complexityResult.isResourceExhaustionRisk());
        
        // Analyze user behavior
        BehaviorAnalysisResult behaviorResult = analyzeBehavior(userId, input);
        result.setBehaviorScore(behaviorResult.getScore());
        result.setSuspiciousBehavior(behaviorResult.isSuspicious());
        
        // Determine overall security status
        result.setSecure(determineSecurityStatus(result));
        
        // Update user behavior profile
        updateUserBehaviorProfile(userId, result);
        
        log.info("Input validation completed for user {}: secure={}, promptInjection={}, malicious={}, complexity={}", 
                userId, result.isSecure(), result.isPromptInjectionDetected(), 
                result.isMaliciousContentDetected(), result.getComplexityScore());
        
        return result;
    }

    /**
     * Sanitizes input by removing or escaping potentially dangerous content
     */
    public String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String sanitized = input;
        
        // Remove script tags and javascript
        sanitized = sanitized.replaceAll("(?i)<\\s*script[^>]*>.*?</\\s*script\\s*>", "");
        sanitized = sanitized.replaceAll("(?i)javascript\\s*:", "");
        
        // Remove HTML tags that could be dangerous
        sanitized = sanitized.replaceAll("(?i)<\\s*(iframe|object|embed|form)[^>]*>.*?</\\s*\\1\\s*>", "");
        
        // Escape special characters
        sanitized = sanitized.replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&#x27;");
        
        // Remove excessive whitespace and control characters
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        return sanitized;
    }

    /**
     * Detects prompt injection attempts
     */
    private PromptInjectionResult detectPromptInjection(String input) {
        PromptInjectionResult result = new PromptInjectionResult();
        List<String> matchedPatterns = new ArrayList<>();
        
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                matchedPatterns.add(pattern.pattern());
            }
        }
        
        result.setDetected(!matchedPatterns.isEmpty());
        result.setMatchedPatterns(matchedPatterns);
        
        return result;
    }

    /**
     * Detects malicious content
     */
    private MaliciousContentResult detectMaliciousContent(String input) {
        MaliciousContentResult result = new MaliciousContentResult();
        List<String> matchedPatterns = new ArrayList<>();
        
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                matchedPatterns.add(pattern.pattern());
            }
        }
        
        result.setDetected(!matchedPatterns.isEmpty());
        result.setMatchedPatterns(matchedPatterns);
        
        return result;
    }

    /**
     * Analyzes input complexity to prevent resource exhaustion
     */
    private InputComplexityResult analyzeInputComplexity(String userId, String input) {
        InputComplexityResult result = new InputComplexityResult();
        
        // Calculate complexity score based on various factors
        int complexityScore = 0;
        
        // Length factor
        complexityScore += Math.min(input.length() / 100, 10);
        
        // Word count factor
        String[] words = input.split("\\s+");
        complexityScore += Math.min(words.length / 50, 10);
        
        // Special character density
        long specialChars = input.chars().filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)).count();
        complexityScore += Math.min((int)(specialChars * 10 / input.length()), 10);
        
        // Nested structure detection (brackets, parentheses)
        int nestingLevel = calculateNestingLevel(input);
        complexityScore += Math.min(nestingLevel * 2, 10);
        
        // Repetitive pattern detection
        if (hasRepetitivePatterns(input)) {
            complexityScore += 5;
        }
        
        result.setScore(complexityScore);
        result.setResourceExhaustionRisk(complexityScore > 25);
        
        // Track complexity history
        trackComplexityHistory(userId, new InputComplexityMetric(LocalDateTime.now(), complexityScore));
        
        return result;
    }

    /**
     * Analyzes user behavior patterns
     */
    private BehaviorAnalysisResult analyzeBehavior(String userId, String input) {
        BehaviorAnalysisResult result = new BehaviorAnalysisResult();
        
        UserBehaviorProfile profile = userBehaviorProfiles.getOrDefault(userId, new UserBehaviorProfile(userId));
        
        // Analyze request frequency
        profile.addRequest(LocalDateTime.now());
        int recentRequestCount = profile.getRecentRequestCount(Duration.ofMinutes(5));
        
        // Analyze input patterns
        boolean hasUnusualPatterns = detectUnusualInputPatterns(input, profile);
        
        // Calculate behavior score
        int behaviorScore = 0;
        
        if (recentRequestCount > 20) behaviorScore += 10; // High frequency
        if (recentRequestCount > 50) behaviorScore += 20; // Very high frequency
        if (hasUnusualPatterns) behaviorScore += 15;
        if (input.length() > 5000) behaviorScore += 10; // Very long input
        
        result.setScore(behaviorScore);
        result.setSuspicious(behaviorScore > 25);
        
        return result;
    }

    /**
     * Determines overall security status
     */
    private boolean determineSecurityStatus(ValidationResult result) {
        if (result.isPromptInjectionDetected() || result.isMaliciousContentDetected()) {
            return false;
        }
        
        if (result.isResourceExhaustionRisk() || result.isSuspiciousBehavior()) {
            return false;
        }
        
        if (result.getComplexityScore() > 30 || result.getBehaviorScore() > 30) {
            return false;
        }
        
        return true;
    }

    /**
     * Updates user behavior profile
     */
    private void updateUserBehaviorProfile(String userId, ValidationResult result) {
        UserBehaviorProfile profile = userBehaviorProfiles.computeIfAbsent(userId, UserBehaviorProfile::new);
        profile.addValidationResult(result);
    }

    /**
     * Calculates nesting level of brackets and parentheses
     */
    private int calculateNestingLevel(String input) {
        int maxLevel = 0;
        int currentLevel = 0;
        
        for (char ch : input.toCharArray()) {
            if (ch == '(' || ch == '[' || ch == '{') {
                currentLevel++;
                maxLevel = Math.max(maxLevel, currentLevel);
            } else if (ch == ')' || ch == ']' || ch == '}') {
                currentLevel = Math.max(0, currentLevel - 1);
            }
        }
        
        return maxLevel;
    }

    /**
     * Detects repetitive patterns that might indicate automated attacks
     */
    private boolean hasRepetitivePatterns(String input) {
        String[] words = input.split("\\s+");
        if (words.length < 10) return false;
        
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word.toLowerCase(), wordCount.getOrDefault(word.toLowerCase(), 0) + 1);
        }
        
        // Check if any word appears more than 30% of the time
        int totalWords = words.length;
        return wordCount.values().stream().anyMatch(count -> count > totalWords * 0.3);
    }

    /**
     * Detects unusual input patterns based on user history
     */
    private boolean detectUnusualInputPatterns(String input, UserBehaviorProfile profile) {
        // Check for sudden change in input style
        double avgLength = profile.getAverageInputLength();
        if (avgLength > 0 && (input.length() > avgLength * 3 || input.length() < avgLength * 0.3)) {
            return true;
        }
        
        // Check for unusual character patterns
        long uppercaseRatio = input.chars().filter(Character::isUpperCase).count() * 100 / input.length();
        if (uppercaseRatio > 80) return true;
        
        return false;
    }

    /**
     * Tracks complexity history for analysis
     */
    private void trackComplexityHistory(String userId, InputComplexityMetric metric) {
        complexityHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(metric);
        
        // Keep only recent history (last 100 entries)
        List<InputComplexityMetric> history = complexityHistory.get(userId);
        if (history.size() > 100) {
            history.subList(0, history.size() - 100).clear();
        }
    }

    // Inner classes for results and tracking
    
    public static class ValidationResult {
        private String userId;
        private String input;
        private LocalDateTime timestamp;
        private boolean secure;
        private boolean promptInjectionDetected;
        private List<String> injectionPatterns = new ArrayList<>();
        private boolean maliciousContentDetected;
        private List<String> maliciousPatterns = new ArrayList<>();
        private int complexityScore;
        private boolean resourceExhaustionRisk;
        private int behaviorScore;
        private boolean suspiciousBehavior;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public boolean isPromptInjectionDetected() { return promptInjectionDetected; }
        public void setPromptInjectionDetected(boolean promptInjectionDetected) { this.promptInjectionDetected = promptInjectionDetected; }
        public List<String> getInjectionPatterns() { return injectionPatterns; }
        public void setInjectionPatterns(List<String> injectionPatterns) { this.injectionPatterns = injectionPatterns; }
        public boolean isMaliciousContentDetected() { return maliciousContentDetected; }
        public void setMaliciousContentDetected(boolean maliciousContentDetected) { this.maliciousContentDetected = maliciousContentDetected; }
        public List<String> getMaliciousPatterns() { return maliciousPatterns; }
        public void setMaliciousPatterns(List<String> maliciousPatterns) { this.maliciousPatterns = maliciousPatterns; }
        public int getComplexityScore() { return complexityScore; }
        public void setComplexityScore(int complexityScore) { this.complexityScore = complexityScore; }
        public boolean isResourceExhaustionRisk() { return resourceExhaustionRisk; }
        public void setResourceExhaustionRisk(boolean resourceExhaustionRisk) { this.resourceExhaustionRisk = resourceExhaustionRisk; }
        public int getBehaviorScore() { return behaviorScore; }
        public void setBehaviorScore(int behaviorScore) { this.behaviorScore = behaviorScore; }
        public boolean isSuspiciousBehavior() { return suspiciousBehavior; }
        public void setSuspiciousBehavior(boolean suspiciousBehavior) { this.suspiciousBehavior = suspiciousBehavior; }
    }

    private static class PromptInjectionResult {
        private boolean detected;
        private List<String> matchedPatterns = new ArrayList<>();

        public boolean isDetected() { return detected; }
        public void setDetected(boolean detected) { this.detected = detected; }
        public List<String> getMatchedPatterns() { return matchedPatterns; }
        public void setMatchedPatterns(List<String> matchedPatterns) { this.matchedPatterns = matchedPatterns; }
    }

    private static class MaliciousContentResult {
        private boolean detected;
        private List<String> matchedPatterns = new ArrayList<>();

        public boolean isDetected() { return detected; }
        public void setDetected(boolean detected) { this.detected = detected; }
        public List<String> getMatchedPatterns() { return matchedPatterns; }
        public void setMatchedPatterns(List<String> matchedPatterns) { this.matchedPatterns = matchedPatterns; }
    }

    private static class InputComplexityResult {
        private int score;
        private boolean resourceExhaustionRisk;

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public boolean isResourceExhaustionRisk() { return resourceExhaustionRisk; }
        public void setResourceExhaustionRisk(boolean resourceExhaustionRisk) { this.resourceExhaustionRisk = resourceExhaustionRisk; }
    }

    private static class BehaviorAnalysisResult {
        private int score;
        private boolean suspicious;

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public boolean isSuspicious() { return suspicious; }
        public void setSuspicious(boolean suspicious) { this.suspicious = suspicious; }
    }

    private static class UserBehaviorProfile {
        private final String userId;
        private final List<LocalDateTime> requestTimestamps = new ArrayList<>();
        private final List<ValidationResult> validationHistory = new ArrayList<>();

        public UserBehaviorProfile(String userId) {
            this.userId = userId;
        }

        public void addRequest(LocalDateTime timestamp) {
            requestTimestamps.add(timestamp);
            // Keep only recent requests (last 1000)
            if (requestTimestamps.size() > 1000) {
                requestTimestamps.subList(0, requestTimestamps.size() - 1000).clear();
            }
        }

        public int getRecentRequestCount(Duration duration) {
            LocalDateTime cutoff = LocalDateTime.now().minus(duration);
            return (int) requestTimestamps.stream()
                    .filter(timestamp -> timestamp.isAfter(cutoff))
                    .count();
        }

        public void addValidationResult(ValidationResult result) {
            validationHistory.add(result);
            // Keep only recent history (last 100)
            if (validationHistory.size() > 100) {
                validationHistory.subList(0, validationHistory.size() - 100).clear();
            }
        }

        public double getAverageInputLength() {
            if (validationHistory.isEmpty()) return 0;
            return validationHistory.stream()
                    .mapToInt(result -> result.getInput().length())
                    .average()
                    .orElse(0);
        }
    }

    private static class InputComplexityMetric {
        private final LocalDateTime timestamp;
        private final int score;

        public InputComplexityMetric(LocalDateTime timestamp, int score) {
            this.timestamp = timestamp;
            this.score = score;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public int getScore() { return score; }
    }
}