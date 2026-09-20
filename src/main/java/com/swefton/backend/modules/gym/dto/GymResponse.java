package com.swefton.backend.modules.gym.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.swefton.backend.modules.image.dto.ImagePojo;

public record GymResponse(
        Long id,
        Long facilityId,
        Long ownerId,
        String category,
        String name,
        String description,
        String publicEmail,
        String phoneNumber,
        String websiteUrl,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country,
        String formattedAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String type,
        Integer capacity,
        boolean open24Hours,
        String status,
        ImagePojo logoImage,
        ImagePojo coverImage,
        List<ImagePojo> galleryImages,
        BigDecimal averageRating,
        long ratingCount,
        LocalDateTime createdAt) {
}
