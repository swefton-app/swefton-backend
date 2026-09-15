package com.swefton.backend.modules.auth.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.auth.dto.request.LoginRequest;
import com.swefton.backend.modules.auth.dto.request.RefreshTokenRequest;
import com.swefton.backend.modules.auth.dto.request.RegisterRequest;
import com.swefton.backend.modules.auth.dto.request.ResendVerificationCodeRequest;
import com.swefton.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.swefton.backend.modules.auth.dto.response.AuthResponse;
import com.swefton.backend.modules.auth.dto.response.RegisterResponse;
import com.swefton.backend.modules.auth.dto.response.TokenResponse;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.RoleRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.security.token.RefreshTokenService;
import com.swefton.backend.security.token.RevokedTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RevokedTokenService revokedTokenService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        RoleCode requestedRole = request.role() == null ? RoleCode.USER : request.role();
        if (requestedRole == RoleCode.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ADMIN cannot be selected during registration");
        }

        Role role = roleRepository.findByCode(requestedRole)
                .orElseThrow(() -> new IllegalStateException("Role not configured"));

        User user = new User();
        user.setEmail(email);
        user.setAuthSubject(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setEnabled(true);
        user.setEmailConfirmed(false);

        User savedUser = userRepository.save(user);
        try {
            emailVerificationService.sendInitialCode(savedUser);
        } catch (MailException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is unavailable. Please try again later.",
                    exception);
        }

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().getCode(),
                savedUser.isEmailConfirmed());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }
        if (!user.isEmailConfirmed()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Verify your email before logging in");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return authTokenService.issue(user, false);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        Long userId = refreshTokenService.consume(request.refreshToken());
        User user = userRepository.findById(userId).orElseThrow(this::invalidCredentials);

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }
        return authTokenService.issue(user);
    }

    public void logout(Jwt jwt) {
        revokedTokenService.revoke(jwt);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(this::invalidVerificationCode);

        if (user.isEmailConfirmed()) {
            return;
        }

        emailVerificationService.verifyCode(user.getId(), request.code());
        user.setEmailConfirmed(true);
        userRepository.save(user);
    }

    public void resendVerificationCode(ResendVerificationCodeRequest request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(user -> !user.isEmailConfirmed())
                .ifPresent(emailVerificationService::resendCode);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private ResponseStatusException invalidVerificationCode() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired verification code");
    }
}
