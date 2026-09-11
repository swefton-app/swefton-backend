package com.swefton.backend.modules.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.auth.api.AuthApi;
import com.swefton.backend.modules.auth.dto.request.LoginRequest;
import com.swefton.backend.modules.auth.dto.request.GoogleAuthRequest;
import com.swefton.backend.modules.auth.dto.request.RefreshTokenRequest;
import com.swefton.backend.modules.auth.dto.request.RegisterRequest;
import com.swefton.backend.modules.auth.dto.request.ResendVerificationCodeRequest;
import com.swefton.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.swefton.backend.modules.auth.dto.response.RegisterResponse;
import com.swefton.backend.modules.auth.dto.response.AuthResponse;
import com.swefton.backend.modules.auth.dto.response.TokenResponse;
import com.swefton.backend.modules.auth.service.AuthService;
import com.swefton.backend.modules.auth.service.GoogleAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(AuthApi.BASE_PATH)
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<Void> resendVerificationCode(
            @Valid @RequestBody ResendVerificationCodeRequest request) {
        authService.resendVerificationCode(request);
        return ResponseEntity.noContent().build();
    }
}
