package com.swefton.backend.modules.payment.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.payment.dto.CardPaymentRequest;
import com.swefton.backend.modules.payment.entity.Payment;
import com.swefton.backend.modules.payment.repository.PaymentRepository;
import com.swefton.backend.modules.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DevelopmentPaymentService {
    private final PaymentRepository repository;

    @Transactional
    public Payment charge(User payer, String targetType, Long targetId, BigDecimal regularAmount,
            BigDecimal paidAmount, String currency, CardPaymentRequest card) {
        validate(card);
        Payment payment = new Payment();
        payment.setPayer(payer);
        payment.setReference("DEV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
        payment.setTargetType(targetType);
        payment.setTargetId(targetId);
        payment.setRegularAmount(regularAmount);
        payment.setPaidAmount(paidAmount);
        payment.setCurrency(currency);
        payment.setStatus("SUCCEEDED");
        return repository.saveAndFlush(payment);
    }

    private void validate(CardPaymentRequest card) {
        String number = card.cardNumber().replace(" ", "");
        if (number.length() < 13 || number.length() > 19 || !luhn(number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid card number");
        }
        YearMonth expiry = YearMonth.of(card.expiryYear(), card.expiryMonth());
        if (expiry.isBefore(YearMonth.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This card has expired");
        }
        if (card.cardholderName().trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the cardholder name");
        }
    }

    private boolean luhn(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = number.length() - 1; index >= 0; index--) {
            int digit = number.charAt(index) - '0';
            if (doubleDigit && (digit *= 2) > 9) digit -= 9;
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
