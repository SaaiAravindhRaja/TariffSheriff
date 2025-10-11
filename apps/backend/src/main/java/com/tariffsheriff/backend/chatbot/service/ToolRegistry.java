package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;

import java.util.List;

/**
 * Registry for managing available tools and their execution
 */
public interface ToolRegistry {
    
    /**
     * Get all available tool definitions
     */
    List<ToolDefinition> getAvailableTools();
    
    /**
     * Execute a tool call
     */
    ToolResult executeToolCall(ToolCall toolCall);
    
    /**
     * Check if a tool is available
     */
    boolean isToolAvailable(String toolName);
}