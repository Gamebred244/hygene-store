package com.codeop.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String supportAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String fromAddress,
                        @Value("${app.support.email:}") String supportAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.supportAddress = supportAddress;
    }

    public void sendPasswordReset(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (!fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setSubject("Reset your password");
        message.setText("Click the link to reset your password:\n" + resetLink);
        mailSender.send(message);
    }

    public void sendSupportContact(String name, String email, String body) {
        String target = !supportAddress.isBlank() ? supportAddress : fromAddress;
        if (target.isBlank()) {
            throw new IllegalStateException("Support email is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(target);
        if (!fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setReplyTo(email);
        message.setSubject("Support request from " + name);
        message.setText(body);
        mailSender.send(message);
    }
}
