package com.swefton.backend.modules.membership.dto;

public record GymMachineSummaryResponse(
        String name,
        Integer quantity,
        String status) {
}
