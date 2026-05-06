package com.lockerroom.dispatchservice.notify.template;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    Optional<MessageTemplate> findTopByEventTypeAndChannelAndEnabledTrueOrderByVersionDesc(
            DispatchEventType eventType, DispatchChannel channel);
}
