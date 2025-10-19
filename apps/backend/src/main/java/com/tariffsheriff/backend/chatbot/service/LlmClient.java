package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.config.OpenAiProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.model.OpenAiRequest;
import com.tariffsheriff.backend.chatbot.model.OpenAiResponse;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP client for OpenAI API interactions
 */
@Service
public class LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);
    
    private final WebClient webClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public LlmClient(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openAiProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    /**
     * Select appropriate tool for the user query
     * Phase 1: Tool Selection
     */
    public ToolCall selectTool(String query, List<ToolDefinition> availableTools, 
                              List<ConversationService.ConversationMessage> conversationHistory) {
        logger.debug("Selecting tool for query: {}", query);
        
        try {
            OpenAiRequest request = buildToolSelectionRequest(query, availableTools, conversationHistory);
            OpenAiResponse response = sendRequest(request);
            
            return parseToolCall(response);
            
        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to select tool from LLM for query: '{}', available tools: {}", 
                    query, availableTools.stream().map(ToolDefinition::getName).collect(Collectors.toList()), e);
            throw new LlmServiceException("I'm having trouble understanding your question. Please try again.", e);
        }
    }
    
    /**
     * Generate conversational response from tool results
     * Phase 2: Response Generation
     */
    public String generateResponse(String query, String toolData, 
                                   List<ConversationService.ConversationMessage> conversationHistory) {
        logger.debug("Generating response for query: {}", query);
        
        try {
            OpenAiRequest request = buildResponseGenerationRequest(query, toolData, conversationHistory);
            OpenAiResponse response = sendRequest(request);
            
            return parseTextResponse(response);
            
        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate response from LLM for query: '{}', tool data length: {} chars", 
                    query, toolData != null ? toolData.length() : 0, e);
            throw new LlmServiceException("I'm having trouble formulating a response. Please try again.", e);
        }
    }
    
    /**
     * Send request to OpenAI API with retry logic
     */
    private OpenAiResponse sendRequest(OpenAiRequest request) {
        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(Duration.ofMillis(openAiProperties.getTimeoutMs()))
                    .retryWhen(Retry.fixedDelay(openAiProperties.getMaxRetries(), Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))
                    .block();
                    
        } catch (WebClientResponseException e) {
            logger.error("OpenAI API error: {} - {}, Request model: {}, messages: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), 
                    request.getModel(), request.getMessages().size());
            handleApiError(e);
            throw new LlmServiceException("Failed to communicate with AI service");
            
        } catch (Exception e) {
            logger.error("Unexpected error calling OpenAI API, Request model: {}, messages: {}", 
                    request.getModel(), request.getMessages().size(), e);
            throw new LlmServiceException("I'm having trouble connecting to my AI service. Please try again in a moment.", e);
        }
    }
    
    /**
     * Handle API errors with user-friendly messages
     */
    private void handleApiError(WebClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new LlmServiceException("AI service authentication failed. Please contact support.");
        } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            throw new LlmServiceException("Too many requests. Please try again in a moment.");
        } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
            logger.error("Bad request to OpenAI: {}", e.getResponseBodyAsString());
            throw new LlmServiceException("Invalid request format. Please try rephrasing your question.");
        } else if (e.getStatusCode().is5xxServerError()) {
            throw new LlmServiceException("AI service is temporarily unavailable. Please try again in a moment.");
        }
    }
    
    /**
     * Build request for tool selection phase
     */
    private OpenAiRequest buildToolSelectionRequest(String query, List<ToolDefinition> availableTools,
                                                    List<ConversationService.ConversationMessage> conversationHistory) {
        // System message to guide the AI
        String systemPrompt = "You are a helpful AI assistant for Tariff Sheriff, a trade and tariff information system. " +
                "Your role is to help users find tariff rates, HS codes, trade agreements, and other trade-related information. " +
                "When a user asks a question, select the most appropriate tool to fetch the data they need. " +
                "If you can answer directly without tools (like greetings or general questions), respond naturally without calling a tool. " +
                "Use the conversation history to understand context and follow-up questions.";
        
        List<OpenAiRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAiRequest.Message("system", systemPrompt));
        
        // Add conversation history for context
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            for (ConversationService.ConversationMessage historyMsg : conversationHistory) {
                messages.add(new OpenAiRequest.Message(historyMsg.getRole(), historyMsg.getContent()));
            }
        }
        
        // Add current user query
        messages.add(new OpenAiRequest.Message("user", query));
        
        // Convert tool definitions to OpenAI format
        List<OpenAiRequest.Tool> tools = availableTools.stream()
                .filter(ToolDefinition::isEnabled)
                .map(this::convertToOpenAiTool)
                .collect(Collectors.toList());
        
        OpenAiRequest request = new OpenAiRequest(openAiProperties.getModel(), messages);
        request.setTools(tools);
        request.setToolChoice("auto"); // Let the model decide whether to use a tool
        request.setTemperature(openAiProperties.getTemperature());
        request.setMaxTokens(openAiProperties.getMaxTokens());
        
        return request;
    }
    
    /**
     * Build request for response generation phase
     */
    private OpenAiRequest buildResponseGenerationRequest(String query, String toolData,
                                                         List<ConversationService.ConversationMessage> conversationHistory) {
        String systemPrompt = "You are a helpful AI assistant for Tariff Sheriff. " +
                "Generate natural, conversational responses that explain trade and tariff information clearly. " +
                "Use simple language and explain technical terms. " +
                "Be concise but complete. " +
                "If data is missing or not found, suggest alternative approaches or related information. " +
                "Use the conversation history to maintain context and provide coherent follow-up responses.";
        
        List<OpenAiRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAiRequest.Message("system", systemPrompt));
        
        // Add conversation history for context
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            for (ConversationService.ConversationMessage historyMsg : conversationHistory) {
                messages.add(new OpenAiRequest.Message(historyMsg.getRole(), historyMsg.getContent()));
            }
        }
        
        // Add current query and tool data
        String userPrompt = String.format(
                "User asked: %s\n\n" +
                "Tool returned this data: %s\n\n" +
                "Please provide a helpful, conversational response that:\n" +
                "- Directly answers the user's question\n" +
                "- Explains the data in user-friendly terms\n" +
                "- Highlights key information\n" +
                "- Suggests related information if helpful",
                query, toolData
        );
        messages.add(new OpenAiRequest.Message("user", userPrompt));
        
        OpenAiRequest request = new OpenAiRequest(openAiProperties.getModel(), messages);
        request.setTemperature(openAiProperties.getTemperature());
        request.setMaxTokens(openAiProperties.getMaxTokens());
        
        return request;
    }
    
    /**
     * Convert ToolDefinition to OpenAI Tool format
     */
    private OpenAiRequest.Tool convertToOpenAiTool(ToolDefinition toolDef) {
        OpenAiRequest.FunctionDefinition function = new OpenAiRequest.FunctionDefinition(
                toolDef.getName(),
                toolDef.getDescription(),
                toolDef.getParameters()
        );
        return new OpenAiRequest.Tool(function);
    }
    
    /**
     * Parse tool call from OpenAI response
     */
    private ToolCall parseToolCall(OpenAiResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new LlmServiceException("Empty response from AI service");
        }
        
        OpenAiResponse.Choice choice = response.getChoices().get(0);
        OpenAiResponse.Message message = choice.getMessage();
        
        if (message == null) {
            throw new LlmServiceException("Invalid response format from AI service");
        }
        
        // Check if the model wants to call a tool
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            OpenAiResponse.ToolCall toolCall = message.getToolCalls().get(0);
            OpenAiResponse.Function function = toolCall.getFunction();
            
            if (function == null || function.getName() == null) {
                throw new LlmServiceException("Invalid tool call format from AI service");
            }
            
            // Parse function arguments from JSON string
            Map<String, Object> arguments = parseArguments(function.getArguments());
            
            return new ToolCall(function.getName(), arguments, toolCall.getId());
        }
        
        // Check if the model responded directly without a tool
        if (message.getContent() != null && !message.getContent().trim().isEmpty()) {
            logger.debug("LLM returned direct response without tool call");
            return new ToolCall(ToolCall.DIRECT_RESPONSE_TOOL, Map.of("text", message.getContent().trim()));
        }
        
        // No tool call and no content
        throw new LlmServiceException("I couldn't understand your question. Please try rephrasing it.");
    }
    
    /**
     * Parse text response from OpenAI response
     */
    private String parseTextResponse(OpenAiResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new LlmServiceException("Empty response from AI service");
        }
        
        OpenAiResponse.Choice choice = response.getChoices().get(0);
        OpenAiResponse.Message message = choice.getMessage();
        
        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new LlmServiceException("AI service returned an empty response");
        }
        
        return message.getContent().trim();
    }
    
    /**
     * Parse function arguments from JSON string
     */
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);
            return arguments;
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse function arguments: {}", argumentsJson, e);
            return Map.of();
        }
    }
    
}
