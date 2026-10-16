package com.swefton.backend.modules.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swefton.backend.modules.payment.entity.Payment;

public record PaymentResponse(String reference, BigDecimal paidAmount, String currency, String status, LocalDateTime paidAt) {
    public PaymentResponse(Payment payment) {
        this(payment.getReference(), payment.getPaidAmount(), payment.getCurrency(), payment.getStatus(), payment.getCreatedAt());
    }
}
