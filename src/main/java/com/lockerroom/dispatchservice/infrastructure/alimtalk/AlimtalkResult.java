package com.lockerroom.dispatchservice.infrastructure.alimtalk;

public record AlimtalkResult(
        boolean success,
        String providerMessageId,
        String resultCode,
        String resultMessage
) {
    public static AlimtalkResult success(String providerMessageId) {
        return new AlimtalkResult(true, providerMessageId, "OK", null);
    }

    public static AlimtalkResult failure(String resultCode, String resultMessage) {
        return new AlimtalkResult(false, null, resultCode, resultMessage);
    }
}
