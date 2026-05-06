package com.lockerroom.dispatchservice.infrastructure.sms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogStubSmsSenderTest {

    private final LogStubSmsSender sender = new LogStubSmsSender();

    @Test
    void send_returnsSuccessWithProviderMessageId() {
        SmsResult result = sender.send(SmsMessage.of("01012345678", "01099998888", "hello"));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).startsWith("STUB-");
        assertThat(result.resultCode()).isEqualTo("OK");
    }

    @Test
    void send_blankTo_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SmsMessage.of("", "from", "text"));
    }

    @Test
    void send_blankText_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SmsMessage.of("01012345678", "from", ""));
    }
}
