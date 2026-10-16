package com.swefton.backend.security.jwt;

import com.swefton.backend.modules.user.entity.User;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    private final String issuer;

    private final long accessTokenSeconds;

    public JwtService(

            JwtEncoder jwtEncoder,

            @Value("${security.jwt.issuer}")
            String issuer,

            @Value("${security.jwt.access-token-seconds}")
            long accessTokenSeconds
    ) {

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        if (accessTokenSeconds <= 0) {
            throw new IllegalArgumentException("Access-token lifetime must be positive");
        }
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public AccessToken generateAccessToken(
            User user
    ) {

        Instant now = Instant.now();

        Instant expiresAt =
                now.plusSeconds(
                        accessTokenSeconds
                );

        JwtClaimsSet claims =
                JwtClaimsSet.builder()

                        .issuer(issuer)

                        // user ID
                        .subject(
                                user.getId().toString()
                        )

                        .issuedAt(now)

                        .expiresAt(expiresAt)

                        // unique JWT ID
                        .id(
                                UUID.randomUUID()
                                        .toString()
                        )

                        // ONLY ONE ROLE
                        .claim(
                                "role",
                                user.getRole()
                                        .getCode()
                        )

                        .claim(
                                "email_verified",
                                user.isEmailConfirmed()
                        )

                        .build();

        String token =
                jwtEncoder
                        .encode(
                                JwtEncoderParameters
                                        .from(claims)
                        )
                        .getTokenValue();

        return new AccessToken(
                token,
                accessTokenSeconds
        );
    }

    public record AccessToken(

            String value,

            long expiresIn

    ) {
    }
}
