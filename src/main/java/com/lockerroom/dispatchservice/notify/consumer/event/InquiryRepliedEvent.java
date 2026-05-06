package com.lockerroom.dispatchservice.notify.consumer.event;

public record InquiryRepliedEvent(
        String eventId,
        Long userId,
        Long inquiryId,
        Long replyId
) {
}
