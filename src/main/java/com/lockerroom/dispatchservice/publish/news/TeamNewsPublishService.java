package com.lockerroom.dispatchservice.publish.news;

import org.springframework.stereotype.Service;

import com.lockerroom.dispatchservice.common.IdempotencyKeyGenerator;
import com.lockerroom.dispatchservice.common.metrics.DispatchMetrics;
import com.lockerroom.dispatchservice.infrastructure.client.ResourceServiceClient;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatePostRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatedIdResponse;
import com.lockerroom.dispatchservice.infrastructure.kafka.NonRetryableException;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchLogTxService;
import com.lockerroom.dispatchservice.log.DispatchLogTxService.EnsureResult;
import com.lockerroom.dispatchservice.log.DispatchStatus;
import com.lockerroom.dispatchservice.publish.news.event.TeamNewsReadyEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamNewsPublishService {

    private final DispatchLogTxService logTxService;
    private final ResourceServiceClient resourceClient;
    private final IdempotencyKeyGenerator keyGenerator;
    private final DispatchMetrics metrics;

    public void handle(TeamNewsReadyEvent event) {
        validate(event);

        EnsureResult ensure = logTxService.ensurePending(
                event.eventId(), DispatchEventType.NEWS_PUBLISH, DispatchChannel.NONE, null);

        if (ensure.alreadyDone()) {
            log.info("News publish already done: eventId={}, status={}",
                    event.eventId(), ensure.existingStatus());
            return;
        }

        try {
            CreatedIdResponse response = resourceClient.createPost(
                    new CreatePostRequest(event.boardId(), event.title(), event.content(), true),
                    keyGenerator.fromEventId(event.eventId()));
            logTxService.markSuccess(ensure.logId(), String.valueOf(response.id()));
            metrics.recordOutcome(DispatchEventType.NEWS_PUBLISH, DispatchChannel.NONE, DispatchStatus.SUCCESS);
            log.info("News published: eventId={}, teamId={}, postId={}",
                    event.eventId(), event.teamId(), response.id());
        } catch (RuntimeException e) {
            logTxService.markFailure(ensure.logId(),
                    e.getClass().getSimpleName(),
                    e.getMessage());
            metrics.recordOutcome(DispatchEventType.NEWS_PUBLISH, DispatchChannel.NONE, DispatchStatus.FAILED);
            throw e;
        }
    }

    private void validate(TeamNewsReadyEvent event) {
        if (event == null) {
            throw new NonRetryableException("event is null");
        }
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new NonRetryableException("eventId is blank");
        }
        if (event.boardId() == null) {
            throw new NonRetryableException("boardId is null");
        }
        if (event.title() == null || event.title().isBlank()) {
            throw new NonRetryableException("title is blank");
        }
        if (event.content() == null || event.content().isBlank()) {
            throw new NonRetryableException("content is blank");
        }
    }
}
