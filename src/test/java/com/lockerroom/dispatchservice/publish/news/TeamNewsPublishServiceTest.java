package com.lockerroom.dispatchservice.publish.news;

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
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatePostRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatedIdResponse;
import com.lockerroom.dispatchservice.infrastructure.kafka.NonRetryableException;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchLogTxService;
import com.lockerroom.dispatchservice.log.DispatchStatus;
import com.lockerroom.dispatchservice.publish.news.event.TeamNewsReadyEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamNewsPublishServiceTest {

    @Mock
    DispatchLogTxService logTxService;

    @Mock
    ResourceServiceClient resourceClient;

    @Mock
    IdempotencyKeyGenerator keyGenerator;

    @Mock
    DispatchMetrics metrics;

    @InjectMocks
    TeamNewsPublishService service;

    @Test
    void handle_normalEvent_createsPostAndMarksSuccess() {
        TeamNewsReadyEvent event = new TeamNewsReadyEvent(
                "evt-1", 7L, "FOOTBALL", 42L, "title", "body", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(80L));
        when(keyGenerator.fromEventId("evt-1")).thenReturn("idem-1");
        when(resourceClient.createPost(any(CreatePostRequest.class), eq("idem-1")))
                .thenReturn(new CreatedIdResponse(999L));

        service.handle(event);

        ArgumentCaptor<CreatePostRequest> req = ArgumentCaptor.forClass(CreatePostRequest.class);
        verify(resourceClient).createPost(req.capture(), eq("idem-1"));
        assertThat(req.getValue().boardId()).isEqualTo(42L);
        assertThat(req.getValue().title()).isEqualTo("title");
        assertThat(req.getValue().isAiGenerated()).isTrue();
        verify(logTxService).markSuccess(80L, "999");
    }

    @Test
    void handle_alreadyDone_skipsExternalCall() {
        TeamNewsReadyEvent event = new TeamNewsReadyEvent(
                "evt-dup", 7L, "FOOTBALL", 42L, "t", "b", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.skipped(81L, DispatchStatus.SUCCESS));

        service.handle(event);

        verify(resourceClient, never()).createPost(any(), anyString());
    }

    @Test
    void handle_blankBoardId_throwsNonRetryable() {
        TeamNewsReadyEvent event = new TeamNewsReadyEvent(
                "evt-1", 7L, "FOOTBALL", null, "t", "b", Instant.now());

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(NonRetryableException.class);
    }

    @Test
    void handle_blankTitle_throwsNonRetryable() {
        TeamNewsReadyEvent event = new TeamNewsReadyEvent(
                "evt-1", 7L, "FOOTBALL", 42L, " ", "b", Instant.now());

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(NonRetryableException.class);
    }

    @Test
    void handle_passesCorrectEventTypeAndChannel() {
        TeamNewsReadyEvent event = new TeamNewsReadyEvent(
                "evt-1", 7L, "FOOTBALL", 42L, "t", "b", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(1L));
        when(keyGenerator.fromEventId(any())).thenReturn("k");
        when(resourceClient.createPost(any(), anyString())).thenReturn(new CreatedIdResponse(1L));

        service.handle(event);

        verify(logTxService).ensurePending(
                eq("evt-1"), eq(DispatchEventType.NEWS_PUBLISH), eq(DispatchChannel.NONE), eq(null));
    }

    @Test
    void handle_resourceFails_marksFailureAndRethrows() {
        TeamNewsReadyEvent event = new TeamNewsReadyEvent(
                "evt-1", 7L, "FOOTBALL", 42L, "t", "b", Instant.now());
        when(logTxService.ensurePending(any(), any(), any(), any()))
                .thenReturn(DispatchLogTxService.EnsureResult.pending(82L));
        when(keyGenerator.fromEventId(any())).thenReturn("k");
        when(resourceClient.createPost(any(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.handle(event)).isInstanceOf(RuntimeException.class);

        verify(logTxService).markFailure(eq(82L), anyString(), anyString());
    }
}
