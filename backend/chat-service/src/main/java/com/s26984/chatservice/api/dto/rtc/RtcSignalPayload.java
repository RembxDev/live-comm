package com.s26984.chatservice.api.dto.rtc;

import com.fasterxml.jackson.databind.JsonNode;

public interface RtcSignalPayload {
    String type();
    String sdp();
    JsonNode candidate();
    String fromSessionId();
    String toSessionId();
    String roomId();
}