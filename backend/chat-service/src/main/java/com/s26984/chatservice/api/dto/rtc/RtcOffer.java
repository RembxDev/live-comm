package com.s26984.chatservice.api.dto.rtc;

import com.fasterxml.jackson.databind.JsonNode;

public record RtcOffer(
        String type,
        String sdp,
        JsonNode candidate,
        String fromSessionId,
        String toSessionId,
        String roomId
) implements RtcSignalPayload {

    public RtcOffer {
        type = type == null || type.isBlank() ? "offer" : type;
    }
}