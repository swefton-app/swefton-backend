package com.swefton.backend.modules.membership.dto;

import java.math.BigDecimal;

import com.swefton.backend.modules.membership.entity.MembershipPlan;
import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;

public record MembershipPlanResponse(
        Long id,
        String name,
        Integer visitsPerWeek,
        BigDecimal price,
        BigDecimal effectivePrice,
        String currency,
        Integer durationDays,
        Integer durationMonths,
        boolean active,
        PriceOfferResponse offer) {

    public MembershipPlanResponse(MembershipPlan plan) {
        this(plan.getId(), generatedName(plan), plan.getVisitsPerWeek(), plan.getPrice(), plan.getPrice(), plan.getCurrency(),
                plan.getDurationDays(), Math.max(1, (plan.getDurationDays() + 29) / 30), plan.isActive(), null);
    }

    private static String generatedName(MembershipPlan plan) {
        int months = Math.max(1, (plan.getDurationDays() + 29) / 30);
        return months + (months == 1 ? " month · " : " months · ") + plan.getVisitsPerWeek() + " days/week";
    }
}
