package com.lockerroom.dispatchservice.common.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchStatus;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchMetricsTest {

    private MeterRegistry registry;
    private DispatchMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new DispatchMetrics(registry);
    }

    @Test
    void recordOutcome_incrementsCounterWithTags() {
        metrics.recordOutcome(DispatchEventType.QNA_PUBLISH, DispatchChannel.NONE, DispatchStatus.SUCCESS);

        double count = registry.counter(DispatchMetrics.EVENTS_METRIC,
                "event_type", "QNA_PUBLISH",
                "channel", "NONE",
                "status", "SUCCESS").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void recordOutcome_separatesByStatus() {
        metrics.recordOutcome(DispatchEventType.NOTI_COMMENT, DispatchChannel.MAIL, DispatchStatus.SUCCESS);
        metrics.recordOutcome(DispatchEventType.NOTI_COMMENT, DispatchChannel.MAIL, DispatchStatus.FAILED);
        metrics.recordOutcome(DispatchEventType.NOTI_COMMENT, DispatchChannel.MAIL, DispatchStatus.SUCCESS);

        double success = registry.counter(DispatchMetrics.EVENTS_METRIC,
                "event_type", "NOTI_COMMENT", "channel", "MAIL", "status", "SUCCESS").count();
        double failed = registry.counter(DispatchMetrics.EVENTS_METRIC,
                "event_type", "NOTI_COMMENT", "channel", "MAIL", "status", "FAILED").count();
        assertThat(success).isEqualTo(2.0);
        assertThat(failed).isEqualTo(1.0);
    }

    @Test
    void recordOutcome_nullChannel_isNoneTag() {
        metrics.recordOutcome(DispatchEventType.QNA_PUBLISH, null, DispatchStatus.SKIPPED);

        double count = registry.counter(DispatchMetrics.EVENTS_METRIC,
                "event_type", "QNA_PUBLISH", "channel", "NONE", "status", "SKIPPED").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void recordDlq_incrementsDlqCounter() {
        metrics.recordDlq("ai.qna-answer.ready.dlq", "QNA_PUBLISH");
        metrics.recordDlq("ai.qna-answer.ready.dlq", "QNA_PUBLISH");

        double count = registry.counter(DispatchMetrics.DLQ_METRIC,
                "topic", "ai.qna-answer.ready.dlq",
                "event_type", "QNA_PUBLISH").count();
        assertThat(count).isEqualTo(2.0);
    }

    @Test
    void recordDlq_nullTopic_usesUnknown() {
        metrics.recordDlq(null, null);

        double count = registry.counter(DispatchMetrics.DLQ_METRIC,
                "topic", "unknown", "event_type", "unknown").count();
        assertThat(count).isEqualTo(1.0);
    }
}
