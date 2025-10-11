package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;

/**
 * Interface for chatbot tools that can be called by the LLM
 */
public interface ChatbotTool {
    
    /**
     * Get the unique name of this tool
     */
    String getName();
    
    /**
     * Get the tool definition for LLM function calling
     */
    ToolDefinition getDefinition();
    
    /**
     * Execute the tool with the given parameters
     */
    ToolResult execute(ToolCall toolCall);
}