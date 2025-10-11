package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of tool registry for managing available tools
 */
@Service
public class ToolRegistryImpl implements ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistryImpl.class);
    
    private final Map<String, ToolDefinition> toolDefinitions;
    private final Map<String, ToolExecutor> toolExecutors;
    
    public ToolRegistryImpl() {
        this.toolDefinitions = new HashMap<>();
        this.toolExecutors = new HashMap<>();
        initializeTools();
    }
    
    @Override
    public List<ToolDefinition> getAvailableTools() {
        return new ArrayList<>(toolDefinitions.values());
    }
    
    @Override
    public ToolResult executeToolCall(ToolCall toolCall) {
        String toolName = toolCall.getName();
        
        if (!isToolAvailable(toolName)) {
            logger.warn("Attempted to execute unavailable tool: {}", toolName);
            return ToolResult.error(toolName, "Tool not available: " + toolName);
        }
        
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            logger.error("No executor found for tool: {}", toolName);
            return ToolResult.error(toolName, "Tool executor not found");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            ToolResult result = executor.execute(toolCall);
            long executionTime = System.currentTimeMillis() - startTime;
            
            result.setExecutionTimeMs(executionTime);
            result.setToolName(toolName);
            
            logger.debug("Tool {} executed in {}ms", toolName, executionTime);
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return ToolResult.error(toolName, "Tool execution failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isToolAvailable(String toolName) {
        ToolDefinition definition = toolDefinitions.get(toolName);
        return definition != null && definition.isEnabled();
    }
    
    /**
     * Register a tool with its definition and executor
     */
    public void registerTool(ToolDefinition definition, ToolExecutor executor) {
        toolDefinitions.put(definition.getName(), definition);
        toolExecutors.put(definition.getName(), executor);
        logger.info("Registered tool: {}", definition.getName());
    }
    
    /**
     * Initialize default tools (placeholders for now)
     */
    private void initializeTools() {
        // Tool definitions will be registered by individual tool implementations
        // This method serves as a placeholder for future tool registration
        logger.info("Tool registry initialized");
    }
    
    /**
     * Interface for tool executors
     */
    public interface ToolExecutor {
        ToolResult execute(ToolCall toolCall) throws ToolExecutionException;
    }
}