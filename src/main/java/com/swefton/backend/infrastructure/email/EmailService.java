package com.swefton.backend.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

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
                    true,
                    "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("Verify your Swefton account");
            helper.setText(
                    plainText(code, expirationMinutes),
                    htmlText(code, expirationMinutes));
            helper.addInline(
                    "sweftonLogo",
                    new ClassPathResource("static/images/SweftonLogoHeader.png"),
                    "image/png");
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

                Need help? Contact us at frankokaloshi@swefton.com
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
                <body style="margin:0; padding:0; background-color:#050608; color:#f6f8fc; font-family:Arial, Helvetica, sans-serif;">
                    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">
                        Your Swefton verification code is {{CODE}}.
                    </div>

                    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%; background-color:#050608; background-image:radial-gradient(circle at 50% 0%, rgba(120,56,255,0.12), rgba(5,6,8,0) 55%);">
                        <tr>
                            <td align="center" style="padding:40px 16px 40px;">
                                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%; max-width:560px;">

                                    <!-- Gradient border wrapper -->
                                    <tr>
                                        <td style="padding:1px; border-radius:20px; background:linear-gradient(135deg, #02d9f5 0%, #1677ff 35%, #7838ff 70%, #d938f0 100%);">
                                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%; background-color:#0c0f16; border-radius:19px; overflow:hidden;">

                                                <!-- Logo + heading, single unified surface -->
                                                <tr>
                                                    <td align="center" style="padding:40px 36px 8px;">
                                                        <img src="cid:sweftonLogo" width="360" alt="Swefton" style="display:block; width:360px; height:auto; border:0; outline:none; text-decoration:none;">
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <td align="center" style="padding:8px 36px 36px;">
                                                        <div style="display:inline-block; padding:7px 14px; border:1px solid #1677ff; border-radius:999px; background-color:#141925; color:#02d9f5; font-size:11px; line-height:14px; font-weight:700; letter-spacing:1.5px;">EMAIL VERIFICATION</div>

                                                        <h1 style="margin:22px 0 12px; color:#f6f8fc; font-size:28px; line-height:36px; font-weight:750;">Verify your email</h1>
                                                        <p style="margin:0; max-width:420px; color:#aeb6c5; font-size:15px; line-height:24px;">
                                                            Welcome to Swefton. Use the secure code below to finish creating your account.
                                                        </p>

                                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:30px auto 24px;">
                                                            <tr>
                                                                <td align="center" style="padding:1px; border-radius:15px; background:linear-gradient(120deg, #02d9f5, #7838ff);">
                                                                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="width:100%; border-radius:14px; background-color:#141925;">
                                                                        <tr>
                                                                            <td align="center" style="padding:19px 30px; color:#f6f8fc; font-family:'Courier New', Courier, monospace; font-size:34px; line-height:40px; font-weight:700; letter-spacing:8px; white-space:nowrap;">
                                                                                {{CODE}}
                                                                            </td>
                                                                        </tr>
                                                                    </table>
                                                                </td>
                                                            </tr>
                                                        </table>

                                                        <p style="margin:0; color:#7f8797; font-size:13px; line-height:22px;">
                                                            This code expires in <strong style="color:#02d9f5;">{{MINUTES}} minutes</strong>.<br>
                                                            For your security, never share this code with anyone.
                                                        </p>

                                                        <div style="height:1px; margin:32px 0 22px; background:linear-gradient(90deg, rgba(52,59,73,0) 0%, #242935 50%, rgba(52,59,73,0) 100%); font-size:0; line-height:0;">&nbsp;</div>

                                                        <p style="margin:0; color:#596171; font-size:12px; line-height:19px;">
                                                            Didn't create a Swefton account? You can safely ignore this email.
                                                        </p>
                                                    </td>
                                                </tr>

                                                <!-- Footer -->
                                                <tr>
                                                    <td align="center" style="padding:22px 36px 30px; background-color:#07080c;">
                                                        <p style="margin:0 0 6px; color:#7f8797; font-size:12px; line-height:18px;">
                                                            Need help? Contact us at
                                                            <a href="mailto:frankokaloshi@swefton.com" style="color:#02d9f5; text-decoration:none; font-weight:600;">frankokaloshi@swefton.com</a>
                                                        </p>
                                                        <p style="margin:0; color:#596171; font-size:12px; line-height:18px;">
                                                            &copy; Swefton &middot; Your fitness journey, connected.
                                                        </p>
                                                    </td>
                                                </tr>

                                            </table>
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
