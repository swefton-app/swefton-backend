package com.swefton.backend.security.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.time.Duration;

import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final String PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    private final long refreshTokenSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(

            StringRedisTemplate redisTemplate,

            @Value("${security.auth.refresh-token-seconds}") long refreshTokenSeconds) {

        this.redisTemplate = redisTemplate;
        if (refreshTokenSeconds <= 0) {
            throw new IllegalArgumentException("Refresh-token lifetime must be positive");
        }
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public RefreshToken create(
            Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(
                randomBytes);

        String rawToken = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        randomBytes);

        String key = PREFIX + hash(rawToken);

        redisTemplate
                .opsForValue()
                .set(
                        key,
                        userId.toString(),
                        Duration.ofSeconds(
                                refreshTokenSeconds));

        return new RefreshToken(
                rawToken,
                refreshTokenSeconds);
    }

    public Long consume(
            String rawToken) {

        requireToken(rawToken);

        String key = PREFIX + hash(rawToken);

        String userId = redisTemplate
                .opsForValue()
                .getAndDelete(key);

        if (userId == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }

        return Long.parseLong(userId);
    }

    public void revoke(
            String rawToken) {

        requireToken(rawToken);

        redisTemplate.delete(
                PREFIX + hash(rawToken));
    }

    private String hash(
            String rawToken) {

        try {

            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            byte[] hashed = digest.digest(
                    rawToken.getBytes(
                            StandardCharsets.UTF_8));

            return Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashed);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }

    private void requireToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token must not be blank");
        }
    }

    public record RefreshToken(

            String value,

            long expiresIn

    ) {
    }
}
