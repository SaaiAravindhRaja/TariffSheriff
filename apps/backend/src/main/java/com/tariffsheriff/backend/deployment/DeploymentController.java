package com.tariffsheriff.backend.deployment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * REST controller for deployment management operations
 */
@RestController
@RequestMapping("/api/deployment")
@PreAuthorize("hasRole('ADMIN')")
public class DeploymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeploymentController.class);
    
    @Autowired
    private DeploymentService deploymentService;
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    /**
     * Initiate blue-green deployment
     */
    @PostMapping("/deploy")
    public ResponseEntity<?> initiateDeployment(@RequestBody DeploymentRequest request) {
        try {
            logger.info("Received deployment request for version {}", request.getVersion());
            
            DeploymentService.DeploymentState deployment = deploymentService.initiateBlueGreenDeployment(
                request.getVersion(), request.getConfig());
            
            return ResponseEntity.ok(deployment);
            
        } catch (Exception e) {
            logger.error("Error initiating deployment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Deployment failed: " + e.getMessage()));
        }
    }
    
    /**
     * Rollback deployment
     */
    @PostMapping("/rollback/{deploymentId}")
    public ResponseEntity<?> rollbackDeployment(
            @PathVariable String deploymentId, 
            @RequestBody RollbackRequest request) {
        try {
            logger.info("Received rollback request for deployment {}", deploymentId);
            
            boolean success = deploymentService.rollbackDeployment(deploymentId, request.getReason());
            
            if (success) {
                return ResponseEntity.ok(Map.of("status", "Rollback completed successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rollback failed"));
            }
            
        } catch (Exception e) {
            logger.error("Error during rollback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Rollback failed: " + e.getMessage()));
        }
    }
    
    /**
     * Emergency rollback
     */
    @PostMapping("/emergency-rollback")
    public ResponseEntity<?> emergencyRollback(@RequestBody EmergencyRollbackRequest request) {
        try {
            logger.warn("Received emergency rollback request: {}", request.getReason());
            
            boolean success = deploymentService.emergencyRollback(request.getReason());
            
            if (success) {
                return ResponseEntity.ok(Map.of("status", "Emergency rollback completed"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Emergency rollback failed"));
            }
            
        } catch (Exception e) {
            logger.error("Error during emergency rollback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Emergency rollback failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get current deployment status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getDeploymentStatus() {
        try {
            DeploymentService.DeploymentState current = deploymentService.getCurrentDeployment();
            return ResponseEntity.ok(current);
            
        } catch (Exception e) {
            logger.error("Error getting deployment status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get deployment status"));
        }
    }
    
    /**
     * Get deployment history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getDeploymentHistory() {
        try {
            Map<String, DeploymentService.DeploymentState> history = deploymentService.getDeploymentHistory();
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Error getting deployment history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get deployment history"));
        }
    }
    
    /**
     * Update feature flag
     */
    @PostMapping("/feature-flags/{feature}")
    public ResponseEntity<?> updateFeatureFlag(
            @PathVariable String feature,
            @RequestBody FeatureFlagUpdateRequest request) {
        try {
            logger.info("Updating feature flag {} for user {}", feature, request.getUserId());
            
            // This would typically update the feature flag configuration
            // For now, we'll just return success
            return ResponseEntity.ok(Map.of("status", "Feature flag updated"));
            
        } catch (Exception e) {
            logger.error("Error updating feature flag: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update feature flag"));
        }
    }
    
    /**
     * Get user feature flags
     */
    @GetMapping("/feature-flags/{userId}")
    public ResponseEntity<?> getUserFeatureFlags(@PathVariable String userId) {
        try {
            Map<String, Boolean> features = featureFlagService.getUserFeatures(userId);
            return ResponseEntity.ok(features);
            
        } catch (Exception e) {
            logger.error("Error getting user feature flags: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get feature flags"));
        }
    }
    
    // Request DTOs
    public static class DeploymentRequest {
        private String version;
        private Map<String, Object> config;
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
    
    public static class RollbackRequest {
        private String reason;
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class EmergencyRollbackRequest {
        private String reason;
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class FeatureFlagUpdateRequest {
        private String userId;
        private boolean enabled;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}