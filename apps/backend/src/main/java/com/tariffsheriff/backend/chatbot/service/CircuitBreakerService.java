package com.tariffsheriff.backend.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Circuit breaker service for managing external dependencies with automatic recovery,
 * retry logic with exponential backoff, and fallback routing capabilities
 */
@Service
public class CircuitBreakerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerService.class);
    
    // Circuit breakers for different services
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    // Default configuration
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final long DEFAULT_RETRY_DELAY_MS = 1000; // 1 second
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    /**
     * Execute a call through circuit breaker with default configuration
     */
    public <T> T executeWithCircuitBreaker(String serviceName, Supplier<T> operation) {
        return executeWithCircuitBreaker(serviceName, operation, null, null);
    }
    
    /**
     * Execute a call through circuit breaker with fallback
     */
    public <T> T executeWithCircuitBreaker(String serviceName, Supplier<T> operation, Supplier<T> fallback) {
        return executeWithCircuitBreaker(serviceName, operation, fallback, null);
    }
    
    /**
     * Execute a call through circuit breaker with custom configuration
     */
    public <T> T executeWithCircuitBreaker(String serviceName, Supplier<T> operation, 
                                         Supplier<T> fallback, CircuitBreakerConfig config) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName, config);
        
        try {
            return circuitBreaker.execute(operation, fallback);
        } catch (CircuitBreakerOpenException e) {
            logger.warn("Circuit breaker is OPEN for service {}: {}", serviceName, e.getMessage());
            if (fallback != null) {
                logger.info("Executing fallback for service {}", serviceName);
                return fallback.get();
            }
            throw e;
        } catch (Exception e) {
            logger.error("Error executing operation for service {}: {}", serviceName, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get circuit breaker status for a service
     */
    public CircuitBreakerStatus getCircuitBreakerStatus(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
        return circuitBreaker != null ? circuitBreaker.getStatus() : null;
    }
    
    /**
     * Get all circuit breaker statuses
     */
    public Map<String, CircuitBreakerStatus> getAllCircuitBreakerStatuses() {
        Map<String, CircuitBreakerStatus> statuses = new ConcurrentHashMap<>();
        for (Map.Entry<String, CircuitBreaker> entry : circuitBreakers.entrySet()) {
            statuses.put(entry.getKey(), entry.getValue().getStatus());
        }
        return statuses;
    }
    
    /**
     * Manually reset a circuit breaker
     */
    public void resetCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
        if (circuitBreaker != null) {
            circuitBreaker.reset();
            logger.info("Circuit breaker manually reset for service {}", serviceName);
        }
    }
    
    /**
     * Manually open a circuit breaker
     */
    public void openCircuitBreaker(String serviceName, String reason) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
        if (circuitBreaker != null) {
            circuitBreaker.forceOpen(reason);
            logger.warn("Circuit breaker manually opened for service {}: {}", serviceName, reason);
        }
    }
    
    /**
     * Get or create circuit breaker for a service
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String serviceName, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(serviceName, k -> {
            CircuitBreakerConfig finalConfig = config != null ? config : createDefaultConfig();
            logger.info("Creating circuit breaker for service {} with config: {}", serviceName, finalConfig);
            return new CircuitBreaker(serviceName, finalConfig);
        });
    }
    
    /**
     * Create default circuit breaker configuration
     */
    private CircuitBreakerConfig createDefaultConfig() {
        return new CircuitBreakerConfig(
                DEFAULT_FAILURE_THRESHOLD,
                DEFAULT_TIMEOUT_MS,
                DEFAULT_RETRY_DELAY_MS,
                DEFAULT_MAX_RETRIES,
                DEFAULT_BACKOFF_MULTIPLIER
        );
    }
    
    /**
     * Check if circuit breaker is open for a service
     */
    public boolean isCircuitOpen(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
        if (circuitBreaker == null) {
            return false;
        }
        return circuitBreaker.state == CircuitBreakerState.OPEN;
    }
    
    /**
     * Record a success for a service
     */
    public void recordSuccess(String serviceName) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName, null);
        circuitBreaker.recordSuccess();
    }
    
    /**
     * Record a failure for a service
     */
    public void recordFailure(String serviceName) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName, null);
        circuitBreaker.recordFailure(new RuntimeException("Service call failed"));
    }
    
    /**
     * Circuit breaker implementation with state management
     */
    private static class CircuitBreaker {
        private final String serviceName;
        private final CircuitBreakerConfig config;
        
        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private volatile LocalDateTime lastFailureTime;
        private volatile LocalDateTime lastSuccessTime;
        private volatile LocalDateTime stateChangeTime = LocalDateTime.now();
        private volatile String lastError;
        private volatile boolean manuallyOpened = false;
        private volatile String manualOpenReason;
        
        CircuitBreaker(String serviceName, CircuitBreakerConfig config) {
            this.serviceName = serviceName;
            this.config = config;
        }
        
        <T> T execute(Supplier<T> operation, Supplier<T> fallback) {
            totalExecutions.incrementAndGet();
            
            // Check if circuit breaker should allow the call
            if (!canExecute()) {
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN for service " + serviceName);
            }
            
            // Execute with retry logic
            return executeWithRetry(operation, fallback);
        }
        
        private boolean canExecute() {
            if (manuallyOpened) {
                return false;
            }
            
            switch (state) {
                case CLOSED:
                    return true;
                    
                case OPEN:
                    // Check if timeout period has passed
                    if (lastFailureTime != null && 
                        ChronoUnit.MILLIS.between(lastFailureTime, LocalDateTime.now()) >= config.getTimeoutMs()) {
                        // Transition to HALF_OPEN
                        changeState(CircuitBreakerState.HALF_OPEN);
                        return true;
                    }
                    return false;
                    
                case HALF_OPEN:
                    return true;
                    
                default:
                    return false;
            }
        }
        
        private <T> T executeWithRetry(Supplier<T> operation, Supplier<T> fallback) {
            int attempts = 0;
            long delay = config.getRetryDelayMs();
            Exception lastException = null;
            
            while (attempts <= config.getMaxRetries()) {
                try {
                    T result = operation.get();
                    onSuccess();
                    return result;
                    
                } catch (Exception e) {
                    lastException = e;
                    attempts++;
                    
                    if (attempts <= config.getMaxRetries()) {
                        logger.debug("Attempt {} failed for service {}, retrying in {}ms: {}", 
                                attempts, serviceName, delay, e.getMessage());
                        
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        
                        // Exponential backoff
                        delay = (long) (delay * config.getBackoffMultiplier());
                    }
                }
            }
            
            // All retries failed
            onFailure(lastException);
            
            if (fallback != null) {
                logger.info("All retries failed for service {}, executing fallback", serviceName);
                return fallback.get();
            }
            
            throw new RuntimeException("All retries failed for service " + serviceName, lastException);
        }
        
        private void onSuccess() {
            successCount.incrementAndGet();
            lastSuccessTime = LocalDateTime.now();
            
            switch (state) {
                case HALF_OPEN:
                    // Successful call in HALF_OPEN state, close the circuit
                    changeState(CircuitBreakerState.CLOSED);
                    failureCount.set(0);
                    logger.info("Circuit breaker CLOSED for service {} after successful call", serviceName);
                    break;
                    
                case CLOSED:
                    // Reset failure count on success
                    failureCount.set(0);
                    break;
            }
        }
        
        private void onFailure(Exception exception) {
            int failures = failureCount.incrementAndGet();
            lastFailureTime = LocalDateTime.now();
            lastError = exception != null ? exception.getMessage() : "Unknown error";
            
            switch (state) {
                case CLOSED:
                    if (failures >= config.getFailureThreshold()) {
                        changeState(CircuitBreakerState.OPEN);
                        logger.warn("Circuit breaker OPENED for service {} after {} failures", 
                                serviceName, failures);
                    }
                    break;
                    
                case HALF_OPEN:
                    // Failure in HALF_OPEN state, go back to OPEN
                    changeState(CircuitBreakerState.OPEN);
                    logger.warn("Circuit breaker returned to OPEN state for service {} after failure in HALF_OPEN", 
                            serviceName);
                    break;
            }
        }
        
        private void changeState(CircuitBreakerState newState) {
            CircuitBreakerState oldState = this.state;
            this.state = newState;
            this.stateChangeTime = LocalDateTime.now();
            
            logger.info("Circuit breaker state changed for service {}: {} -> {}", 
                    serviceName, oldState, newState);
        }
        
        void reset() {
            changeState(CircuitBreakerState.CLOSED);
            failureCount.set(0);
            successCount.set(0);
            lastFailureTime = null;
            lastSuccessTime = null;
            lastError = null;
            manuallyOpened = false;
            manualOpenReason = null;
            logger.info("Circuit breaker RESET for service {}", serviceName);
        }
        
        void recordSuccess() {
            onSuccess();
        }
        
        void recordFailure(Exception exception) {
            onFailure(exception);
        }
        
        void forceOpen(String reason) {
            changeState(CircuitBreakerState.OPEN);
            manuallyOpened = true;
            manualOpenReason = reason;
        }
        
        CircuitBreakerStatus getStatus() {
            return new CircuitBreakerStatus(
                    serviceName,
                    state,
                    failureCount.get(),
                    successCount.get(),
                    totalExecutions.get(),
                    lastFailureTime,
                    lastSuccessTime,
                    stateChangeTime,
                    lastError,
                    manuallyOpened,
                    manualOpenReason,
                    config
            );
        }
    }
    
    /**
     * Circuit breaker states
     */
    public enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Failing fast, not allowing calls
        HALF_OPEN  // Testing if service has recovered
    }
    
    /**
     * Circuit breaker configuration
     */
    public static class CircuitBreakerConfig {
        private final int failureThreshold;
        private final long timeoutMs;
        private final long retryDelayMs;
        private final int maxRetries;
        private final double backoffMultiplier;
        
        public CircuitBreakerConfig(int failureThreshold, long timeoutMs, long retryDelayMs, 
                                  int maxRetries, double backoffMultiplier) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
            this.retryDelayMs = retryDelayMs;
            this.maxRetries = maxRetries;
            this.backoffMultiplier = backoffMultiplier;
        }
        
        public int getFailureThreshold() { return failureThreshold; }
        public long getTimeoutMs() { return timeoutMs; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public int getMaxRetries() { return maxRetries; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        
        @Override
        public String toString() {
            return String.format("CircuitBreakerConfig{failureThreshold=%d, timeoutMs=%d, retryDelayMs=%d, maxRetries=%d, backoffMultiplier=%.1f}",
                    failureThreshold, timeoutMs, retryDelayMs, maxRetries, backoffMultiplier);
        }
    }
    
    /**
     * Circuit breaker status information
     */
    public static class CircuitBreakerStatus {
        private final String serviceName;
        private final CircuitBreakerState state;
        private final int failureCount;
        private final int successCount;
        private final long totalExecutions;
        private final LocalDateTime lastFailureTime;
        private final LocalDateTime lastSuccessTime;
        private final LocalDateTime stateChangeTime;
        private final String lastError;
        private final boolean manuallyOpened;
        private final String manualOpenReason;
        private final CircuitBreakerConfig config;
        
        public CircuitBreakerStatus(String serviceName, CircuitBreakerState state, int failureCount, 
                                  int successCount, long totalExecutions, LocalDateTime lastFailureTime,
                                  LocalDateTime lastSuccessTime, LocalDateTime stateChangeTime, 
                                  String lastError, boolean manuallyOpened, String manualOpenReason,
                                  CircuitBreakerConfig config) {
            this.serviceName = serviceName;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.totalExecutions = totalExecutions;
            this.lastFailureTime = lastFailureTime;
            this.lastSuccessTime = lastSuccessTime;
            this.stateChangeTime = stateChangeTime;
            this.lastError = lastError;
            this.manuallyOpened = manuallyOpened;
            this.manualOpenReason = manualOpenReason;
            this.config = config;
        }
        
        // Getters
        public String getServiceName() { return serviceName; }
        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public long getTotalExecutions() { return totalExecutions; }
        public LocalDateTime getLastFailureTime() { return lastFailureTime; }
        public LocalDateTime getLastSuccessTime() { return lastSuccessTime; }
        public LocalDateTime getStateChangeTime() { return stateChangeTime; }
        public String getLastError() { return lastError; }
        public boolean isManuallyOpened() { return manuallyOpened; }
        public String getManualOpenReason() { return manualOpenReason; }
        public CircuitBreakerConfig getConfig() { return config; }
        
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successCount / totalExecutions : 0.0;
        }
        
        public boolean isHealthy() {
            return state == CircuitBreakerState.CLOSED && !manuallyOpened;
        }
        
        public String getStatusDescription() {
            if (manuallyOpened) {
                return "Manually opened: " + manualOpenReason;
            }
            
            switch (state) {
                case CLOSED:
                    return "Operating normally";
                case OPEN:
                    return "Circuit open - failing fast";
                case HALF_OPEN:
                    return "Testing service recovery";
                default:
                    return "Unknown state";
            }
        }
    }
    
    /**
     * Exception thrown when circuit breaker is open
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}