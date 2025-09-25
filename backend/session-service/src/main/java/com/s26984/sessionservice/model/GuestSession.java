package com.s26984.sessionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestSession {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "captcha_a")
    private Integer captchaA;

    @Column(name = "captcha_b")
    private Integer captchaB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (sessionId == null) sessionId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}