package com.taskvantage.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

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
            System.out.println("Email sent successfully to " + to);

        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}