package com.swefton.backend.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.stereotype.Component;

import com.swefton.backend.security.token.RevokedTokenService;

@Component
public class RevokedJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private final RevokedTokenService revokedTokenService;

    public RevokedJwtValidator(
            RevokedTokenService revokedTokenService
    ) {

        this.revokedTokenService =
                revokedTokenService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(
            Jwt jwt
    ) {

        if (jwt.getId() == null) {

            return OAuth2TokenValidatorResult
                    .failure(
                            new OAuth2Error(
                                    "invalid_token",
                                    "JWT does not contain jti",
                                    null
                            )
                    );
        }

        if (
                revokedTokenService
                        .isRevoked(
                                jwt.getId()
                        )
        ) {

            return OAuth2TokenValidatorResult
                    .failure(
                            new OAuth2Error(
                                    "invalid_token",
                                    "JWT has been revoked",
                                    null
                            )
                    );
        }

        return OAuth2TokenValidatorResult
                .success();
    }
}
