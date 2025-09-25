package com.s26984.chatservice.ws;

import com.s26984.chatservice.api.dto.rtc.RtcAnswer;
import com.s26984.chatservice.api.dto.rtc.RtcBye;
import com.s26984.chatservice.api.dto.rtc.RtcIceCandidate;
import com.s26984.chatservice.api.dto.rtc.RtcOffer;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebRtcWsController {
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/rooms/{roomId}/webrtc/offer")
    public void offer(@DestinationVariable String roomId, Principal p, @Payload RtcOffer offer) {
        messaging.convertAndSend("/topic/rooms." + roomId + ".webrtc.offer", offer);
    }

    @MessageMapping("/rooms/{roomId}/webrtc/answer")
    public void answer(@DestinationVariable String roomId, Principal p, @Payload RtcAnswer answer) {
        messaging.convertAndSend("/topic/rooms."+roomId+".webrtc.answer", answer);
    }

    @MessageMapping("/rooms/{roomId}/webrtc/candidate")
    public void candidate(@DestinationVariable String roomId, Principal p, @Payload RtcIceCandidate c) {
        messaging.convertAndSend("/topic/rooms."+roomId+".webrtc.candidate", c);
    }

    @MessageMapping("/rooms/{roomId}/webrtc/bye")
    public void bye(@DestinationVariable String roomId, Principal p, @Payload RtcBye bye) {
        messaging.convertAndSend("/topic/rooms."+roomId+".webrtc.bye", bye);
    }

}
