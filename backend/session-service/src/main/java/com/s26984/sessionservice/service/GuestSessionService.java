package com.s26984.sessionservice.service;

import com.s26984.sessionservice.api.dto.CreateGuestSessionRequest;
import com.s26984.sessionservice.api.dto.GuestSessionResponse;
import com.s26984.sessionservice.api.dto.VerifyGuestSessionRequest;
import com.s26984.sessionservice.model.GuestSession;
import com.s26984.sessionservice.repo.GuestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class GuestSessionService {

    private final GuestSessionRepository repo;
    private final SecureRandom rnd = new SecureRandom();

    @Transactional
    public GuestSessionResponse create(CreateGuestSessionRequest req) {
        UUID id = UUID.randomUUID();
        int a = 1 + rnd.nextInt(9);
        int b = 1 + rnd.nextInt(9);

        GuestSession gs = GuestSession.builder()
                .sessionId(id)
                .email(req.email())
                .verificationToken(UUID.randomUUID().toString().replace("-", ""))
                .verified(false)
                .captchaA(a)
                .captchaB(b)
                .createdAt(Instant.now())
                .build();

        repo.save(gs);

        return new GuestSessionResponse(
                gs.getSessionId(), gs.getEmail(), gs.isVerified(), gs.getCreatedAt(), gs.getVerificationToken()
        );
    }

    @Transactional
    public GuestSessionResponse verify(VerifyGuestSessionRequest req) {
        GuestSession gs = repo.findById(req.sessionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));

        int expected = gs.getCaptchaA() + gs.getCaptchaB();
        if (!gs.getVerificationToken().equals(req.verificationToken())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid token");
        }
        if (expected != req.captchaResult()) {
            throw new ResponseStatusException(BAD_REQUEST, "Wrong captcha");
        }

        gs.setVerified(true);
        repo.save(gs);

        return new GuestSessionResponse(gs.getSessionId(), gs.getEmail(), true, gs.getCreatedAt(), null);
    }

    public boolean exists(UUID sessionId) {
        return repo.existsById(sessionId);
    }

    public GuestSessionResponse get(UUID sessionId) {
        GuestSession gs = repo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));
        return new GuestSessionResponse(gs.getSessionId(), gs.getEmail(), gs.isVerified(), gs.getCreatedAt(), null);
    }
}
