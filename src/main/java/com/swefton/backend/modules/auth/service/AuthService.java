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
import com.swefton.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.swefton.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.swefton.backend.modules.auth.dto.request.ResetPasswordRequest;
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
    private final PasswordResetService passwordResetService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String requestedRole = request.role() == null ? RoleCode.USER : request.role();
        if (RoleCode.ADMIN.equals(requestedRole) || RoleCode.STAFF.equals(requestedRole)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This role cannot be selected during registration");
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

    public void forgotPassword(ForgotPasswordRequest request) {
        try {
            userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                    .filter(User::isEnabled)
                    .filter(user -> user.getPasswordHash() != null && !user.getPasswordHash().isBlank())
                    .ifPresent(passwordResetService::issueCode);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is unavailable. Please try again later.",
                    exception);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(User::isEnabled)
                .filter(candidate -> candidate.getPasswordHash() != null && !candidate.getPasswordHash().isBlank())
                .orElseThrow(this::invalidResetCode);
        passwordResetService.verifyCode(user.getId(), request.code());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password changes are unavailable for Google-only accounts");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
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

    private ResponseStatusException invalidResetCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset code");
    }
}
