package com.swefton.backend.security.token;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RevokedTokenService {

    private static final String PREFIX =
            "auth:revoked:";

    private final StringRedisTemplate redisTemplate;

    public RevokedTokenService(
            StringRedisTemplate redisTemplate
    ) {

        this.redisTemplate = redisTemplate;
    }

    public void revoke(
            Jwt jwt
    ) {

        String jti = jwt.getId();

        Instant expiresAt =
                jwt.getExpiresAt();

        if (
                jti == null ||
                expiresAt == null
        ) {
            return;
        }

        Duration remaining =
                Duration.between(
                        Instant.now(),
                        expiresAt
                );

        if (
                remaining.isNegative() ||
                remaining.isZero()
        ) {
            return;
        }

        redisTemplate
                .opsForValue()
                .set(
                        PREFIX + jti,
                        "1",
                        remaining
                );
    }

    public boolean isRevoked(
            String jti
    ) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        PREFIX + jti
                )
        );
    }
}
