package com.s26984.chatservice.config;

import com.s26984.chatservice.client.SessionClient;
import com.s26984.chatservice.client.SessionGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final SessionGateway gateway;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
            String sid = acc.getFirstNativeHeader("x-session-id");
            if (sid == null || sid.isBlank()) {
                throw new MessagingException("Missing x-session-id");
            }
            final UUID uuid;
            try {
                uuid = UUID.fromString(sid);
            } catch (Exception e) {
                throw new MessagingException("Invalid x-session-id");
            }
            boolean ok = false;
            try {
                ok = gateway.sessionExists(uuid);
            } catch (Exception ignored) { }
            if (!ok) {
                throw new MessagingException("Unauthorized STOMP CONNECT");
            }
            Principal p = () -> sid;
            acc.setUser(p);
        }
        return message;
    }
}
