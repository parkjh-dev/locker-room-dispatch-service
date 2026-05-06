package com.lockerroom.dispatchservice.infrastructure.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.lockerroom.dispatchservice.common.response.ApiResponse;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreateCommentRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatePostRequest;
import com.lockerroom.dispatchservice.infrastructure.client.dto.CreatedIdResponse;
import com.lockerroom.dispatchservice.infrastructure.client.dto.RecipientContactResponse;
import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.dispatchservice.infrastructure.security.ClientCredentialsTokenProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ResourceServiceClient {

    private static final ParameterizedTypeReference<ApiResponse<CreatedIdResponse>> CREATED_REF =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<RecipientContactResponse>> RECIPIENT_REF =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final ClientCredentialsTokenProvider tokenProvider;

    public ResourceServiceClient(RestClient resourceServiceRestClient,
                                 ClientCredentialsTokenProvider tokenProvider) {
        this.restClient = resourceServiceRestClient;
        this.tokenProvider = tokenProvider;
    }

    public CreatedIdResponse createComment(Long postId, CreateCommentRequest request, String idempotencyKey) {
        return executeWith401Retry(() -> {
            ApiResponse<CreatedIdResponse> response = restClient.post()
                    .uri("/api/v1/posts/{postId}/comments", postId)
                    .header("Authorization", bearer())
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(CREATED_REF);
            return unwrap(response);
        });
    }

    public CreatedIdResponse createPost(CreatePostRequest request, String idempotencyKey) {
        return executeWith401Retry(() -> {
            ApiResponse<CreatedIdResponse> response = restClient.post()
                    .uri("/api/v1/posts")
                    .header("Authorization", bearer())
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(CREATED_REF);
            return unwrap(response);
        });
    }

    public RecipientContactResponse getRecipientContact(Long userId) {
        return executeWith401Retry(() -> {
            ApiResponse<RecipientContactResponse> response = restClient.get()
                    .uri("/api/v1/internal/users/{userId}/contact", userId)
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(RECIPIENT_REF);
            return unwrap(response);
        });
    }

    private <T> T executeWith401Retry(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status == HttpStatus.UNAUTHORIZED) {
                log.warn("resource-service returned 401, invalidating token and retrying once");
                tokenProvider.invalidate();
                try {
                    return call.get();
                } catch (RestClientResponseException retried) {
                    throw new CustomException(ErrorCode.RESOURCE_SERVICE_CALL_FAILED,
                            "After token refresh: " + retried.getStatusCode() + " " + retried.getResponseBodyAsString());
                }
            }
            throw new CustomException(ErrorCode.RESOURCE_SERVICE_CALL_FAILED,
                    status + " " + e.getResponseBodyAsString());
        }
    }

    private String bearer() {
        return "Bearer " + tokenProvider.getAccessToken();
    }

    private static <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new CustomException(ErrorCode.RESOURCE_SERVICE_CALL_FAILED, "empty response");
        }
        return response.data();
    }
}
