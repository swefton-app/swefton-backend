package com.swefton.backend.modules.trainer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.trainer.dto.TrainerCheckoutRequest;
import com.swefton.backend.modules.trainer.dto.TrainerSubscriptionResponse;
import com.swefton.backend.modules.trainer.service.TrainerSubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trainer-subscriptions")
public class TrainerSubscriptionController {
    private final TrainerSubscriptionService service;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TrainerSubscriptionResponse>> mine() { return ResponseEntity.ok(service.mine()); }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TrainerSubscriptionResponse> checkout(@Valid @RequestBody TrainerCheckoutRequest request) {
        return ResponseEntity.status(201).body(service.checkout(request));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<List<TrainerSubscriptionResponse>> incoming() { return ResponseEntity.ok(service.incoming()); }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<TrainerSubscriptionResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.decide(id, true));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<TrainerSubscriptionResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.decide(id, false));
    }
}
