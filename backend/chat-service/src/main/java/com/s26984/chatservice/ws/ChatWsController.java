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

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messaging;


    @MessageMapping("/chat/{sessionId}")
    public void receive(@DestinationVariable String sessionId,
                        @Valid CreateChatMessageRequest dto) {

        UUID sid = UUID.fromString(sessionId);
        ChatMessageResponse saved = chatService.saveIncoming(sid, dto);

        messaging.convertAndSend("/topic/chat/" + sid, saved);
        log.debug("Saved and broadcast message {} for session {}", saved.id(), sid);
    }
}
