package com.tariffsheriff.backend.chatbot.controller;

import com.tariffsheriff.backend.chatbot.service.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simplified REST controller for tool management
 */
@RestController
@RequestMapping("/api/chatbot/tools")
@Tag(name = "Tool Management", description = "Endpoints for managing chatbot tools")
public class ToolManagementController {
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    /**
     * Get list of available tools
     */
    @GetMapping("/available")
    @Operation(summary = "Get available tools", description = "Returns list of currently available tools")
    public ResponseEntity<?> getAvailableTools() {
        return ResponseEntity.ok(toolRegistry.getAvailableTools());
    }
}