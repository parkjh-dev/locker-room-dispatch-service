package com.lockerroom.dispatchservice.infrastructure.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerErrorHandlerTest {

    private final ConsumerErrorHandler config = new ConsumerErrorHandler();

    @Test
    void defaultErrorHandler_isCreated() {
        DefaultErrorHandler handler = config.defaultErrorHandler();

        assertThat(handler).isNotNull();
    }
}
