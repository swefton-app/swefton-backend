package com.swefton.backend.modules.membership.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.membership.dto.AccessPassResponse;
import com.swefton.backend.modules.membership.dto.CreateAccessPassRequest;
import com.swefton.backend.modules.membership.dto.CreateMembershipRequest;
import com.swefton.backend.modules.membership.dto.GymMembershipResponse;
import com.swefton.backend.modules.membership.service.GymMembershipService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/v1/gym-memberships")
public class GymMembershipController {

    private final GymMembershipService gymMembershipService;

    @GetMapping
    public ResponseEntity<List<GymMembershipResponse>> mine() {
        return ResponseEntity.ok(gymMembershipService.mine());
    }

    @PostMapping
    public ResponseEntity<GymMembershipResponse> request(@Valid @RequestBody CreateMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymMembershipService.request(request));
    }

    @PostMapping("/{membershipId}/passes")
    public ResponseEntity<AccessPassResponse> createPass(
            @PathVariable Long membershipId,
            @Valid @RequestBody CreateAccessPassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gymMembershipService.createPass(membershipId, request.direction()));
    }

    @PostMapping("/{membershipId}/cancel")
    public ResponseEntity<GymMembershipResponse> cancel(@PathVariable Long membershipId) {
        return ResponseEntity.ok(gymMembershipService.cancel(membershipId));
    }
}
