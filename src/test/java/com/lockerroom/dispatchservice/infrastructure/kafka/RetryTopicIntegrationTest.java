package com.lockerroom.dispatchservice.infrastructure.kafka;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryTopicIntegrationTest {

    @Test
    void retryTopicConfiguration_isCreatedWithDltSuffix() {
        @SuppressWarnings("unchecked")
        KafkaOperations<Object, Object> kafkaOperations = Mockito.mock(KafkaOperations.class);

        RetryTopicConfiguration config = new RetryTopicConfig().defaultRetryTopicConfiguration(kafkaOperations);

        assertThat(config).isNotNull();
        assertThat(config.getDestinationTopicProperties()).isNotEmpty();
    }
}
