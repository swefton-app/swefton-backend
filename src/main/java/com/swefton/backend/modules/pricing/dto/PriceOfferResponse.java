package com.swefton.backend.modules.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swefton.backend.modules.pricing.entity.PriceOffer;

public record PriceOfferResponse(
        Long id,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        String status) {

    public PriceOfferResponse(PriceOffer offer, boolean active) {
        this(offer.getId(), offer.getOriginalPrice(), offer.getSalePrice(), offer.getStartsAt(), offer.getEndsAt(), active,
                resolveStatus(offer, active));
    }

    private static String resolveStatus(PriceOffer offer, boolean active) {
        if (offer.getCancelledAt() != null || offer.getEndsAt().isBefore(LocalDateTime.now())) {
            return "CLOSED";
        }
        return active ? "ACTIVE" : "SCHEDULED";
    }
}
