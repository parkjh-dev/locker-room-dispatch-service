package com.lockerroom.dispatchservice.notify.channel;

import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.infrastructure.mail.MailMessage;
import com.lockerroom.dispatchservice.infrastructure.mail.MailResult;
import com.lockerroom.dispatchservice.infrastructure.mail.MailSender;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailChannel implements DispatchChannelStrategy {

    private final MailSender mailSender;

    @Override
    public DispatchChannel channel() {
        return DispatchChannel.MAIL;
    }

    @Override
    public boolean canSend(Recipient recipient) {
        return recipient != null && recipient.email() != null && !recipient.email().isBlank();
    }

    @Override
    public ChannelSendOutcome send(Recipient recipient, RenderedMessage message) {
        MailResult result = mailSender.send(MailMessage.text(
                recipient.email(),
                message.subject(),
                message.body()));
        if (result.success()) {
            return ChannelSendOutcome.ok(null);
        }
        return ChannelSendOutcome.failed(result.errorCode(), result.errorMessage());
    }
}
