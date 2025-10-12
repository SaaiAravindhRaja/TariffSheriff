package com.tariffsheriff.backend.monitoring;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and managing AI-specific metrics
 */
@Service
public class AiMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiMetricsService.class);
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Counters
    private final Counter queryCounter;
    private final Counter successfulQueryCounter;
    private final Counter failedQueryCounter;
    private final Counter orchestrationCounter;
    private final Counter agentExecutionCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    
    // Timers
    private final Timer queryProcessingTimer;
    private final Timer orchestrationTimer;
    private final Timer agentExecutionTimer;
    private final Timer reasoningTimer;
    private final Timer dataRetrievalTimer;
    
    // Gauges
    private final AtomicLong activeQueries = new AtomicLong(0);
    private final AtomicLong queuedQueries = new AtomicLong(0);
    private final AtomicLong activeAgents = new AtomicLong(0);
    private final AtomicLong memoryUsage = new AtomicLong(0);
    
    // Distribution summaries
    private final DistributionSummary queryComplexityDistribution;
    private final DistributionSummary responseTokensDistribution;
    private final DistributionSummary agentCountDistribution;
    
    // Custom metrics storage
    private final Map<String, Double> customMetrics = new ConcurrentHashMap<>();
    private final Map<String, Instant> queryStartTimes = new ConcurrentHashMap<>();
    
    public AiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.queryCounter = Counter.builder("ai.queries.total")
            .description("Total number of AI queries processed")
            .register(meterRegistry);
            
        this.successfulQueryCounter = Counter.builder("ai.queries.successful")
            .description("Number of successful AI queries")
            .register(meterRegistry);
            
        this.failedQueryCounter = Counter.builder("ai.queries.failed")
            .description("Number of failed AI queries")
            .register(meterRegistry);
            
        this.orchestrationCounter = Counter.builder("ai.orchestration.total")
            .description("Total number of orchestration operations")
            .register(meterRegistry);
            
        this.agentExecutionCounter = Counter.builder("ai.agents.executions")
            .description("Total number of agent executions")
            .register(meterRegistry);
            
        this.cacheHitCounter = Counter.builder("ai.cache.hits")
            .description("Number of cache hits")
            .register(meterRegistry);
            
        this.cacheMissCounter = Counter.builder("ai.cache.misses")
            .description("Number of cache misses")
            .register(meterRegistry);
        
        // Initialize timers
        this.queryProcessingTimer = Timer.builder("ai.query.processing.time")
            .description("Time taken to process AI queries")
            .register(meterRegistry);
            
        this.orchestrationTimer = Timer.builder("ai.orchestration.time")
            .description("Time taken for orchestration operations")
            .register(meterRegistry);
            
        this.agentExecutionTimer = Timer.builder("ai.agent.execution.time")
            .description("Time taken for agent executions")
            .register(meterRegistry);
            
        this.reasoningTimer = Timer.builder("ai.reasoning.time")
            .description("Time taken for reasoning operations")
            .register(meterRegistry);
            
        this.dataRetrievalTimer = Timer.builder("ai.data.retrieval.time")
            .description("Time taken for data retrieval operations")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("ai.queries.active", activeQueries, AtomicLong::get)
            .description("Number of currently active queries")
            .register(meterRegistry);
            
        Gauge.builder("ai.queries.queued", queuedQueries, AtomicLong::get)
            .description("Number of queued queries")
            .register(meterRegistry);
            
        Gauge.builder("ai.agents.active", activeAgents, AtomicLong::get)
            .description("Number of currently active agents")
            .register(meterRegistry);
            
        Gauge.builder("ai.memory.usage", memoryUsage, AtomicLong::get)
            .description("AI system memory usage in bytes")
            .register(meterRegistry);
        
        // Initialize distribution summaries
        this.queryComplexityDistribution = DistributionSummary.builder("ai.query.complexity")
            .description("Distribution of query complexity scores")
            .register(meterRegistry);
            
        this.responseTokensDistribution = DistributionSummary.builder("ai.response.tokens")
            .description("Distribution of response token counts")
            .register(meterRegistry);
            
        this.agentCountDistribution = DistributionSummary.builder("ai.query.agent.count")
            .description("Distribution of agents used per query")
            .register(meterRegistry);
    }
    
    /**
     * Record query start
     */
    public void recordQueryStart(String queryId) {
        queryCounter.increment();
        activeQueries.incrementAndGet();
        queryStartTimes.put(queryId, Instant.now());
        
        logger.debug("Query started: {}", queryId);
    }
    
    /**
     * Record successful query completion
     */
    public void recordQuerySuccess(String queryId, int responseTokens, double complexityScore, int agentCount) {
        successfulQueryCounter.increment();
        activeQueries.decrementAndGet();
        
        // Record timing
        Instant startTime = queryStartTimes.remove(queryId);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            queryProcessingTimer.record(duration);
        }
        
        // Record distributions
        responseTokensDistribution.record(responseTokens);
        queryComplexityDistribution.record(complexityScore);
        agentCountDistribution.record(agentCount);
        
        logger.debug("Query completed successfully: {} (tokens: {}, complexity: {}, agents: {})", 
            queryId, responseTokens, complexityScore, agentCount);
    }
    
    /**
     * Record query failure
     */
    public void recordQueryFailure(String queryId, String errorType, String errorMessage) {
        failedQueryCounter.increment();
        meterRegistry.counter("ai.queries.failed", "error.type", errorType).increment();
        activeQueries.decrementAndGet();
        
        // Record timing
        Instant startTime = queryStartTimes.remove(queryId);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            queryProcessingTimer.record(duration);
        }
        
        logger.warn("Query failed: {} - {} ({})", queryId, errorType, errorMessage);
    }
    
    /**
     * Record orchestration operation
     */
    public Timer.Sample recordOrchestrationStart() {
        orchestrationCounter.increment();
        return Timer.start(meterRegistry);
    }
    
    /**
     * Record orchestration completion
     */
    public void recordOrchestrationEnd(Timer.Sample sample, boolean success) {
        sample.stop(orchestrationTimer);
        orchestrationCounter.increment();
        if (success) {
            meterRegistry.counter("ai.orchestration.total", "result", "success").increment();
        } else {
            meterRegistry.counter("ai.orchestration.total", "result", "failure").increment();
        }
    }
    
    /**
     * Record agent execution
     */
    public Timer.Sample recordAgentExecutionStart(String agentType) {
        agentExecutionCounter.increment();
        meterRegistry.counter("ai.agent.executions.total", "agent.type", agentType).increment();
        activeAgents.incrementAndGet();
        return Timer.start(meterRegistry);
    }
    
    /**
     * Record agent execution completion
     */
    public void recordAgentExecutionEnd(Timer.Sample sample, String agentType, boolean success) {
        sample.stop(meterRegistry.timer("ai.agent.execution.time", "agent.type", agentType));
        activeAgents.decrementAndGet();
        
        if (success) {
            meterRegistry.counter("ai.agent.executions.total", "agent.type", agentType, "result", "success").increment();
        } else {
            meterRegistry.counter("ai.agent.executions.total", "agent.type", agentType, "result", "failure").increment();
        }
    }
    
    /**
     * Record reasoning operation
     */
    public Timer.Sample recordReasoningStart() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Record reasoning completion
     */
    public void recordReasoningEnd(Timer.Sample sample, int inferenceSteps, double confidenceScore) {
        sample.stop(reasoningTimer);
        
        // Record custom metrics
        customMetrics.put("reasoning.inference.steps.last", (double) inferenceSteps);
        customMetrics.put("reasoning.confidence.last", confidenceScore);
    }
    
    /**
     * Record data retrieval operation
     */
    public Timer.Sample recordDataRetrievalStart(String dataSource) {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Record data retrieval completion
     */
    public void recordDataRetrievalEnd(Timer.Sample sample, String dataSource, boolean success, int recordCount) {
        sample.stop(meterRegistry.timer("ai.data.retrieval.time", "data.source", dataSource));
        
        if (success) {
            customMetrics.put("data.retrieval." + dataSource + ".records.last", (double) recordCount);
        }
    }
    
    /**
     * Record cache hit
     */
    public void recordCacheHit(String cacheType) {
        cacheHitCounter.increment();
        meterRegistry.counter("ai.cache.hits", "cache.type", cacheType).increment();
    }
    
    /**
     * Record cache miss
     */
    public void recordCacheMiss(String cacheType) {
        cacheMissCounter.increment();
        meterRegistry.counter("ai.cache.misses", "cache.type", cacheType).increment();
    }
    
    /**
     * Update queue size
     */
    public void updateQueueSize(long size) {
        queuedQueries.set(size);
    }
    
    /**
     * Update memory usage
     */
    public void updateMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }
    
    /**
     * Record custom metric
     */
    public void recordCustomMetric(String name, double value) {
        customMetrics.put(name, value);
        
        // Also record as a gauge if it doesn't exist
        Gauge.builder("ai.custom." + name.replace(".", "_"), customMetrics, map -> map.getOrDefault(name, 0.0))
            .description("Custom AI metric: " + name)
            .register(meterRegistry);
    }
    
    /**
     * Get current metrics summary
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        
        // Basic counters
        summary.put("queries.total", queryCounter.count());
        summary.put("queries.successful", successfulQueryCounter.count());
        summary.put("queries.failed", failedQueryCounter.count());
        summary.put("orchestration.total", orchestrationCounter.count());
        summary.put("agent.executions", agentExecutionCounter.count());
        
        // Current state
        summary.put("queries.active", activeQueries.get());
        summary.put("queries.queued", queuedQueries.get());
        summary.put("agents.active", activeAgents.get());
        summary.put("memory.usage", memoryUsage.get());
        
        // Timing statistics
        summary.put("query.processing.time.mean", queryProcessingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        summary.put("orchestration.time.mean", orchestrationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        summary.put("agent.execution.time.mean", agentExecutionTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        
        // Cache statistics
        double totalCacheOperations = cacheHitCounter.count() + cacheMissCounter.count();
        if (totalCacheOperations > 0) {
            summary.put("cache.hit.ratio", cacheHitCounter.count() / totalCacheOperations);
        } else {
            summary.put("cache.hit.ratio", 0.0);
        }
        
        // Custom metrics
        summary.putAll(customMetrics);
        
        return summary;
    }
    
    /**
     * Reset all metrics (for testing)
     */
    public void resetMetrics() {
        activeQueries.set(0);
        queuedQueries.set(0);
        activeAgents.set(0);
        memoryUsage.set(0);
        customMetrics.clear();
        queryStartTimes.clear();
        
        logger.info("AI metrics reset");
    }
}