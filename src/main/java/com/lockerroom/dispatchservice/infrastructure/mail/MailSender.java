package com.lockerroom.dispatchservice.infrastructure.mail;

public interface MailSender {

    MailResult send(MailMessage message);
}
