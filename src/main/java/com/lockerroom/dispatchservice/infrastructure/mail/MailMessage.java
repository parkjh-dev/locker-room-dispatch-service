package com.lockerroom.dispatchservice.infrastructure.mail;

public record MailMessage(
        String to,
        String subject,
        String body,
        boolean html
) {
    public MailMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Mail to must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Mail subject must not be blank");
        }
        if (body == null) {
            throw new IllegalArgumentException("Mail body must not be null");
        }
    }

    public static MailMessage text(String to, String subject, String body) {
        return new MailMessage(to, subject, body, false);
    }

    public static MailMessage html(String to, String subject, String body) {
        return new MailMessage(to, subject, body, true);
    }
}
