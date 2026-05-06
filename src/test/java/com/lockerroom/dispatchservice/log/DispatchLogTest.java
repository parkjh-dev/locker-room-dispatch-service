package com.lockerroom.dispatchservice.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchLogTest {

    @Test
    void markSuccess_setsStatusAndProviderMessageId() {
        DispatchLog log = newPending();

        log.markSuccess("MID-123");

        assertThat(log.getStatus()).isEqualTo(DispatchStatus.SUCCESS);
        assertThat(log.getProviderMessageId()).isEqualTo("MID-123");
        assertThat(log.getErrorCode()).isNull();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void markFailure_incrementsRetryCountAndStoresError() {
        DispatchLog log = newPending();

        log.markFailure("E1", "msg1");
        log.markFailure("E2", "msg2");

        assertThat(log.getStatus()).isEqualTo(DispatchStatus.FAILED);
        assertThat(log.getRetryCount()).isEqualTo(2);
        assertThat(log.getErrorCode()).isEqualTo("E2");
        assertThat(log.getErrorMessage()).isEqualTo("msg2");
    }

    @Test
    void markFailure_truncatesLongErrorMessage() {
        DispatchLog log = newPending();
        String longMessage = "x".repeat(2000);

        log.markFailure("E", longMessage);

        assertThat(log.getErrorMessage()).hasSize(1000);
    }

    @Test
    void markSkipped_setsStatusToSkipped() {
        DispatchLog log = newPending();

        log.markSkipped("OPT_OUT");

        assertThat(log.getStatus()).isEqualTo(DispatchStatus.SKIPPED);
        assertThat(log.getErrorMessage()).isEqualTo("OPT_OUT");
    }

    private DispatchLog newPending() {
        return DispatchLog.builder()
                .eventId("evt-1")
                .eventType(DispatchEventType.NOTI_COMMENT)
                .channel(DispatchChannel.MAIL)
                .status(DispatchStatus.PENDING)
                .retryCount(0)
                .targetUserId(1L)
                .build();
    }
}
