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

/**
 * Resource optimization and management service
 * Features:
 * - Connection pooling for external API calls
 * - Memory management and garbage collection optimization
 * - Resource allocation policies based on query complexity
 * - Auto-scaling triggers based on performance metrics
 */
@Service
public class ResourceOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceOptimizationService.class);
    
    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;
    
    // Configuration
    @Value("${resource.optimization.enabled:true}")
    private boolean optimizationEnabled;
    
    @Value("${resource.connection-pool.max-size:50}")
    private int maxConnectionPoolSize;
    
    @Value("${resource.connection-pool.min-size:5}")
    private int minConnectionPoolSize;
    
    @Value("${resource.memory.gc-threshold:0.8}")
    private double gcThreshold;
    
    @Value("${resource.memory.optimization-interval-minutes:10}")
    private int memoryOptimizationIntervalMinutes;
    
    @Value("${resource.scaling.cpu-threshold:0.7}")
    private double scalingCpuThreshold;
    
    @Value("${resource.scaling.memory-threshold:0.8}")
    private double scalingMemoryThreshold;
    
    // Connection pools for different services
    private final Map<String, ConnectionPool> connectionPools = new ConcurrentHashMap<>();
    
    // Resource allocation tracking
    private final Map<String, ResourceAllocation> resourceAllocations = new ConcurrentHashMap<>();
    private final AtomicInteger totalActiveConnections = new AtomicInteger(0);
    private final AtomicLong totalMemoryAllocated = new AtomicLong(0);
    
    // Auto-scaling metrics
    private final ConcurrentLinkedQueue<ResourceMetrics> recentMetrics = new ConcurrentLinkedQueue<>();
    private volatile ScalingRecommendation lastScalingRecommendation;
    
    // Executors
    private final ScheduledExecutorService optimizationExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService scalingExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public ResourceOptimizationService() {
        initializeConnectionPools();
        
        if (optimizationEnabled) {
            // Schedule memory optimization
            optimizationExecutor.scheduleAtFixedRate(this::optimizeMemoryUsage, 
                memoryOptimizationIntervalMinutes, memoryOptimizationIntervalMinutes, TimeUnit.MINUTES);
            
            // Schedule connection pool optimization
            optimizationExecutor.scheduleAtFixedRate(this::optimizeConnectionPools, 5, 5, TimeUnit.MINUTES);
            
            // Schedule auto-scaling analysis
            scalingExecutor.scheduleAtFixedRate(this::analyzeScalingNeeds, 2, 2, TimeUnit.MINUTES);
            
            logger.info("Resource optimization service started");
        }
    }
    
    /**
     * Initialize connection pools for different services
     */
    private void initializeConnectionPools() {
        // LLM API connection pool
        createConnectionPool("llm_api", 10, 30, Duration.ofSeconds(30));
        
        // External trade APIs
        createConnectionPool("tariff_api", 5, 15, Duration.ofSeconds(20));
        createConnectionPool("trade_data_api", 5, 15, Duration.ofSeconds(20));
        createConnectionPool("customs_api", 3, 10, Duration.ofSeconds(25));
        
        // Internal services
        createConnectionPool("database", 10, 25, Duration.ofSeconds(10));
        createConnectionPool("cache", 5, 15, Duration.ofSeconds(5));
        
        logger.info("Initialized {} connection pools", connectionPools.size());
    }
    
    /**
     * Create connection pool for specific service
     */
    private void createConnectionPool(String serviceId, int minSize, int maxSize, Duration timeout) {
        ConnectionPool pool = new ConnectionPool(serviceId, minSize, maxSize, timeout);
        connectionPools.put(serviceId, pool);
        
        logger.debug("Created connection pool for {}: min={}, max={}, timeout={}ms", 
            serviceId, minSize, maxSize, timeout.toMillis());
    }
    
    /**
     * Get connection from pool
     */
    public Connection getConnection(String serviceId) throws InterruptedException {
        ConnectionPool pool = connectionPools.get(serviceId);
        if (pool == null) {
            throw new IllegalArgumentException("Unknown service: " + serviceId);
        }
        
        Connection connection = pool.getConnection();
        totalActiveConnections.incrementAndGet();
        
        logger.debug("Acquired connection for service: {} (active: {})", 
            serviceId, totalActiveConnections.get());
        
        return connection;
    }
    
    /**
     * Return connection to pool
     */
    public void returnConnection(String serviceId, Connection connection) {
        ConnectionPool pool = connectionPools.get(serviceId);
        if (pool != null) {
            pool.returnConnection(connection);
            totalActiveConnections.decrementAndGet();
            
            logger.debug("Returned connection for service: {} (active: {})", 
                serviceId, totalActiveConnections.get());
        }
    }
    
    /**
     * Allocate resources based on query complexity
     */
    public ResourceAllocation allocateResources(String queryId, QueryComplexity complexity, 
                                              Map<String, Object> requirements) {
        ResourceAllocation allocation = new ResourceAllocation(queryId, complexity);
        
        // Determine resource allocation based on complexity
        switch (complexity) {
            case LOW:
                allocation.setMaxMemoryMB(50);
                allocation.setMaxConnections(2);
                allocation.setTimeoutSeconds(30);
                allocation.setPriority(ResourcePriority.NORMAL);
                break;
                
            case MEDIUM:
                allocation.setMaxMemoryMB(100);
                allocation.setMaxConnections(3);
                allocation.setTimeoutSeconds(60);
                allocation.setPriority(ResourcePriority.HIGH);
                break;
                
            case HIGH:
                allocation.setMaxMemoryMB(200);
                allocation.setMaxConnections(5);
                allocation.setTimeoutSeconds(120);
                allocation.setPriority(ResourcePriority.HIGH);
                break;
                
            case CRITICAL:
                allocation.setMaxMemoryMB(500);
                allocation.setMaxConnections(8);
                allocation.setTimeoutSeconds(300);
                allocation.setPriority(ResourcePriority.CRITICAL);
                break;
        }
        
        // Apply custom requirements
        applyCustomRequirements(allocation, requirements);
        
        // Check resource availability
        if (!checkResourceAvailability(allocation)) {
            logger.warn("Insufficient resources for query {}, reducing allocation", queryId);
            reduceAllocation(allocation);
        }
        
        resourceAllocations.put(queryId, allocation);
        totalMemoryAllocated.addAndGet(allocation.getMaxMemoryMB());
        
        logger.debug("Allocated resources for query {}: memory={}MB, connections={}, priority={}", 
            queryId, allocation.getMaxMemoryMB(), allocation.getMaxConnections(), allocation.getPriority());
        
        return allocation;
    }
    
    /**
     * Apply custom resource requirements
     */
    private void applyCustomRequirements(ResourceAllocation allocation, Map<String, Object> requirements) {
        if (requirements == null) return;
        
        if (requirements.containsKey("memory_mb")) {
            Integer memoryMB = (Integer) requirements.get("memory_mb");
            if (memoryMB != null && memoryMB > 0) {
                allocation.setMaxMemoryMB(Math.min(memoryMB, 1000)); // Cap at 1GB
            }
        }
        
        if (requirements.containsKey("max_connections")) {
            Integer maxConnections = (Integer) requirements.get("max_connections");
            if (maxConnections != null && maxConnections > 0) {
                allocation.setMaxConnections(Math.min(maxConnections, 10)); // Cap at 10
            }
        }
        
        if (requirements.containsKey("timeout_seconds")) {
            Integer timeoutSeconds = (Integer) requirements.get("timeout_seconds");
            if (timeoutSeconds != null && timeoutSeconds > 0) {
                allocation.setTimeoutSeconds(Math.min(timeoutSeconds, 600)); // Cap at 10 minutes
            }
        }
    }
    
    /**
     * Check if resources are available for allocation
     */
    private boolean checkResourceAvailability(ResourceAllocation allocation) {
        // Check memory availability
        Runtime runtime = Runtime.getRuntime();
        long availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        long requiredMemory = allocation.getMaxMemoryMB() * 1024 * 1024;
        
        if (requiredMemory > availableMemory * 0.8) { // Keep 20% buffer
            return false;
        }
        
        // Check connection availability
        int availableConnections = maxConnectionPoolSize - totalActiveConnections.get();
        if (allocation.getMaxConnections() > availableConnections) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Reduce resource allocation when resources are constrained
     */
    private void reduceAllocation(ResourceAllocation allocation) {
        allocation.setMaxMemoryMB(Math.max(allocation.getMaxMemoryMB() / 2, 25));
        allocation.setMaxConnections(Math.max(allocation.getMaxConnections() / 2, 1));
        allocation.setTimeoutSeconds(Math.max(allocation.getTimeoutSeconds() / 2, 15));
        
        logger.debug("Reduced allocation for query {}: memory={}MB, connections={}", 
            allocation.getQueryId(), allocation.getMaxMemoryMB(), allocation.getMaxConnections());
    }
    
    /**
     * Release resources for completed query
     */
    public void releaseResources(String queryId) {
        ResourceAllocation allocation = resourceAllocations.remove(queryId);
        if (allocation != null) {
            totalMemoryAllocated.addAndGet(-allocation.getMaxMemoryMB());
            
            logger.debug("Released resources for query {}: memory={}MB", 
                queryId, allocation.getMaxMemoryMB());
        }
    }
    
    /**
     * Optimize memory usage
     */
    private void optimizeMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / runtime.maxMemory();
            
            logger.debug("Memory usage: {:.2f}% ({} MB used, {} MB free)", 
                memoryUsage * 100, usedMemory / (1024 * 1024), freeMemory / (1024 * 1024));
            
            if (memoryUsage > gcThreshold) {
                logger.info("Memory usage above threshold ({:.2f}%), triggering garbage collection", 
                    memoryUsage * 100);
                
                // Trigger garbage collection
                System.gc();
                
                // Wait a bit and check again
                Thread.sleep(1000);
                
                long newFreeMemory = runtime.freeMemory();
                long freedMemory = newFreeMemory - freeMemory;
                
                logger.info("Garbage collection freed {} MB of memory", 
                    freedMemory / (1024 * 1024));
                
                // If still high, consider more aggressive optimization
                double newMemoryUsage = (double) (runtime.totalMemory() - newFreeMemory) / runtime.maxMemory();
                if (newMemoryUsage > gcThreshold) {
                    performAggressiveMemoryOptimization();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during memory optimization", e);
        }
    }
    
    /**
     * Perform aggressive memory optimization
     */
    private void performAggressiveMemoryOptimization() {
        logger.warn("Performing aggressive memory optimization");
        
        // Clear non-essential caches
        // This would integrate with cache services to clear old entries
        
        // Reduce connection pool sizes temporarily
        for (ConnectionPool pool : connectionPools.values()) {
            pool.reduceSize();
        }
        
        // Force another GC
        System.gc();
        
        logger.info("Aggressive memory optimization completed");
    }
    
    /**
     * Optimize connection pools
     */
    private void optimizeConnectionPools() {
        try {
            for (Map.Entry<String, ConnectionPool> entry : connectionPools.entrySet()) {
                String serviceId = entry.getKey();
                ConnectionPool pool = entry.getValue();
                
                ConnectionPoolStats stats = pool.getStats();
                
                // Adjust pool size based on usage patterns
                if (stats.getUtilization() > 0.8 && stats.getCurrentSize() < pool.getMaxSize()) {
                    pool.expandPool();
                    logger.debug("Expanded connection pool for {}: new size = {}", 
                        serviceId, pool.getCurrentSize());
                } else if (stats.getUtilization() < 0.3 && stats.getCurrentSize() > pool.getMinSize()) {
                    pool.shrinkPool();
                    logger.debug("Shrunk connection pool for {}: new size = {}", 
                        serviceId, pool.getCurrentSize());
                }
                
                // Clean up idle connections
                pool.cleanupIdleConnections();
            }
            
        } catch (Exception e) {
            logger.error("Error during connection pool optimization", e);
        }
    }
    
    /**
     * Analyze auto-scaling needs
     */
    private void analyzeScalingNeeds() {
        try {
            // Collect current metrics
            ResourceMetrics currentMetrics = collectResourceMetrics();
            recentMetrics.offer(currentMetrics);
            
            // Keep only recent metrics (last 30 data points)
            while (recentMetrics.size() > 30) {
                recentMetrics.poll();
            }
            
            // Analyze scaling needs
            ScalingRecommendation recommendation = analyzeScalingRecommendation(currentMetrics);
            
            if (recommendation != null && !recommendation.equals(lastScalingRecommendation)) {
                logger.info("New scaling recommendation: {}", recommendation);
                lastScalingRecommendation = recommendation;
                
                // In production, this would trigger actual scaling actions
                triggerScalingAction(recommendation);
            }
            
        } catch (Exception e) {
            logger.error("Error during scaling analysis", e);
        }
    }
    
    /**
     * Collect current resource metrics
     */
    private ResourceMetrics collectResourceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        double cpuUsage = getCurrentCpuUsage();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        int activeConnections = totalActiveConnections.get();
        int activeQueries = resourceAllocations.size();
        
        return new ResourceMetrics(Instant.now(), cpuUsage, memoryUsage, 
            activeConnections, activeQueries);
    }
    
    /**
     * Analyze scaling recommendation based on metrics
     */
    private ScalingRecommendation analyzeScalingRecommendation(ResourceMetrics currentMetrics) {
        if (recentMetrics.size() < 5) {
            return null; // Need more data
        }
        
        // Calculate average metrics over recent period
        double avgCpuUsage = recentMetrics.stream()
                .mapToDouble(ResourceMetrics::getCpuUsage)
                .average()
                .orElse(0.0);
        
        double avgMemoryUsage = recentMetrics.stream()
                .mapToDouble(ResourceMetrics::getMemoryUsage)
                .average()
                .orElse(0.0);
        
        // Determine scaling action
        if (avgCpuUsage > scalingCpuThreshold || avgMemoryUsage > scalingMemoryThreshold) {
            return new ScalingRecommendation(ScalingAction.SCALE_UP, 
                "High resource usage detected", calculateScaleUpFactor(avgCpuUsage, avgMemoryUsage));
        } else if (avgCpuUsage < 0.3 && avgMemoryUsage < 0.4) {
            return new ScalingRecommendation(ScalingAction.SCALE_DOWN, 
                "Low resource usage detected", 0.8);
        }
        
        return null; // No scaling needed
    }
    
    /**
     * Calculate scale up factor based on resource usage
     */
    private double calculateScaleUpFactor(double cpuUsage, double memoryUsage) {
        double maxUsage = Math.max(cpuUsage, memoryUsage);
        
        if (maxUsage > 0.9) {
            return 2.0; // Double capacity for very high usage
        } else if (maxUsage > 0.8) {
            return 1.5; // 50% increase for high usage
        } else {
            return 1.2; // 20% increase for moderate usage
        }
    }
    
    /**
     * Trigger scaling action
     */
    private void triggerScalingAction(ScalingRecommendation recommendation) {
        logger.info("Triggering scaling action: {} with factor {}", 
            recommendation.getAction(), recommendation.getScaleFactor());
        
        switch (recommendation.getAction()) {
            case SCALE_UP:
                scaleUp(recommendation.getScaleFactor());
                break;
            case SCALE_DOWN:
                scaleDown(recommendation.getScaleFactor());
                break;
        }
    }
    
    /**
     * Scale up resources
     */
    private void scaleUp(double scaleFactor) {
        logger.info("Scaling up resources by factor: {}", scaleFactor);
        
        // Increase connection pool sizes
        for (ConnectionPool pool : connectionPools.values()) {
            pool.scaleUp(scaleFactor);
        }
        
        // In production, this would also:
        // - Request more compute instances
        // - Increase thread pool sizes
        // - Allocate more memory
    }
    
    /**
     * Scale down resources
     */
    private void scaleDown(double scaleFactor) {
        logger.info("Scaling down resources by factor: {}", scaleFactor);
        
        // Decrease connection pool sizes
        for (ConnectionPool pool : connectionPools.values()) {
            pool.scaleDown(scaleFactor);
        }
        
        // In production, this would also:
        // - Release compute instances
        // - Reduce thread pool sizes
        // - Free up memory
    }
    
    /**
     * Get current CPU usage (simplified)
     */
    private double getCurrentCpuUsage() {
        // In production, this would use JMX or system monitoring
        return Math.random() * 0.4 + 0.2; // Simulate 20-60% CPU usage
    }
    
    /**
     * Get resource optimization statistics
     */
    public ResourceOptimizationStats getStats() {
        Map<String, ConnectionPoolStats> poolStats = new HashMap<>();
        
        for (Map.Entry<String, ConnectionPool> entry : connectionPools.entrySet()) {
            poolStats.put(entry.getKey(), entry.getValue().getStats());
        }
        
        ResourceMetrics currentMetrics = collectResourceMetrics();
        
        return new ResourceOptimizationStats(
            poolStats,
            totalActiveConnections.get(),
            totalMemoryAllocated.get(),
            resourceAllocations.size(),
            currentMetrics,
            lastScalingRecommendation
        );
    }
    
    /**
     * Shutdown resource optimization service
     */
    public void shutdown() {
        logger.info("Shutting down resource optimization service");
        
        optimizationExecutor.shutdown();
        scalingExecutor.shutdown();
        
        // Close all connection pools
        for (ConnectionPool pool : connectionPools.values()) {
            pool.close();
        }
        
        try {
            if (!optimizationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                optimizationExecutor.shutdownNow();
            }
            if (!scalingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scalingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            optimizationExecutor.shutdownNow();
            scalingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Supporting classes and enums
    
    public enum QueryComplexity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum ResourcePriority {
        LOW, NORMAL, HIGH, CRITICAL
    }
    
    public enum ScalingAction {
        SCALE_UP, SCALE_DOWN, NO_ACTION
    }
    
    /**
     * Resource allocation for a query
     */
    public static class ResourceAllocation {
        private final String queryId;
        private final QueryComplexity complexity;
        private final Instant createdAt;
        
        private int maxMemoryMB;
        private int maxConnections;
        private int timeoutSeconds;
        private ResourcePriority priority;
        
        public ResourceAllocation(String queryId, QueryComplexity complexity) {
            this.queryId = queryId;
            this.complexity = complexity;
            this.createdAt = Instant.now();
        }
        
        // Getters and setters
        public String getQueryId() { return queryId; }
        public QueryComplexity getComplexity() { return complexity; }
        public Instant getCreatedAt() { return createdAt; }
        public int getMaxMemoryMB() { return maxMemoryMB; }
        public void setMaxMemoryMB(int maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; }
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public ResourcePriority getPriority() { return priority; }
        public void setPriority(ResourcePriority priority) { this.priority = priority; }
    }
    
    /**
     * Connection pool implementation
     */
    public static class ConnectionPool {
        private final String serviceId;
        private final int minSize;
        private final int maxSize;
        private final Duration timeout;
        private final BlockingQueue<Connection> availableConnections;
        private final Set<Connection> allConnections;
        private final AtomicInteger currentSize = new AtomicInteger(0);
        private final AtomicLong totalConnectionsCreated = new AtomicLong(0);
        private final AtomicLong totalConnectionsUsed = new AtomicLong(0);
        
        public ConnectionPool(String serviceId, int minSize, int maxSize, Duration timeout) {
            this.serviceId = serviceId;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.timeout = timeout;
            this.availableConnections = new LinkedBlockingQueue<>();
            this.allConnections = ConcurrentHashMap.newKeySet();
            
            // Initialize with minimum connections
            for (int i = 0; i < minSize; i++) {
                createConnection();
            }
        }
        
        public Connection getConnection() throws InterruptedException {
            Connection connection = availableConnections.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            
            if (connection == null) {
                // Try to create new connection if under max size
                if (currentSize.get() < maxSize) {
                    connection = createConnection();
                } else {
                    throw new RuntimeException("Connection pool exhausted for service: " + serviceId);
                }
            }
            
            if (connection != null) {
                connection.markAsUsed();
                totalConnectionsUsed.incrementAndGet();
            }
            
            return connection;
        }
        
        public void returnConnection(Connection connection) {
            if (connection != null && allConnections.contains(connection)) {
                connection.markAsAvailable();
                availableConnections.offer(connection);
            }
        }
        
        private Connection createConnection() {
            Connection connection = new Connection(serviceId + "_conn_" + totalConnectionsCreated.incrementAndGet());
            allConnections.add(connection);
            availableConnections.offer(connection);
            currentSize.incrementAndGet();
            return connection;
        }
        
        public void expandPool() {
            if (currentSize.get() < maxSize) {
                createConnection();
            }
        }
        
        public void shrinkPool() {
            if (currentSize.get() > minSize && !availableConnections.isEmpty()) {
                Connection connection = availableConnections.poll();
                if (connection != null) {
                    allConnections.remove(connection);
                    connection.close();
                    currentSize.decrementAndGet();
                }
            }
        }
        
        public void reduceSize() {
            while (currentSize.get() > minSize && !availableConnections.isEmpty()) {
                shrinkPool();
            }
        }
        
        public void scaleUp(double factor) {
            int targetSize = Math.min((int) (currentSize.get() * factor), maxSize);
            while (currentSize.get() < targetSize) {
                expandPool();
            }
        }
        
        public void scaleDown(double factor) {
            int targetSize = Math.max((int) (currentSize.get() * factor), minSize);
            while (currentSize.get() > targetSize) {
                shrinkPool();
            }
        }
        
        public void cleanupIdleConnections() {
            // Remove connections that have been idle for too long
            List<Connection> idleConnections = new ArrayList<>();
            
            for (Connection connection : allConnections) {
                if (connection.isIdle() && connection.getIdleTime().compareTo(Duration.ofMinutes(10)) > 0) {
                    idleConnections.add(connection);
                }
            }
            
            for (Connection connection : idleConnections) {
                if (currentSize.get() > minSize) {
                    availableConnections.remove(connection);
                    allConnections.remove(connection);
                    connection.close();
                    currentSize.decrementAndGet();
                }
            }
        }
        
        public ConnectionPoolStats getStats() {
            int available = availableConnections.size();
            int inUse = currentSize.get() - available;
            double utilization = currentSize.get() > 0 ? (double) inUse / currentSize.get() : 0.0;
            
            return new ConnectionPoolStats(serviceId, currentSize.get(), available, inUse, 
                utilization, totalConnectionsCreated.get(), totalConnectionsUsed.get());
        }
        
        public void close() {
            for (Connection connection : allConnections) {
                connection.close();
            }
            allConnections.clear();
            availableConnections.clear();
        }
        
        public int getCurrentSize() { return currentSize.get(); }
        public int getMinSize() { return minSize; }
        public int getMaxSize() { return maxSize; }
    }
    
    /**
     * Connection wrapper
     */
    public static class Connection {
        private final String connectionId;
        private final Instant createdAt;
        private volatile Instant lastUsed;
        private volatile boolean inUse;
        private volatile boolean closed;
        
        public Connection(String connectionId) {
            this.connectionId = connectionId;
            this.createdAt = Instant.now();
            this.lastUsed = createdAt;
            this.inUse = false;
            this.closed = false;
        }
        
        public void markAsUsed() {
            this.inUse = true;
            this.lastUsed = Instant.now();
        }
        
        public void markAsAvailable() {
            this.inUse = false;
            this.lastUsed = Instant.now();
        }
        
        public boolean isIdle() {
            return !inUse && !closed;
        }
        
        public Duration getIdleTime() {
            return Duration.between(lastUsed, Instant.now());
        }
        
        public void close() {
            this.closed = true;
        }
        
        public String getConnectionId() { return connectionId; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastUsed() { return lastUsed; }
        public boolean isInUse() { return inUse; }
        public boolean isClosed() { return closed; }
    }
    
    /**
     * Connection pool statistics
     */
    public static class ConnectionPoolStats {
        private final String serviceId;
        private final int currentSize;
        private final int availableConnections;
        private final int connectionsInUse;
        private final double utilization;
        private final long totalConnectionsCreated;
        private final long totalConnectionsUsed;
        
        public ConnectionPoolStats(String serviceId, int currentSize, int availableConnections,
                                 int connectionsInUse, double utilization, 
                                 long totalConnectionsCreated, long totalConnectionsUsed) {
            this.serviceId = serviceId;
            this.currentSize = currentSize;
            this.availableConnections = availableConnections;
            this.connectionsInUse = connectionsInUse;
            this.utilization = utilization;
            this.totalConnectionsCreated = totalConnectionsCreated;
            this.totalConnectionsUsed = totalConnectionsUsed;
        }
        
        public String getServiceId() { return serviceId; }
        public int getCurrentSize() { return currentSize; }
        public int getAvailableConnections() { return availableConnections; }
        public int getConnectionsInUse() { return connectionsInUse; }
        public double getUtilization() { return utilization; }
        public long getTotalConnectionsCreated() { return totalConnectionsCreated; }
        public long getTotalConnectionsUsed() { return totalConnectionsUsed; }
    }
    
    /**
     * Resource metrics snapshot
     */
    public static class ResourceMetrics {
        private final Instant timestamp;
        private final double cpuUsage;
        private final double memoryUsage;
        private final int activeConnections;
        private final int activeQueries;
        
        public ResourceMetrics(Instant timestamp, double cpuUsage, double memoryUsage,
                             int activeConnections, int activeQueries) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.activeConnections = activeConnections;
            this.activeQueries = activeQueries;
        }
        
        public Instant getTimestamp() { return timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public int getActiveConnections() { return activeConnections; }
        public int getActiveQueries() { return activeQueries; }
    }
    
    /**
     * Scaling recommendation
     */
    public static class ScalingRecommendation {
        private final ScalingAction action;
        private final String reason;
        private final double scaleFactor;
        private final Instant createdAt;
        
        public ScalingRecommendation(ScalingAction action, String reason, double scaleFactor) {
            this.action = action;
            this.reason = reason;
            this.scaleFactor = scaleFactor;
            this.createdAt = Instant.now();
        }
        
        public ScalingAction getAction() { return action; }
        public String getReason() { return reason; }
        public double getScaleFactor() { return scaleFactor; }
        public Instant getCreatedAt() { return createdAt; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            ScalingRecommendation that = (ScalingRecommendation) obj;
            return Double.compare(that.scaleFactor, scaleFactor) == 0 &&
                   action == that.action &&
                   Objects.equals(reason, that.reason);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(action, reason, scaleFactor);
        }
        
        @Override
        public String toString() {
            return String.format("ScalingRecommendation{action=%s, reason='%s', scaleFactor=%.2f}", 
                action, reason, scaleFactor);
        }
    }
    
    /**
     * Resource optimization statistics
     */
    public static class ResourceOptimizationStats {
        private final Map<String, ConnectionPoolStats> connectionPoolStats;
        private final int totalActiveConnections;
        private final long totalMemoryAllocatedMB;
        private final int activeResourceAllocations;
        private final ResourceMetrics currentMetrics;
        private final ScalingRecommendation lastScalingRecommendation;
        
        public ResourceOptimizationStats(Map<String, ConnectionPoolStats> connectionPoolStats,
                                       int totalActiveConnections, long totalMemoryAllocatedMB,
                                       int activeResourceAllocations, ResourceMetrics currentMetrics,
                                       ScalingRecommendation lastScalingRecommendation) {
            this.connectionPoolStats = new HashMap<>(connectionPoolStats);
            this.totalActiveConnections = totalActiveConnections;
            this.totalMemoryAllocatedMB = totalMemoryAllocatedMB;
            this.activeResourceAllocations = activeResourceAllocations;
            this.currentMetrics = currentMetrics;
            this.lastScalingRecommendation = lastScalingRecommendation;
        }
        
        public Map<String, ConnectionPoolStats> getConnectionPoolStats() { return connectionPoolStats; }
        public int getTotalActiveConnections() { return totalActiveConnections; }
        public long getTotalMemoryAllocatedMB() { return totalMemoryAllocatedMB; }
        public int getActiveResourceAllocations() { return activeResourceAllocations; }
        public ResourceMetrics getCurrentMetrics() { return currentMetrics; }
        public ScalingRecommendation getLastScalingRecommendation() { return lastScalingRecommendation; }
        
        public double getOverallConnectionUtilization() {
            if (connectionPoolStats.isEmpty()) return 0.0;
            
            return connectionPoolStats.values().stream()
                    .mapToDouble(ConnectionPoolStats::getUtilization)
                    .average()
                    .orElse(0.0);
        }
        
        public int getTotalConnectionPoolSize() {
            return connectionPoolStats.values().stream()
                    .mapToInt(ConnectionPoolStats::getCurrentSize)
                    .sum();
        }
    }
}