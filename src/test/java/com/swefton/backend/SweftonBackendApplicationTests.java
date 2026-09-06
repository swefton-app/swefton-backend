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
		"REDIS_HOST=localhost",
		"REDIS_PORT=6379",
		"CORS_ALLOWED_ORIGINS=http://localhost:3000",
		"STORAGE_PROVIDER=local",
		"FILE_UPLOAD_DIR=target/test-uploads",
		"MEDIA_VIDEO_MAX_DURATION_SECONDS=60",
		"SERVER_PORT=0"
})
class SweftonBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
