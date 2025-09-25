package com.s26984.sessionservice.maintenance;

import com.s26984.sessionservice.repo.GuestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExpiredGuestSessionsCleaner {

    private final GuestSessionRepository repo;


    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void cleanup() {
        repo.deleteAllExpiredUnverifiedSessions(Instant.now());
    }
}
