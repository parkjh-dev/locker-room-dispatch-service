package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GabiaSmsPropertiesTest {

    @Test
    void blankBaseUrl_isReplacedWithDefault() {
        GabiaSmsProperties props = new GabiaSmsProperties("", "id", "key", "01012345678", null, null);

        assertThat(props.baseUrl()).isEqualTo("https://sms.gabia.com");
    }

    @Test
    void blankSendPath_isReplacedWithDefault() {
        GabiaSmsProperties props = new GabiaSmsProperties("https://x", "id", "key", "01012345678", "", null);

        assertThat(props.sendPath()).isEqualTo("/api/send/sms");
    }

    @Test
    void zeroOrNegativeByteThreshold_isReplacedWith90() {
        GabiaSmsProperties a = new GabiaSmsProperties("u", "i", "k", "f", "/p", 0);
        GabiaSmsProperties b = new GabiaSmsProperties("u", "i", "k", "f", "/p", -5);
        GabiaSmsProperties c = new GabiaSmsProperties("u", "i", "k", "f", "/p", null);

        assertThat(a.smsByteThreshold()).isEqualTo(90);
        assertThat(b.smsByteThreshold()).isEqualTo(90);
        assertThat(c.smsByteThreshold()).isEqualTo(90);
    }

    @Test
    void customValues_areKept() {
        GabiaSmsProperties props = new GabiaSmsProperties(
                "https://my.gabia",
                "myid",
                "mykey",
                "01099998888",
                "/api/v3/send",
                120);

        assertThat(props.baseUrl()).isEqualTo("https://my.gabia");
        assertThat(props.apiId()).isEqualTo("myid");
        assertThat(props.apiKey()).isEqualTo("mykey");
        assertThat(props.defaultFrom()).isEqualTo("01099998888");
        assertThat(props.sendPath()).isEqualTo("/api/v3/send");
        assertThat(props.smsByteThreshold()).isEqualTo(120);
    }
}
