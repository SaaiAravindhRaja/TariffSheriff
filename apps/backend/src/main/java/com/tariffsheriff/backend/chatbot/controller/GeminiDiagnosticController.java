package com.tariffsheriff.backend.chatbot.controller;

import com.tariffsheriff.backend.chatbot.config.GeminiProperties;
import com.tariffsheriff.backend.chatbot.dto.DiagnosticResult;
import com.tariffsheriff.backend.chatbot.service.LlmClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic controller for Gemini API integration testing.
 * Provides endpoints to verify API configuration, test connectivity, and check health.
 * 
 * These endpoints are restricted to authenticated users for security.
 */
@RestController
@RequestMapping("/api/chatbot/diagnostics/gemini")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000", "http://127.0.0.1:8080"})
public class GeminiDiagnosticController {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiDiagnosticController.class);
    
    private final GeminiProperties geminiProperties;
    private final LlmClient llmClient;
    
    // Track last health check for caching
    private volatile HealthCheckResult lastHealthCheck;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 30000; // 30 seconds
    
    /**
     * Get Gemini API configuration status (without exposing the API key).
     * 
     * @return Configuration details including model, base URL, and whether API key is configured
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfig() {
        logger.info("[GEMINI-DIAGNOSTIC] Configuration check requested");
        
        Map<String, Object> config = new HashMap<>();
        
        try {
            boolean apiKeyConfigured = geminiProperties.getApiKey() != null 
                    && !geminiProperties.getApiKey().isBlank()
                    && !geminiProperties.getApiKey().equals("your-api-key-here");
            
            config.put("configured", apiKeyConfigured);
            config.put("model", geminiProperties.getModel());
            config.put("baseUrl", geminiProperties.getBaseUrl());
            config.put("timeout", geminiProperties.getTimeoutMs());
            config.put("maxTokens", geminiProperties.getMaxTokens());
            config.put("temperature", geminiProperties.getTemperature());
            
            // Add API key format validation (without exposing the key)
            if (apiKeyConfigured) {
                String apiKey = geminiProperties.getApiKey();
                boolean validFormat = apiKey.startsWith("AIza");
                config.put("apiKeyFormatValid", validFormat);
                config.put("apiKeyLength", apiKey.length());
                
                if (!validFormat) {
                    logger.warn("[GEMINI-DIAGNOSTIC] API key format may be invalid (expected to start with 'AIza')");
                }
            } else {
                config.put("apiKeyFormatValid", false);
                config.put("apiKeyLength", 0);
                logger.warn("[GEMINI-DIAGNOSTIC] API key is not configured");
            }
            
            logger.info("[GEMINI-DIAGNOSTIC] Configuration: configured={}, model={}, validFormat={}", 
                    apiKeyConfigured, geminiProperties.getModel(), config.get("apiKeyFormatValid"));
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            logger.error("[GEMINI-DIAGNOSTIC] Error retrieving configuration", e);
            config.put("error", "Failed to retrieve configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(config);
        }
    }
    
    /**
     * Test Gemini API connectivity with a simple request.
     * 
     * @param testRequest Request containing the test query and optional type
     * @return Diagnostic result with timing, raw request/response, and success status
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DiagnosticResult> testConnectivity(@RequestBody(required = false) TestRequest testRequest) {
        String query = testRequest != null && testRequest.getQuery() != null 
                ? testRequest.getQuery() 
                : "Hello";
        
        logger.info("[GEMINI-DIAGNOSTIC] Connectivity test requested with query: {}", query);
        
        try {
            DiagnosticResult result = llmClient.testConnectivity(query);
            
            if (result.isSuccess()) {
                logger.info("[GEMINI-DIAGNOSTIC] Connectivity test PASSED - timing={}ms", result.getTimingMs());
                return ResponseEntity.ok(result);
            } else {
                logger.error("[GEMINI-DIAGNOSTIC] Connectivity test FAILED - error={}, phase={}", 
                        result.getError(), result.getPhase());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
            }
            
        } catch (Exception e) {
            logger.error("[GEMINI-DIAGNOSTIC] Unexpected error during connectivity test", e);
            
            DiagnosticResult errorResult = DiagnosticResult.builder()
                    .success(false)
                    .phase("test_execution")
                    .error("Unexpected error: " + e.getMessage())
                    .addMetadata("exceptionType", e.getClass().getSimpleName())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * Get health status of Gemini API integration.
     * Performs a lightweight connectivity check and caches the result for 30 seconds.
     * 
     * @return Health status with connectivity information
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<HealthCheckResult> getHealth() {
        logger.info("[GEMINI-DIAGNOSTIC] Health check requested");
        
        try {
            long now = System.currentTimeMillis();
            
            // Return cached result if recent
            if (lastHealthCheck != null && (now - lastHealthCheckTime) < HEALTH_CHECK_CACHE_MS) {
                logger.debug("[GEMINI-DIAGNOSTIC] Returning cached health check result");
                return ResponseEntity.ok(lastHealthCheck);
            }
            
            // Perform new health check
            logger.info("[GEMINI-DIAGNOSTIC] Performing new health check");
            DiagnosticResult testResult = llmClient.testConnectivity("Health check");
            
            HealthCheckResult healthResult = new HealthCheckResult();
            healthResult.setHealthy(testResult.isSuccess());
            healthResult.setMessage(testResult.isSuccess() 
                    ? "Gemini API is reachable and responding" 
                    : "Gemini API connectivity issue: " + testResult.getError());
            healthResult.setLastTestTime(Instant.now().toString());
            healthResult.setResponseTimeMs(testResult.getTimingMs());
            healthResult.setApiKeyConfigured(geminiProperties.getApiKey() != null 
                    && !geminiProperties.getApiKey().isBlank());
            healthResult.setModel(geminiProperties.getModel());
            
            if (!testResult.isSuccess()) {
                healthResult.setError(testResult.getError());
                healthResult.setPhase(testResult.getPhase());
            }
            
            // Cache the result
            lastHealthCheck = healthResult;
            lastHealthCheckTime = now;
            
            logger.info("[GEMINI-DIAGNOSTIC] Health check complete: healthy={}, timing={}ms", 
                    healthResult.isHealthy(), healthResult.getResponseTimeMs());
            
            if (healthResult.isHealthy()) {
                return ResponseEntity.ok(healthResult);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthResult);
            }
            
        } catch (Exception e) {
            logger.error("[GEMINI-DIAGNOSTIC] Error during health check", e);
            
            HealthCheckResult errorResult = new HealthCheckResult();
            errorResult.setHealthy(false);
            errorResult.setMessage("Health check failed");
            errorResult.setError("Unexpected error: " + e.getMessage());
            errorResult.setLastTestTime(Instant.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * Request DTO for test endpoint
     */
    public static class TestRequest {
        private String query;
        private String type; // "conversational" or "tool-selection" (for future use)
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
    
    /**
     * Response DTO for health endpoint
     */
    public static class HealthCheckResult {
        private boolean healthy;
        private String message;
        private String lastTestTime;
        private long responseTimeMs;
        private boolean apiKeyConfigured;
        private String model;
        private String error;
        private String phase;
        
        // Getters and setters
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getLastTestTime() {
            return lastTestTime;
        }
        
        public void setLastTestTime(String lastTestTime) {
            this.lastTestTime = lastTestTime;
        }
        
        public long getResponseTimeMs() {
            return responseTimeMs;
        }
        
        public void setResponseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }
        
        public boolean isApiKeyConfigured() {
            return apiKeyConfigured;
        }
        
        public void setApiKeyConfigured(boolean apiKeyConfigured) {
            this.apiKeyConfigured = apiKeyConfigured;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getPhase() {
            return phase;
        }
        
        public void setPhase(String phase) {
            this.phase = phase;
        }
    }
}
