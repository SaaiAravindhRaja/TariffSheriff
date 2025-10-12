package com.tariffsheriff.backend.chatbot.controller;

import com.tariffsheriff.backend.chatbot.service.ToolHealthMonitor;
import com.tariffsheriff.backend.chatbot.service.ToolRegistryImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for tool management and monitoring
 */
@RestController
@RequestMapping("/api/chatbot/tools")
@Tag(name = "Tool Management", description = "Endpoints for managing and monitoring chatbot tools")
public class ToolManagementController {
    
    @Autowired
    private ToolRegistryImpl toolRegistry;
    
    @Autowired
    private ToolHealthMonitor toolHealthMonitor;
    
    /**
     * Get information about all tools including configuration and health status
     */
    @GetMapping("/info")
    @Operation(summary = "Get tool information", description = "Returns configuration and health information for all tools")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getToolInfo() {
        Map<String, Object> toolInfo = toolRegistry.getToolInfo();
        return ResponseEntity.ok(toolInfo);
    }
    
    /**
     * Get health status for all tools
     */
    @GetMapping("/health")
    @Operation(summary = "Get tool health status", description = "Returns health status for all tools")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, ToolHealthMonitor.EnhancedToolHealthStatus>> getToolHealth() {
        Map<String, ToolHealthMonitor.EnhancedToolHealthStatus> healthStatus = toolHealthMonitor.getAllToolHealth();
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * Get health status for a specific tool
     */
    @GetMapping("/health/{toolName}")
    @Operation(summary = "Get specific tool health", description = "Returns health status for a specific tool")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ToolHealthMonitor.EnhancedToolHealthStatus> getToolHealth(@PathVariable String toolName) {
        ToolHealthMonitor.EnhancedToolHealthStatus healthStatus = toolHealthMonitor.getToolHealth(toolName);
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * Get list of available tools
     */
    @GetMapping("/available")
    @Operation(summary = "Get available tools", description = "Returns list of currently available tools")
    public ResponseEntity<?> getAvailableTools() {
        return ResponseEntity.ok(toolRegistry.getAvailableTools());
    }
}