package com.s26984.chatservice.api.dto.rtc;

import com.fasterxml.jackson.databind.JsonNode;

public record RtcSignalMessage(
        String type,
        String sdp,
        JsonNode candidate,
        String fromSessionId,
        String toSessionId,
        String roomId
) {
}