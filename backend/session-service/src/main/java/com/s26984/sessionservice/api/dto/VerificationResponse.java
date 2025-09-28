package com.s26984.sessionservice.api.dto;

import java.util.UUID;

public record VerificationResponse(UUID sessionId, String token) {
}