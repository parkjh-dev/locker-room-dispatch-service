package com.lockerroom.dispatchservice.infrastructure.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.lockerroom.dispatchservice.common.response.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCustom_returnsStatusAndCodeFromErrorCode() {
        CustomException ex = new CustomException(ErrorCode.DISPATCH_DUPLICATE_EVENT);

        ResponseEntity<ApiResponse<Void>> response = handler.handleCustom(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DISPATCH_DUPLICATE_EVENT");
    }

    @Test
    void handleCustom_withDetailMessage_usesDetailAsMessage() {
        CustomException ex = new CustomException(ErrorCode.SMS_SEND_FAILED, "발신번호 미등록");

        ResponseEntity<ApiResponse<Void>> response = handler.handleCustom(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody().message()).isEqualTo("발신번호 미등록");
    }

    @Test
    void handleUnknown_returnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new RuntimeException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().code()).isEqualTo("COMMON_INTERNAL_ERROR");
    }
}
