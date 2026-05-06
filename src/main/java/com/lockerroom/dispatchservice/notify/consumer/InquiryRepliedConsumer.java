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
import com.lockerroom.dispatchservice.notify.consumer.event.InquiryRepliedEvent;
import com.lockerroom.dispatchservice.notify.orchestrator.NotificationDispatchCommand;
import com.lockerroom.dispatchservice.notify.orchestrator.NotificationDispatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryRepliedConsumer {

    public static final String TOPIC = "notification.inquiry-replied";

    private final NotificationDispatchService dispatchService;
    private final DispatchMetrics metrics;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 5_000L, multiplier = 6.0, maxDelay = 60_000L),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = TOPIC, groupId = "dispatch-service")
    public void onMessage(InquiryRepliedEvent event) {
        Map<String, String> vars = new HashMap<>();
        vars.put("inquiryId", String.valueOf(event.inquiryId()));
        vars.put("replyId", String.valueOf(event.replyId()));

        dispatchService.dispatch(new NotificationDispatchCommand(
                event.eventId(),
                DispatchEventType.NOTI_INQUIRY_REPLIED,
                event.userId(),
                vars));
    }

    @DltHandler
    public void onDlt(InquiryRepliedEvent event,
                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] notification.inquiry-replied: topic={}, eventId={}, userId={}",
                topic, event != null ? event.eventId() : null, event != null ? event.userId() : null);
        metrics.recordDlq(topic, DispatchEventType.NOTI_INQUIRY_REPLIED.name());
    }
}
