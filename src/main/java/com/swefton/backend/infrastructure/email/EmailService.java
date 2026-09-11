package com.swefton.backend.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private final String from;

    public EmailService(

            JavaMailSender mailSender,

            @Value("${app.mail.from}") String from) {

        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendVerificationCode(
            String recipient,
            String code,
            long expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("Verify your Swefton account");
            helper.setText(
                    plainText(code, expirationMinutes),
                    htmlText(code, expirationMinutes));
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "Could not prepare verification email",
                    exception);
        }
    }

    private String plainText(String code, long expirationMinutes) {
        return """
                Verify your Swefton account

                Use this verification code to finish creating your account:

                %s

                This code expires in %d minutes. Never share it with anyone.

                If you did not create a Swefton account, you can safely ignore this email.
                """.formatted(code, expirationMinutes);
    }

    private String htmlText(String code, long expirationMinutes) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Verify your Swefton account</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f1f5f9; color:#0f172a; font-family:Arial, Helvetica, sans-serif;">
                    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">
                        Your Swefton verification code is {{CODE}}.
                    </div>

                    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%; background-color:#f1f5f9;">
                        <tr>
                            <td align="center" style="padding:40px 16px;">
                                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%; max-width:600px;">
                                    <tr>
                                        <td align="center" style="padding:0 0 20px;">
                                            <span style="font-size:24px; line-height:30px; font-weight:800; letter-spacing:2px; color:#0f172a;">SWEFTON</span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#ffffff; border:1px solid #e2e8f0; border-radius:16px; overflow:hidden; box-shadow:0 8px 24px rgba(15, 23, 42, 0.08);">
                                            <div style="height:6px; background-color:#22c55e; font-size:0; line-height:0;">&nbsp;</div>
                                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                                                <tr>
                                                    <td align="center" style="padding:42px 36px 36px;">
                                                        <div style="display:inline-block; width:56px; height:56px; border-radius:50%; background-color:#dcfce7; color:#15803d; font-size:28px; line-height:56px; font-weight:700;">&#10003;</div>

                                                        <h1 style="margin:24px 0 12px; color:#0f172a; font-size:30px; line-height:38px; font-weight:750;">Verify your email</h1>
                                                        <p style="margin:0; max-width:440px; color:#475569; font-size:16px; line-height:26px;">
                                                            Welcome to Swefton. Enter the verification code below to finish creating your account.
                                                        </p>

                                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:30px auto 24px;">
                                                            <tr>
                                                                <td align="center" style="padding:18px 26px; border:1px solid #bbf7d0; border-radius:12px; background-color:#f0fdf4; color:#14532d; font-family:'Courier New', Courier, monospace; font-size:36px; line-height:42px; font-weight:700; letter-spacing:8px; white-space:nowrap;">
                                                                    {{CODE}}
                                                                </td>
                                                            </tr>
                                                        </table>

                                                        <p style="margin:0; color:#64748b; font-size:14px; line-height:22px;">
                                                            This code expires in <strong style="color:#334155;">{{MINUTES}} minutes</strong>.<br>
                                                            For your security, never share this code with anyone.
                                                        </p>

                                                        <div style="height:1px; margin:32px 0 24px; background-color:#e2e8f0; font-size:0; line-height:0;">&nbsp;</div>

                                                        <p style="margin:0; color:#94a3b8; font-size:13px; line-height:20px;">
                                                            Didn't create a Swefton account? You can safely ignore this email.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding:22px 20px 0; color:#94a3b8; font-size:12px; line-height:18px;">
                                            &copy; Swefton &middot; Your fitness journey, connected.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .replace("{{CODE}}", code)
                .replace("{{MINUTES}}", Long.toString(expirationMinutes));
    }
}
