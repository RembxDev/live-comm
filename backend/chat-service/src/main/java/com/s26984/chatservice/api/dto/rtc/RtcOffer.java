package com.s26984.chatservice.api.dto.rtc;

public record RtcOffer(
        String roomId,
        String fromSessionId,
        String toSessionId,
        String sdp
) {}
