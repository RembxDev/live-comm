package com.s26984.sessionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuestSession {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    private String email;

    @Column(name = "verification_token")
    private String verificationToken;

    private boolean verified;

    @Column(name = "captcha_a")
    private Integer captchaA;

    @Column(name = "captcha_b")
    private Integer captchaB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (sessionId == null) sessionId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
