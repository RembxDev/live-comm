package com.s26984.chatservice.api.dto.rtc;

public record RtcBye(
        String roomId,
        String fromSessionId,
        String toSessionId
) {}
