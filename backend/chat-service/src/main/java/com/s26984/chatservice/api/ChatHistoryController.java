package com.s26984.chatservice.api;

import com.s26984.chatservice.api.dto.ChatMessageResponse;
import com.s26984.chatservice.api.dto.CreateChatMessageRequest;
import com.s26984.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping("/messages/{sessionId}")
    public List<ChatMessageResponse> getHistory(@PathVariable UUID sessionId) {
        return chatService.history(sessionId);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse postMessage(@RequestBody CreateChatMessageRequest request) {
        return chatService.create(request);
    }
}