package com.s26984.chatservice.api;

import com.s26984.chatservice.api.dto.ChatMessageResponse;
import com.s26984.chatservice.api.dto.CreateChatMessageRequest;
import com.s26984.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping("/{roomId}/recent")
    public List<ChatMessageResponse> getHistory(@PathVariable String roomId) {
        return chatService.history(roomId);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse postMessage(@RequestBody CreateChatMessageRequest request) {
        return chatService.create(request);
    }
}