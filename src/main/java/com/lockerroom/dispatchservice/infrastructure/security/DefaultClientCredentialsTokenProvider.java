package com.lockerroom.dispatchservice.infrastructure.security;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.exceptions.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultClientCredentialsTokenProvider implements ClientCredentialsTokenProvider {

    static final String REGISTRATION_ID = "dispatch-service";
    static final String PRINCIPAL_NAME = "dispatch-service-system";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(PRINCIPAL_NAME)
                .build();
        OAuth2AuthorizedClient client = authorizedClientManager.authorize(request);
        if (client == null || client.getAccessToken() == null) {
            throw new CustomException(ErrorCode.OAUTH2_TOKEN_ISSUE_FAILED,
                    "Failed to obtain client credentials token for " + REGISTRATION_ID);
        }
        return client.getAccessToken().getTokenValue();
    }

    @Override
    public void invalidate() {
        authorizedClientService.removeAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);
    }
}
