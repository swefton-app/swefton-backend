package com.swefton.backend.infrastructure.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTests {

    @Test
    void sendsPlainTextAndHtmlVerificationEmailAsMultipartMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        EmailService emailService = new EmailService(mailSender, "test@swefton.local");

        assertDoesNotThrow(() -> emailService.sendVerificationCode(
                "user@example.com",
                "123456",
                10));
        verify(mailSender).send(message);
    }
}
