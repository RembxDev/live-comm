package com.s26984.sessionservice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuestSessionResponse(
        UUID sessionId,
        String email,
        boolean verified,
        Instant createdAt,
        String verificationToken
) {}
