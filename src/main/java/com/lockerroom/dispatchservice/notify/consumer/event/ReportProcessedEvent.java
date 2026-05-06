package com.lockerroom.dispatchservice.notify.consumer.event;

public record ReportProcessedEvent(
        String eventId,
        Long userId,
        Long reportId,
        String decision
) {
}
