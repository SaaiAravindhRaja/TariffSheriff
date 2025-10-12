package com.tariffsheriff.backend.chatbot.dto;

import java.util.Map;

/**
 * Represents a function call request from the LLM
 */
public class ToolCall {
    
    public static final String DIRECT_RESPONSE_TOOL = "__direct_text_response";
    private static final String DIRECT_RESPONSE_TOOL_LABEL = "direct_response";

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

    public static String getDirectResponseToolLabel() {
        return DIRECT_RESPONSE_TOOL_LABEL;
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
    
    /**
     * Get a string argument value with default
     */
    public String getStringArgument(String key, String defaultValue) {
        String value = getStringArgument(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get a generic argument value
     */
    public Object getArgument(String key) {
        return arguments != null ? arguments.get(key) : null;
    }
    
    /**
     * Get a BigDecimal argument value
     */
    public java.math.BigDecimal getBigDecimalArgument(String key) {
        Object value = getArgument(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) value;
        } else if (value instanceof Number) {
            return new java.math.BigDecimal(value.toString());
        } else if (value instanceof String) {
            try {
                return new java.math.BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
}