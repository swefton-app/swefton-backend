package com.swefton.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.swefton.backend.config.data.DevelopmentDataSeeder;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.RoleRepository;
import com.swefton.backend.modules.user.repository.UserRepository;

@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:swefton-test;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=test-only",
        "MAIL_HOST=localhost",
        "MAIL_PORT=1025",
        "MAIL_USERNAME=test-only",
        "MAIL_PASSWORD=test-only",
        "MAIL_FROM=test@example.invalid",
        "EMAIL_VERIFICATION_CODE_TTL_SECONDS=600",
        "EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS=60",
        "EMAIL_VERIFICATION_MAX_ATTEMPTS=5",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "CORS_ALLOWED_ORIGINS=http://localhost:3000",
        "GOOGLE_CLIENT_ID=test-google-client-id",
        "STORAGE_PROVIDER=local",
        "FILE_UPLOAD_DIR=target/test-uploads",
        "MEDIA_VIDEO_MAX_DURATION_SECONDS=60",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_ACCESS_TOKEN_SECONDS=900",
        "REFRESH_TOKEN_SECONDS=2592000",
        "app.seed.enabled=true",
        "app.seed.password=test-password-123",
        "SERVER_PORT=0"
})
class SweftonBackendApplicationTests {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DevelopmentDataSeeder dataSeeder;

    @Test
    @Transactional
    void contextLoadsAndSeedsDevelopmentData() throws Exception {
        dataSeeder.run(null);

        assertEquals(4, roleRepository.count());
        assertEquals(3, userRepository.count());
        assertSeededUser("admin@swefton.local", RoleCode.ADMIN);
        assertSeededUser("trainer@swefton.local", RoleCode.TRAINER);
        assertSeededUser("user@swefton.local", RoleCode.USER);
    }

    private void assertSeededUser(String email, RoleCode roleCode) {
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals("seed|" + email, user.getAuthSubject());
        assertEquals(roleCode, user.getRole().getCode());
        assertTrue(user.isEmailConfirmed());
        assertTrue(user.isEnabled());
        assertTrue(user.isOnboardingCompleted());
        assertTrue(passwordEncoder.matches("test-password-123", user.getPasswordHash()));
    }
}
