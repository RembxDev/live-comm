package com.s26984.chatservice.api.dto;

import java.time.Instant;
import java.util.Set;

public record PresenceEvent(
        String kind,        // JOIN | LEAVE | SNAPSHOT
        String roomId,
        String sessionId,
        Set<String> members,
        Instant ts
) {}
