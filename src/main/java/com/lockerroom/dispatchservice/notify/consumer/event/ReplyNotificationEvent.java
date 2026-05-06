package com.lockerroom.dispatchservice.notify.consumer.event;

public record ReplyNotificationEvent(
        String eventId,
        Long userId,
        Long postId,
        Long parentCommentId,
        Long replyId,
        String actorNickname
) {
}
