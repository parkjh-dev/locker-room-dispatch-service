package com.lockerroom.dispatchservice.infrastructure.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import jakarta.validation.ConstraintViolationException;

@Configuration
public class ConsumerErrorHandler {

    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                ConstraintViolationException.class,
                NonRetryableException.class
        );
        return handler;
    }
}
