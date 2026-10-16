package com.swefton.backend.modules.membership.dto;

import java.math.BigDecimal;

import com.swefton.backend.modules.image.dto.ImagePojo;

public record GymDirectorySummaryResponse(
        Long id,
        Long facilityId,
        String name,
        String formattedAddress,
        String city,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm,
        ImagePojo logoImage,
        long peopleInsideCount,
        boolean checkedInByMe,
        String membershipStatus) {
}
