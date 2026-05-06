package com.lockerroom.dispatchservice.infrastructure.sms;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.common.PiiMasker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "dispatch.sms.provider", havingValue = "stub", matchIfMissing = true)
public class LogStubSmsSender implements SmsSender {

    @Override
    public SmsResult send(SmsMessage message) {
        String providerMessageId = "STUB-" + UUID.randomUUID();
        log.info("[STUB-SMS] to={}, from={}, text={} (providerMessageId={})",
                PiiMasker.phone(message.to()), message.from(), message.text(), providerMessageId);
        return SmsResult.success(providerMessageId);
    }
}
