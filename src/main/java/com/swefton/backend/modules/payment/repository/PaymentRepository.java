package com.swefton.backend.modules.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
