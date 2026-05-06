package com.lockerroom.dispatchservice.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_withData_returnsOkCodeAndData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("hello");
    }

    @Test
    void success_withoutData_returnsNullData() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isNull();
    }

    @Test
    void error_returnsGivenCodeAndMessage() {
        ApiResponse<String> response = ApiResponse.error("E_TEST", "잘못됨");

        assertThat(response.code()).isEqualTo("E_TEST");
        assertThat(response.message()).isEqualTo("잘못됨");
        assertThat(response.data()).isNull();
    }
}
