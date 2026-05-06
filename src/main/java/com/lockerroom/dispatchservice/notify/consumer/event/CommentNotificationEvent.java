package com.lockerroom.dispatchservice.notify.consumer.event;

public record CommentNotificationEvent(
        String eventId,
        Long userId,
        Long postId,
        Long commentId,
        String actorNickname
) {
}
