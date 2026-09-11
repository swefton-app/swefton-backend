package com.swefton.backend.config.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.swefton.backend.security.jwt.RevokedJwtValidator;

@Configuration
public class JwtConfig {

        @Bean
        public SecretKey jwtSecretKey(
                        @Value("${security.jwt.secret}") String base64Secret) {

                byte[] keyBytes = Base64.getDecoder()
                                .decode(base64Secret);

                if (keyBytes.length < 32) {
                        throw new IllegalArgumentException(
                                        "JWT secret must be at least 32 bytes");
                }

                return new SecretKeySpec(
                                keyBytes,
                                "HmacSHA256");
        }

        @Bean
        public JwtEncoder jwtEncoder(
                        SecretKey secretKey) {

                return NimbusJwtEncoder
                                .withSecretKey(secretKey)
                                .algorithm(MacAlgorithm.HS256)
                                .build();
        }

        @Bean
        public JwtDecoder jwtDecoder(

                        SecretKey secretKey,

                        RevokedJwtValidator revokedJwtValidator,

                        @Value("${security.jwt.issuer}") String issuer) {

                NimbusJwtDecoder decoder = NimbusJwtDecoder
                                .withSecretKey(secretKey)
                                .macAlgorithm(
                                                MacAlgorithm.HS256)
                                .build();

                OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                                JwtValidators
                                                .createDefaultWithIssuer(
                                                                issuer),

                                revokedJwtValidator);

                decoder.setJwtValidator(validator);

                return decoder;
        }
}
