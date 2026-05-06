package com.lockerroom.dispatchservice.notify.recipient;

import java.util.List;

import com.lockerroom.dispatchservice.log.DispatchChannel;

public record Recipient(
        Long userId,
        String email,
        String phone,
        List<DispatchChannel> preferredChannels,
        boolean optIn
) {
    public Recipient {
        preferredChannels = preferredChannels == null ? List.of() : List.copyOf(preferredChannels);
    }

    public boolean isOptOut() {
        return !optIn;
    }
}
