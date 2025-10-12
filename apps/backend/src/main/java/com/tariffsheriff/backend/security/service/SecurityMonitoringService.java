package com.tariffsheriff.backend.security.service;

import com.tariffsheriff.backend.audit.service.AuditService;
import com.tariffsheriff.backend.security.model.SecurityThreat;
import com.tariffsheriff.backend.security.repository.SecurityThreatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security monitoring service for threat detection and anomaly monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitoringService {

    private final SecurityThreatRepository threatRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // Real-time monitoring data
    private final Map<String, UserSecurityProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<String, IpSecurityProfile> ipProfiles = new ConcurrentHashMap<>();
    
    // Anomaly detection thresholds
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final double ANOMALY_THRESHOLD = 0.8;
    private static final int SUSPICIOUS_BEHAVIOR_SCORE_THRESHOLD = 75;

    /**
     * Monitors user activity for anomalous patterns
     */
    @Async
    public CompletableFuture<Void> monitorUserActivity(String userId, String clientIp, 
                                                      String userAgent, String activity) {
        try {
            // Update user security profile
            UserSecurityProfile userProfile = userProfiles.computeIfAbsent(userId, UserSecurityProfile::new);
            userProfile.recordActivity(activity, LocalDateTime.now());
            
            // Update IP security profile
            IpSecurityProfile ipProfile = ipProfiles.computeIfAbsent(clientIp, IpSecurityProfile::new);
            ipProfile.recordActivity(userId, userAgent, LocalDateTime.now());
            
            // Detect anomalies
            detectUserAnomalies(userId, userProfile, clientIp, userAgent);
            detectIpAnomalies(clientIp, ipProfile, userId, userAgent);
            
        } catch (Exception e) {
            log.error("Error monitoring user activity for user: {}", userId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Detects and reports security threats
     */
    @Async
    public CompletableFuture<SecurityThreat> detectThreat(ThreatDetectionData data) {
        try {
            SecurityThreat threat = new SecurityThreat();
            threat.setThreatId(UUID.randomUUID().toString());
            threat.setThreatType(data.getThreatType());
            threat.setSeverity(data.getSeverity());
            threat.setUserId(data.getUserId());
            threat.setClientIp(data.getClientIp());
            threat.setUserAgent(data.getUserAgent());
            threat.setDescription(data.getDescription());
            threat.setDetectionRule(data.getDetectionRule());
            threat.setConfidenceScore(data.getConfidenceScore());
            
            // Store evidence as JSON
            if (data.getEvidence() != null && !data.getEvidence().isEmpty()) {
                threat.setEvidence(objectMapper.writeValueAsString(data.getEvidence()));
            }
            
            threat = threatRepository.save(threat);
            
            // Log security audit event
            AuditService.SecurityAuditData auditData = new AuditService.SecurityAuditData();
            auditData.setUserId(data.getUserId());
            auditData.setClientIp(data.getClientIp());
            auditData.setUserAgent(data.getUserAgent());
            auditData.setAction("THREAT_DETECTED");
            auditData.setDescription(data.getDescription());
            auditData.setRiskLevel(mapSeverityToRiskLevel(data.getSeverity()));
            auditData.setThreatType(data.getThreatType().toString());
            auditData.setSecurityRule(data.getDetectionRule());
            
            auditService.logSecurityEvent(auditData);
            
            // Trigger automated response if needed
            if (data.getSeverity() == SecurityThreat.ThreatSeverity.HIGH || 
                data.getSeverity() == SecurityThreat.ThreatSeverity.CRITICAL) {
                triggerAutomatedResponse(threat);
            }
            
            log.warn("Security threat detected: {} - {} (Confidence: {})", 
                    threat.getThreatId(), data.getDescription(), data.getConfidenceScore());
            
            return CompletableFuture.completedFuture(threat);
            
        } catch (Exception e) {
            log.error("Error detecting security threat", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Analyzes AI usage patterns for security threats
     */
    public void analyzeAiUsagePattern(String userId, String query, String response, 
                                    List<String> toolsUsed, Long processingTime) {
        try {
            // Check for prompt injection attempts
            if (containsPromptInjection(query)) {
                ThreatDetectionData threatData = new ThreatDetectionData();
                threatData.setThreatType(SecurityThreat.ThreatType.PROMPT_INJECTION);
                threatData.setSeverity(SecurityThreat.ThreatSeverity.HIGH);
                threatData.setUserId(userId);
                threatData.setDescription("Potential prompt injection detected in AI query");
                threatData.setDetectionRule("PROMPT_INJECTION_PATTERN_MATCH");
                threatData.setConfidenceScore(0.9);
                
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("query", query.substring(0, Math.min(query.length(), 500))); // Truncate for storage
                evidence.put("detectionTime", LocalDateTime.now());
                threatData.setEvidence(evidence);
                
                detectThreat(threatData);
            }
            
            // Check for data exfiltration patterns
            if (isDataExfiltrationAttempt(query, response, toolsUsed)) {
                ThreatDetectionData threatData = new ThreatDetectionData();
                threatData.setThreatType(SecurityThreat.ThreatType.DATA_EXFILTRATION);
                threatData.setSeverity(SecurityThreat.ThreatSeverity.HIGH);
                threatData.setUserId(userId);
                threatData.setDescription("Potential data exfiltration attempt detected");
                threatData.setDetectionRule("DATA_EXFILTRATION_PATTERN");
                threatData.setConfidenceScore(0.8);
                
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("toolsUsed", toolsUsed);
                evidence.put("responseLength", response.length());
                evidence.put("processingTime", processingTime);
                threatData.setEvidence(evidence);
                
                detectThreat(threatData);
            }
            
            // Check for resource exhaustion attempts
            if (processingTime != null && processingTime > 60000) { // More than 1 minute
                ThreatDetectionData threatData = new ThreatDetectionData();
                threatData.setThreatType(SecurityThreat.ThreatType.SUSPICIOUS_BEHAVIOR);
                threatData.setSeverity(SecurityThreat.ThreatSeverity.MEDIUM);
                threatData.setUserId(userId);
                threatData.setDescription("Unusually long processing time detected");
                threatData.setDetectionRule("RESOURCE_EXHAUSTION_TIME");
                threatData.setConfidenceScore(0.7);
                
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("processingTime", processingTime);
                evidence.put("queryLength", query.length());
                threatData.setEvidence(evidence);
                
                detectThreat(threatData);
            }
            
        } catch (Exception e) {
            log.error("Error analyzing AI usage pattern for user: {}", userId, e);
        }
    }

    /**
     * Gets security dashboard data
     */
    public SecurityDashboard getSecurityDashboard() {
        SecurityDashboard dashboard = new SecurityDashboard();
        
        // Active threats by severity
        List<Object[]> threatCounts = threatRepository.countActiveThreatsBySeverity();
        Map<String, Long> threatsBySeverity = new HashMap<>();
        for (Object[] count : threatCounts) {
            threatsBySeverity.put(count[0].toString(), (Long) count[1]);
        }
        dashboard.setActiveThreatsBySeverity(threatsBySeverity);
        
        // Recent threats
        List<SecurityThreat> recentThreats = threatRepository.findTop50ByOrderByDetectedAtDesc();
        dashboard.setRecentThreats(recentThreats);
        
        // Unresolved high-severity threats
        List<SecurityThreat> criticalThreats = threatRepository.findByStatusInAndSeverityInOrderByDetectedAtAsc(
                Arrays.asList(SecurityThreat.ThreatStatus.ACTIVE, SecurityThreat.ThreatStatus.INVESTIGATING),
                Arrays.asList(SecurityThreat.ThreatSeverity.HIGH, SecurityThreat.ThreatSeverity.CRITICAL)
        );
        dashboard.setCriticalThreats(criticalThreats);
        
        // System health metrics
        dashboard.setActiveUserSessions(userProfiles.size());
        dashboard.setActiveIpAddresses(ipProfiles.size());
        dashboard.setLastUpdated(LocalDateTime.now());
        
        return dashboard;
    }

    /**
     * Investigates a specific threat
     */
    public ThreatInvestigationResult investigateThreat(String threatId) {
        Optional<SecurityThreat> threatOpt = threatRepository.findById(Long.parseLong(threatId));
        if (threatOpt.isEmpty()) {
            return null;
        }
        
        SecurityThreat threat = threatOpt.get();
        ThreatInvestigationResult result = new ThreatInvestigationResult();
        result.setThreat(threat);
        
        // Get related threats from same user/IP
        if (threat.getUserId() != null) {
            List<SecurityThreat> userThreats = threatRepository.findByUserIdAndDetectedAtBetween(
                    threat.getUserId(), 
                    threat.getDetectedAt().minusDays(7), 
                    threat.getDetectedAt().plusDays(1)
            );
            result.setRelatedUserThreats(userThreats);
        }
        
        if (threat.getClientIp() != null) {
            List<SecurityThreat> ipThreats = threatRepository.findByClientIpAndDetectedAtBetween(
                    threat.getClientIp(),
                    threat.getDetectedAt().minusDays(7),
                    threat.getDetectedAt().plusDays(1)
            );
            result.setRelatedIpThreats(ipThreats);
        }
        
        // Add investigation recommendations
        result.setRecommendations(generateInvestigationRecommendations(threat));
        
        return result;
    }

    /**
     * Detects user anomalies
     */
    private void detectUserAnomalies(String userId, UserSecurityProfile profile, 
                                   String clientIp, String userAgent) {
        // Check request rate
        int recentRequests = profile.getRequestCount(LocalDateTime.now().minusMinutes(1));
        if (recentRequests > MAX_REQUESTS_PER_MINUTE) {
            ThreatDetectionData threatData = new ThreatDetectionData();
            threatData.setThreatType(SecurityThreat.ThreatType.RATE_LIMIT_VIOLATION);
            threatData.setSeverity(SecurityThreat.ThreatSeverity.MEDIUM);
            threatData.setUserId(userId);
            threatData.setClientIp(clientIp);
            threatData.setUserAgent(userAgent);
            threatData.setDescription("Rate limit violation: " + recentRequests + " requests in 1 minute");
            threatData.setDetectionRule("RATE_LIMIT_EXCEEDED");
            threatData.setConfidenceScore(1.0);
            
            detectThreat(threatData);
        }
        
        // Check for unusual activity patterns
        double anomalyScore = profile.calculateAnomalyScore();
        if (anomalyScore > ANOMALY_THRESHOLD) {
            ThreatDetectionData threatData = new ThreatDetectionData();
            threatData.setThreatType(SecurityThreat.ThreatType.ANOMALOUS_USAGE);
            threatData.setSeverity(SecurityThreat.ThreatSeverity.MEDIUM);
            threatData.setUserId(userId);
            threatData.setClientIp(clientIp);
            threatData.setUserAgent(userAgent);
            threatData.setDescription("Anomalous user behavior detected (score: " + anomalyScore + ")");
            threatData.setDetectionRule("BEHAVIORAL_ANOMALY");
            threatData.setConfidenceScore(anomalyScore);
            
            detectThreat(threatData);
        }
    }

    /**
     * Detects IP-based anomalies
     */
    private void detectIpAnomalies(String clientIp, IpSecurityProfile profile, 
                                 String userId, String userAgent) {
        // Check for multiple user accounts from same IP
        if (profile.getUniqueUserCount() > 10) {
            ThreatDetectionData threatData = new ThreatDetectionData();
            threatData.setThreatType(SecurityThreat.ThreatType.SUSPICIOUS_BEHAVIOR);
            threatData.setSeverity(SecurityThreat.ThreatSeverity.MEDIUM);
            threatData.setUserId(userId);
            threatData.setClientIp(clientIp);
            threatData.setUserAgent(userAgent);
            threatData.setDescription("Multiple user accounts from single IP: " + profile.getUniqueUserCount());
            threatData.setDetectionRule("MULTIPLE_USERS_SINGLE_IP");
            threatData.setConfidenceScore(0.8);
            
            detectThreat(threatData);
        }
        
        // Check for bot-like behavior
        if (profile.isBotLikeBehavior()) {
            ThreatDetectionData threatData = new ThreatDetectionData();
            threatData.setThreatType(SecurityThreat.ThreatType.BOT_ACTIVITY);
            threatData.setSeverity(SecurityThreat.ThreatSeverity.LOW);
            threatData.setUserId(userId);
            threatData.setClientIp(clientIp);
            threatData.setUserAgent(userAgent);
            threatData.setDescription("Bot-like activity detected from IP");
            threatData.setDetectionRule("BOT_BEHAVIOR_PATTERN");
            threatData.setConfidenceScore(0.7);
            
            detectThreat(threatData);
        }
    }

    /**
     * Checks for prompt injection patterns
     */
    private boolean containsPromptInjection(String query) {
        if (query == null) return false;
        
        String lowerQuery = query.toLowerCase();
        String[] injectionPatterns = {
            "ignore previous instructions",
            "forget everything above",
            "system: you are now",
            "act as if you are",
            "pretend to be",
            "roleplay as",
            "{{",
            "${",
            "<script>",
            "javascript:"
        };
        
        for (String pattern : injectionPatterns) {
            if (lowerQuery.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks for data exfiltration patterns
     */
    private boolean isDataExfiltrationAttempt(String query, String response, List<String> toolsUsed) {
        // Check for excessive data requests
        if (response.length() > 50000) { // Very long response
            return true;
        }
        
        // Check for multiple data source access
        if (toolsUsed.size() > 5) {
            return true;
        }
        
        // Check for bulk data request patterns
        String lowerQuery = query.toLowerCase();
        String[] exfiltrationPatterns = {
            "export all",
            "download all",
            "get all data",
            "list all",
            "dump database"
        };
        
        for (String pattern : exfiltrationPatterns) {
            if (lowerQuery.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Triggers automated response to threats
     */
    private void triggerAutomatedResponse(SecurityThreat threat) {
        List<String> actions = new ArrayList<>();
        
        switch (threat.getThreatType()) {
            case BRUTE_FORCE_ATTACK:
            case RATE_LIMIT_VIOLATION:
                // Temporarily block IP
                actions.add("IP_TEMPORARY_BLOCK");
                break;
                
            case PROMPT_INJECTION:
            case MALICIOUS_INPUT:
                // Increase monitoring for user
                actions.add("INCREASE_USER_MONITORING");
                break;
                
            case DATA_EXFILTRATION:
                // Block user and alert administrators
                actions.add("USER_TEMPORARY_BLOCK");
                actions.add("ADMIN_ALERT");
                break;
                
            default:
                actions.add("LOG_AND_MONITOR");
        }
        
        try {
            threat.setMitigationActions(objectMapper.writeValueAsString(actions));
            threatRepository.save(threat);
            
            log.info("Automated response triggered for threat {}: {}", threat.getThreatId(), actions);
        } catch (JsonProcessingException e) {
            log.error("Error storing mitigation actions for threat: {}", threat.getThreatId(), e);
        }
    }

    /**
     * Maps threat severity to audit risk level
     */
    private com.tariffsheriff.backend.audit.model.AuditEvent.RiskLevel mapSeverityToRiskLevel(
            SecurityThreat.ThreatSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return com.tariffsheriff.backend.audit.model.AuditEvent.RiskLevel.CRITICAL;
            case HIGH:
                return com.tariffsheriff.backend.audit.model.AuditEvent.RiskLevel.HIGH;
            case MEDIUM:
                return com.tariffsheriff.backend.audit.model.AuditEvent.RiskLevel.MEDIUM;
            case LOW:
            default:
                return com.tariffsheriff.backend.audit.model.AuditEvent.RiskLevel.LOW;
        }
    }

    /**
     * Generates investigation recommendations
     */
    private List<String> generateInvestigationRecommendations(SecurityThreat threat) {
        List<String> recommendations = new ArrayList<>();
        
        switch (threat.getThreatType()) {
            case PROMPT_INJECTION:
                recommendations.add("Review user's recent AI interactions for similar patterns");
                recommendations.add("Check if user has legitimate business need for complex queries");
                recommendations.add("Consider implementing additional input validation");
                break;
                
            case BRUTE_FORCE_ATTACK:
                recommendations.add("Check for other attacks from same IP range");
                recommendations.add("Review authentication logs for pattern analysis");
                recommendations.add("Consider implementing CAPTCHA or account lockout");
                break;
                
            case DATA_EXFILTRATION:
                recommendations.add("Audit all data accessed by user in past 30 days");
                recommendations.add("Check for unauthorized data downloads or exports");
                recommendations.add("Review user's access permissions and role");
                break;
                
            default:
                recommendations.add("Monitor user activity for additional suspicious behavior");
                recommendations.add("Review system logs for related events");
        }
        
        return recommendations;
    }

    /**
     * Scheduled cleanup of old security profiles
     */
    @Scheduled(cron = "0 0 3 * * ?") // Run daily at 3 AM
    public void cleanupSecurityProfiles() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        userProfiles.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity().isBefore(cutoff));
        
        ipProfiles.entrySet().removeIf(entry ->
            entry.getValue().getLastActivity().isBefore(cutoff));
        
        log.info("Cleaned up old security profiles. Active users: {}, Active IPs: {}", 
                userProfiles.size(), ipProfiles.size());
    }

    // Data classes and inner classes
    
    public static class ThreatDetectionData {
        private SecurityThreat.ThreatType threatType;
        private SecurityThreat.ThreatSeverity severity;
        private String userId;
        private String clientIp;
        private String userAgent;
        private String description;
        private String detectionRule;
        private Double confidenceScore;
        private Map<String, Object> evidence = new HashMap<>();

        // Getters and setters
        public SecurityThreat.ThreatType getThreatType() { return threatType; }
        public void setThreatType(SecurityThreat.ThreatType threatType) { this.threatType = threatType; }
        public SecurityThreat.ThreatSeverity getSeverity() { return severity; }
        public void setSeverity(SecurityThreat.ThreatSeverity severity) { this.severity = severity; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDetectionRule() { return detectionRule; }
        public void setDetectionRule(String detectionRule) { this.detectionRule = detectionRule; }
        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
        public Map<String, Object> getEvidence() { return evidence; }
        public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
    }

    public static class SecurityDashboard {
        private Map<String, Long> activeThreatsBySeverity = new HashMap<>();
        private List<SecurityThreat> recentThreats = new ArrayList<>();
        private List<SecurityThreat> criticalThreats = new ArrayList<>();
        private int activeUserSessions;
        private int activeIpAddresses;
        private LocalDateTime lastUpdated;

        // Getters and setters
        public Map<String, Long> getActiveThreatsBySeverity() { return activeThreatsBySeverity; }
        public void setActiveThreatsBySeverity(Map<String, Long> activeThreatsBySeverity) { this.activeThreatsBySeverity = activeThreatsBySeverity; }
        public List<SecurityThreat> getRecentThreats() { return recentThreats; }
        public void setRecentThreats(List<SecurityThreat> recentThreats) { this.recentThreats = recentThreats; }
        public List<SecurityThreat> getCriticalThreats() { return criticalThreats; }
        public void setCriticalThreats(List<SecurityThreat> criticalThreats) { this.criticalThreats = criticalThreats; }
        public int getActiveUserSessions() { return activeUserSessions; }
        public void setActiveUserSessions(int activeUserSessions) { this.activeUserSessions = activeUserSessions; }
        public int getActiveIpAddresses() { return activeIpAddresses; }
        public void setActiveIpAddresses(int activeIpAddresses) { this.activeIpAddresses = activeIpAddresses; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class ThreatInvestigationResult {
        private SecurityThreat threat;
        private List<SecurityThreat> relatedUserThreats = new ArrayList<>();
        private List<SecurityThreat> relatedIpThreats = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();

        // Getters and setters
        public SecurityThreat getThreat() { return threat; }
        public void setThreat(SecurityThreat threat) { this.threat = threat; }
        public List<SecurityThreat> getRelatedUserThreats() { return relatedUserThreats; }
        public void setRelatedUserThreats(List<SecurityThreat> relatedUserThreats) { this.relatedUserThreats = relatedUserThreats; }
        public List<SecurityThreat> getRelatedIpThreats() { return relatedIpThreats; }
        public void setRelatedIpThreats(List<SecurityThreat> relatedIpThreats) { this.relatedIpThreats = relatedIpThreats; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    // Security profile classes
    
    private static class UserSecurityProfile {
        private final String userId;
        private final List<ActivityRecord> activities = new ArrayList<>();
        private LocalDateTime lastActivity;

        public UserSecurityProfile(String userId) {
            this.userId = userId;
            this.lastActivity = LocalDateTime.now();
        }

        public void recordActivity(String activity, LocalDateTime timestamp) {
            activities.add(new ActivityRecord(activity, timestamp));
            lastActivity = timestamp;
            
            // Keep only recent activities (last 1000)
            if (activities.size() > 1000) {
                activities.subList(0, activities.size() - 1000).clear();
            }
        }

        public int getRequestCount(LocalDateTime since) {
            return (int) activities.stream()
                    .filter(activity -> activity.timestamp.isAfter(since))
                    .count();
        }

        public double calculateAnomalyScore() {
            // Simple anomaly detection based on activity patterns
            if (activities.size() < 10) return 0.0;
            
            // Check for unusual request patterns
            Map<String, Long> activityCounts = new HashMap<>();
            for (ActivityRecord activity : activities) {
                activityCounts.merge(activity.activity, 1L, Long::sum);
            }
            
            // Calculate entropy of activities
            double entropy = 0.0;
            int totalActivities = activities.size();
            for (Long count : activityCounts.values()) {
                double probability = (double) count / totalActivities;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
            
            // Low entropy indicates repetitive behavior (potentially automated)
            return entropy < 1.0 ? 1.0 - entropy : 0.0;
        }

        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    private static class IpSecurityProfile {
        private final String clientIp;
        private final Set<String> uniqueUsers = new HashSet<>();
        private final Set<String> userAgents = new HashSet<>();
        private final List<ActivityRecord> activities = new ArrayList<>();
        private LocalDateTime lastActivity;

        public IpSecurityProfile(String clientIp) {
            this.clientIp = clientIp;
            this.lastActivity = LocalDateTime.now();
        }

        public void recordActivity(String userId, String userAgent, LocalDateTime timestamp) {
            uniqueUsers.add(userId);
            if (userAgent != null) {
                userAgents.add(userAgent);
            }
            activities.add(new ActivityRecord("request", timestamp));
            lastActivity = timestamp;
            
            // Keep only recent activities
            if (activities.size() > 1000) {
                activities.subList(0, activities.size() - 1000).clear();
            }
        }

        public int getUniqueUserCount() {
            return uniqueUsers.size();
        }

        public boolean isBotLikeBehavior() {
            // Check for bot-like patterns
            if (userAgents.size() == 1) {
                String userAgent = userAgents.iterator().next();
                if (userAgent != null) {
                    String lowerAgent = userAgent.toLowerCase();
                    return lowerAgent.contains("bot") || lowerAgent.contains("crawler") || 
                           lowerAgent.contains("spider") || lowerAgent.length() < 20;
                }
            }
            
            // Check for very regular request patterns
            if (activities.size() > 50) {
                // Calculate time intervals between requests
                List<Long> intervals = new ArrayList<>();
                for (int i = 1; i < Math.min(activities.size(), 100); i++) {
                    long interval = java.time.Duration.between(
                            activities.get(i-1).timestamp, 
                            activities.get(i).timestamp
                    ).toMillis();
                    intervals.add(interval);
                }
                
                // Check if intervals are very regular (bot-like)
                if (!intervals.isEmpty()) {
                    double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
                    double variance = intervals.stream()
                            .mapToDouble(interval -> Math.pow(interval - avgInterval, 2))
                            .average().orElse(0);
                    
                    // Low variance indicates regular timing (bot-like)
                    return variance < avgInterval * 0.1;
                }
            }
            
            return false;
        }

        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    private static class ActivityRecord {
        private final String activity;
        private final LocalDateTime timestamp;

        public ActivityRecord(String activity, LocalDateTime timestamp) {
            this.activity = activity;
            this.timestamp = timestamp;
        }
    }
}