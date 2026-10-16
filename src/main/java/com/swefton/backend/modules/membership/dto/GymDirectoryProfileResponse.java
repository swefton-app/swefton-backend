package com.swefton.backend.modules.membership.dto;

import java.math.BigDecimal;
import java.util.List;

import com.swefton.backend.modules.image.dto.ImagePojo;

public record GymDirectoryProfileResponse(
        Long id,
        Long facilityId,
        String name,
        String description,
        String formattedAddress,
        String city,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm,
        String phoneNumber,
        String websiteUrl,
        ImagePojo logoImage,
        ImagePojo coverImage,
        List<ImagePojo> galleryImages,
        long peopleInsideCount,
        List<OpeningHoursResponse> openingHours,
        List<GymMachineSummaryResponse> machines,
        List<MembershipPlanResponse> plans,
        GymMembershipResponse membership) {
}
