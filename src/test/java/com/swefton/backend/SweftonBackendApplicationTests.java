package com.swefton.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
        "SERVER_PORT=0"
})
class SweftonBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
