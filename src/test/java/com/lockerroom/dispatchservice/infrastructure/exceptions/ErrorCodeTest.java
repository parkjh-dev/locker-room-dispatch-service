package com.lockerroom.dispatchservice.infrastructure.exceptions;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void allErrorCodes_haveNonBlankCodeAndMessageAndStatus() {
        Arrays.stream(ErrorCode.values()).forEach(code -> {
            assertThat(code.getCode()).isNotBlank();
            assertThat(code.getMessage()).isNotBlank();
            assertThat(code.getStatus()).isNotNull();
        });
    }

    @Test
    void codes_areUnique() {
        long unique = Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).distinct().count();
        assertThat(unique).isEqualTo(ErrorCode.values().length);
    }
}
