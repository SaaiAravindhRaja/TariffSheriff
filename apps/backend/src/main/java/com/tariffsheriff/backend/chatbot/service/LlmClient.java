package com.tariffsheriff.backend.chatbot.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.tariffsheriff.backend.chatbot.config.OpenAiProperties;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Thin wrapper around the official OpenAI Java SDK for chat completions.
 */
@Service
public class LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

    private final OpenAiProperties properties;
    private final OpenAIClient client;

    public LlmClient(OpenAiProperties properties) {
        this.properties = properties;
        if (!properties.hasApiKey()) {
            this.client = null;
            logger.warn("OpenAI API key not configured; chatbot responses will be disabled until OPENAI_API_KEY is set.");
            return;
        }

        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .maxRetries(properties.getMaxRetries())
                .timeout(Duration.ofMillis(properties.getTimeoutMs()));

        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }

        this.client = builder.build();
    }

    public ChatCompletionCreateParams.Builder newChatBuilder() {
        ensureClient();
        return ChatCompletionCreateParams.builder()
                .model(ChatModel.of(properties.getModel()))
                .temperature(properties.getTemperature())
                .maxCompletionTokens((long) properties.getMaxTokens());
    }

    public ChatCompletion createCompletion(ChatCompletionCreateParams params) {
        ensureClient();
        try {
            ChatCompletion completion = client.chat().completions().create(params);
            logUsage(completion);
            return completion;
        } catch (Exception e) {
            throw new LlmServiceException("Failed to communicate with AI service", e);
        }
    }

    public String extractContent(ChatCompletion completion) {
        return completion.choices().stream()
                .map(ChatCompletion.Choice::message)
                .map(message -> message.content().orElse("").trim())
                .filter(content -> !content.isEmpty())
                .findFirst()
                .orElseThrow(() -> new LlmServiceException("AI service returned no content"));
    }

    private void ensureClient() {
        if (client == null) {
            throw new LlmServiceException(
                    "OpenAI API key is not configured.",
                    "Set the OPENAI_API_KEY (and optionally OPENAI_BASE_URL) environment variables to enable AI responses.");
        }
    }

    private void logUsage(ChatCompletion completion) {
        completion.usage().ifPresent(usage ->
                logger.debug("OpenAI tokens prompt={} completion={} total={}",
                        usage.promptTokens(), usage.completionTokens(), usage.totalTokens())
        );
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    public boolean isEnabled() {
        return client != null;
    }
}
