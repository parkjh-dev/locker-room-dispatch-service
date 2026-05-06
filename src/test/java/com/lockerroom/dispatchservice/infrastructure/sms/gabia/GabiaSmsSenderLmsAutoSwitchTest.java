package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GabiaSmsSenderLmsAutoSwitchTest {

    @Test
    void textBelowThreshold_isSms() {
        GabiaSmsProperties props = new GabiaSmsProperties("u", "i", "k", "f", "/p", 90);
        GabiaSmsSender sender = new GabiaSmsSender(mock(RestClient.class), props);

        assertThat(sender.decideType("hello")).isEqualTo(GabiaSmsSender.TYPE_SMS);
    }

    @Test
    void textExactlyAtThreshold_isSms() {
        GabiaSmsProperties props = new GabiaSmsProperties("u", "i", "k", "f", "/p", 5);
        GabiaSmsSender sender = new GabiaSmsSender(mock(RestClient.class), props);

        assertThat(sender.decideType("hello")).isEqualTo(GabiaSmsSender.TYPE_SMS);
    }

    @Test
    void textAboveThreshold_isLms() {
        GabiaSmsProperties props = new GabiaSmsProperties("u", "i", "k", "f", "/p", 5);
        GabiaSmsSender sender = new GabiaSmsSender(mock(RestClient.class), props);

        assertThat(sender.decideType("hello world")).isEqualTo(GabiaSmsSender.TYPE_LMS);
    }

    @Test
    void koreanCharactersUtf8_3BytesEach() {
        GabiaSmsProperties props = new GabiaSmsProperties("u", "i", "k", "f", "/p", 90);
        GabiaSmsSender sender = new GabiaSmsSender(mock(RestClient.class), props);

        assertThat(sender.decideType("가".repeat(30))).isEqualTo(GabiaSmsSender.TYPE_SMS);
        assertThat(sender.decideType("가".repeat(31))).isEqualTo(GabiaSmsSender.TYPE_LMS);
    }

    @Test
    void nullText_isSms() {
        GabiaSmsProperties props = new GabiaSmsProperties("u", "i", "k", "f", "/p", 90);
        GabiaSmsSender sender = new GabiaSmsSender(mock(RestClient.class), props);

        assertThat(sender.decideType(null)).isEqualTo(GabiaSmsSender.TYPE_SMS);
    }
}
