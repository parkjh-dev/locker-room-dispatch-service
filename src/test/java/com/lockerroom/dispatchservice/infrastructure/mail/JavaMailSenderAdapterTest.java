package com.lockerroom.dispatchservice.infrastructure.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

class JavaMailSenderAdapterTest {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(false);

    private JavaMailSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(ServerSetupTest.SMTP.getPort());
        mailSender.setUsername("test");
        mailSender.setPassword("test");
        adapter = new JavaMailSenderAdapter(mailSender);
        ReflectionTestUtils.setField(adapter, "defaultFrom", "noreply@lockerroom.local");
    }

    @Test
    void send_textMail_arrivesAtSmtpServer() throws Exception {
        MailResult result = adapter.send(MailMessage.text("user@example.com", "subject", "hello"));

        assertThat(result.success()).isTrue();
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("subject");
        assertThat(received[0].getContent().toString()).contains("hello");
    }

    @Test
    void send_whenSmtpHostUnreachable_returnsFailureResult() {
        JavaMailSenderImpl badSender = new JavaMailSenderImpl();
        badSender.setHost("localhost");
        badSender.setPort(1);
        JavaMailSenderAdapter badAdapter = new JavaMailSenderAdapter(badSender);
        ReflectionTestUtils.setField(badAdapter, "defaultFrom", "noreply@lockerroom.local");

        MailResult result = badAdapter.send(MailMessage.text("user@example.com", "subject", "body"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isNotBlank();
    }
}
