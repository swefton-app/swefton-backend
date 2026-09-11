package com.swefton.backend.modules.auth.service;

import org.springframework.stereotype.Service;

import com.swefton.backend.modules.auth.dto.response.AuthResponse;
import com.swefton.backend.modules.auth.dto.response.TokenResponse;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.security.jwt.JwtService;
import com.swefton.backend.security.token.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse issue(User user, boolean newUser) {
        IssuedTokens tokens = issueTokens(user);

        return AuthResponse.builder()
                .tokenType(tokens.tokenType())
                .accessToken(tokens.accessToken())
                .accessTokenExpiresIn(tokens.accessTokenExpiresIn())
                .refreshToken(tokens.refreshToken())
                .refreshTokenExpiresIn(tokens.refreshTokenExpiresIn())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().getCode())
                .newUser(newUser)
                .onboardingCompleted(user.isOnboardingCompleted())
                .build();
    }

    public TokenResponse issue(User user) {
        IssuedTokens tokens = issueTokens(user);

        return new TokenResponse(
                tokens.tokenType(),
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresIn());
    }

    private IssuedTokens issueTokens(User user) {
        JwtService.AccessToken accessToken = jwtService.generateAccessToken(user);
        RefreshTokenService.RefreshToken refreshToken = refreshTokenService.create(user.getId());

        return new IssuedTokens(
                "Bearer",
                accessToken.value(),
                accessToken.expiresIn(),
                refreshToken.value(),
                refreshToken.expiresIn());
    }

    private record IssuedTokens(
            String tokenType,
            String accessToken,
            long accessTokenExpiresIn,
            String refreshToken,
            long refreshTokenExpiresIn) {
    }
}
