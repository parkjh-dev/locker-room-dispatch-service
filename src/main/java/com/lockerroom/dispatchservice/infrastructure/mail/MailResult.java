package com.lockerroom.dispatchservice.infrastructure.mail;

public record MailResult(
        boolean success,
        String errorCode,
        String errorMessage
) {
    public static MailResult ok() {
        return new MailResult(true, null, null);
    }

    public static MailResult failure(String errorCode, String errorMessage) {
        return new MailResult(false, errorCode, errorMessage);
    }
}
