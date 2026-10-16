package com.swefton.backend.modules.trainer.dto;

import java.math.BigDecimal;

import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;
import com.swefton.backend.modules.trainer.entity.TrainerPackage;

public record TrainerPackageResponse(
        Long id, String name, String description, Integer sessionsPerWeek, Integer durationDays,
        BigDecimal price, BigDecimal effectivePrice, String currency, boolean active, PriceOfferResponse offer) {

    public TrainerPackageResponse(TrainerPackage item, BigDecimal effectivePrice, PriceOfferResponse offer) {
        this(item.getId(), item.getName(), item.getDescription(), item.getSessionsPerWeek(), item.getDurationDays(),
                item.getPrice(), effectivePrice, item.getCurrency(), item.isActive(), offer);
    }
}
