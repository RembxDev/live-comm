package com.s26984.chatservice.api.dto.rtc;

public record RtcIceCandidate(
        String roomId,
        String fromSessionId,
        String toSessionId,
        String candidate,
        String sdpMid,
        Integer sdpMLineIndex
) {}
