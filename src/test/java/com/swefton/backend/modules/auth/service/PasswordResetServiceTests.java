package com.swefton.backend.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.swefton.backend.infrastructure.email.EmailService;
import com.swefton.backend.modules.user.entity.User;

class PasswordResetServiceTests {

    @Test
    void issuesAndEmailsPasswordResetCode() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        EmailService emailService = mock(EmailService.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        PasswordResetService service = new PasswordResetService(redisTemplate, emailService, 600, 60, 5);
        User user = user(42L, "user@example.com");

        assertTrue(service.issueCode(user));

        verify(emailService).sendPasswordResetCode(eq("user@example.com"), anyString(), eq(10L));
        verify(values).set(eq("auth:password-reset:code:42"), anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void doesNotSendAnotherEmailDuringCooldown() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        EmailService emailService = mock(EmailService.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        PasswordResetService service = new PasswordResetService(redisTemplate, emailService, 600, 60, 5);

        assertFalse(service.issueCode(user(42L, "user@example.com")));
        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString(), anyLong());
    }

    @Test
    void clearsCodeAndCooldownWhenEmailCannotBeSent() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        EmailService emailService = mock(EmailService.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("mail failed"))
                .when(emailService).sendPasswordResetCode(anyString(), anyString(), anyLong());

        PasswordResetService service = new PasswordResetService(redisTemplate, emailService, 600, 60, 5);

        assertThrows(IllegalStateException.class, () -> service.issueCode(user(42L, "user@example.com")));
        verify(redisTemplate).delete("auth:password-reset:code:42");
        verify(redisTemplate).delete("auth:password-reset:resend:42");
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
