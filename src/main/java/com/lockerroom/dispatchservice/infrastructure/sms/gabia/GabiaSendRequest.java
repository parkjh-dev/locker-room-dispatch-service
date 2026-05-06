package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GabiaSendRequest(
        String type,
        String from,
        String to,
        String subject,
        String text,
        String refkey
) {
}
