package com.lockerroom.dispatchservice.notify.orchestrator;

import java.util.Map;

import com.lockerroom.dispatchservice.log.DispatchEventType;

public record NotificationDispatchCommand(
        String eventId,
        DispatchEventType eventType,
        Long userId,
        Map<String, String> variables
) {
    public NotificationDispatchCommand {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
