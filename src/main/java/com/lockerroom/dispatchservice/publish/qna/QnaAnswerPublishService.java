package com.lockerroom.dispatchservice.publish.qna;

import org.springframework.stereotype.Service;

import com.lockerroom.dispatchservice.common.IdempotencyKeyGenerator;
import com.lockerroom.dispatchservice.common.metrics.DispatchMetrics;
import com.lockerroom.dispatchservice.infrastructure.client.ResourceServiceClient;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreateCommentRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatedIdResponse;
import com.lockerroom.dispatchservice.infrastructure.kafka.NonRetryableException;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchLogTxService;
import com.lockerroom.dispatchservice.log.DispatchLogTxService.EnsureResult;
import com.lockerroom.dispatchservice.log.DispatchStatus;
import com.lockerroom.dispatchservice.publish.qna.event.QnaAnswerReadyEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaAnswerPublishService {

    private final DispatchLogTxService logTxService;
    private final ResourceServiceClient resourceClient;
    private final IdempotencyKeyGenerator keyGenerator;
    private final DispatchMetrics metrics;

    public void handle(QnaAnswerReadyEvent event) {
        validate(event);

        EnsureResult ensure = logTxService.ensurePending(
                event.eventId(), DispatchEventType.QNA_PUBLISH, DispatchChannel.NONE, null);

        if (ensure.alreadyDone()) {
            log.info("QNA publish already done: eventId={}, status={}",
                    event.eventId(), ensure.existingStatus());
            return;
        }

        try {
            CreatedIdResponse response = resourceClient.createComment(
                    event.postId(),
                    new CreateCommentRequest(event.content(), true),
                    keyGenerator.fromEventId(event.eventId()));
            logTxService.markSuccess(ensure.logId(), String.valueOf(response.id()));
            metrics.recordOutcome(DispatchEventType.QNA_PUBLISH, DispatchChannel.NONE, DispatchStatus.SUCCESS);
            log.info("QNA published: eventId={}, postId={}, commentId={}",
                    event.eventId(), event.postId(), response.id());
        } catch (RuntimeException e) {
            logTxService.markFailure(ensure.logId(),
                    e.getClass().getSimpleName(),
                    e.getMessage());
            metrics.recordOutcome(DispatchEventType.QNA_PUBLISH, DispatchChannel.NONE, DispatchStatus.FAILED);
            throw e;
        }
    }

    private void validate(QnaAnswerReadyEvent event) {
        if (event == null) {
            throw new NonRetryableException("event is null");
        }
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new NonRetryableException("eventId is blank");
        }
        if (event.postId() == null) {
            throw new NonRetryableException("postId is null");
        }
        if (event.content() == null || event.content().isBlank()) {
            throw new NonRetryableException("content is blank");
        }
    }
}
