package com.tariffsheriff.backend.chatbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request model for Gemini generateContent API
 */
public class GeminiRequest {
    
    private List<Content> contents;
    private List<Tool> tools;
    
    @JsonProperty("toolConfig")
    private ToolConfig toolConfig;
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;
    
    @JsonProperty("system_instruction")
    private Content systemInstruction;
    
    public GeminiRequest() {}
    
    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
    }
    
    public List<Content> getContents() {
        return contents;
    }
    
    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
    
    public List<Tool> getTools() {
        return tools;
    }
    
    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }
    
    public ToolConfig getToolConfig() {
        return toolConfig;
    }
    
    public void setToolConfig(ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
    }
    
    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }
    
    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }
    
    public Content getSystemInstruction() {
        return systemInstruction;
    }
    
    public void setSystemInstruction(Content systemInstruction) {
        this.systemInstruction = systemInstruction;
    }
    
    /**
     * Content part of the request
     */
    public static class Content {
        private String role;
        private List<Part> parts;
        
        public Content() {}
        
        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public List<Part> getParts() {
            return parts;
        }
        
        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }
    
    /**
     * Part of the content
     */
    public static class Part {
        private String text;
        
        @JsonProperty("functionCall")
        private FunctionCall functionCall;
        
        @JsonProperty("functionResponse")
        private FunctionResponse functionResponse;
        
        public Part() {}
        
        public Part(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public FunctionCall getFunctionCall() {
            return functionCall;
        }
        
        public void setFunctionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
        }
        
        public FunctionResponse getFunctionResponse() {
            return functionResponse;
        }
        
        public void setFunctionResponse(FunctionResponse functionResponse) {
            this.functionResponse = functionResponse;
        }
    }
    
    /**
     * Function call in the request
     */
    public static class FunctionCall {
        private String name;
        private Object args;
        
        public FunctionCall() {}
        
        public FunctionCall(String name, Object args) {
            this.name = name;
            this.args = args;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Object getArgs() {
            return args;
        }
        
        public void setArgs(Object args) {
            this.args = args;
        }
    }
    
    /**
     * Function response in the request
     */
    public static class FunctionResponse {
        private String name;
        private Object response;
        
        public FunctionResponse() {}
        
        public FunctionResponse(String name, Object response) {
            this.name = name;
            this.response = response;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Object getResponse() {
            return response;
        }
        
        public void setResponse(Object response) {
            this.response = response;
        }
    }
    
    /**
     * Tool definition for function calling
     */
    public static class Tool {
        @JsonProperty("functionDeclarations")
        private List<FunctionDeclaration> functionDeclarations;
        
        public Tool() {}
        
        public Tool(List<FunctionDeclaration> functionDeclarations) {
            this.functionDeclarations = functionDeclarations;
        }
        
        public List<FunctionDeclaration> getFunctionDeclarations() {
            return functionDeclarations;
        }
        
        public void setFunctionDeclarations(List<FunctionDeclaration> functionDeclarations) {
            this.functionDeclarations = functionDeclarations;
        }
    }
    
    /**
     * Function declaration
     */
    public static class FunctionDeclaration {
        private String name;
        private String description;
        private Object parameters;
        
        public FunctionDeclaration() {}
        
        public FunctionDeclaration(String name, String description, Object parameters) {
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
        
        public Object getParameters() {
            return parameters;
        }
        
        public void setParameters(Object parameters) {
            this.parameters = parameters;
        }
    }
    
    /**
     * Tool configuration
     */
    public static class ToolConfig {
        @JsonProperty("functionCallingConfig")
        private FunctionCallingConfig functionCallingConfig;
        
        public ToolConfig() {}
        
        public ToolConfig(FunctionCallingConfig functionCallingConfig) {
            this.functionCallingConfig = functionCallingConfig;
        }
        
        public FunctionCallingConfig getFunctionCallingConfig() {
            return functionCallingConfig;
        }
        
        public void setFunctionCallingConfig(FunctionCallingConfig functionCallingConfig) {
            this.functionCallingConfig = functionCallingConfig;
        }
    }
    
    /**
     * Function calling configuration
     */
    public static class FunctionCallingConfig {
        private String mode;
        
        @JsonProperty("allowedFunctionNames")
        private List<String> allowedFunctionNames;
        
        public FunctionCallingConfig() {}
        
        public FunctionCallingConfig(String mode) {
            this.mode = mode;
        }
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public List<String> getAllowedFunctionNames() {
            return allowedFunctionNames;
        }
        
        public void setAllowedFunctionNames(List<String> allowedFunctionNames) {
            this.allowedFunctionNames = allowedFunctionNames;
        }
    }
    
    /**
     * Generation configuration
     */
    public static class GenerationConfig {
        private Double temperature;
        
        @JsonProperty("maxOutputTokens")
        private Integer maxOutputTokens;
        
        public GenerationConfig() {}
        
        public GenerationConfig(Double temperature, Integer maxOutputTokens) {
            this.temperature = temperature;
            this.maxOutputTokens = maxOutputTokens;
        }
        
        public Double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
        
        public Integer getMaxOutputTokens() {
            return maxOutputTokens;
        }
        
        public void setMaxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }
    }
}