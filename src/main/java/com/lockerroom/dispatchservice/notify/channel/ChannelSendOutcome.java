package com.lockerroom.dispatchservice.notify.channel;

public record ChannelSendOutcome(
        boolean success,
        String providerMessageId,
        String errorCode,
        String errorMessage
) {
    public static ChannelSendOutcome ok(String providerMessageId) {
        return new ChannelSendOutcome(true, providerMessageId, null, null);
    }

    public static ChannelSendOutcome failed(String errorCode, String errorMessage) {
        return new ChannelSendOutcome(false, null, errorCode, errorMessage);
    }
}
