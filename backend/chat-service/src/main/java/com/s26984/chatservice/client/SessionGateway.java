package com.s26984.chatservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionGateway {

    private final SessionClient client;

    @CircuitBreaker(name = "session", fallbackMethod = "existsFallback")
    @Retry(name = "session")
    public boolean sessionExists(UUID id) {
        return client.sessionExists(id);
    }

    private boolean existsFallback(UUID id, Throwable t) {
        return false;
    }
}
