package com.swefton.backend.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.RoleRepository;
import com.swefton.backend.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminSeederTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new AdminSeeder(userRepository, roleRepository, passwordEncoder);
        ReflectionTestUtils.setField(seeder, "configuredEmail", "admin@swefton.com");
        ReflectionTestUtils.setField(seeder, "configuredPassword", "12345678");
    }

    @Test
    void seedsAConfirmedEnabledAdministratorWithAnEncodedPassword() {
        Role adminRole = new Role();
        adminRole.setId(4L);
        adminRole.setCode(RoleCode.ADMIN);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmailIgnoreCase("admin@swefton.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("12345678")).thenReturn("encoded-password");

        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@swefton.com");
        assertThat(saved.getAuthSubject()).isEqualTo("admin@swefton.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isSameAs(adminRole);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isEmailConfirmed()).isTrue();
        assertThat(saved.isOnboardingCompleted()).isTrue();
    }
}

