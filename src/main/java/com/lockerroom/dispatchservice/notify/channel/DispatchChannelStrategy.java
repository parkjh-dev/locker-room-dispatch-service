package com.lockerroom.dispatchservice.notify.channel;

import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;

public interface DispatchChannelStrategy {

    DispatchChannel channel();

    boolean canSend(Recipient recipient);

    ChannelSendOutcome send(Recipient recipient, RenderedMessage message);
}
