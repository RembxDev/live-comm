package com.s26984.chatservice.api.dto.rtc;

public record RtcAnswer(
        String roomId,
        String fromSessionId,
        String toSessionId,
        String sdp
) {}
