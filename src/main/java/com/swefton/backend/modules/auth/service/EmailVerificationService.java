package com.swefton.backend.modules.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.infrastructure.email.EmailService;
import com.swefton.backend.modules.user.entity.User;

@Service
public class EmailVerificationService {

    private static final String CODE_PREFIX = "auth:email-verification:code:";
    private static final String ATTEMPTS_PREFIX = "auth:email-verification:attempts:";
    private static final String RESEND_PREFIX = "auth:email-verification:resend:";

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long codeTtlSeconds;
    private final long resendCooldownSeconds;
    private final int maxAttempts;

    public EmailVerificationService(
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            @Value("${app.email-verification.code-ttl-seconds}") long codeTtlSeconds,
            @Value("${app.email-verification.resend-cooldown-seconds}") long resendCooldownSeconds,
            @Value("${app.email-verification.max-attempts}") int maxAttempts) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.codeTtlSeconds = codeTtlSeconds;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    public void sendInitialCode(User user) {
        issueAndSendCode(user, false);
    }

    public boolean resendCode(User user) {
        return issueAndSendCode(user, true);
    }

    private boolean issueAndSendCode(User user, boolean enforceCooldown) {
        Long userId = user.getId();
        String cooldownKey = RESEND_PREFIX + userId;

        if (enforceCooldown) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    cooldownKey,
                    "1",
                    Duration.ofSeconds(resendCooldownSeconds));
            if (!Boolean.TRUE.equals(acquired)) {
                return false;
            }
        } else {
            redisTemplate.opsForValue().set(
                    cooldownKey,
                    "1",
                    Duration.ofSeconds(resendCooldownSeconds));
        }

        String code = generateCode();
        String codeKey = CODE_PREFIX + userId;
        String attemptsKey = ATTEMPTS_PREFIX + userId;
        redisTemplate.opsForValue().set(
                codeKey,
                hash(code),
                Duration.ofSeconds(codeTtlSeconds));
        redisTemplate.delete(attemptsKey);

        try {
            emailService.sendVerificationCode(user.getEmail(), code, codeTtlSeconds / 60);
        } catch (RuntimeException exception) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
        return true;
    }

    public void verifyCode(Long userId, String suppliedCode) {
        String codeKey = CODE_PREFIX + userId;
        String attemptsKey = ATTEMPTS_PREFIX + userId;
        String storedHash = redisTemplate.opsForValue().get(codeKey);

        if (storedHash == null) {
            throw invalidCode();
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofSeconds(codeTtlSeconds));
        }
        if (attempts != null && attempts > maxAttempts) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            throw invalidCode();
        }

        boolean matches = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                hash(suppliedCode).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw invalidCode();
        }

        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(RESEND_PREFIX + userId);
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired verification code");
    }
}
