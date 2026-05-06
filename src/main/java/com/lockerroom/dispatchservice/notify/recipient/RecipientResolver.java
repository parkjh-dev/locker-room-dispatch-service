package com.lockerroom.dispatchservice.notify.recipient;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.lockerroom.dispatchservice.infrastructure.client.ResourceServiceClient;
import com.lockerroom.dispatchservice.infrastructure.client.dto.RecipientContactResponse;
import com.lockerroom.dispatchservice.infrastructure.configuration.RecipientCacheConfig;
import com.lockerroom.dispatchservice.log.DispatchChannel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RecipientResolver {

    private final ResourceServiceClient resourceClient;
    private final Cache<Long, Recipient> cache;

    public RecipientResolver(ResourceServiceClient resourceClient,
                             @Qualifier(RecipientCacheConfig.RECIPIENT_CACHE) Cache<Long, Recipient> cache) {
        this.resourceClient = resourceClient;
        this.cache = cache;
    }

    public Optional<Recipient> resolve(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        Recipient cached = cache.getIfPresent(userId);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            RecipientContactResponse response = resourceClient.getRecipientContact(userId);
            Recipient recipient = toDomain(response);
            cache.put(userId, recipient);
            return Optional.of(recipient);
        } catch (RuntimeException e) {
            log.warn("Failed to resolve recipient: userId={}, error={}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void invalidate(Long userId) {
        if (userId != null) {
            cache.invalidate(userId);
        }
    }

    private static Recipient toDomain(RecipientContactResponse r) {
        List<DispatchChannel> channels = r.preferredChannels() == null
                ? List.of()
                : r.preferredChannels().stream()
                        .map(s -> {
                            try {
                                return DispatchChannel.valueOf(s);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(c -> c != null && c != DispatchChannel.NONE)
                        .toList();
        return new Recipient(r.userId(), r.email(), r.phone(), channels, r.notificationOptIn());
    }
}
