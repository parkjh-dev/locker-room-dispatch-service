package com.lockerroom.dispatchservice.infrastructure.security;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.exceptions.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultClientCredentialsTokenProviderTest {

    @Mock
    OAuth2AuthorizedClientManager manager;

    @Mock
    OAuth2AuthorizedClientService service;

    @InjectMocks
    DefaultClientCredentialsTokenProvider provider;

    @Test
    void getAccessToken_returnsTokenValue() {
        when(manager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient("the-token"));

        String token = provider.getAccessToken();

        assertThat(token).isEqualTo("the-token");
    }

    @Test
    void getAccessToken_whenManagerReturnsNull_throwsOAuth2Error() {
        when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> provider.getAccessToken())
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.OAUTH2_TOKEN_ISSUE_FAILED);
    }

    @Test
    void invalidate_callsServiceRemoveAuthorizedClient() {
        provider.invalidate();

        verify(service).removeAuthorizedClient(
                DefaultClientCredentialsTokenProvider.REGISTRATION_ID,
                DefaultClientCredentialsTokenProvider.PRINCIPAL_NAME);
    }

    @Test
    void getAccessToken_passesPrincipalNameAndRegistrationId() {
        when(manager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient("t"));

        provider.getAccessToken();

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(manager).authorize(captor.capture());
        assertThat(captor.getValue().getClientRegistrationId())
                .isEqualTo(DefaultClientCredentialsTokenProvider.REGISTRATION_ID);
        assertThat(captor.getValue().getPrincipal().getName())
                .isEqualTo(DefaultClientCredentialsTokenProvider.PRINCIPAL_NAME);
    }

    private OAuth2AuthorizedClient authorizedClient(String tokenValue) {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId(DefaultClientCredentialsTokenProvider.REGISTRATION_ID)
                .clientId("dispatch-service")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost/token")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, tokenValue,
                Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2AuthorizedClient(registration,
                DefaultClientCredentialsTokenProvider.PRINCIPAL_NAME, accessToken);
    }
}
