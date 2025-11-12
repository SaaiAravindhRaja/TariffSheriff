package com.tariffsheriff.backend.chatbot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tariffsheriff.backend.chatbot.model.ChatConversation;
import com.tariffsheriff.backend.chatbot.model.ChatMessageEntity;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByConversationOrderByCreatedAtAsc(ChatConversation conversation);
}
