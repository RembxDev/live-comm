package com.s26984.sessionservice.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateGuestSessionRequest(
        @Email @NotBlank String email
) {}
