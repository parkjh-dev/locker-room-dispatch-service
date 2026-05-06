package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GabiaSendResponse(
        @JsonAlias({"result", "code"}) String result,
        String message,
        @JsonAlias({"ref_key", "refKey", "messageId", "message_id"}) String refKey
) {

    public boolean isSuccess() {
        return "0000".equals(result) || "0".equals(result) || "OK".equalsIgnoreCase(result);
    }
}
