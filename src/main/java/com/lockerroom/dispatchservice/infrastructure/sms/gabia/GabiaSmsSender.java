package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.lockerroom.dispatchservice.infrastructure.sms.SmsMessage;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsResult;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsSender;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "dispatch.sms.provider", havingValue = "gabia")
public class GabiaSmsSender implements SmsSender {

    static final String TYPE_SMS = "sms";
    static final String TYPE_LMS = "lms";

    private final RestClient restClient;
    private final GabiaSmsProperties properties;

    public GabiaSmsSender(RestClient gabiaSmsRestClient, GabiaSmsProperties properties) {
        this.restClient = gabiaSmsRestClient;
        this.properties = properties;
    }

    @Override
    public SmsResult send(SmsMessage message) {
        String type = decideType(message.text());
        String from = message.from() != null && !message.from().isBlank()
                ? message.from()
                : properties.defaultFrom();
        GabiaSendRequest request = new GabiaSendRequest(
                type,
                from,
                message.to(),
                message.subject(),
                message.text(),
                UUID.randomUUID().toString());

        try {
            GabiaSendResponse response = restClient.post()
                    .uri(properties.sendPath())
                    .body(request)
                    .retrieve()
                    .body(GabiaSendResponse.class);

            if (response == null) {
                return SmsResult.failure("EMPTY_RESPONSE", "Gabia returned null body");
            }
            if (response.isSuccess()) {
                return SmsResult.success(response.refKey());
            }
            return SmsResult.failure(response.result(), response.message());
        } catch (RestClientResponseException e) {
            log.warn("Gabia SMS HTTP error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return SmsResult.failure(
                    "HTTP_" + e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.warn("Gabia SMS network error: {}", e.getMessage());
            return SmsResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    String decideType(String text) {
        if (text == null) return TYPE_SMS;
        int byteLen = text.getBytes(StandardCharsets.UTF_8).length;
        return byteLen > properties.smsByteThreshold() ? TYPE_LMS : TYPE_SMS;
    }
}
