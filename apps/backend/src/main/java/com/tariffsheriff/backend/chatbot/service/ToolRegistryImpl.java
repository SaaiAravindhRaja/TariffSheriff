package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.config.ToolProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of tool registry for managing available tools
 */
@Service
public class ToolRegistryImpl implements ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistryImpl.class);
    
    private final Map<String, ToolDefinition> toolDefinitions;
    private final Map<String, ToolExecutor> toolExecutors;
    private final ExecutorService executorService;
    
    @Autowired
    private ToolProperties toolProperties;
    
    @Autowired(required = false)
    private ToolHealthMonitor toolHealthMonitor;
    
    public ToolRegistryImpl() {
        this.toolDefinitions = new HashMap<>();
        this.toolExecutors = new HashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        initializeTools();
    }
    
    @Override
    public List<ToolDefinition> getAvailableTools() {
        // Filter tools based on configuration
        return toolDefinitions.values().stream()
                .filter(tool -> {
                    ToolProperties.ToolConfig config = toolProperties.getToolConfig(tool.getName());
                    return config.isEnabled();
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    @Override
    public ToolResult executeToolCall(ToolCall toolCall) {
        String toolName = toolCall.getName();
        
        if (!isToolAvailable(toolName)) {
            logger.warn("Attempted to execute unavailable tool: {}", toolName);
            ToolResult errorResult = ToolResult.error(toolName, "Tool not available: " + toolName);
            recordFailure(toolName, "Tool not available");
            return errorResult;
        }
        
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            logger.error("No executor found for tool: {}", toolName);
            ToolResult errorResult = ToolResult.error(toolName, "Tool executor not found");
            recordFailure(toolName, "Tool executor not found");
            return errorResult;
        }
        
        ToolProperties.ToolConfig config = toolProperties.getToolConfig(toolName);
        return executeWithRetryAndTimeout(toolCall, executor, config);
    }
    
    /**
     * Execute tool with retry logic and timeout
     */
    private ToolResult executeWithRetryAndTimeout(ToolCall toolCall, ToolExecutor executor, 
                                                 ToolProperties.ToolConfig config) {
        String toolName = toolCall.getName();
        int maxRetries = config.getMaxRetries();
        int timeoutMs = config.getTimeoutMs();
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                long startTime = System.currentTimeMillis();
                
                // Execute with timeout
                CompletableFuture<ToolResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return executor.execute(toolCall);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executorService);
                
                ToolResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                long executionTime = System.currentTimeMillis() - startTime;
                
                result.setExecutionTimeMs(executionTime);
                result.setToolName(toolName);
                
                if (result.isSuccess()) {
                    recordSuccess(toolName, executionTime);
                    logger.debug("Tool {} executed successfully in {}ms (attempt {})", 
                               toolName, executionTime, attempt + 1);
                    return result;
                } else {
                    logger.warn("Tool {} failed on attempt {}: {}", toolName, attempt + 1, result.getError());
                    if (attempt == maxRetries) {
                        recordFailure(toolName, result.getError());
                        return result;
                    }
                }
                
            } catch (TimeoutException e) {
                String errorMsg = String.format("Tool execution timed out after %dms", timeoutMs);
                logger.error("Tool {} timed out on attempt {}", toolName, attempt + 1);
                
                if (attempt == maxRetries) {
                    recordFailure(toolName, errorMsg);
                    return ToolResult.error(toolName, errorMsg);
                }
                
            } catch (Exception e) {
                String errorMsg = "Tool execution failed: " + e.getMessage();
                logger.error("Error executing tool {} on attempt {}: {}", toolName, attempt + 1, e.getMessage(), e);
                
                if (attempt == maxRetries) {
                    recordFailure(toolName, errorMsg);
                    return ToolResult.error(toolName, errorMsg);
                }
            }
            
            // Wait before retry (exponential backoff)
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(Math.min(1000 * (1L << attempt), 5000)); // Max 5 seconds
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    recordFailure(toolName, "Tool execution interrupted");
                    return ToolResult.error(toolName, "Tool execution interrupted");
                }
            }
        }
        
        recordFailure(toolName, "All retry attempts failed");
        return ToolResult.error(toolName, "Tool execution failed after " + (maxRetries + 1) + " attempts");
    }
    
    @Override
    public boolean isToolAvailable(String toolName) {
        ToolDefinition definition = toolDefinitions.get(toolName);
        if (definition == null) {
            return false;
        }
        
        ToolProperties.ToolConfig config = toolProperties.getToolConfig(toolName);
        return config.isEnabled() && definition.isEnabled();
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
     * Record successful tool execution for health monitoring
     */
    private void recordSuccess(String toolName, long executionTimeMs) {
        if (toolHealthMonitor != null) {
            toolHealthMonitor.recordSuccess(toolName, executionTimeMs);
        }
    }
    
    /**
     * Record failed tool execution for health monitoring
     */
    private void recordFailure(String toolName, String errorMessage) {
        if (toolHealthMonitor != null) {
            toolHealthMonitor.recordFailure(toolName, errorMessage);
        }
    }
    
    /**
     * Get tool configuration and health information
     */
    public Map<String, Object> getToolInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Add configuration info
        info.put("totalTools", toolDefinitions.size());
        info.put("availableTools", getAvailableTools().size());
        info.put("maxConcurrentExecutions", toolProperties.getMaxConcurrentExecutions());
        info.put("defaultTimeoutMs", toolProperties.getDefaultTimeoutMs());
        info.put("healthChecksEnabled", toolProperties.isEnableHealthChecks());
        
        // Add health info if available
        if (toolHealthMonitor != null) {
            info.put("healthStatus", toolHealthMonitor.getAllToolHealth());
        }
        
        return info;
    }
    
    /**
     * Interface for tool executors
     */
    public interface ToolExecutor {
        ToolResult execute(ToolCall toolCall) throws ToolExecutionException;
    }
}