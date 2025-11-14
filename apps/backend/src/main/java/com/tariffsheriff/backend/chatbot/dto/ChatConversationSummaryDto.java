package com.tariffsheriff.backend.chatbot.dto;

import java.time.LocalDateTime;

public record ChatConversationSummaryDto(
        String conversationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
