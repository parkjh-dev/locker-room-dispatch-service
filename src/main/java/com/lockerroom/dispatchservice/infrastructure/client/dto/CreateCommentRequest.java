package com.lockerroom.dispatchservice.infrastructure.client.dto;

public record CreateCommentRequest(
        String content,
        boolean isAiGenerated
) {
}
