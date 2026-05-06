package com.lockerroom.dispatchservice.common;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyGenerator {

    private static final String NAMESPACE = "dispatch-service:";

    public String fromEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return UUID.nameUUIDFromBytes(
                (NAMESPACE + eventId).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
