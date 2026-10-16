package com.swefton.backend.modules.trainer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.pricing.dto.PriceOfferRequest;
import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;
import com.swefton.backend.modules.trainer.dto.TrainerPackageRequest;
import com.swefton.backend.modules.trainer.dto.TrainerPackageResponse;
import com.swefton.backend.modules.trainer.service.TrainerPackageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
@RequestMapping("/api/v1/trainer/packages")
public class TrainerPackageController {
    private final TrainerPackageService service;

    @GetMapping
    public ResponseEntity<List<TrainerPackageResponse>> mine() { return ResponseEntity.ok(service.mine()); }

    @PostMapping
    public ResponseEntity<TrainerPackageResponse> create(@Valid @RequestBody TrainerPackageRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<TrainerPackageResponse> update(@PathVariable Long packageId,
            @Valid @RequestBody TrainerPackageRequest request) {
        return ResponseEntity.ok(service.update(packageId, request));
    }

    @PostMapping("/{packageId}/offers")
    public ResponseEntity<PriceOfferResponse> createOffer(@PathVariable Long packageId,
            @Valid @RequestBody PriceOfferRequest request) {
        return ResponseEntity.status(201).body(service.createOffer(packageId, request));
    }

    @GetMapping("/{packageId}/offers")
    public ResponseEntity<List<PriceOfferResponse>> offers(@PathVariable Long packageId) {
        return ResponseEntity.ok(service.offerHistory(packageId));
    }

    @DeleteMapping("/{packageId}/offers/{offerId}")
    public ResponseEntity<Void> cancelOffer(@PathVariable Long packageId, @PathVariable Long offerId) {
        service.cancelOffer(packageId, offerId);
        return ResponseEntity.noContent().build();
    }
}
