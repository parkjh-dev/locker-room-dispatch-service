package com.lockerroom.dispatchservice.infrastructure.alimtalk;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogStubAlimtalkSenderTest {

    private final LogStubAlimtalkSender sender = new LogStubAlimtalkSender();

    @Test
    void send_returnsSuccessWithProviderMessageId() {
        AlimtalkResult result = sender.send(new AlimtalkMessage(
                "01012345678",
                "TPL-001",
                Map.of("nickname", "tester"),
                null));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).startsWith("STUB-AT-");
    }

    @Test
    void blankTemplateCode_throws() {
        assertThatThrownBy(() -> new AlimtalkMessage("01012345678", "", Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullVariables_areReplacedWithEmptyMap() {
        AlimtalkMessage message = new AlimtalkMessage("01012345678", "TPL", null, null);

        assertThat(message.variables()).isEmpty();
    }
}
