package com.lockerroom.dispatchservice.infrastructure.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.http.HttpClient;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreateCommentRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatePostRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatedIdResponse;
import com.lockerroom.dispatchservice.infrastructure.client.dto.RecipientContactResponse;
import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.security.ClientCredentialsTokenProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServiceClientTest {

    private WireMockServer wireMock;
    private ClientCredentialsTokenProvider tokenProvider;
    private ResourceServiceClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());

        tokenProvider = mock(ClientCredentialsTokenProvider.class);
        when(tokenProvider.getAccessToken()).thenReturn("token-1", "token-2");

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        client = new ResourceServiceClient(restClient, tokenProvider);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void createComment_sendsBearerAndIdempotencyKey_andUnwrapsApiResponse() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/posts/42/comments"))
                .withHeader("Authorization", equalTo("Bearer token-1"))
                .withHeader("Idempotency-Key", equalTo("idem-123"))
                .withRequestBody(matchingJsonPath("$.content", equalTo("hello")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"code":"SUCCESS","message":"성공","data":{"id":777}}
                                """)));

        CreatedIdResponse response = client.createComment(
                42L, new CreateCommentRequest("hello", true), "idem-123");

        assertThat(response.id()).isEqualTo(777);
    }

    @Test
    void createPost_sendsBoardIdAndIsAiGenerated() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/posts"))
                .withRequestBody(equalToJson("""
                        {"boardId":7,"title":"t","content":"c","isAiGenerated":true}
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"code":"SUCCESS","message":"성공","data":{"id":555}}
                                """)));

        CreatedIdResponse response = client.createPost(
                new CreatePostRequest(7L, "t", "c", true), "idem-1");

        assertThat(response.id()).isEqualTo(555);
    }

    @Test
    void getRecipientContact_unwrapsContactResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/internal/users/9/contact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"code":"SUCCESS","message":"성공","data":{
                                  "userId":9,"email":"a@b.c","phone":"01012345678",
                                  "preferredChannels":["MAIL","SMS"],"notificationOptIn":true
                                }}
                                """)));

        RecipientContactResponse contact = client.getRecipientContact(9L);

        assertThat(contact.userId()).isEqualTo(9L);
        assertThat(contact.email()).isEqualTo("a@b.c");
        assertThat(contact.preferredChannels()).containsExactly("MAIL", "SMS");
        assertThat(contact.notificationOptIn()).isTrue();
    }

    @Test
    void on401_invalidatesTokenAndRetriesOnce() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/posts/1/comments"))
                .withHeader("Authorization", equalTo("Bearer token-1"))
                .willReturn(aResponse().withStatus(401)));
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/posts/1/comments"))
                .withHeader("Authorization", equalTo("Bearer token-2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"code":"SUCCESS","message":"성공","data":{"id":1}}
                                """)));

        CreatedIdResponse response = client.createComment(
                1L, new CreateCommentRequest("c", false), "idem-2");

        assertThat(response.id()).isEqualTo(1L);
        verify(tokenProvider, times(1)).invalidate();
        verify(tokenProvider, times(2)).getAccessToken();
    }

    @Test
    void on500_throwsCustomExceptionWithoutInvalidatingToken() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/posts/1/comments"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.createComment(
                1L, new CreateCommentRequest("c", false), "idem-3"))
                .isInstanceOf(CustomException.class);

        verify(tokenProvider, never()).invalidate();
    }
}
