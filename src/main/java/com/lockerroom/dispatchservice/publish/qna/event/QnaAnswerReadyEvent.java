package com.lockerroom.dispatchservice.publish.qna.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QnaAnswerReadyEvent(
        String eventId,
        Long postId,
        String content,
        String model,
        Instant generatedAt
) {
}
