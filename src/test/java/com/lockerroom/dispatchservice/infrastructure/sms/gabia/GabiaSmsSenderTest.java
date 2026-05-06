package com.lockerroom.dispatchservice.infrastructure.sms.gabia;

import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsMessage;
import com.lockerroom.dispatchservice.infrastructure.sms.SmsResult;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class GabiaSmsSenderTest {

    private WireMockServer wireMock;
    private GabiaSmsSender sender;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());

        GabiaSmsProperties props = new GabiaSmsProperties(
                "http://localhost:" + wireMock.port(),
                "test-id", "test-key", "01099998888",
                "/api/send/sms", 90);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Authorization", "Basic dGVzdC1pZDp0ZXN0LWtleQ==")
                .defaultHeader("Content-Type", "application/json")
                .build();
        sender = new GabiaSmsSender(restClient, props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void send_shortText_usesSmsType_andReturnsSuccess() {
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .withRequestBody(matchingJsonPath("$.type", WireMock.equalTo("sms")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"result":"0000","message":"OK","ref_key":"GABIA-MID-1"}
                                """)));

        SmsResult result = sender.send(SmsMessage.of("01012345678", "01099998888", "short"));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("GABIA-MID-1");
    }

    @Test
    void send_longText_autoSwitchesToLms() {
        String longText = "가".repeat(40);
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .withRequestBody(matchingJsonPath("$.type", WireMock.equalTo("lms")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"result":"0000","message":"OK","ref_key":"GABIA-MID-2"}
                                """)));

        SmsResult result = sender.send(new SmsMessage("01012345678", "01099998888", "subject", longText));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("GABIA-MID-2");
    }

    @Test
    void send_usesDefaultFromWhenMessageFromIsBlank() {
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .withRequestBody(matchingJsonPath("$.from", WireMock.equalTo("01099998888")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"result":"0000","message":"OK","ref_key":"M"}
                                """)));

        SmsResult result = sender.send(new SmsMessage("01012345678", null, null, "hello"));

        assertThat(result.success()).isTrue();
    }

    @Test
    void send_4xxResponse_returnsFailureWithHttpStatusCode() {
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"unauthorized\"}")));

        SmsResult result = sender.send(SmsMessage.of("01012345678", "01099998888", "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.resultCode()).isEqualTo("HTTP_401");
    }

    @Test
    void send_5xxResponse_returnsFailureWithHttpStatusCode() {
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .willReturn(aResponse().withStatus(503)));

        SmsResult result = sender.send(SmsMessage.of("01012345678", "01099998888", "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.resultCode()).isEqualTo("HTTP_503");
    }

    @Test
    void send_providerReturnsErrorCode_returnsFailureWithProviderCode() {
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"result":"E001","message":"Invalid sender number"}
                                """)));

        SmsResult result = sender.send(SmsMessage.of("01012345678", "01099998888", "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.resultCode()).isEqualTo("E001");
        assertThat(result.resultMessage()).isEqualTo("Invalid sender number");
    }

    @Test
    void send_acceptsAlternateSuccessKeys() {
        wireMock.stubFor(post(urlPathEqualTo("/api/send/sms"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"code":"0","message":"ok","messageId":"MID-X"}
                                """)));

        SmsResult result = sender.send(SmsMessage.of("01012345678", "01099998888", "x"));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("MID-X");
    }
}
