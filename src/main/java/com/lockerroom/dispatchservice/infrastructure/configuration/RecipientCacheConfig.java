package com.lockerroom.dispatchservice.infrastructure.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;

@Configuration
public class RecipientCacheConfig {

    public static final String RECIPIENT_CACHE = "recipientCache";

    @Bean(RECIPIENT_CACHE)
    public Cache<Long, Recipient> recipientCache(
            @Value("${dispatch.recipient-cache.expire-minutes:10}") long expireMinutes,
            @Value("${dispatch.recipient-cache.max-size:10000}") long maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(expireMinutes))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }
}
