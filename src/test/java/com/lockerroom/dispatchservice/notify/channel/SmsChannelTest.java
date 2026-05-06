package com.lockerroom.dispatchservice.notify.channel;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lockerroom.dispatchservice.infrastructure.sms.SmsMessage;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsResult;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsSender;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsChannelTest {

    @Mock
    SmsSender smsSender;

    @InjectMocks
    SmsChannel channel;

    @Test
    void channel_isSms() {
        assertThat(channel.channel()).isEqualTo(DispatchChannel.SMS);
    }

    @Test
    void canSend_returnsTrueWhenPhonePresent() {
        Recipient recipient = new Recipient(1L, null, "01012345678", List.of(), true);
        assertThat(channel.canSend(recipient)).isTrue();
    }

    @Test
    void canSend_returnsFalseWithoutPhone() {
        assertThat(channel.canSend(new Recipient(1L, "a@b.c", null, List.of(), true))).isFalse();
        assertThat(channel.canSend(new Recipient(1L, "a@b.c", "", List.of(), true))).isFalse();
        assertThat(channel.canSend(null)).isFalse();
    }

    @Test
    void send_passesPhoneAndBody_andReturnsOk() {
        Recipient recipient = new Recipient(1L, null, "01012345678", List.of(), true);
        when(smsSender.send(any(SmsMessage.class))).thenReturn(SmsResult.success("MID"));

        ChannelSendOutcome outcome = channel.send(recipient, new RenderedMessage("subject", "body"));

        ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsSender).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("01012345678");
        assertThat(captor.getValue().text()).isEqualTo("body");
        assertThat(outcome.success()).isTrue();
        assertThat(outcome.providerMessageId()).isEqualTo("MID");
    }

    @Test
    void send_returnsFailedWhenSenderFails() {
        Recipient recipient = new Recipient(1L, null, "01012345678", List.of(), true);
        when(smsSender.send(any(SmsMessage.class)))
                .thenReturn(SmsResult.failure("E001", "invalid number"));

        ChannelSendOutcome outcome = channel.send(recipient, new RenderedMessage("s", "b"));

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("E001");
        assertThat(outcome.errorMessage()).isEqualTo("invalid number");
    }
}
