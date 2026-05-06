package com.lockerroom.dispatchservice.infrastructure.client.dto;

public record CreatePostRequest(
        Long boardId,
        String title,
        String content,
        boolean isAiGenerated
) {
}
