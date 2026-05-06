package com.lockerroom.dispatchservice.publish.news.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamNewsReadyEvent(
        String eventId,
        Long teamId,
        String sport,
        Long boardId,
        String title,
        String content,
        Instant generatedAt
) {
}
