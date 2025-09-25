package com.s26984.chatservice.presence;

import com.s26984.chatservice.api.dto.PresenceEvent;
import com.s26984.chatservice.api.dto.TypingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class PresenceWsController {

    private final PresenceRegistry registry;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/rooms/{roomId}/presence/join")
    public void join(@DestinationVariable String roomId,
                     Principal p,
                     @Header("simpSessionId") String simpSessionId) {
        String sid = p != null ? p.getName() : "anon";
        Set<String> members = registry.join(roomId, sid);

        messaging.convertAndSend("/topic/rooms." + roomId + ".presence",
                new PresenceEvent("JOIN", roomId, sid, members, Instant.now()));


        messaging.convertAndSendToUser(sid, "/queue/presence",
                new PresenceEvent("SNAPSHOT", roomId, sid, members, Instant.now()));
    }

    @MessageMapping("/rooms/{roomId}/presence/leave")
    public void leave(@DestinationVariable String roomId,
                      Principal p,
                      @Header("simpSessionId") String simpSessionId) {
        String sid = p != null ? p.getName() : "anon";
        Set<String> members = registry.leave(roomId, sid);

        messaging.convertAndSend("/topic/rooms." + roomId + ".presence",
                new PresenceEvent("LEAVE", roomId, sid, members, Instant.now()));
    }

    @MessageMapping("/rooms/{roomId}/typing")
    public void typing(@DestinationVariable String roomId,
                       Principal p,
                       @Payload boolean typing) {
        String sid = p != null ? p.getName() : "anon";
        messaging.convertAndSend("/topic/rooms." + roomId + ".typing",
                new TypingEvent(roomId, sid, typing, Instant.now()));
    }
}
