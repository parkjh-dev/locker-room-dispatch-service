package com.lockerroom.dispatchservice.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskerTest {

    @Test
    void phone_masksMiddleDigits() {
        assertThat(PiiMasker.phone("01012345678")).isEqualTo("010****5678");
    }

    @Test
    void phone_short_returnsAsIs() {
        assertThat(PiiMasker.phone("0102")).isEqualTo("0102");
    }

    @Test
    void phone_null_returnsNull() {
        assertThat(PiiMasker.phone(null)).isNull();
    }

    @Test
    void email_masksLocalPart() {
        assertThat(PiiMasker.email("alice@example.com")).isEqualTo("a***@example.com");
    }

    @Test
    void email_singleCharLocalPart_returnsAsIs() {
        assertThat(PiiMasker.email("a@b.c")).isEqualTo("a@b.c");
    }

    @Test
    void email_noAt_returnsAsIs() {
        assertThat(PiiMasker.email("not-an-email")).isEqualTo("not-an-email");
    }

    @Test
    void email_null_returnsNull() {
        assertThat(PiiMasker.email(null)).isNull();
    }
}
