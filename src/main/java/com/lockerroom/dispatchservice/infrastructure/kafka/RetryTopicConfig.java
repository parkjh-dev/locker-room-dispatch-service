package com.lockerroom.dispatchservice.infrastructure.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

@Configuration
public class RetryTopicConfig {

    @Bean
    public RetryTopicConfiguration defaultRetryTopicConfiguration(KafkaOperations<?, ?> kafkaTemplate) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .maxAttempts(3)
                .exponentialBackoff(5_000L, 6.0, 60_000L)
                .autoCreateTopicsWith(1, (short) 1)
                .dltSuffix(".dlq")
                .doNotRetryOnDltFailure()
                .create(kafkaTemplate);
    }
}
