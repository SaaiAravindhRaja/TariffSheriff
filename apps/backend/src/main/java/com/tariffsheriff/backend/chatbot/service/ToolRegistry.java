package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import com.tariffsheriff.backend.chatbot.service.tools.ChatbotTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Simplified tool registry for managing chatbot tools
 * Spring auto-wires all ChatbotTool implementations
 */
@Service
public class ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final List<ChatbotTool> tools;
    
    @Autowired
    public ToolRegistry(List<ChatbotTool> tools) {
        this.tools = tools;
        logger.info("ToolRegistry initialized with {} tools: {}", 
            tools.size(), 
            tools.stream().map(ChatbotTool::getName).collect(Collectors.joining(", ")));
    }
    
    /**
     * Get all available tool definitions
     */
    public List<ToolDefinition> getAvailableTools() {
        return tools.stream()
                .map(ChatbotTool::getDefinition)
                .filter(ToolDefinition::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * Execute a tool call
     */
    public ToolResult executeToolCall(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        String toolName = toolCall.getName();
        
        if (toolName == null || toolName.isEmpty()) {
            logger.error("Invalid tool call: missing tool name");
            return ToolResult.error("unknown", "Invalid tool call: missing tool name");
        }
        
        try {
            ChatbotTool tool = tools.stream()
                    .filter(t -> t.getName().equals(toolName))
                    .findFirst()
                    .orElse(null);
            
            if (tool == null) {
                logger.warn("Tool not found: {}", toolName);
                return ToolResult.error(toolName, "Tool '" + toolName + "' is not available");
            }
            
            logger.debug("Executing tool: {} with parameters: {}", toolName, toolCall.getArguments());
            ToolResult result = tool.execute(toolCall);
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            result.setToolName(toolName);
            
            if (result.isSuccess()) {
                logger.debug("Tool {} executed successfully in {}ms", toolName, executionTime);
            } else {
                logger.warn("Tool {} failed: {}", toolName, result.getError());
            }
            
            return result;
            
        } catch (ToolExecutionException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Tool {} execution failed: {}", toolName, e.getMessage(), e);
            ToolResult errorResult = ToolResult.error(toolName, e.getMessage());
            errorResult.setExecutionTimeMs(executionTime);
            return errorResult;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error executing tool {}: {}", toolName, e.getMessage(), e);
            ToolResult errorResult = ToolResult.error(toolName, 
                "An unexpected error occurred while executing the tool");
            errorResult.setExecutionTimeMs(executionTime);
            return errorResult;
        }
    }
    
    /**
     * Check if a tool is available
     */
    public boolean isToolAvailable(String toolName) {
        return tools.stream()
                .anyMatch(t -> t.getName().equals(toolName) && t.getDefinition().isEnabled());
    }
}
