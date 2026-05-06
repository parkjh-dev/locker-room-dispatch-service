package com.lockerroom.dispatchservice.notify.channel;

import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.infrastructure.sms.SmsMessage;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsResult;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsSender;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SmsChannel implements DispatchChannelStrategy {

    private final SmsSender smsSender;

    @Override
    public DispatchChannel channel() {
        return DispatchChannel.SMS;
    }

    @Override
    public boolean canSend(Recipient recipient) {
        return recipient != null && recipient.phone() != null && !recipient.phone().isBlank();
    }

    @Override
    public ChannelSendOutcome send(Recipient recipient, RenderedMessage message) {
        SmsResult result = smsSender.send(new SmsMessage(
                recipient.phone(),
                null,
                message.subject(),
                message.body()));
        if (result.success()) {
            return ChannelSendOutcome.ok(result.providerMessageId());
        }
        return ChannelSendOutcome.failed(result.resultCode(), result.resultMessage());
    }
}
