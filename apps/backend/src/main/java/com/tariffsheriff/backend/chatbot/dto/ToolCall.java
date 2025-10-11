package com.tariffsheriff.backend.chatbot.dto;

import java.util.Map;

/**
 * Represents a function call request from the LLM
 */
public class ToolCall {
    
    private String name;
    private Map<String, Object> arguments;
    private String id;
    
    public ToolCall() {}
    
    public ToolCall(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
    
    public ToolCall(String name, Map<String, Object> arguments, String id) {
        this.name = name;
        this.arguments = arguments;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
    
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get a typed argument value
     */
    @SuppressWarnings("unchecked")
    public <T> T getArgument(String key, Class<T> type) {
        Object value = arguments != null ? arguments.get(key) : null;
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Get a string argument value
     */
    public String getStringArgument(String key) {
        return getArgument(key, String.class);
    }
}