package com.swefton.backend.modules.facility.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFacilityRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 40) String category,
        @Size(max = 5000) String description,
        @Email @Size(max = 254) String publicEmail,
        @Pattern(regexp = "^[0-9+() .-]{7,30}$", message = "must be a valid phone number")
        String phoneNumber,
        @Size(max = 500) String websiteUrl,
        @NotBlank @Size(max = 255) String addressLine,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 500) String formattedAddress,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 7)
        BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 7)
        BigDecimal longitude,
        @NotBlank @Size(max = 40) String type,
        @Min(1) @Max(1_000_000) Integer capacity,
        boolean open24Hours,
        Long logoImageId,
        Long coverImageId,
        @Size(max = 20) List<Long> galleryImageIds) {
}
