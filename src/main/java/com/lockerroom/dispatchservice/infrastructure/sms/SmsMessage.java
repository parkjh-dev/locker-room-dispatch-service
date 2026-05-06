package com.lockerroom.dispatchservice.infrastructure.sms;

public record SmsMessage(
        String to,
        String from,
        String subject,
        String text
) {
    public SmsMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("SMS to must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("SMS text must not be blank");
        }
    }

    public static SmsMessage of(String to, String from, String text) {
        return new SmsMessage(to, from, null, text);
    }
}
