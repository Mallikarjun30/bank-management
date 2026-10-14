package com.bank.management.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final String fromAddress;

    public PasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${MAIL_FROM:no-reply@example.com}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }

    public void sendResetLink(String email, String rawToken) {
        String resetUrl = frontendBaseUrl
                + "/reset-password?token="
                + rawToken;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Bank Management System - Password Reset");

            helper.setText(
                    """
                    Hello,

                    We received a request to reset your password.

                    Open the following link to choose a new password:
                    %s

                    This link expires according to the password-reset
                    token lifetime configured by the application.

                    If you did not request this change, you can ignore
                    this email.

                    Do not share this link with anyone.
                    """.formatted(resetUrl),
                    false
            );

            mailSender.send(message);

        } catch (MessagingException | MailException exception) {
            throw new PasswordResetEmailException(
                    "Unable to send password reset email",
                    exception
            );
        }
    }
}