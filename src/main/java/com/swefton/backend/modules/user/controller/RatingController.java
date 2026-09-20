package com.swefton.backend.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.user.api.RatingApi;
import com.swefton.backend.modules.user.dto.RatingOverviewResponse;
import com.swefton.backend.modules.user.dto.RatingRequest;
import com.swefton.backend.modules.user.dto.RatingResponse;
import com.swefton.backend.modules.user.service.RatingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(RatingApi.BASE_PATH)
public class RatingController {

    private final RatingService ratingService;

    @PutMapping("/facilities/{facilityId}")
    public ResponseEntity<RatingResponse> rateFacility(
            @PathVariable Long facilityId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.rateFacility(facilityId, request));
    }

    @GetMapping("/facilities/{facilityId}")
    public ResponseEntity<RatingOverviewResponse> getFacilityRatings(@PathVariable Long facilityId) {
        return ResponseEntity.ok(ratingService.getFacilityRatings(facilityId));
    }

    @DeleteMapping("/facilities/{facilityId}")
    public ResponseEntity<Void> deleteFacilityRating(@PathVariable Long facilityId) {
        ratingService.deleteFacilityRating(facilityId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/trainers/{trainerId}")
    public ResponseEntity<RatingResponse> rateTrainer(
            @PathVariable Long trainerId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.rateTrainer(trainerId, request));
    }

    @GetMapping("/trainers/{trainerId}")
    public ResponseEntity<RatingOverviewResponse> getTrainerRatings(@PathVariable Long trainerId) {
        return ResponseEntity.ok(ratingService.getTrainerRatings(trainerId));
    }

    @DeleteMapping("/trainers/{trainerId}")
    public ResponseEntity<Void> deleteTrainerRating(@PathVariable Long trainerId) {
        ratingService.deleteTrainerRating(trainerId);
        return ResponseEntity.noContent().build();
    }
}
