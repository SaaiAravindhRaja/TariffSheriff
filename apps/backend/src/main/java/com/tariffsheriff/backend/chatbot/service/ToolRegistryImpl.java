package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.config.ToolProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import com.tariffsheriff.backend.chatbot.service.tools.ChatbotTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified implementation of tool registry for managing available tools
 */
@Service
public class ToolRegistryImpl implements ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistryImpl.class);
    
    private final Map<String, ToolDefinition> toolDefinitions;
    private final Map<String, ToolExecutor> toolExecutors;
    
    @Autowired
    private ToolProperties toolProperties;
    
    @Autowired(required = false)
    private List<ChatbotTool> chatbotTools;
    
    public ToolRegistryImpl() {
        this.toolDefinitions = new HashMap<>();
        this.toolExecutors = new HashMap<>();
    }
    
    @jakarta.annotation.PostConstruct
    public void initializeTools() {
        if (chatbotTools != null) {
            for (ChatbotTool tool : chatbotTools) {
                try {
                    ToolDefinition definition = tool.getDefinition();
                    ToolExecutor executor = tool::execute;
                    registerTool(definition, executor);
                    logger.info("Registered tool: {}", tool.getName());
                } catch (Exception e) {
                    logger.error("Failed to register tool: {}", tool.getName(), e);
                }
            }
            logger.info("Tool registration completed. {} tools available.", toolDefinitions.size());
        } else {
            logger.warn("No ChatbotTool beans found for registration.");
        }
    }
    
    @Override
    public List<ToolDefinition> getAvailableTools() {
        // Return all enabled tools
        List<ToolDefinition> availableTools = new ArrayList<>();
        for (ToolDefinition tool : toolDefinitions.values()) {
            if (isToolAvailable(tool.getName())) {
                availableTools.add(tool);
            }
        }
        return availableTools;
    }
    
    @Override
    public ToolResult executeToolCall(ToolCall toolCall) {
        String toolName = toolCall.getName();
        long startTime = System.currentTimeMillis();
        
        // Validate tool parameters
        if (toolCall == null || toolName == null || toolName.isEmpty()) {
            logger.error("Invalid tool call: toolCall or toolName is null/empty");
            return ToolResult.error("unknown", "Invalid tool call: missing tool name");
        }
        
        // Check if tool is available
        if (!isToolAvailable(toolName)) {
            logger.warn("Attempted to execute unavailable tool: {}", toolName);
            return ToolResult.error(toolName, "Tool '" + toolName + "' is not available. Please check the tool name and try again.");
        }
        
        // Get tool executor
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            logger.error("No executor found for tool: {}", toolName);
            return ToolResult.error(toolName, "Tool '" + toolName + "' is not properly configured. Please contact support.");
        }
        
        // Execute tool
        try {
            logger.debug("Executing tool: {} with parameters: {}", toolName, toolCall.getArguments());
            ToolResult result = executor.execute(toolCall);
            
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
            logger.error("Tool {} execution failed with parameters: {}, error: {}", 
                    toolName, toolCall.getArguments(), e.getMessage(), e);
            ToolResult errorResult = ToolResult.error(toolName, e.getMessage());
            errorResult.setExecutionTimeMs(executionTime);
            return errorResult;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error executing tool {} with parameters: {}, error: {}", 
                    toolName, toolCall.getArguments(), e.getMessage(), e);
            ToolResult errorResult = ToolResult.error(toolName, "An unexpected error occurred while executing the tool. Please try again.");
            errorResult.setExecutionTimeMs(executionTime);
            return errorResult;
        }
    }
    
    @Override
    public boolean isToolAvailable(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            return false;
        }
        
        ToolDefinition definition = toolDefinitions.get(toolName);
        if (definition == null) {
            return false;
        }
        
        // Check if tool is enabled in configuration
        ToolProperties.ToolConfig config = toolProperties.getToolConfig(toolName);
        return config.isEnabled() && definition.isEnabled();
    }
    
    /**
     * Register a tool with its definition and executor
     */
    public void registerTool(ToolDefinition definition, ToolExecutor executor) {
        if (definition == null || executor == null) {
            logger.error("Cannot register tool: definition or executor is null");
            return;
        }
        
        toolDefinitions.put(definition.getName(), definition);
        toolExecutors.put(definition.getName(), executor);
        logger.info("Registered tool: {}", definition.getName());
    }
    
    /**
     * Interface for tool executors
     */
    public interface ToolExecutor {
        ToolResult execute(ToolCall toolCall) throws ToolExecutionException;
    }
}