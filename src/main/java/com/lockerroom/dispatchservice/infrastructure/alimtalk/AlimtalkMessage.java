package com.lockerroom.dispatchservice.infrastructure.alimtalk;

import java.util.Map;

public record AlimtalkMessage(
        String to,
        String templateCode,
        Map<String, String> variables,
        String fallbackText
) {
    public AlimtalkMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Alimtalk to must not be blank");
        }
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("Alimtalk templateCode must not be blank");
        }
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
