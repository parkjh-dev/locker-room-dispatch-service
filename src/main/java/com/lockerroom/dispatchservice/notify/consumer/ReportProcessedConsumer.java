package com.lockerroom.dispatchservice.notify.consumer;

import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.common.metrics.DispatchMetrics;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.notify.consumer.event.ReportProcessedEvent;
import com.lockerroom.dispatchservice.notify.orchestrator.NotificationDispatchCommand;
import com.lockerroom.dispatchservice.notify.orchestrator.NotificationDispatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportProcessedConsumer {

    public static final String TOPIC = "notification.report-processed";

    private final NotificationDispatchService dispatchService;
    private final DispatchMetrics metrics;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 5_000L, multiplier = 6.0, maxDelay = 60_000L),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = TOPIC, groupId = "dispatch-service")
    public void onMessage(ReportProcessedEvent event) {
        Map<String, String> vars = new HashMap<>();
        vars.put("reportId", String.valueOf(event.reportId()));
        vars.put("decision", event.decision() == null ? "" : event.decision());

        dispatchService.dispatch(new NotificationDispatchCommand(
                event.eventId(),
                DispatchEventType.NOTI_REPORT_PROCESSED,
                event.userId(),
                vars));
    }

    @DltHandler
    public void onDlt(ReportProcessedEvent event,
                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] notification.report-processed: topic={}, eventId={}, userId={}",
                topic, event != null ? event.eventId() : null, event != null ? event.userId() : null);
        metrics.recordDlq(topic, DispatchEventType.NOTI_REPORT_PROCESSED.name());
    }
}
