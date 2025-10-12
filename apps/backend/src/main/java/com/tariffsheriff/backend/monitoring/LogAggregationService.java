package com.tariffsheriff.backend.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for aggregating and analyzing logs from AI components
 */
@Service
public class LogAggregationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogAggregationService.class);
    
    @Value("${logging.aggregation.enabled:true}")
    private boolean aggregationEnabled;
    
    @Value("${logging.aggregation.max-entries:10000}")
    private int maxLogEntries;
    
    @Value("${logging.aggregation.retention-hours:24}")
    private int retentionHours;
    
    // Log storage
    private final Queue<LogEntry> logEntries = new ConcurrentLinkedQueue<>();
    private final Map<String, LogPattern> logPatterns = new ConcurrentHashMap<>();
    private final Map<String, LogStatistics> logStatistics = new ConcurrentHashMap<>();
    
    // Pattern matching for log analysis
    private final List<Pattern> errorPatterns = Arrays.asList(
        Pattern.compile(".*ERROR.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*EXCEPTION.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*FAILED.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*TIMEOUT.*", Pattern.CASE_INSENSITIVE)
    );
    
    private final List<Pattern> warningPatterns = Arrays.asList(
        Pattern.compile(".*WARN.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*WARNING.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*DEPRECATED.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*SLOW.*", Pattern.CASE_INSENSITIVE)
    );
    
    private final List<Pattern> aiSpecificPatterns = Arrays.asList(
        Pattern.compile(".*AI.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*ORCHESTRAT.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*AGENT.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*REASONING.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*QUERY.*", Pattern.CASE_INSENSITIVE)
    );
    
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
    
    public static class LogEntry {
        private String id;
        private Instant timestamp;
        private LogLevel level;
        private String logger;
        private String message;
        private String thread;
        private Map<String, String> metadata;
        private String stackTrace;
        
        public LogEntry(LogLevel level, String logger, String message) {
            this.id = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
            this.level = level;
            this.logger = logger;
            this.message = message;
            this.thread = Thread.currentThread().getName();
            this.metadata = new HashMap<>();
        }
        
        // Getters and setters
        public String getId() { return id; }
        public Instant getTimestamp() { return timestamp; }
        public LogLevel getLevel() { return level; }
        public String getLogger() { return logger; }
        public String getMessage() { return message; }
        public String getThread() { return thread; }
        public Map<String, String> getMetadata() { return metadata; }
        public void addMetadata(String key, String value) { this.metadata.put(key, value); }
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    }
    
    public static class LogPattern {
        private String pattern;
        private int frequency;
        private Instant firstSeen;
        private Instant lastSeen;
        private LogLevel mostCommonLevel;
        private Set<String> affectedLoggers;
        private Map<String, Integer> levelCounts;
        
        public LogPattern(String pattern) {
            this.pattern = pattern;
            this.frequency = 0;
            this.firstSeen = Instant.now();
            this.lastSeen = Instant.now();
            this.affectedLoggers = new HashSet<>();
            this.levelCounts = new HashMap<>();
        }
        
        public void incrementFrequency(LogLevel level, String logger) {
            this.frequency++;
            this.lastSeen = Instant.now();
            this.affectedLoggers.add(logger);
            this.levelCounts.merge(level.name(), 1, Integer::sum);
            
            // Update most common level
            this.mostCommonLevel = LogLevel.valueOf(
                levelCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(level.name())
            );
        }
        
        // Getters
        public String getPattern() { return pattern; }
        public int getFrequency() { return frequency; }
        public Instant getFirstSeen() { return firstSeen; }
        public Instant getLastSeen() { return lastSeen; }
        public LogLevel getMostCommonLevel() { return mostCommonLevel; }
        public Set<String> getAffectedLoggers() { return affectedLoggers; }
        public Map<String, Integer> getLevelCounts() { return levelCounts; }
    }
    
    public static class LogStatistics {
        private String component;
        private Map<LogLevel, Long> levelCounts;
        private long totalEntries;
        private Instant periodStart;
        private Instant periodEnd;
        private double errorRate;
        private List<String> topErrors;
        private Map<String, Long> hourlyDistribution;
        
        public LogStatistics(String component) {
            this.component = component;
            this.levelCounts = new EnumMap<>(LogLevel.class);
            this.totalEntries = 0;
            this.periodStart = Instant.now();
            this.periodEnd = Instant.now();
            this.topErrors = new ArrayList<>();
            this.hourlyDistribution = new HashMap<>();
            
            // Initialize level counts
            for (LogLevel level : LogLevel.values()) {
                levelCounts.put(level, 0L);
            }
        }
        
        public void addEntry(LogEntry entry) {
            this.totalEntries++;
            this.levelCounts.merge(entry.getLevel(), 1L, Long::sum);
            this.periodEnd = entry.getTimestamp();
            
            // Update hourly distribution
            String hour = LocalDateTime.ofInstant(entry.getTimestamp(), ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("HH:00"));
            this.hourlyDistribution.merge(hour, 1L, Long::sum);
            
            // Calculate error rate
            long errorCount = levelCounts.get(LogLevel.ERROR) + levelCounts.get(LogLevel.FATAL);
            this.errorRate = totalEntries > 0 ? (double) errorCount / totalEntries : 0.0;
            
            // Track top errors
            if (entry.getLevel() == LogLevel.ERROR || entry.getLevel() == LogLevel.FATAL) {
                if (topErrors.size() < 10) {
                    topErrors.add(entry.getMessage());
                }
            }
        }
        
        // Getters
        public String getComponent() { return component; }
        public Map<LogLevel, Long> getLevelCounts() { return levelCounts; }
        public long getTotalEntries() { return totalEntries; }
        public Instant getPeriodStart() { return periodStart; }
        public Instant getPeriodEnd() { return periodEnd; }
        public double getErrorRate() { return errorRate; }
        public List<String> getTopErrors() { return topErrors; }
        public Map<String, Long> getHourlyDistribution() { return hourlyDistribution; }
    }
    
    /**
     * Add log entry for aggregation
     */
    public void addLogEntry(LogLevel level, String loggerName, String message) {
        if (!aggregationEnabled) {
            return;
        }
        
        try {
            LogEntry entry = new LogEntry(level, loggerName, message);
            
            // Add AI-specific metadata
            if (isAiRelatedLog(message)) {
                entry.addMetadata("ai_related", "true");
                entry.addMetadata("component", extractAiComponent(loggerName));
            }
            
            // Add to storage
            logEntries.offer(entry);
            
            // Maintain size limit
            while (logEntries.size() > maxLogEntries) {
                logEntries.poll();
            }
            
            // Update patterns
            updateLogPatterns(entry);
            
            // Update statistics
            updateLogStatistics(entry);
            
            logger.debug("Added log entry: {} - {}", level, message);
            
        } catch (Exception e) {
            logger.error("Error adding log entry: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Add log entry with stack trace
     */
    public void addLogEntry(LogLevel level, String loggerName, String message, Throwable throwable) {
        LogEntry entry = new LogEntry(level, loggerName, message);
        if (throwable != null) {
            entry.setStackTrace(getStackTraceString(throwable));
            entry.addMetadata("exception_type", throwable.getClass().getSimpleName());
        }
        
        addLogEntry(entry);
    }
    
    /**
     * Add pre-constructed log entry
     */
    public void addLogEntry(LogEntry entry) {
        if (!aggregationEnabled) {
            return;
        }
        
        logEntries.offer(entry);
        
        // Maintain size limit
        while (logEntries.size() > maxLogEntries) {
            logEntries.poll();
        }
        
        updateLogPatterns(entry);
        updateLogStatistics(entry);
    }
    
    /**
     * Get log entries with filtering
     */
    public List<LogEntry> getLogEntries(LogLevel minLevel, String loggerFilter, 
                                       Instant since, int limit) {
        return logEntries.stream()
            .filter(entry -> entry.getLevel().ordinal() >= minLevel.ordinal())
            .filter(entry -> loggerFilter == null || entry.getLogger().contains(loggerFilter))
            .filter(entry -> since == null || entry.getTimestamp().isAfter(since))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit > 0 ? limit : Integer.MAX_VALUE)
            .collect(Collectors.toList());
    }
    
    /**
     * Get AI-specific log entries
     */
    public List<LogEntry> getAiLogEntries(Instant since, int limit) {
        return logEntries.stream()
            .filter(entry -> "true".equals(entry.getMetadata().get("ai_related")))
            .filter(entry -> since == null || entry.getTimestamp().isAfter(since))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit > 0 ? limit : Integer.MAX_VALUE)
            .collect(Collectors.toList());
    }
    
    /**
     * Get error log entries
     */
    public List<LogEntry> getErrorLogEntries(Instant since, int limit) {
        return getLogEntries(LogLevel.ERROR, null, since, limit);
    }
    
    /**
     * Get log patterns
     */
    public Map<String, LogPattern> getLogPatterns() {
        return new HashMap<>(logPatterns);
    }
    
    /**
     * Get log statistics
     */
    public Map<String, LogStatistics> getLogStatistics() {
        return new HashMap<>(logStatistics);
    }
    
    /**
     * Get aggregated log analysis
     */
    public Map<String, Object> getLogAnalysis(Instant since) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Filter entries by time
        List<LogEntry> filteredEntries = logEntries.stream()
            .filter(entry -> since == null || entry.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
        
        // Overall statistics
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalEntries", filteredEntries.size());
        overview.put("timeRange", Map.of(
            "start", since != null ? since : filteredEntries.stream()
                .map(LogEntry::getTimestamp)
                .min(Instant::compareTo)
                .orElse(Instant.now()),
            "end", Instant.now()
        ));
        
        // Level distribution
        Map<LogLevel, Long> levelDistribution = filteredEntries.stream()
            .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
        overview.put("levelDistribution", levelDistribution);
        
        // Error rate
        long errorCount = levelDistribution.getOrDefault(LogLevel.ERROR, 0L) + 
                         levelDistribution.getOrDefault(LogLevel.FATAL, 0L);
        double errorRate = filteredEntries.size() > 0 ? (double) errorCount / filteredEntries.size() : 0.0;
        overview.put("errorRate", errorRate);
        
        analysis.put("overview", overview);
        
        // Top loggers
        Map<String, Long> topLoggers = filteredEntries.stream()
            .collect(Collectors.groupingBy(LogEntry::getLogger, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        analysis.put("topLoggers", topLoggers);
        
        // AI-specific analysis
        List<LogEntry> aiEntries = filteredEntries.stream()
            .filter(entry -> "true".equals(entry.getMetadata().get("ai_related")))
            .collect(Collectors.toList());
        
        Map<String, Object> aiAnalysis = new HashMap<>();
        aiAnalysis.put("totalAiEntries", aiEntries.size());
        aiAnalysis.put("aiErrorRate", aiEntries.size() > 0 ? 
            aiEntries.stream().mapToLong(entry -> 
                entry.getLevel() == LogLevel.ERROR || entry.getLevel() == LogLevel.FATAL ? 1 : 0
            ).sum() / (double) aiEntries.size() : 0.0);
        
        // AI component distribution
        Map<String, Long> aiComponents = aiEntries.stream()
            .map(entry -> entry.getMetadata().getOrDefault("component", "unknown"))
            .collect(Collectors.groupingBy(component -> component, Collectors.counting()));
        aiAnalysis.put("componentDistribution", aiComponents);
        
        analysis.put("aiAnalysis", aiAnalysis);
        
        // Recent errors
        List<Map<String, Object>> recentErrors = filteredEntries.stream()
            .filter(entry -> entry.getLevel() == LogLevel.ERROR || entry.getLevel() == LogLevel.FATAL)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(20)
            .map(entry -> Map.of(
                "timestamp", entry.getTimestamp(),
                "level", entry.getLevel(),
                "logger", entry.getLogger(),
                "message", entry.getMessage(),
                "metadata", entry.getMetadata()
            ))
            .collect(Collectors.toList());
        analysis.put("recentErrors", recentErrors);
        
        return analysis;
    }
    
    /**
     * Update log patterns
     */
    private void updateLogPatterns(LogEntry entry) {
        String message = entry.getMessage();
        
        // Check against known patterns
        for (Pattern pattern : errorPatterns) {
            if (pattern.matcher(message).matches()) {
                LogPattern logPattern = logPatterns.computeIfAbsent("ERROR_PATTERN", LogPattern::new);
                logPattern.incrementFrequency(entry.getLevel(), entry.getLogger());
            }
        }
        
        for (Pattern pattern : warningPatterns) {
            if (pattern.matcher(message).matches()) {
                LogPattern logPattern = logPatterns.computeIfAbsent("WARNING_PATTERN", LogPattern::new);
                logPattern.incrementFrequency(entry.getLevel(), entry.getLogger());
            }
        }
        
        for (Pattern pattern : aiSpecificPatterns) {
            if (pattern.matcher(message).matches()) {
                LogPattern logPattern = logPatterns.computeIfAbsent("AI_PATTERN", LogPattern::new);
                logPattern.incrementFrequency(entry.getLevel(), entry.getLogger());
            }
        }
    }
    
    /**
     * Update log statistics
     */
    private void updateLogStatistics(LogEntry entry) {
        String component = extractAiComponent(entry.getLogger());
        LogStatistics stats = logStatistics.computeIfAbsent(component, LogStatistics::new);
        stats.addEntry(entry);
    }
    
    /**
     * Check if log is AI-related
     */
    private boolean isAiRelatedLog(String message) {
        return aiSpecificPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(message).find());
    }
    
    /**
     * Extract AI component from logger name
     */
    private String extractAiComponent(String logger) {
        if (logger.contains("orchestration")) return "orchestration";
        if (logger.contains("agent")) return "agents";
        if (logger.contains("reasoning")) return "reasoning";
        if (logger.contains("context")) return "context";
        if (logger.contains("planning")) return "planning";
        if (logger.contains("chatbot")) return "chatbot";
        if (logger.contains("ai")) return "ai";
        return "other";
    }
    
    /**
     * Convert throwable to string
     */
    private String getStackTraceString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Cleanup old log entries
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldEntries() {
        if (!aggregationEnabled) {
            return;
        }
        
        Instant cutoff = Instant.now().minusSeconds(retentionHours * 3600L);
        
        int removedCount = 0;
        Iterator<LogEntry> iterator = logEntries.iterator();
        while (iterator.hasNext()) {
            LogEntry entry = iterator.next();
            if (entry.getTimestamp().isBefore(cutoff)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up {} old log entries older than {} hours", removedCount, retentionHours);
        }
    }
    
    /**
     * Get current log entry count
     */
    public int getLogEntryCount() {
        return logEntries.size();
    }
    
    /**
     * Clear all log entries (for testing)
     */
    public void clearLogEntries() {
        logEntries.clear();
        logPatterns.clear();
        logStatistics.clear();
        logger.info("All log entries cleared");
    }
}