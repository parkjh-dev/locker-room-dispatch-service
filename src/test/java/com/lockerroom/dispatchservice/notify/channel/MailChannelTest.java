package com.lockerroom.dispatchservice.notify.channel;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lockerroom.dispatchservice.infrastructure.mail.MailMessage;
import com.lockerroom.dispatchservice.infrastructure.mail.MailResult;
import com.lockerroom.dispatchservice.infrastructure.mail.MailSender;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.notify.recipient.Recipient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailChannelTest {

    @Mock
    MailSender mailSender;

    @InjectMocks
    MailChannel channel;

    @Test
    void channel_isMail() {
        assertThat(channel.channel()).isEqualTo(DispatchChannel.MAIL);
    }

    @Test
    void canSend_returnsTrueForRecipientWithEmail() {
        Recipient recipient = new Recipient(1L, "a@b.c", null, List.of(), true);

        assertThat(channel.canSend(recipient)).isTrue();
    }

    @Test
    void canSend_returnsFalseForRecipientWithoutEmail() {
        assertThat(channel.canSend(new Recipient(1L, null, "01012345678", List.of(), true))).isFalse();
        assertThat(channel.canSend(new Recipient(1L, "", "01012345678", List.of(), true))).isFalse();
        assertThat(channel.canSend(null)).isFalse();
    }

    @Test
    void send_delegatesToMailSender_andReturnsOkOnSuccess() {
        Recipient recipient = new Recipient(1L, "user@example.com", null, List.of(), true);
        when(mailSender.send(any(MailMessage.class))).thenReturn(MailResult.ok());

        ChannelSendOutcome outcome = channel.send(recipient, new RenderedMessage("subject", "body"));

        assertThat(outcome.success()).isTrue();
        ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
        org.mockito.Mockito.verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("user@example.com");
        assertThat(captor.getValue().subject()).isEqualTo("subject");
        assertThat(captor.getValue().body()).isEqualTo("body");
    }

    @Test
    void send_returnsFailedWhenMailSenderFails() {
        Recipient recipient = new Recipient(1L, "user@example.com", null, List.of(), true);
        when(mailSender.send(any(MailMessage.class)))
                .thenReturn(MailResult.failure("MailException", "smtp down"));

        ChannelSendOutcome outcome = channel.send(recipient, new RenderedMessage("s", "b"));

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("MailException");
        assertThat(outcome.errorMessage()).isEqualTo("smtp down");
    }
}
