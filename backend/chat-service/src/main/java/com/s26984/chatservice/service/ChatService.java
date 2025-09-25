package com.s26984.chatservice.service;

import com.s26984.chatservice.api.dto.ChatMessageResponse;
import com.s26984.chatservice.api.dto.CreateChatMessageRequest;
import com.s26984.chatservice.client.SessionClient;
import com.s26984.chatservice.model.ChatMessage;
import com.s26984.chatservice.repo.ChatMessageRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository repository;
    private final SessionClient sessionClient;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Room ID is required");
        }
        return repository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional
    public ChatMessageResponse create(CreateChatMessageRequest req) {
        validateSessionOrThrow(req.sessionId());
        ChatMessage entity = toEntity(req.sessionId(), req);
        entity = repository.save(entity);
        return toDto(entity);
    }


    private void validateSessionOrThrow(UUID sessionId) {
        if (sessionId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Session is required");
        }
        try {
            if (!sessionClient.sessionExists(sessionId)) {
                throw new ResponseStatusException(UNAUTHORIZED, "Invalid session: " + sessionId);
            }
        } catch (FeignException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unable to verify session", e);
        }
    }


    private ChatMessage toEntity(UUID sessionId, CreateChatMessageRequest req) {
        return ChatMessage.builder()
                .roomId(req.roomId())
                .sessionId(sessionId)
                .sender(req.sender())
                .type(req.type())
                .content(req.content())
                .createdAt(Instant.now())
                .build();
    }

    private ChatMessageResponse toDto(ChatMessage e) {
        return new ChatMessageResponse(
                e.getMessageId(),
                e.getSessionId(),
                e.getSender(),
                e.getType(),
                e.getContent(),
                e.getRoomId(),
                e.getCreatedAt()
        );
    }
}
