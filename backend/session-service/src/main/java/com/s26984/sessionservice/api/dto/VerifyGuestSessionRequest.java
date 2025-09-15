package com.s26984.sessionservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyGuestSessionRequest(
        @NotNull UUID sessionId,
        @NotBlank String verificationToken,
        @NotNull Integer captchaResult
) {}
