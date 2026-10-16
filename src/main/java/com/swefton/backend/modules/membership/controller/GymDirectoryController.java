package com.swefton.backend.modules.membership.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.membership.dto.GymDirectoryProfileResponse;
import com.swefton.backend.modules.membership.dto.GymDirectorySummaryResponse;
import com.swefton.backend.modules.membership.service.GymDirectoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/v1/gym-directory")
public class GymDirectoryController {

    private final GymDirectoryService gymDirectoryService;

    @GetMapping
    public ResponseEntity<List<GymDirectorySummaryResponse>> search(
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(gymDirectoryService.search(latitude, longitude, query));
    }

    @GetMapping("/{gymId}")
    public ResponseEntity<GymDirectoryProfileResponse> profile(
            @PathVariable Long gymId,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude) {
        return ResponseEntity.ok(gymDirectoryService.profile(gymId, latitude, longitude));
    }
}
