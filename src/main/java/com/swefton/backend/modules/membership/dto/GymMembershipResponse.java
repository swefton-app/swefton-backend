package com.swefton.backend.modules.membership.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.swefton.backend.modules.payment.dto.PaymentResponse;

public record GymMembershipResponse(
        Long id,
        Long facilityId,
        Long gymId,
        String gymName,
        String memberName,
        String memberEmail,
        String status,
        MembershipPlanResponse plan,
        PaymentResponse payment,
        LocalDateTime requestedAt,
        LocalDate startsOn,
        LocalDate endsOn,
        int entriesThisWeek,
        Integer remainingEntriesThisWeek,
        boolean checkedIn) {
}
