package com.lockerroom.dispatchservice.infrastructure.sms;

public record SmsResult(
        boolean success,
        String providerMessageId,
        String resultCode,
        String resultMessage
) {
    public static SmsResult success(String providerMessageId) {
        return new SmsResult(true, providerMessageId, "OK", null);
    }

    public static SmsResult failure(String resultCode, String resultMessage) {
        return new SmsResult(false, null, resultCode, resultMessage);
    }
}
