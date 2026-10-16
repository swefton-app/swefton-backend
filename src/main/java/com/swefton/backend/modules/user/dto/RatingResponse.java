package com.swefton.backend.modules.user.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.user.entity.UserRating;

public record RatingResponse(
        Long id,
        Long userId,
        Long facilityId,
        Long trainerId,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public RatingResponse(UserRating rating) {
        this(
                rating.getId(),
                rating.getUser().getId(),
                rating.getFacility() == null ? null : rating.getFacility().getId(),
                rating.getTrainer() == null ? null : rating.getTrainer().getId(),
                rating.getRating(),
                rating.getComment(),
                rating.getCreatedAt(),
                rating.getUpdatedAt());
    }
}
