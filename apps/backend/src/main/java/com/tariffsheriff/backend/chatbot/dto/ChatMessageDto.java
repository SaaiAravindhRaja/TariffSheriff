package com.tariffsheriff.backend.chatbot.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
        String role,
        String content,
        LocalDateTime createdAt
) {}
