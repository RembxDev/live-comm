package com.s26984.chatservice.ws;

import com.s26984.chatservice.api.dto.ChatMessageResponse;
import com.s26984.chatservice.api.dto.CreateChatMessageRequest;
import com.s26984.chatservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messaging;


    @MessageMapping("/rooms/{roomId}/message")
    public void sendToRoom(@DestinationVariable String roomId,
                           @Valid CreateChatMessageRequest dto,
                           Principal principal) {

        if (dto.roomId() == null || !dto.roomId().equals(roomId)) {
            dto = new CreateChatMessageRequest(dto.sessionId(), dto.sender(), dto.type(), dto.content(), roomId);
        }

        ChatMessageResponse saved = chatService.create(dto);

        messaging.convertAndSend("/topic/rooms." + saved.roomId() + ".messages", saved);

        log.debug("WS message -> room={} by={}", saved.roomId(),
                principal != null ? principal.getName() : "anon");
    }
}
