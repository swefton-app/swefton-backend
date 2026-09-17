package com.swefton.backend.modules.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.auth.dto.request.GoogleAuthRequest;
import com.swefton.backend.modules.auth.dto.response.AuthResponse;
import com.swefton.backend.modules.auth.entity.UserAuthProvider;
import com.swefton.backend.modules.auth.enums.AuthProvider;
import com.swefton.backend.modules.auth.provider.google.GoogleIdentity;
import com.swefton.backend.modules.auth.provider.google.GoogleIdentityService;
import com.swefton.backend.modules.auth.repository.UserAuthProviderRepository;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.RoleRepository;
import com.swefton.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String AUTH_SUBJECT_PREFIX = "google|";

    private final GoogleIdentityService googleIdentityService;
    private final UserAuthProviderRepository authProviderRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenService authTokenService;

    @Transactional
    public AuthResponse authenticate(GoogleAuthRequest request) {
        GoogleIdentity identity = googleIdentityService.verify(request.getCredential());

        return authProviderRepository
                .findByProviderAndProviderSubject(AuthProvider.GOOGLE, identity.getSubject())
                .map(provider -> loginExistingUser(provider, identity))
                .orElseGet(() -> registerGoogleUser(identity, request.getRole()));
    }

    private AuthResponse loginExistingUser(UserAuthProvider provider, GoogleIdentity identity) {
        User user = provider.getUser();
        requireEnabled(user);

        provider.setProviderEmail(normalizeEmail(identity.getEmail()));
        user.setLastLoginAt(LocalDateTime.now());

        authProviderRepository.save(provider);
        userRepository.save(user);

        return authTokenService.issue(user, false);
    }

    private AuthResponse registerGoogleUser(GoogleIdentity identity, String requestedRole) {
        String email = normalizeEmail(identity.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists. Sign in with your existing method "
                            + "and link Google from account settings.");
        }

        String roleCode = requestedRole == null ? RoleCode.USER : requestedRole;
        if (RoleCode.ADMIN.equals(roleCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ADMIN cannot be selected during registration");
        }

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role not configured: " + roleCode));

        User user = new User();
        user.setEmail(email);
        user.setAuthSubject(AUTH_SUBJECT_PREFIX + identity.getSubject());
        user.setPasswordHash(null);
        user.setRole(role);
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        user.setOnboardingCompleted(false);
        user.setLastLoginAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        UserAuthProvider provider = new UserAuthProvider();
        provider.setUser(savedUser);
        provider.setProvider(AuthProvider.GOOGLE);
        provider.setProviderSubject(identity.getSubject());
        provider.setProviderEmail(email);
        authProviderRepository.save(provider);

        return authTokenService.issue(savedUser, true);
    }

    private void requireEnabled(User user) {
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
