package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.config.GeminiProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.model.GeminiRequest;
import com.tariffsheriff.backend.chatbot.model.GeminiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP client for Gemini API interactions
 */
@Service
public class LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);
    
    private final WebClient webClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public LlmClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(geminiProperties.getBaseUrl())
                .build();
    }
    
    /**
     * Send a chat request to Gemini API for tool selection
     */
    public ToolCall sendToolSelectionRequest(String query, List<ToolDefinition> availableTools) {
        logger.debug("Sending tool selection request for query: {}", query);
        
        try {
            GeminiRequest request = buildToolSelectionRequest(query, availableTools);
            GeminiResponse response = sendRequest(request);
            
            return parseToolCall(response);
            
        } catch (Exception e) {
            logger.error("Failed to get tool selection from LLM", e);
            throw new LlmServiceException("Failed to analyze your query. Please try again.", e);
        }
    }
    
    /**
     * Send a chat request to Gemini API for response generation
     */
    public String sendResponseGenerationRequest(String query, String toolResult) {
        logger.debug("Sending response generation request for query: {}", query);
        
        try {
            GeminiRequest request = buildResponseGenerationRequest(query, toolResult);
            GeminiResponse response = sendRequest(request);
            
            return parseTextResponse(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate response from LLM", e);
            throw new LlmServiceException("Failed to generate a response. Please try again.", e);
        }
    }
    
    /**
     * Send generic request to Gemini API
     */
    private GeminiResponse sendRequest(GeminiRequest request) {
        String url = String.format("/models/%s:generateContent?key=%s", 
                geminiProperties.getModel(), geminiProperties.getApiKey());
        
        try {
            return webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofMillis(geminiProperties.getTimeoutMs()))
                    .block();
                    
        } catch (WebClientResponseException e) {
            logger.error("Gemini API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new LlmServiceException("Invalid API key configuration");
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new LlmServiceException("Rate limit exceeded. Please try again in a moment.");
            } else if (e.getStatusCode().is5xxServerError()) {
                throw new LlmServiceException("Gemini service is temporarily unavailable");
            } else {
                throw new LlmServiceException("Failed to communicate with AI service");
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error calling Gemini API", e);
            throw new LlmServiceException("AI service is temporarily unavailable", e);
        }
    }
    
    /**
     * Build request for tool selection phase
     */
    private GeminiRequest buildToolSelectionRequest(String query, List<ToolDefinition> availableTools) {
        // Create content with user query
        GeminiRequest.Part queryPart = new GeminiRequest.Part(query);
        GeminiRequest.Content userContent = new GeminiRequest.Content("user", List.of(queryPart));
        
        // Convert tool definitions to Gemini format
        List<GeminiRequest.FunctionDeclaration> functionDeclarations = availableTools.stream()
                .filter(ToolDefinition::isEnabled)
                .map(this::convertToFunctionDeclaration)
                .collect(Collectors.toList());
        
        GeminiRequest.Tool tool = new GeminiRequest.Tool(functionDeclarations);
        
        // Configure function calling
        GeminiRequest.FunctionCallingConfig functionConfig = new GeminiRequest.FunctionCallingConfig("AUTO");
        GeminiRequest.ToolConfig toolConfig = new GeminiRequest.ToolConfig(functionConfig);
        
        // Configure generation
        GeminiRequest.GenerationConfig generationConfig = new GeminiRequest.GenerationConfig(
                geminiProperties.getTemperature(),
                geminiProperties.getMaxTokens()
        );
        
        GeminiRequest request = new GeminiRequest(List.of(userContent));
        request.setTools(List.of(tool));
        request.setToolConfig(toolConfig);
        request.setGenerationConfig(generationConfig);
        
        return request;
    }
    
    /**
     * Build request for response generation phase
     */
    private GeminiRequest buildResponseGenerationRequest(String query, String toolResult) {
        String prompt = String.format(
                "User query: %s\n\nTool result: %s\n\n" +
                "Please provide a helpful, conversational response based on the tool result. " +
                "Format the information clearly and explain any technical terms. " +
                "If the tool result indicates no data was found, suggest alternative approaches.",
                query, toolResult
        );
        
        GeminiRequest.Part promptPart = new GeminiRequest.Part(prompt);
        GeminiRequest.Content userContent = new GeminiRequest.Content("user", List.of(promptPart));
        
        GeminiRequest.GenerationConfig generationConfig = new GeminiRequest.GenerationConfig(
                geminiProperties.getTemperature(),
                geminiProperties.getMaxTokens()
        );
        
        GeminiRequest request = new GeminiRequest(List.of(userContent));
        request.setGenerationConfig(generationConfig);
        
        return request;
    }
    
    /**
     * Convert ToolDefinition to Gemini FunctionDeclaration
     */
    private GeminiRequest.FunctionDeclaration convertToFunctionDeclaration(ToolDefinition toolDef) {
        return new GeminiRequest.FunctionDeclaration(
                toolDef.getName(),
                toolDef.getDescription(),
                toolDef.getParameters()
        );
    }
    
    /**
     * Parse tool call from Gemini response
     */
    private ToolCall parseToolCall(GeminiResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new LlmServiceException("Empty response from AI service");
        }
        
        GeminiResponse.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
            throw new LlmServiceException("Invalid response format from AI service");
        }
        
        // Look for function call in response parts
        for (GeminiResponse.Part part : candidate.getContent().getParts()) {
            if (part.getFunctionCall() != null) {
                GeminiResponse.FunctionCall functionCall = part.getFunctionCall();
                
                // Convert args to Map<String, Object>
                Map<String, Object> arguments = convertArgsToMap(functionCall.getArgs());
                
                return new ToolCall(functionCall.getName(), arguments);
            }
        }
        
        // If no function call found, throw exception
        throw new LlmServiceException("AI service did not select a tool for your query. Please try rephrasing your question.");
    }
    
    /**
     * Parse text response from Gemini response
     */
    private String parseTextResponse(GeminiResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new LlmServiceException("Empty response from AI service");
        }
        
        GeminiResponse.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
            throw new LlmServiceException("Invalid response format from AI service");
        }
        
        // Look for text in response parts
        for (GeminiResponse.Part part : candidate.getContent().getParts()) {
            if (part.getText() != null && !part.getText().trim().isEmpty()) {
                return part.getText().trim();
            }
        }
        
        throw new LlmServiceException("AI service returned an empty response");
    }
    
    /**
     * Convert function call args to Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertArgsToMap(Object args) {
        if (args == null) {
            return Map.of();
        }
        
        if (args instanceof Map) {
            return (Map<String, Object>) args;
        }
        
        // Try to convert using ObjectMapper
        try {
            String json = objectMapper.writeValueAsString(args);
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to convert function args to map: {}", args, e);
            return Map.of();
        }
    }
}