package com.lockerroom.dispatchservice.publish.news;

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
import com.lockerroom.dispatchservice.publish.news.event.TeamNewsReadyEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamNewsPublishConsumer {

    public static final String TOPIC = "ai.team-news.ready";

    private final TeamNewsPublishService service;
    private final DispatchMetrics metrics;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 5_000L, multiplier = 6.0, maxDelay = 60_000L),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = TOPIC, groupId = "dispatch-service")
    public void onMessage(TeamNewsReadyEvent event) {
        service.handle(event);
    }

    @DltHandler
    public void onDlt(TeamNewsReadyEvent event,
                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] team-news published to DLT: topic={}, eventId={}, teamId={}",
                topic, event != null ? event.eventId() : null, event != null ? event.teamId() : null);
        metrics.recordDlq(topic, DispatchEventType.NEWS_PUBLISH.name());
    }
}
