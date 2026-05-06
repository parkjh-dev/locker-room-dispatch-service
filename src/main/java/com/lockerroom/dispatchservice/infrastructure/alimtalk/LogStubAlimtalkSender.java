package com.lockerroom.dispatchservice.infrastructure.alimtalk;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.common.PiiMasker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "dispatch.alimtalk.provider", havingValue = "stub", matchIfMissing = true)
public class LogStubAlimtalkSender implements AlimtalkSender {

    @Override
    public AlimtalkResult send(AlimtalkMessage message) {
        String providerMessageId = "STUB-AT-" + UUID.randomUUID();
        log.info("[STUB-ALIMTALK] to={}, template={}, variables={} (providerMessageId={})",
                PiiMasker.phone(message.to()), message.templateCode(), message.variables(), providerMessageId);
        return AlimtalkResult.success(providerMessageId);
    }
}
