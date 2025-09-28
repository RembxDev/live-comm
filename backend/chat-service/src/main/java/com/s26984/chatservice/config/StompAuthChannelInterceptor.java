package com.s26984.chatservice.config;

import com.s26984.chatservice.client.SessionClient;
import com.s26984.chatservice.client.SessionGateway;
import com.s26984.chatservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Missing or invalid Authorization header");
            }

            String jwt = authHeader.substring(7);
            if (!StringUtils.hasText(jwt)) {
                throw new MessagingException("Missing JWT token");
            }

            try {
                String sessionId = jwtService.extractSessionId(jwt);
                if (sessionId != null) {
                    Principal principal = () -> sessionId;
                    accessor.setUser(principal);
                } else {
                    throw new MessagingException("Invalid JWT token");
                }
            } catch (Exception e) {
                throw new MessagingException("Unauthorized STOMP CONNECT: " + e.getMessage());
            }
        }
        return message;
    }
}
