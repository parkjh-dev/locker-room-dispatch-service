package com.lockerroom.dispatchservice.infrastructure.client.dto;

import java.util.List;

public record RecipientContactResponse(
        Long userId,
        String email,
        String phone,
        List<String> preferredChannels,
        boolean notificationOptIn
) {
}
