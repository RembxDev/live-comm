package com.s26984.chatservice.ws;

import com.s26984.chatservice.api.dto.rtc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class WebRtcWsController {
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/rtc/{targetSessionId}/offer")
    public void offer(@DestinationVariable String targetSessionId,
                      Principal principal,
                      @Payload RtcOffer payload) {
        sendToTarget("offer", targetSessionId, principal, payload);
    }

    @MessageMapping("/rtc/{targetSessionId}/answer")
    public void answer(@DestinationVariable String targetSessionId,
                       Principal principal,
                       @Payload RtcAnswer payload) {
        sendToTarget("answer", targetSessionId, principal, payload);
    }

    @MessageMapping("/rtc/{targetSessionId}/candidate")
    public void candidate(@DestinationVariable String targetSessionId,
                          Principal principal,
                          @Payload RtcIceCandidate payload) {
        sendToTarget("candidate", targetSessionId, principal, payload);
    }

    @MessageMapping("/rtc/{targetSessionId}/bye")
    public void bye(@DestinationVariable String targetSessionId,
                    Principal principal,
                    @Payload RtcBye payload) {
        sendToTarget("bye", targetSessionId, principal, payload);
    }


    private void sendToTarget(String expectedType,
                              String targetSessionId,
                              Principal principal,
                              RtcSignalPayload payload) {
        RtcSignalMessage message = sanitize(expectedType, targetSessionId, principal, payload);
        messaging.convertAndSendToUser(message.toSessionId(), "/queue/rtc", message);
    }

    private RtcSignalMessage sanitize(String expectedType,
                                      String targetSessionId,
                                      Principal principal,
                                      RtcSignalPayload payload) {
        if (!StringUtils.hasText(targetSessionId)) {
            throw new IllegalArgumentException("Target session id is required");
        }
        String sender = Optional.ofNullable(principal)
                .map(Principal::getName)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated principal required"));

        String payloadType = Optional.ofNullable(payload.type())
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .orElse(expectedType);
        if (!expectedType.equalsIgnoreCase(payloadType)) {
            throw new IllegalArgumentException("Invalid RTC signal type: " + payload.type());
        }

        String payloadFrom = payload.fromSessionId();
        if (StringUtils.hasText(payloadFrom) && !sender.equals(payloadFrom)) {
            throw new IllegalArgumentException("fromSessionId mismatch");
        }

        String to = StringUtils.hasText(payload.toSessionId()) ? payload.toSessionId() : targetSessionId;
        if (!targetSessionId.equals(to)) {
            throw new IllegalArgumentException("toSessionId mismatch");
        }

        return new RtcSignalMessage(expectedType,
                payload.sdp(),
                payload.candidate(),
                sender,
                to,
                payload.roomId());
    }
}