package com.tariffsheriff.backend.chatbot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request model for OpenAI Chat Completions API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiRequest {
    
    private String model;
    private List<Message> messages;
    private List<Tool> tools;
    
    @JsonProperty("tool_choice")
    private Object toolChoice;
    
    private Double temperature;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    public OpenAiRequest() {}
    
    public OpenAiRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
    
    // Getters and setters
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public List<Tool> getTools() {
        return tools;
    }
    
    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }
    
    public Object getToolChoice() {
        return toolChoice;
    }
    
    public void setToolChoice(Object toolChoice) {
        this.toolChoice = toolChoice;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    /**
     * Message in the conversation
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private String content;
        
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
        
        @JsonProperty("tool_call_id")
        private String toolCallId;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }
        
        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }
        
        public String getToolCallId() {
            return toolCallId;
        }
        
        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }
    }
    
    /**
     * Tool call in a message
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;
        
        public ToolCall() {}
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Function getFunction() {
            return function;
        }
        
        public void setFunction(Function function) {
            this.function = function;
        }
    }
    
    /**
     * Function call details
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        private String name;
        private String arguments;
        
        public Function() {}
        
        public Function(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getArguments() {
            return arguments;
        }
        
        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
    
    /**
     * Tool definition
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tool {
        private String type;
        private FunctionDefinition function;
        
        public Tool() {}
        
        public Tool(FunctionDefinition function) {
            this.type = "function";
            this.function = function;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public FunctionDefinition getFunction() {
            return function;
        }
        
        public void setFunction(FunctionDefinition function) {
            this.function = function;
        }
    }
    
    /**
     * Function definition for tools
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;
        
        public FunctionDefinition() {}
        
        public FunctionDefinition(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
