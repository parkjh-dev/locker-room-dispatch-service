package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dispatch.sms.gabia")
public record GabiaSmsProperties(
        String baseUrl,
        String apiId,
        String apiKey,
        String defaultFrom,
        String sendPath,
        Integer smsByteThreshold
) {
    public GabiaSmsProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://sms.gabia.com";
        }
        if (sendPath == null || sendPath.isBlank()) {
            sendPath = "/api/send/sms";
        }
        if (smsByteThreshold == null || smsByteThreshold <= 0) {
            smsByteThreshold = 90;
        }
    }
}
