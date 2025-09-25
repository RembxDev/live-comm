package com.s26984.sessionservice.service;

import com.s26984.sessionservice.api.dto.CreateGuestSessionRequest;
import com.s26984.sessionservice.api.dto.GuestSessionResponse;
import com.s26984.sessionservice.api.dto.VerifyGuestSessionRequest;
import com.s26984.sessionservice.model.GuestSession;
import com.s26984.sessionservice.repo.GuestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class GuestSessionService {

    private final GuestSessionRepository repo;
    private final Clock clock = Clock.systemUTC();

    @Value("${session.verification.ttl:PT15M}")
    private Duration sessionTtl;

    @Value("${session.captcha.min:1}")
    private int captchaMin;

    @Value("${session.captcha.max:9}")
    private int captchaMax;

    @Value("${session.token.length:6}")
    private int tokenLength;

    private final SecureRandom random = new SecureRandom();
    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Transactional
    public GuestSessionResponse create(CreateGuestSessionRequest req) {
        final String normalizedEmail = req.email().trim().toLowerCase();
        final Instant now = Instant.now(clock);


        var existing = repo.findTop1ByEmailAndVerifiedIsFalseOrderByCreatedAtDesc(normalizedEmail).orElse(null);
        if (existing != null && existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)) {
            return new GuestSessionResponse(
                    existing.getSessionId(),
                    existing.getEmail(),
                    false,
                    existing.getCreatedAt(),
                    existing.getVerificationToken(),
                    existing.getCaptchaA(),
                    existing.getCaptchaB()
            );
        }


        int a = randBetween(captchaMin, captchaMax);
        int b = randBetween(captchaMin, captchaMax);
        String token = randomToken(tokenLength);

        GuestSession gs = GuestSession.builder()
                .sessionId(UUID.randomUUID())
                .email(normalizedEmail)
                .verificationToken(token)
                .verified(false)
                .captchaA(a)
                .captchaB(b)
                .createdAt(now)
                .expiresAt(now.plus(sessionTtl))
                .build();

        repo.save(gs);

        return new GuestSessionResponse(
                gs.getSessionId(),
                gs.getEmail(),
                gs.isVerified(),
                gs.getCreatedAt(),
                gs.getVerificationToken(),
                gs.getCaptchaA(),
                gs.getCaptchaB()
        );
    }

    @Transactional
    public GuestSessionResponse verify(VerifyGuestSessionRequest req) {
        GuestSession gs = repo.findById(req.sessionId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));

        if (gs.isVerified()) {
            return new GuestSessionResponse(gs.getSessionId(), gs.getEmail(), true, gs.getCreatedAt(), null, null, null);
        }

        final Instant now = Instant.now(clock);
        if (gs.getExpiresAt() != null && gs.getExpiresAt().isBefore(now)) {
            throw new ResponseStatusException(BAD_REQUEST, "Session token expired");
        }

        String provided = req.verificationToken().trim();
        String expectedToken = gs.getVerificationToken();
        if (expectedToken == null || !expectedToken.equalsIgnoreCase(provided)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid verification token");
        }

        Integer expectedSum = (gs.getCaptchaA() == null || gs.getCaptchaB() == null)
                ? null
                : gs.getCaptchaA() + gs.getCaptchaB();
        if (expectedSum == null || req.captchaResult() == null || !expectedSum.equals(req.captchaResult())) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid captcha result");
        }

        gs.setVerified(true);
        gs.setVerificationToken(null);
        gs.setCaptchaA(null);
        gs.setCaptchaB(null);
        gs.setExpiresAt(null);
        repo.save(gs);

        return new GuestSessionResponse(gs.getSessionId(), gs.getEmail(), true, gs.getCreatedAt(), null, null, null);
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID sessionId) {
        return repo.existsById(sessionId);
    }

    @Transactional(readOnly = true)
    public GuestSessionResponse get(UUID sessionId) {
        GuestSession gs = repo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));
        return new GuestSessionResponse(
                gs.getSessionId(),
                gs.getEmail(),
                gs.isVerified(),
                gs.getCreatedAt(),
                null, null, null
        );
    }



    private int randBetween(int min, int maxInclusive) {
        if (min > maxInclusive) { int t = min; min = maxInclusive; maxInclusive = t; }
        return min + random.nextInt((maxInclusive - min) + 1);
    }

    private String randomToken(int len) {
        if (len < 1) len = 6;
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(TOKEN_ALPHABET.charAt(random.nextInt(TOKEN_ALPHABET.length())));
        }
        return sb.toString();
    }
}
