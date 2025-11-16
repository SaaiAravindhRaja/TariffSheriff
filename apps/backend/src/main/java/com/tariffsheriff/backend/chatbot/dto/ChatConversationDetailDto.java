package com.tariffsheriff.backend.chatbot.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatConversationDetailDto(
        String conversationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ChatMessageDto> messages
) {}
