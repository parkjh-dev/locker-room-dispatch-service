package com.lockerroom.dispatchservice.common.metrics;

import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchStatus;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DispatchMetrics {

    public static final String EVENTS_METRIC = "dispatch.events.total";
    public static final String DLQ_METRIC = "dispatch.dlq.total";

    private final MeterRegistry registry;

    public void recordOutcome(DispatchEventType eventType,
                              DispatchChannel channel,
                              DispatchStatus status) {
        registry.counter(EVENTS_METRIC,
                Tags.of(
                        "event_type", safeName(eventType),
                        "channel", safeName(channel == null ? DispatchChannel.NONE : channel),
                        "status", safeName(status)
                )).increment();
    }

    public void recordDlq(String topic, String eventType) {
        registry.counter(DLQ_METRIC,
                Tags.of(
                        "topic", topic == null ? "unknown" : topic,
                        "event_type", eventType == null ? "unknown" : eventType
                )).increment();
    }

    private static String safeName(Enum<?> value) {
        return value == null ? "UNKNOWN" : value.name();
    }
}
