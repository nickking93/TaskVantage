package com.taskvantage.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Method to send email with default HTML content (set to true)
    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, true);  // Defaults to HTML email
    }

    // Overloaded method to allow HTML or plain text email
    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(to);
            helper.setSubject(subject);

            // Set the email as HTML content
            helper.setText(body, isHtml);  // Set the body and ensure isHtml is true

            // Send the email
            mailSender.send(mimeMessage);
            logger.debug("Email sent successfully");

        } catch (MessagingException e) {
            logger.error("Failed to send email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}