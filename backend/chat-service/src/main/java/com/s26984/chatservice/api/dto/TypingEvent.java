package com.s26984.chatservice.api.dto;

import java.time.Instant;

public record TypingEvent(
        String roomId,
        String sessionId,
        boolean typing,
        Instant ts
) {}
