package com.lockerroom.dispatchservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class DispatchServiceApplicationTests {

    @Test
    void mainClass_isAnnotatedWithSpringBootApplication() {
        assertThatCode(() ->
                DispatchServiceApplication.class.getDeclaredAnnotation(
                        org.springframework.boot.autoconfigure.SpringBootApplication.class)
        ).doesNotThrowAnyException();
    }
}
