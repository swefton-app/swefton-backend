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
public class PasswordResetService {
    private static final String CODE_PREFIX = "auth:password-reset:code:";
    private static final String ATTEMPTS_PREFIX = "auth:password-reset:attempts:";
    private static final String RESEND_PREFIX = "auth:password-reset:resend:";

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long codeTtlSeconds;
    private final long resendCooldownSeconds;
    private final int maxAttempts;

    public PasswordResetService(
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            @Value("${app.password-reset.code-ttl-seconds:600}") long codeTtlSeconds,
            @Value("${app.password-reset.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${app.password-reset.max-attempts:5}") int maxAttempts) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.codeTtlSeconds = codeTtlSeconds;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    public boolean issueCode(User user) {
        String cooldownKey = RESEND_PREFIX + user.getId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey, "1", Duration.ofSeconds(resendCooldownSeconds));
        if (!Boolean.TRUE.equals(acquired)) return false;

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeKey = CODE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(codeKey, hash(code), Duration.ofSeconds(codeTtlSeconds));
        redisTemplate.delete(ATTEMPTS_PREFIX + user.getId());
        try {
            emailService.sendPasswordResetCode(user.getEmail(), code, codeTtlSeconds / 60);
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
        if (storedHash == null) throw invalidCode();

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofSeconds(codeTtlSeconds));
        }
        if (attempts != null && attempts > maxAttempts) {
            clear(userId);
            throw invalidCode();
        }
        if (!MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                hash(suppliedCode).getBytes(StandardCharsets.UTF_8))) {
            throw invalidCode();
        }
        clear(userId);
    }

    private void clear(Long userId) {
        redisTemplate.delete(CODE_PREFIX + userId);
        redisTemplate.delete(ATTEMPTS_PREFIX + userId);
        redisTemplate.delete(RESEND_PREFIX + userId);
    }

    private String hash(String value) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset code");
    }
}
