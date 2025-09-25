package com.s26984.chatservice.api.dto;

import com.s26984.chatservice.model.ChatMessage;

import java.util.UUID;

public record CreateChatMessageRequest(
        UUID sessionId,
        String sender,
        ChatMessage.MessageType type,
        String content,
        String roomId
) {}
