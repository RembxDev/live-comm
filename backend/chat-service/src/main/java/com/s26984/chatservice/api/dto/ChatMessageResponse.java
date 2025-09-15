package com.s26984.chatservice.api.dto;

import com.s26984.chatservice.model.ChatMessage;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        String sender,
        ChatMessage.MessageType type,
        String content,
        Instant createdAt
) {}
