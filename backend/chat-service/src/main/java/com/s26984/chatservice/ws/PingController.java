package com.s26984.chatservice.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class PingController {

    @MessageMapping("/ping")
    @SendTo("/topic/ping")
    public String ping(String body) {
        log.debug("Ping received: {}", body);
        return "pong: " + (body == null ? "" : body);
    }
}
