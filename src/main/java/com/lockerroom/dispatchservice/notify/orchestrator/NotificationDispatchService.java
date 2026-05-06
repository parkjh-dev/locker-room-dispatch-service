package com.lockerroom.dispatchservice.notify.orchestrator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lockerroom.dispatchservice.common.metrics.DispatchMetrics;
import com.lockerroom.dispatchservice.infrastructure.kafka.NonRetryableException;
import com.lockerroom.dispatchservice.log.DispatchChannel;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationDispatchService {

    private final RecipientResolver recipientResolver;
    private final MessageTemplateService templateService;
    private final TemplateRenderer renderer;
    private final DispatchLogTxService logTxService;
    private final DispatchMetrics metrics;
    private final Map<DispatchChannel, DispatchChannelStrategy> strategies;

    public NotificationDispatchService(RecipientResolver recipientResolver,
                                       MessageTemplateService templateService,
                                       TemplateRenderer renderer,
                                       DispatchLogTxService logTxService,
                                       DispatchMetrics metrics,
                                       List<DispatchChannelStrategy> channelStrategies) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.renderer = renderer;
        this.logTxService = logTxService;
        this.metrics = metrics;
        this.strategies = new EnumMap<>(DispatchChannel.class);
        channelStrategies.forEach(s -> this.strategies.put(s.channel(), s));
    }

    public void dispatch(NotificationDispatchCommand cmd) {
        validate(cmd);

        Optional<Recipient> recipientOpt = recipientResolver.resolve(cmd.userId());
        if (recipientOpt.isEmpty()) {
            log.warn("Recipient not resolvable, skipping. eventId={}, userId={}",
                    cmd.eventId(), cmd.userId());
            recordSingleSkip(cmd, "RECIPIENT_NOT_FOUND");
            return;
        }
        Recipient recipient = recipientOpt.get();

        if (recipient.isOptOut()) {
            log.info("Recipient opted out, skipping. eventId={}, userId={}",
                    cmd.eventId(), cmd.userId());
            recordSingleSkip(cmd, "OPT_OUT");
            return;
        }

        if (recipient.preferredChannels().isEmpty()) {
            log.info("Recipient has no preferred channels. eventId={}, userId={}",
                    cmd.eventId(), cmd.userId());
            recordSingleSkip(cmd, "NO_PREFERRED_CHANNELS");
            return;
        }

        boolean anySuccess = false;
        boolean anyFailed = false;
        for (DispatchChannel channel : recipient.preferredChannels()) {
            DispatchChannelStrategy strategy = strategies.get(channel);
            if (strategy == null) {
                log.warn("No strategy registered for channel={}, skipping. eventId={}",
                        channel, cmd.eventId());
                continue;
            }
            if (!strategy.canSend(recipient)) {
                log.info("Strategy={} cannot send (missing contact). eventId={}",
                        channel, cmd.eventId());
                continue;
            }
            ChannelOutcome outcome = sendOnChannel(cmd, recipient, channel, strategy);
            if (outcome == ChannelOutcome.SUCCESS || outcome == ChannelOutcome.ALREADY_SUCCESS) {
                anySuccess = true;
            } else if (outcome == ChannelOutcome.FAILED) {
                anyFailed = true;
            }
        }

        if (anyFailed && !anySuccess) {
            throw new RuntimeException("All channels failed for eventId=" + cmd.eventId());
        }
    }

    private ChannelOutcome sendOnChannel(NotificationDispatchCommand cmd,
                                         Recipient recipient,
                                         DispatchChannel channel,
                                         DispatchChannelStrategy strategy) {
        EnsureResult ensure = logTxService.ensurePending(
                cmd.eventId(), cmd.eventType(), channel, cmd.userId());
        if (ensure.alreadyDone()) {
            return ensure.existingStatus() == com.lockerroom.dispatchservice.log.DispatchStatus.SUCCESS
                    ? ChannelOutcome.ALREADY_SUCCESS
                    : ChannelOutcome.SKIPPED;
        }

        try {
            MessageTemplate template = templateService.findActive(cmd.eventType(), channel);
            String subject = renderer.render(template.getSubject(), cmd.variables());
            String body = renderer.render(template.getBody(), cmd.variables());
            ChannelSendOutcome sendOutcome = strategy.send(recipient, new RenderedMessage(subject, body));

            if (sendOutcome.success()) {
                logTxService.markSuccess(ensure.logId(), sendOutcome.providerMessageId());
                metrics.recordOutcome(cmd.eventType(), channel, DispatchStatus.SUCCESS);
                return ChannelOutcome.SUCCESS;
            }
            logTxService.markFailure(ensure.logId(),
                    sendOutcome.errorCode(), sendOutcome.errorMessage());
            metrics.recordOutcome(cmd.eventType(), channel, DispatchStatus.FAILED);
            return ChannelOutcome.FAILED;
        } catch (RuntimeException e) {
            log.warn("Channel send threw: eventId={}, channel={}, error={}",
                    cmd.eventId(), channel, e.getMessage());
            logTxService.markFailure(ensure.logId(),
                    e.getClass().getSimpleName(), e.getMessage());
            metrics.recordOutcome(cmd.eventType(), channel, DispatchStatus.FAILED);
            return ChannelOutcome.FAILED;
        }
    }

    private void recordSingleSkip(NotificationDispatchCommand cmd, String reason) {
        EnsureResult ensure = logTxService.ensurePending(
                cmd.eventId(), cmd.eventType(), DispatchChannel.NONE, cmd.userId());
        if (!ensure.alreadyDone()) {
            logTxService.markSkipped(ensure.logId(), reason);
            metrics.recordOutcome(cmd.eventType(), DispatchChannel.NONE, DispatchStatus.SKIPPED);
        }
    }

    private void validate(NotificationDispatchCommand cmd) {
        if (cmd == null) {
            throw new NonRetryableException("command is null");
        }
        if (cmd.eventId() == null || cmd.eventId().isBlank()) {
            throw new NonRetryableException("eventId is blank");
        }
        if (cmd.eventType() == null) {
            throw new NonRetryableException("eventType is null");
        }
        if (cmd.userId() == null) {
            throw new NonRetryableException("userId is null");
        }
    }

    private enum ChannelOutcome {
        SUCCESS,
        ALREADY_SUCCESS,
        FAILED,
        SKIPPED
    }
}
