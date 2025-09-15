package com.s26984.chatservice.service;

import com.s26984.chatservice.api.dto.ChatMessageResponse;
import com.s26984.chatservice.api.dto.CreateChatMessageRequest;
import com.s26984.chatservice.client.SessionClient;
import com.s26984.chatservice.model.ChatMessage;
import com.s26984.chatservice.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository repository;
    private final SessionClient sessionClient;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(UUID sessionId) {
        return repository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ChatMessageResponse create(CreateChatMessageRequest req) {

        if (!sessionClient.sessionExists(req.sessionId())) {
            throw new IllegalArgumentException("Session does not exist: " + req.sessionId());
        }
        ChatMessage entity = toEntity(req.sessionId(), req);
        repository.save(entity);
        return toDto(entity);
    }

    public ChatMessageResponse saveIncoming(UUID sessionId, CreateChatMessageRequest req) {
        if (!sessionClient.sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        ChatMessage entity = toEntity(sessionId, req);
        repository.save(entity);
        return toDto(entity);
    }

    private ChatMessage toEntity(UUID sessionId, CreateChatMessageRequest req) {
        return ChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .sender(req.sender())
                .type(req.type())
                .content(req.content())
                .createdAt(Instant.now())
                .build();
    }

    private ChatMessageResponse toDto(ChatMessage e) {
        return new ChatMessageResponse(
                e.getId(),
                e.getSessionId(),
                e.getSender(),
                e.getType(),
                e.getContent(),
                e.getCreatedAt()
        );
    }
}
