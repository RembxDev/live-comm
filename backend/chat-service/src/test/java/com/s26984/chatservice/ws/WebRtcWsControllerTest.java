package com.s26984.chatservice.ws;

import com.s26984.chatservice.api.dto.rtc.RtcOffer;
import com.s26984.chatservice.api.dto.rtc.RtcSignalMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "session.service.base-url=http://localhost")
class WebRtcWsControllerTest {

    @Autowired
    private WebRtcWsController controller;

    @SpyBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private com.s26984.chatservice.client.SessionGateway sessionGateway;

    @Test
    void offerIsForwardedToTargetUserQueue() {
        RtcOffer payload = new RtcOffer("offer", "v=0", null, null, "target-1", "room-1");
        Principal principal = () -> "source-1";

        controller.offer("target-1", principal, payload);

        ArgumentCaptor<RtcSignalMessage> captor = ArgumentCaptor.forClass(RtcSignalMessage.class);
        verify(messagingTemplate).convertAndSendToUser(eq("target-1"), eq("/queue/rtc"), captor.capture());

        RtcSignalMessage forwarded = captor.getValue();
        assertThat(forwarded.type()).isEqualTo("offer");
        assertThat(forwarded.fromSessionId()).isEqualTo("source-1");
        assertThat(forwarded.toSessionId()).isEqualTo("target-1");
        assertThat(forwarded.sdp()).isEqualTo("v=0");
        assertThat(forwarded.roomId()).isEqualTo("room-1");
    }
}
