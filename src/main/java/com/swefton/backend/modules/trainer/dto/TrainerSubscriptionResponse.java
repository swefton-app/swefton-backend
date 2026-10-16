package com.swefton.backend.modules.trainer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.swefton.backend.modules.payment.dto.PaymentResponse;

public record TrainerSubscriptionResponse(
        Long id, Long trainerId, String trainerName, String clientName, String clientEmail,
        String status, TrainerPackageResponse trainerPackage, PaymentResponse payment,
        LocalDateTime requestedAt, LocalDate startsOn, LocalDate endsOn) {
}
