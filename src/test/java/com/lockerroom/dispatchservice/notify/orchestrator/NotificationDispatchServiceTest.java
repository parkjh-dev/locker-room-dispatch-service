package com.lockerroom.dispatchservice.notify.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lockerroom.dispatchservice.common.metrics.DispatchMetrics;
import com.lockerroom.dispatchservice.infrastructure.kafka.NonRetryableException;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;
import com.lockerroom.dispatchservice.log.DispatchLogTxService;
import com.lockerroom.dispatchservice.log.DispatchLogTxService.EnsureResult;
import com.lockerroom.dispatchservice.log.DispatchStatus;
import com.lockerroom.dispatchservice.notify.channel.ChannelSendOutcome;
import com.lockerroom.dispatchservice.notify.channel.DispatchChannelStrategy;
import com.lockerroom.dispatchservice.notify.channel.RenderedMessage;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;
import com.lockerroom.dispatchservice.notify.recipient.RecipientResolver;
import com.lockerroom.dispatchservice.notify.template.MessageTemplate;
import com.lockerroom.dispatchservice.notify.template.MessageTemplateService;
import com.lockerroom.dispatchservice.notify.template.TemplateRenderer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchServiceTest {

    RecipientResolver resolver;
    MessageTemplateService templateService;
    TemplateRenderer renderer;
    DispatchLogTxService logTxService;
    DispatchMetrics metrics;
    DispatchChannelStrategy mailStrategy;
    DispatchChannelStrategy smsStrategy;
    NotificationDispatchService service;

    @BeforeEach
    void setUp() {
        resolver = mock(RecipientResolver.class);
        templateService = mock(MessageTemplateService.class);
        renderer = new TemplateRenderer();
        logTxService = mock(DispatchLogTxService.class);
        metrics = mock(DispatchMetrics.class);
        mailStrategy = mock(DispatchChannelStrategy.class);
        smsStrategy = mock(DispatchChannelStrategy.class);
        when(mailStrategy.channel()).thenReturn(DispatchChannel.MAIL);
        when(smsStrategy.channel()).thenReturn(DispatchChannel.SMS);

        service = new NotificationDispatchService(
                resolver, templateService, renderer, logTxService, metrics,
                List.of(mailStrategy, smsStrategy));
    }

    @Test
    void dispatch_normalFlow_sendsOnAllPreferredChannels() {
        NotificationDispatchCommand cmd = new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L,
                Map.of("actorNickname", "tester"));
        Recipient recipient = new Recipient(1L, "a@b.c", "01012345678",
                List.of(DispatchChannel.MAIL, DispatchChannel.SMS), true);
        when(resolver.resolve(1L)).thenReturn(Optional.of(recipient));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.MAIL), any()))
                .thenReturn(EnsureResult.pending(11L));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.SMS), any()))
                .thenReturn(EnsureResult.pending(12L));
        when(templateService.findActive(any(), any()))
                .thenReturn(stubTemplate("hi {{actorNickname}}"));
        when(mailStrategy.canSend(any())).thenReturn(true);
        when(smsStrategy.canSend(any())).thenReturn(true);
        when(mailStrategy.send(any(), any(RenderedMessage.class)))
                .thenReturn(ChannelSendOutcome.ok("MID-MAIL"));
        when(smsStrategy.send(any(), any(RenderedMessage.class)))
                .thenReturn(ChannelSendOutcome.ok("MID-SMS"));

        service.dispatch(cmd);

        verify(logTxService).markSuccess(11L, "MID-MAIL");
        verify(logTxService).markSuccess(12L, "MID-SMS");
        verify(logTxService, never()).markFailure(anyLong(), anyString(), anyString());
    }

    @Test
    void dispatch_optOutRecipient_recordsSingleSkippedAndExitsEarly() {
        Recipient recipient = new Recipient(1L, "a@b.c", null,
                List.of(DispatchChannel.MAIL), false);
        when(resolver.resolve(1L)).thenReturn(Optional.of(recipient));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.NONE), any()))
                .thenReturn(EnsureResult.pending(20L));

        service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L, Map.of()));

        verify(logTxService).markSkipped(20L, "OPT_OUT");
        verify(mailStrategy, never()).send(any(), any());
        verify(smsStrategy, never()).send(any(), any());
    }

    @Test
    void dispatch_recipientNotFound_recordsRecipientNotFound() {
        when(resolver.resolve(1L)).thenReturn(Optional.empty());
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.NONE), any()))
                .thenReturn(EnsureResult.pending(30L));

        service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L, Map.of()));

        verify(logTxService).markSkipped(30L, "RECIPIENT_NOT_FOUND");
    }

    @Test
    void dispatch_alreadySuccessOnChannel_skipsThatChannel() {
        Recipient recipient = new Recipient(1L, "a@b.c", "01012345678",
                List.of(DispatchChannel.MAIL, DispatchChannel.SMS), true);
        when(resolver.resolve(1L)).thenReturn(Optional.of(recipient));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.MAIL), any()))
                .thenReturn(EnsureResult.skipped(40L, DispatchStatus.SUCCESS));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.SMS), any()))
                .thenReturn(EnsureResult.pending(41L));
        when(templateService.findActive(any(), eq(DispatchChannel.SMS)))
                .thenReturn(stubTemplate("hi"));
        when(smsStrategy.canSend(any())).thenReturn(true);
        when(smsStrategy.send(any(), any())).thenReturn(ChannelSendOutcome.ok("MID-SMS"));

        service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L, Map.of()));

        verify(mailStrategy, never()).send(any(), any());
        verify(smsStrategy, times(1)).send(any(), any());
        verify(logTxService).markSuccess(41L, "MID-SMS");
    }

    @Test
    void dispatch_oneChannelFailsOtherSucceeds_doesNotThrow() {
        Recipient recipient = new Recipient(1L, "a@b.c", "01012345678",
                List.of(DispatchChannel.MAIL, DispatchChannel.SMS), true);
        when(resolver.resolve(1L)).thenReturn(Optional.of(recipient));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.MAIL), any()))
                .thenReturn(EnsureResult.pending(50L));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.SMS), any()))
                .thenReturn(EnsureResult.pending(51L));
        when(templateService.findActive(any(), any())).thenReturn(stubTemplate("hi"));
        when(mailStrategy.canSend(any())).thenReturn(true);
        when(smsStrategy.canSend(any())).thenReturn(true);
        when(mailStrategy.send(any(), any())).thenReturn(ChannelSendOutcome.failed("E", "smtp down"));
        when(smsStrategy.send(any(), any())).thenReturn(ChannelSendOutcome.ok("MID-SMS"));

        service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L, Map.of()));

        verify(logTxService).markFailure(eq(50L), eq("E"), eq("smtp down"));
        verify(logTxService).markSuccess(51L, "MID-SMS");
    }

    @Test
    void dispatch_allChannelsFail_throwsForRetry() {
        Recipient recipient = new Recipient(1L, "a@b.c", "01012345678",
                List.of(DispatchChannel.MAIL), true);
        when(resolver.resolve(1L)).thenReturn(Optional.of(recipient));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.MAIL), any()))
                .thenReturn(EnsureResult.pending(60L));
        when(templateService.findActive(any(), any())).thenReturn(stubTemplate("hi"));
        when(mailStrategy.canSend(any())).thenReturn(true);
        when(mailStrategy.send(any(), any())).thenReturn(ChannelSendOutcome.failed("E", "x"));

        assertThatThrownBy(() -> service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L, Map.of())))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void dispatch_blankEventId_throwsNonRetryable() {
        assertThatThrownBy(() -> service.dispatch(new NotificationDispatchCommand(
                "", DispatchEventType.NOTI_COMMENT, 1L, Map.of())))
                .isInstanceOf(NonRetryableException.class);
    }

    @Test
    void dispatch_nullUserId_throwsNonRetryable() {
        assertThatThrownBy(() -> service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, null, Map.of())))
                .isInstanceOf(NonRetryableException.class);
    }

    @Test
    void dispatch_rendersVariablesIntoSubjectAndBody() {
        Recipient recipient = new Recipient(1L, "a@b.c", null,
                List.of(DispatchChannel.MAIL), true);
        when(resolver.resolve(1L)).thenReturn(Optional.of(recipient));
        when(logTxService.ensurePending(any(), any(), eq(DispatchChannel.MAIL), any()))
                .thenReturn(EnsureResult.pending(70L));
        when(templateService.findActive(any(), any())).thenReturn(MessageTemplate.builder()
                .subject("[Locker Room] {{actor}}")
                .body("Hi {{actor}}!")
                .channel(DispatchChannel.MAIL)
                .eventType(DispatchEventType.NOTI_COMMENT)
                .version(1)
                .enabled(true)
                .build());
        when(mailStrategy.canSend(any())).thenReturn(true);
        when(mailStrategy.send(any(), any(RenderedMessage.class)))
                .thenReturn(ChannelSendOutcome.ok("MID"));

        service.dispatch(new NotificationDispatchCommand(
                "evt-1", DispatchEventType.NOTI_COMMENT, 1L, Map.of("actor", "tester")));

        ArgumentCaptor<RenderedMessage> captor = ArgumentCaptor.forClass(RenderedMessage.class);
        verify(mailStrategy).send(any(), captor.capture());
        RenderedMessage rendered = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(rendered.subject()).isEqualTo("[Locker Room] tester");
        org.assertj.core.api.Assertions.assertThat(rendered.body()).isEqualTo("Hi tester!");
    }

    private MessageTemplate stubTemplate(String body) {
        return MessageTemplate.builder()
                .eventType(DispatchEventType.NOTI_COMMENT)
                .channel(DispatchChannel.MAIL)
                .version(1)
                .subject("subject")
                .body(body)
                .enabled(true)
                .build();
    }
}
