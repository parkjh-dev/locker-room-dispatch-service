package com.lockerroom.dispatchservice.notify.template;

import org.springframework.stereotype.Service;

import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageTemplateService {

    private final MessageTemplateRepository repository;

    public MessageTemplate findActive(DispatchEventType eventType, DispatchChannel channel) {
        return repository
                .findTopByEventTypeAndChannelAndEnabledTrueOrderByVersionDesc(eventType, channel)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.DISPATCH_TEMPLATE_NOT_FOUND,
                        "No active template for eventType=" + eventType + ", channel=" + channel));
    }
}
