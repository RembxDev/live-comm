package com.s26984.sessionservice.controller;

import com.s26984.sessionservice.api.dto.CreateGuestSessionRequest;
import com.s26984.sessionservice.api.dto.GuestSessionResponse;
import com.s26984.sessionservice.api.dto.VerificationResponse;
import com.s26984.sessionservice.api.dto.VerifyGuestSessionRequest;
import com.s26984.sessionservice.service.GuestSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/guest-session")
@RequiredArgsConstructor
public class GuestSessionController {

    private final GuestSessionService service;

    @PostMapping
    public ResponseEntity<GuestSessionResponse> create(@Valid @RequestBody CreateGuestSessionRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }

    @PostMapping("/verify")
    public VerificationResponse verify(@Valid @RequestBody VerifyGuestSessionRequest req) {
        return service.verify(req);
    }

    @GetMapping("/{id}")
    public GuestSessionResponse get(@PathVariable("id") UUID sessionId) {
        return service.get(sessionId);
    }

    @GetMapping("/{id}/exists")
    public boolean exists(@PathVariable("id") UUID sessionId) {
        return service.exists(sessionId);
    }
}