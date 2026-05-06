package com.lockerroom.dispatchservice.publish.qna;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lockerroom.dispatchservice.common.IdempotencyKeyGenerator;
import com.lockerroom.dispatchservice.common.metrics.DispatchMetrics;
import com.lockerroom.dispatchservice.infrastructure.client.ResourceServiceClient;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreateCommentRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatedIdResponse;
import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.dispatchservice.infrastructure.kafka.NonRetryableException;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchLogTxService;
import com.lockerroom.dispatchservice.log.DispatchStatus;
import com.lockerroom.dispatchservice.publish.qna.event.QnaAnswerReadyEvent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QnaAnswerPublishServiceTest {

    @Mock
    DispatchLogTxService logTxService;

    @Mock
    ResourceServiceClient resourceClient;

    @Mock
    IdempotencyKeyGenerator keyGenerator;

    @Mock
    DispatchMetrics metrics;

    @InjectMocks
    QnaAnswerPublishService service;

    @Test
    void handle_normalEvent_callsCreateCommentAndMarksSuccess() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "evt-1", 100L, "AI answer", "gpt-4o-mini", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(50L));
        when(keyGenerator.fromEventId("evt-1")).thenReturn("idem-uuid");
        when(resourceClient.createComment(eq(100L), any(CreateCommentRequest.class), eq("idem-uuid")))
                .thenReturn(new CreatedIdResponse(777L));

        service.handle(event);

        ArgumentCaptor<CreateCommentRequest> req = ArgumentCaptor.forClass(CreateCommentRequest.class);
        verify(resourceClient).createComment(eq(100L), req.capture(), eq("idem-uuid"));
        org.assertj.core.api.Assertions.assertThat(req.getValue().content()).isEqualTo("AI answer");
        org.assertj.core.api.Assertions.assertThat(req.getValue().isAiGenerated()).isTrue();
        verify(logTxService).markSuccess(50L, "777");
        verify(logTxService, never()).markFailure(anyLong(), anyString(), anyString());
    }

    @Test
    void handle_alreadySuccess_skipsExternalCall() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "evt-dup", 100L, "AI answer", "model", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.skipped(60L, DispatchStatus.SUCCESS));

        service.handle(event);

        verify(resourceClient, never()).createComment(anyLong(), any(), anyString());
        verify(logTxService, never()).markSuccess(anyLong(), anyString());
    }

    @Test
    void handle_resourceServiceFails_marksFailureAndRethrows() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "evt-fail", 100L, "x", "model", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(70L));
        when(keyGenerator.fromEventId("evt-fail")).thenReturn("idem");
        when(resourceClient.createComment(anyLong(), any(), anyString()))
                .thenThrow(new CustomException(ErrorCode.RESOURCE_SERVICE_CALL_FAILED, "500"));

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(CustomException.class);

        verify(logTxService).markFailure(eq(70L), anyString(), anyString());
        verify(logTxService, never()).markSuccess(anyLong(), anyString());
    }

    @Test
    void handle_blankEventId_throwsNonRetryable() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "", 100L, "x", "model", Instant.now());

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(NonRetryableException.class);

        verify(logTxService, never()).ensurePending(any(), any(), any(), any());
    }

    @Test
    void handle_blankContent_throwsNonRetryable() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "evt-1", 100L, "", "model", Instant.now());

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(NonRetryableException.class);
    }

    @Test
    void handle_passesCorrectEventTypeAndChannel() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "evt-1", 100L, "x", "model", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(1L));
        when(keyGenerator.fromEventId(any())).thenReturn("k");
        when(resourceClient.createComment(anyLong(), any(), anyString()))
                .thenReturn(new CreatedIdResponse(1L));

        service.handle(event);

        verify(logTxService).ensurePending(
                eq("evt-1"), eq(DispatchEventType.QNA_PUBLISH), eq(DispatchChannel.NONE), eq(null));
    }

    @Test
    void handle_invokesEnsureBeforeCallingExternal() {
        QnaAnswerReadyEvent event = new QnaAnswerReadyEvent(
                "evt-1", 100L, "x", "model", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(1L));
        when(keyGenerator.fromEventId(any())).thenReturn("k");
        when(resourceClient.createComment(anyLong(), any(), anyString()))
                .thenReturn(new CreatedIdResponse(1L));

        service.handle(event);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(logTxService, resourceClient);
        inOrder.verify(logTxService).ensurePending(any(), any(), any(), any());
        inOrder.verify(resourceClient).createComment(anyLong(), any(), anyString());
        inOrder.verify(logTxService).markSuccess(anyLong(), anyString());
        verify(logTxService, times(0)).markFailure(anyLong(), anyString(), anyString());
    }
}
