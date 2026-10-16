package com.swefton.backend.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse{

    private String tokenType;

    private String accessToken;
    private long accessTokenExpiresIn;

    private String refreshToken;
    private long refreshTokenExpiresIn;

    private Long userId;
    private String email;
    private String role;

    private boolean newUser;
    private boolean onboardingCompleted;
}
