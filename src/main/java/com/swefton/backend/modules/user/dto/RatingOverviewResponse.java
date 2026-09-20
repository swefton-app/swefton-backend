package com.swefton.backend.modules.user.dto;

import java.math.BigDecimal;
import java.util.List;

public record RatingOverviewResponse(
        BigDecimal averageRating,
        long ratingCount,
        List<RatingResponse> ratings) {
}
