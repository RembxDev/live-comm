package com.s26984.chatservice.presence;

import com.s26984.chatservice.api.dto.PresenceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PresenceDisconnectListener {

    private final PresenceRegistry registry;
    private final SimpMessagingTemplate messaging;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent ev) {
        var acc = StompHeaderAccessor.wrap(ev.getMessage());
        var user = acc.getUser();
        if (user == null) return;

        String sid = user.getName();

        List<String> rooms = new ArrayList<>(registry.roomsOf(sid));
        for (String roomId : rooms) {
            var members = registry.leave(roomId, sid);
            messaging.convertAndSend("/topic/rooms." + roomId + ".presence",
                    new PresenceEvent("LEAVE", roomId, sid, members, Instant.now()));
        }
    }
}
