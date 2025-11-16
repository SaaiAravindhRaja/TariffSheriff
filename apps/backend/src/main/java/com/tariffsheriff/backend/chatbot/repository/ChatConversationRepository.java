package com.tariffsheriff.backend.chatbot.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tariffsheriff.backend.chatbot.model.ChatConversation;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByPublicId(UUID publicId);
    Optional<ChatConversation> findByPublicIdAndUserEmail(UUID publicId, String userEmail);
    java.util.List<ChatConversation> findByUserEmailOrderByUpdatedAtDesc(String userEmail);
}
