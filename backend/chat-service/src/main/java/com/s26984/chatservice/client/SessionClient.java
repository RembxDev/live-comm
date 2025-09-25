package com.s26984.chatservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "session-service", url = "${session.service.base-url}")
public interface SessionClient {

    @GetMapping("/api/guest-session/{sessionId}/exists")
    boolean sessionExists(@PathVariable("sessionId") UUID sessionId);
}