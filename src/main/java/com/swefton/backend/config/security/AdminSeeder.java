package com.swefton.backend.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.RoleRepository;
import com.swefton.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed.admin", name = "enabled", havingValue = "true")
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.email:admin@swefton.com}")
    private String configuredEmail;

    @Value("${app.seed.admin.password:12345678}")
    private String configuredPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = configuredEmail.trim().toLowerCase();

        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode(RoleCode.ADMIN);
                    return roleRepository.save(role);
                });

        User admin = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
        admin.setEmail(email);
        admin.setAuthSubject(email);
        admin.setPasswordHash(passwordEncoder.encode(configuredPassword));
        admin.setRole(adminRole);
        admin.setEnabled(true);
        admin.setEmailConfirmed(true);
        admin.setOnboardingCompleted(true);
        userRepository.save(admin);
    }
}

