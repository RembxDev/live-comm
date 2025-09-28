package com.s26984.chatservice.api.dto.rtc;

import com.fasterxml.jackson.databind.JsonNode;

public record RtcAnswer(
        String type,
        String sdp,
        JsonNode candidate,
        String fromSessionId,
        String toSessionId,
        String roomId
) implements RtcSignalPayload {

    public RtcAnswer {
        type = type == null || type.isBlank() ? "answer" : type;
    }
}
