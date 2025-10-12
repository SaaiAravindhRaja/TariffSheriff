package com.tariffsheriff.backend.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Intelligent load balancing service for request distribution
 * Features:
 * - Intelligent load balancing for LLM API calls
 * - Request queuing and prioritization based on complexity
 * - Circuit breaker integration for failed service routing
 * - Health-based routing and failover mechanisms
 */
@Service
public class LoadBalancingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancingService.class);
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;
    
    // Configuration
    @Value("${load-balancing.enabled:true}")
    private boolean loadBalancingEnabled;
    
    @Value("${load-balancing.max-queue-size:1000}")
    private int maxQueueSize;
    
    @Value("${load-balancing.request-timeout-seconds:30}")
    private int requestTimeoutSeconds;
    
    @Value("${load-balancing.health-check-interval-seconds:30}")
    private int healthCheckIntervalSeconds;
    
    @Value("${load-balancing.max-concurrent-requests:100}")
    private int maxConcurrentRequests;
    
    // Service endpoints and their health status
    private final ConcurrentHashMap<String, ServiceEndpoint> serviceEndpoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EndpointHealth> endpointHealth = new ConcurrentHashMap<>();
    
    // Request queues by priority
    private final PriorityBlockingQueue<LoadBalancedRequest> requestQueue = 
        new PriorityBlockingQueue<>(1000, Comparator.comparing(LoadBalancedRequest::getPriority));
    
    // Load balancing strategies
    private final Map<String, LoadBalancingStrategy> strategies = new ConcurrentHashMap<>();
    
    // Metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong queuedRequests = new AtomicLong(0);
    private final AtomicInteger currentConcurrentRequests = new AtomicInteger(0);
    
    // Executors
    private final ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService requestProcessor = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService queueProcessor = Executors.newSingleThreadScheduledExecutor();
    
    public LoadBalancingService() {
        initializeStrategies();
        initializeDefaultEndpoints();
        
        if (loadBalancingEnabled) {
            // Schedule health checks
            healthChecker.scheduleAtFixedRate(this::performHealthChecks, 
                healthCheckIntervalSeconds, healthCheckIntervalSeconds, TimeUnit.SECONDS);
            
            // Schedule queue processing
            queueProcessor.scheduleAtFixedRate(this::processRequestQueue, 1, 1, TimeUnit.SECONDS);
            
            logger.info("Load balancing service started with {} strategies", strategies.size());
        }
    }
    
    /**
     * Initialize load balancing strategies
     */
    private void initializeStrategies() {
        strategies.put("round_robin", new RoundRobinStrategy());
        strategies.put("weighted_round_robin", new WeightedRoundRobinStrategy());
        strategies.put("least_connections", new LeastConnectionsStrategy());
        strategies.put("response_time", new ResponseTimeStrategy());
        strategies.put("health_based", new HealthBasedStrategy());
    }
    
    /**
     * Initialize default service endpoints
     */
    private void initializeDefaultEndpoints() {
        // Primary LLM endpoint
        registerEndpoint("llm_primary", "https://api.primary-llm.com", 100, EndpointType.LLM);
        
        // Secondary LLM endpoint
        registerEndpoint("llm_secondary", "https://api.secondary-llm.com", 80, EndpointType.LLM);
        
        // Fallback LLM endpoint
        registerEndpoint("llm_fallback", "https://api.fallback-llm.com", 60, EndpointType.LLM);
        
        // External API endpoints
        registerEndpoint("tariff_api", "https://api.tariff-data.com", 90, EndpointType.EXTERNAL_API);
        registerEndpoint("trade_api", "https://api.trade-data.com", 85, EndpointType.EXTERNAL_API);
    }
    
    /**
     * Register a service endpoint
     */
    public void registerEndpoint(String endpointId, String url, int weight, EndpointType type) {
        ServiceEndpoint endpoint = new ServiceEndpoint(endpointId, url, weight, type);
        serviceEndpoints.put(endpointId, endpoint);
        endpointHealth.put(endpointId, new EndpointHealth(endpointId));
        
        logger.info("Registered endpoint: {} -> {} (weight: {}, type: {})", 
            endpointId, url, weight, type);
    }
    
    /**
     * Execute request with load balancing
     */
    public <T> CompletableFuture<T> executeRequest(String serviceType, RequestComplexity complexity, 
                                                  Supplier<T> requestSupplier) {
        if (!loadBalancingEnabled) {
            return CompletableFuture.supplyAsync(requestSupplier);
        }
        
        totalRequests.incrementAndGet();
        
        // Check if we're at capacity
        if (currentConcurrentRequests.get() >= maxConcurrentRequests) {
            return queueRequest(serviceType, complexity, requestSupplier);
        }
        
        return executeRequestInternal(serviceType, complexity, requestSupplier);
    }
    
    /**
     * Execute request with specific endpoint
     */
    public <T> CompletableFuture<T> executeRequestWithEndpoint(String endpointId, 
                                                              Supplier<T> requestSupplier) {
        ServiceEndpoint endpoint = serviceEndpoints.get(endpointId);
        if (endpoint == null) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Unknown endpoint: " + endpointId));
            return future;
        }
        
        // Check circuit breaker
        if (circuitBreakerService.isCircuitOpen(endpointId)) {
            logger.warn("Circuit breaker is open for endpoint: {}", endpointId);
            return findAlternativeEndpoint(endpoint.getType())
                .map(alt -> executeRequestWithEndpoint(alt.getEndpointId(), requestSupplier))
                .orElseGet(() -> {
                    CompletableFuture<T> future = new CompletableFuture<>();
                    future.completeExceptionally(new RuntimeException("No available endpoints"));
                    return future;
                });
        }
        
        return CompletableFuture.supplyAsync(() -> {
            currentConcurrentRequests.incrementAndGet();
            endpoint.incrementActiveConnections();
            
            long startTime = System.currentTimeMillis();
            
            try {
                T result = requestSupplier.get();
                
                long duration = System.currentTimeMillis() - startTime;
                recordSuccessfulRequest(endpointId, duration);
                
                return result;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                recordFailedRequest(endpointId, duration, e);
                throw new RuntimeException(e);
                
            } finally {
                currentConcurrentRequests.decrementAndGet();
                endpoint.decrementActiveConnections();
            }
        }, requestProcessor);
    }
    
    /**
     * Queue request when at capacity
     */
    private <T> CompletableFuture<T> queueRequest(String serviceType, RequestComplexity complexity, 
                                                 Supplier<T> requestSupplier) {
        if (requestQueue.size() >= maxQueueSize) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Request queue is full"));
            return future;
        }
        
        CompletableFuture<T> future = new CompletableFuture<>();
        LoadBalancedRequest<T> request = new LoadBalancedRequest<>(
            serviceType, complexity, requestSupplier, future);
        
        requestQueue.offer(request);
        queuedRequests.incrementAndGet();
        
        logger.debug("Queued request for service type: {} with complexity: {}", serviceType, complexity);
        
        return future;
    }
    
    /**
     * Execute request internally with load balancing
     */
    private <T> CompletableFuture<T> executeRequestInternal(String serviceType, RequestComplexity complexity, 
                                                           Supplier<T> requestSupplier) {
        // Select appropriate strategy based on service type and complexity
        LoadBalancingStrategy strategy = selectStrategy(serviceType, complexity);
        
        // Get available endpoints for service type
        List<ServiceEndpoint> availableEndpoints = getAvailableEndpoints(serviceType);
        
        if (availableEndpoints.isEmpty()) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("No available endpoints for service type: " + serviceType));
            return future;
        }
        
        // Select endpoint using strategy
        ServiceEndpoint selectedEndpoint = strategy.selectEndpoint(availableEndpoints, endpointHealth);
        
        return executeRequestWithEndpoint(selectedEndpoint.getEndpointId(), requestSupplier);
    }
    
    /**
     * Select load balancing strategy
     */
    private LoadBalancingStrategy selectStrategy(String serviceType, RequestComplexity complexity) {
        // Use health-based strategy for critical requests
        if (complexity == RequestComplexity.HIGH || complexity == RequestComplexity.CRITICAL) {
            return strategies.get("health_based");
        }
        
        // Use response time strategy for LLM requests
        if (serviceType.contains("llm")) {
            return strategies.get("response_time");
        }
        
        // Use least connections for external APIs
        if (serviceType.contains("api")) {
            return strategies.get("least_connections");
        }
        
        // Default to weighted round robin
        return strategies.get("weighted_round_robin");
    }
    
    /**
     * Get available endpoints for service type
     */
    private List<ServiceEndpoint> getAvailableEndpoints(String serviceType) {
        EndpointType targetType = determineEndpointType(serviceType);
        
        return serviceEndpoints.values().stream()
                .filter(endpoint -> endpoint.getType() == targetType)
                .filter(endpoint -> isEndpointHealthy(endpoint.getEndpointId()))
                .filter(endpoint -> !circuitBreakerService.isCircuitOpen(endpoint.getEndpointId()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Determine endpoint type from service type
     */
    private EndpointType determineEndpointType(String serviceType) {
        if (serviceType.contains("llm")) {
            return EndpointType.LLM;
        } else if (serviceType.contains("api")) {
            return EndpointType.EXTERNAL_API;
        } else {
            return EndpointType.INTERNAL_SERVICE;
        }
    }
    
    /**
     * Check if endpoint is healthy
     */
    private boolean isEndpointHealthy(String endpointId) {
        EndpointHealth health = endpointHealth.get(endpointId);
        return health != null && health.isHealthy();
    }
    
    /**
     * Find alternative endpoint of same type
     */
    private Optional<ServiceEndpoint> findAlternativeEndpoint(EndpointType type) {
        return serviceEndpoints.values().stream()
                .filter(endpoint -> endpoint.getType() == type)
                .filter(endpoint -> isEndpointHealthy(endpoint.getEndpointId()))
                .filter(endpoint -> !circuitBreakerService.isCircuitOpen(endpoint.getEndpointId()))
                .findFirst();
    }
    
    /**
     * Record successful request
     */
    private void recordSuccessfulRequest(String endpointId, long durationMs) {
        successfulRequests.incrementAndGet();
        
        EndpointHealth health = endpointHealth.get(endpointId);
        if (health != null) {
            health.recordSuccess(durationMs);
        }
        
        circuitBreakerService.recordSuccess(endpointId);
        
        logger.debug("Successful request to endpoint: {} in {}ms", endpointId, durationMs);
    }
    
    /**
     * Record failed request
     */
    private void recordFailedRequest(String endpointId, long durationMs, Exception error) {
        failedRequests.incrementAndGet();
        
        EndpointHealth health = endpointHealth.get(endpointId);
        if (health != null) {
            health.recordFailure(error);
        }
        
        circuitBreakerService.recordFailure(endpointId);
        
        logger.warn("Failed request to endpoint: {} in {}ms - {}", endpointId, durationMs, error.getMessage());
    }
    
    /**
     * Process request queue
     */
    private void processRequestQueue() {
        try {
            while (!requestQueue.isEmpty() && currentConcurrentRequests.get() < maxConcurrentRequests) {
                LoadBalancedRequest<?> request = requestQueue.poll();
                if (request != null) {
                    queuedRequests.decrementAndGet();
                    processQueuedRequest(request);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing request queue", e);
        }
    }
    
    /**
     * Process individual queued request
     */
    @SuppressWarnings("unchecked")
    private <T> void processQueuedRequest(LoadBalancedRequest<T> request) {
        CompletableFuture<T> executionFuture = executeRequestInternal(
            request.getServiceType(), request.getComplexity(), request.getRequestSupplier());
        
        executionFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                request.getFuture().completeExceptionally(throwable);
            } else {
                request.getFuture().complete(result);
            }
        });
    }
    
    /**
     * Perform health checks on all endpoints
     */
    private void performHealthChecks() {
        try {
            for (ServiceEndpoint endpoint : serviceEndpoints.values()) {
                performHealthCheck(endpoint);
            }
        } catch (Exception e) {
            logger.error("Error performing health checks", e);
        }
    }
    
    /**
     * Perform health check on specific endpoint
     */
    private void performHealthCheck(ServiceEndpoint endpoint) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate health check (in production, this would make actual HTTP request)
                long startTime = System.currentTimeMillis();
                
                // Simulate network call
                Thread.sleep(100 + (long) (Math.random() * 200));
                
                long duration = System.currentTimeMillis() - startTime;
                
                // Simulate occasional failures
                if (Math.random() < 0.05) { // 5% failure rate
                    throw new RuntimeException("Health check failed");
                }
                
                EndpointHealth health = endpointHealth.get(endpoint.getEndpointId());
                if (health != null) {
                    health.recordHealthCheck(true, duration);
                }
                
                return true;
                
            } catch (Exception e) {
                EndpointHealth health = endpointHealth.get(endpoint.getEndpointId());
                if (health != null) {
                    health.recordHealthCheck(false, 0);
                }
                return false;
            }
        }, requestProcessor);
    }
    
    /**
     * Get load balancing statistics
     */
    public LoadBalancingStats getStats() {
        Map<String, EndpointStats> endpointStats = new HashMap<>();
        
        for (Map.Entry<String, EndpointHealth> entry : endpointHealth.entrySet()) {
            EndpointHealth health = entry.getValue();
            ServiceEndpoint endpoint = serviceEndpoints.get(entry.getKey());
            
            endpointStats.put(entry.getKey(), new EndpointStats(
                entry.getKey(),
                endpoint != null ? endpoint.getUrl() : "unknown",
                health.isHealthy(),
                health.getSuccessCount(),
                health.getFailureCount(),
                health.getAverageResponseTime(),
                endpoint != null ? endpoint.getActiveConnections() : 0
            ));
        }
        
        return new LoadBalancingStats(
            totalRequests.get(),
            successfulRequests.get(),
            failedRequests.get(),
            queuedRequests.get(),
            currentConcurrentRequests.get(),
            requestQueue.size(),
            endpointStats
        );
    }
    
    /**
     * Get endpoint health status
     */
    public Map<String, Boolean> getEndpointHealthStatus() {
        return endpointHealth.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isHealthy()
                ));
    }
    
    /**
     * Manually mark endpoint as healthy/unhealthy
     */
    public void setEndpointHealth(String endpointId, boolean healthy) {
        EndpointHealth health = endpointHealth.get(endpointId);
        if (health != null) {
            health.setHealthy(healthy);
            logger.info("Manually set endpoint {} health to: {}", endpointId, healthy);
        }
    }
    
    /**
     * Remove endpoint from load balancing
     */
    public void removeEndpoint(String endpointId) {
        serviceEndpoints.remove(endpointId);
        endpointHealth.remove(endpointId);
        logger.info("Removed endpoint: {}", endpointId);
    }
    
    // Supporting classes and enums
    
    public enum EndpointType {
        LLM, EXTERNAL_API, INTERNAL_SERVICE
    }
    
    public enum RequestComplexity {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);
        
        private final int priority;
        
        RequestComplexity(int priority) {
            this.priority = priority;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    /**
     * Service endpoint configuration
     */
    public static class ServiceEndpoint {
        private final String endpointId;
        private final String url;
        private final int weight;
        private final EndpointType type;
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        private final Instant createdAt;
        
        public ServiceEndpoint(String endpointId, String url, int weight, EndpointType type) {
            this.endpointId = endpointId;
            this.url = url;
            this.weight = weight;
            this.type = type;
            this.createdAt = Instant.now();
        }
        
        public void incrementActiveConnections() {
            activeConnections.incrementAndGet();
        }
        
        public void decrementActiveConnections() {
            activeConnections.decrementAndGet();
        }
        
        public String getEndpointId() { return endpointId; }
        public String getUrl() { return url; }
        public int getWeight() { return weight; }
        public EndpointType getType() { return type; }
        public int getActiveConnections() { return activeConnections.get(); }
        public Instant getCreatedAt() { return createdAt; }
    }
    
    /**
     * Endpoint health tracking
     */
    public static class EndpointHealth {
        private final String endpointId;
        private volatile boolean healthy = true;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final ConcurrentLinkedQueue<Long> recentResponseTimes = new ConcurrentLinkedQueue<>();
        private volatile Instant lastHealthCheck = Instant.now();
        private volatile Instant lastFailure;
        
        public EndpointHealth(String endpointId) {
            this.endpointId = endpointId;
        }
        
        public void recordSuccess(long responseTimeMs) {
            successCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            
            recentResponseTimes.offer(responseTimeMs);
            if (recentResponseTimes.size() > 100) {
                recentResponseTimes.poll();
            }
            
            // Auto-recover if we have recent successes
            if (failureCount.get() > 0 && getRecentSuccessRate() > 0.8) {
                healthy = true;
            }
        }
        
        public void recordFailure(Exception error) {
            failureCount.incrementAndGet();
            lastFailure = Instant.now();
            
            // Mark as unhealthy if failure rate is high
            if (getOverallFailureRate() > 0.2) {
                healthy = false;
            }
        }
        
        public void recordHealthCheck(boolean success, long responseTimeMs) {
            lastHealthCheck = Instant.now();
            
            if (success) {
                recordSuccess(responseTimeMs);
            } else {
                recordFailure(new RuntimeException("Health check failed"));
            }
        }
        
        public double getOverallFailureRate() {
            long total = successCount.get() + failureCount.get();
            return total > 0 ? (double) failureCount.get() / total : 0.0;
        }
        
        public double getRecentSuccessRate() {
            // Calculate success rate for last 20 operations
            long recentTotal = Math.min(20, successCount.get() + failureCount.get());
            if (recentTotal == 0) return 1.0;
            
            long recentFailures = Math.min(failureCount.get(), recentTotal);
            return (double) (recentTotal - recentFailures) / recentTotal;
        }
        
        public double getAverageResponseTime() {
            long successes = successCount.get();
            return successes > 0 ? (double) totalResponseTime.get() / successes : 0.0;
        }
        
        public boolean isHealthy() {
            // Consider unhealthy if no recent health check
            if (Duration.between(lastHealthCheck, Instant.now()).toMinutes() > 5) {
                return false;
            }
            
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public String getEndpointId() { return endpointId; }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public Instant getLastHealthCheck() { return lastHealthCheck; }
        public Instant getLastFailure() { return lastFailure; }
    }
    
    /**
     * Load balanced request wrapper
     */
    public static class LoadBalancedRequest<T> {
        private final String serviceType;
        private final RequestComplexity complexity;
        private final Supplier<T> requestSupplier;
        private final CompletableFuture<T> future;
        private final Instant createdAt;
        
        public LoadBalancedRequest(String serviceType, RequestComplexity complexity, 
                                 Supplier<T> requestSupplier, CompletableFuture<T> future) {
            this.serviceType = serviceType;
            this.complexity = complexity;
            this.requestSupplier = requestSupplier;
            this.future = future;
            this.createdAt = Instant.now();
        }
        
        public int getPriority() {
            return complexity.getPriority();
        }
        
        public String getServiceType() { return serviceType; }
        public RequestComplexity getComplexity() { return complexity; }
        public Supplier<T> getRequestSupplier() { return requestSupplier; }
        public CompletableFuture<T> getFuture() { return future; }
        public Instant getCreatedAt() { return createdAt; }
    }
    
    /**
     * Load balancing strategy interface
     */
    public interface LoadBalancingStrategy {
        ServiceEndpoint selectEndpoint(List<ServiceEndpoint> availableEndpoints, 
                                     Map<String, EndpointHealth> endpointHealth);
    }
    
    /**
     * Round robin load balancing strategy
     */
    public static class RoundRobinStrategy implements LoadBalancingStrategy {
        private final AtomicInteger counter = new AtomicInteger(0);
        
        @Override
        public ServiceEndpoint selectEndpoint(List<ServiceEndpoint> availableEndpoints, 
                                            Map<String, EndpointHealth> endpointHealth) {
            if (availableEndpoints.isEmpty()) {
                throw new RuntimeException("No available endpoints");
            }
            
            int index = counter.getAndIncrement() % availableEndpoints.size();
            return availableEndpoints.get(index);
        }
    }
    
    /**
     * Weighted round robin load balancing strategy
     */
    public static class WeightedRoundRobinStrategy implements LoadBalancingStrategy {
        private final AtomicInteger counter = new AtomicInteger(0);
        
        @Override
        public ServiceEndpoint selectEndpoint(List<ServiceEndpoint> availableEndpoints, 
                                            Map<String, EndpointHealth> endpointHealth) {
            if (availableEndpoints.isEmpty()) {
                throw new RuntimeException("No available endpoints");
            }
            
            // Calculate total weight
            int totalWeight = availableEndpoints.stream()
                    .mapToInt(ServiceEndpoint::getWeight)
                    .sum();
            
            if (totalWeight == 0) {
                // Fallback to round robin if no weights
                int index = counter.getAndIncrement() % availableEndpoints.size();
                return availableEndpoints.get(index);
            }
            
            int targetWeight = counter.getAndIncrement() % totalWeight;
            int currentWeight = 0;
            
            for (ServiceEndpoint endpoint : availableEndpoints) {
                currentWeight += endpoint.getWeight();
                if (currentWeight > targetWeight) {
                    return endpoint;
                }
            }
            
            // Fallback to first endpoint
            return availableEndpoints.get(0);
        }
    }
    
    /**
     * Least connections load balancing strategy
     */
    public static class LeastConnectionsStrategy implements LoadBalancingStrategy {
        @Override
        public ServiceEndpoint selectEndpoint(List<ServiceEndpoint> availableEndpoints, 
                                            Map<String, EndpointHealth> endpointHealth) {
            if (availableEndpoints.isEmpty()) {
                throw new RuntimeException("No available endpoints");
            }
            
            return availableEndpoints.stream()
                    .min(Comparator.comparingInt(ServiceEndpoint::getActiveConnections))
                    .orElse(availableEndpoints.get(0));
        }
    }
    
    /**
     * Response time based load balancing strategy
     */
    public static class ResponseTimeStrategy implements LoadBalancingStrategy {
        @Override
        public ServiceEndpoint selectEndpoint(List<ServiceEndpoint> availableEndpoints, 
                                            Map<String, EndpointHealth> endpointHealth) {
            if (availableEndpoints.isEmpty()) {
                throw new RuntimeException("No available endpoints");
            }
            
            return availableEndpoints.stream()
                    .min(Comparator.comparingDouble(endpoint -> {
                        EndpointHealth health = endpointHealth.get(endpoint.getEndpointId());
                        return health != null ? health.getAverageResponseTime() : Double.MAX_VALUE;
                    }))
                    .orElse(availableEndpoints.get(0));
        }
    }
    
    /**
     * Health based load balancing strategy
     */
    public static class HealthBasedStrategy implements LoadBalancingStrategy {
        @Override
        public ServiceEndpoint selectEndpoint(List<ServiceEndpoint> availableEndpoints, 
                                            Map<String, EndpointHealth> endpointHealth) {
            if (availableEndpoints.isEmpty()) {
                throw new RuntimeException("No available endpoints");
            }
            
            // Sort by health score (combination of success rate and response time)
            return availableEndpoints.stream()
                    .max(Comparator.comparingDouble(endpoint -> {
                        EndpointHealth health = endpointHealth.get(endpoint.getEndpointId());
                        if (health == null) return 0.0;
                        
                        double successRate = health.getRecentSuccessRate();
                        double responseTime = health.getAverageResponseTime();
                        
                        // Health score: success rate weighted by response time
                        return successRate * (1000.0 / Math.max(responseTime, 1.0));
                    }))
                    .orElse(availableEndpoints.get(0));
        }
    }
    
    /**
     * Endpoint statistics
     */
    public static class EndpointStats {
        private final String endpointId;
        private final String url;
        private final boolean healthy;
        private final long successCount;
        private final long failureCount;
        private final double averageResponseTime;
        private final int activeConnections;
        
        public EndpointStats(String endpointId, String url, boolean healthy, 
                           long successCount, long failureCount, 
                           double averageResponseTime, int activeConnections) {
            this.endpointId = endpointId;
            this.url = url;
            this.healthy = healthy;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.averageResponseTime = averageResponseTime;
            this.activeConnections = activeConnections;
        }
        
        public String getEndpointId() { return endpointId; }
        public String getUrl() { return url; }
        public boolean isHealthy() { return healthy; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public int getActiveConnections() { return activeConnections; }
        
        public double getSuccessRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) successCount / total : 0.0;
        }
    }
    
    /**
     * Load balancing statistics
     */
    public static class LoadBalancingStats {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final long queuedRequests;
        private final int currentConcurrentRequests;
        private final int queueSize;
        private final Map<String, EndpointStats> endpointStats;
        
        public LoadBalancingStats(long totalRequests, long successfulRequests, long failedRequests,
                                long queuedRequests, int currentConcurrentRequests, int queueSize,
                                Map<String, EndpointStats> endpointStats) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.queuedRequests = queuedRequests;
            this.currentConcurrentRequests = currentConcurrentRequests;
            this.queueSize = queueSize;
            this.endpointStats = new HashMap<>(endpointStats);
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getQueuedRequests() { return queuedRequests; }
        public int getCurrentConcurrentRequests() { return currentConcurrentRequests; }
        public int getQueueSize() { return queueSize; }
        public Map<String, EndpointStats> getEndpointStats() { return endpointStats; }
        
        public double getOverallSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }
        
        public int getHealthyEndpointCount() {
            return (int) endpointStats.values().stream()
                    .mapToLong(stats -> stats.isHealthy() ? 1 : 0)
                    .sum();
        }
        
        public int getTotalEndpointCount() {
            return endpointStats.size();
        }
    }
}