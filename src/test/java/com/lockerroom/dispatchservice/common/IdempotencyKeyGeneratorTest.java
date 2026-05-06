package com.lockerroom.dispatchservice.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyGeneratorTest {

    private final IdempotencyKeyGenerator generator = new IdempotencyKeyGenerator();

    @Test
    void sameEventId_producesSameUuid() {
        String a = generator.fromEventId("evt-1");
        String b = generator.fromEventId("evt-1");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentEventIds_produceDifferentUuids() {
        String a = generator.fromEventId("evt-1");
        String b = generator.fromEventId("evt-2");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void output_matchesUuidFormat() {
        String key = generator.fromEventId("any-id");

        assertThat(key).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void blankEventId_throws() {
        assertThatThrownBy(() -> generator.fromEventId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullEventId_throws() {
        assertThatThrownBy(() -> generator.fromEventId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
