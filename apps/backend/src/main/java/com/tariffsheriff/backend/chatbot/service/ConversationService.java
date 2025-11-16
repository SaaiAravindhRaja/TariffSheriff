package com.tariffsheriff.backend.chatbot.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tariffsheriff.backend.chatbot.dto.ChatConversationDetailDto;
import com.tariffsheriff.backend.chatbot.dto.ChatConversationSummaryDto;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatMessageDto;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.model.ChatConversation;
import com.tariffsheriff.backend.chatbot.model.ChatMessageEntity;
import com.tariffsheriff.backend.chatbot.model.ChatMessageRole;
import com.tariffsheriff.backend.chatbot.repository.ChatConversationRepository;
import com.tariffsheriff.backend.chatbot.repository.ChatMessageRepository;

@Service
public class ConversationService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    public ConversationService(ChatConversationRepository conversationRepository,
                               ChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ChatConversation ensureConversation(String requestedId, String userEmail) {
        if (!StringUtils.hasText(requestedId)) {
            ChatConversation conversation = new ChatConversation();
            conversation.setUserEmail(userEmail);
            return conversationRepository.save(conversation);
        }

        return loadConversationForUser(requestedId, userEmail);
    }

    @Transactional(readOnly = true)
    public List<ChatQueryRequest.ChatMessage> loadHistory(ChatConversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(entity -> new ChatQueryRequest.ChatMessage(
                        entity.getRole() == ChatMessageRole.ASSISTANT ? "assistant" : "user",
                        entity.getContent()))
                .toList();
    }

    @Transactional
    public void appendExchange(ChatConversation conversation, String userContent, String assistantContent) {
        ChatMessageEntity user = new ChatMessageEntity(conversation, ChatMessageRole.USER, userContent);
        ChatMessageEntity assistant = new ChatMessageEntity(conversation, ChatMessageRole.ASSISTANT, assistantContent);
        messageRepository.save(user);
        messageRepository.save(assistant);
        conversation.touch();
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<ChatConversationSummaryDto> listSummaries(String userEmail) {
        return conversationRepository
                .findByUserEmailOrderByUpdatedAtDesc(userEmail)
                .stream()
                .map(conv -> new ChatConversationSummaryDto(
                        conv.getPublicId().toString(),
                        conv.getCreatedAt(),
                        conv.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatConversationDetailDto getConversationDetail(String conversationId, String userEmail) {
        ChatConversation conversation = loadConversationForUser(conversationId, userEmail);
        List<ChatMessageDto> messages = messageRepository
                .findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(entity -> new ChatMessageDto(
                        entity.getRole() == ChatMessageRole.ASSISTANT ? "assistant" : "user",
                        entity.getContent(),
                        entity.getCreatedAt()))
                .toList();
        return new ChatConversationDetailDto(
                conversation.getPublicId().toString(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }

    @Transactional
    public void deleteConversation(String conversationId, String userEmail) {
        ChatConversation conversation = loadConversationForUser(conversationId, userEmail);
        conversationRepository.delete(conversation);
    }

    private ChatConversation loadConversationForUser(String conversationId, String userEmail) {
        if (!StringUtils.hasText(conversationId)) {
            throw new ChatbotException(
                    "Conversation ID is required.",
                    "Provide a valid conversation identifier.",
                    null);
        }
        UUID publicId;
        try {
            publicId = UUID.fromString(conversationId);
        } catch (IllegalArgumentException ex) {
            throw new ChatbotException(
                    "Conversation ID is invalid.",
                    "Start a new chat and try again.",
                    ex);
        }

        return conversationRepository
                .findByPublicIdAndUserEmail(publicId, userEmail)
                .orElseThrow(() -> new ChatbotException(
                        "Conversation not found or access is denied.",
                        "Start a new chat session.",
                        null));
    }
}
