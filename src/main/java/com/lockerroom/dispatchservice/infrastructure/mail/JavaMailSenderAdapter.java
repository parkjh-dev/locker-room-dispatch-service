package com.lockerroom.dispatchservice.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.common.PiiMasker;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailSenderAdapter implements MailSender {

    private final JavaMailSender javaMailSender;

    @Value("${dispatch.mail.from:noreply@lockerroom.local}")
    private String defaultFrom;

    @Override
    public MailResult send(MailMessage message) {
        try {
            MimeMessage mime = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body(), message.html());
            javaMailSender.send(mime);
            return MailResult.ok();
        } catch (MessagingException | MailException e) {
            log.warn("Mail send failed: to={}, subject={}, error={}",
                    PiiMasker.email(message.to()), message.subject(), e.getMessage());
            return MailResult.failure(e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
