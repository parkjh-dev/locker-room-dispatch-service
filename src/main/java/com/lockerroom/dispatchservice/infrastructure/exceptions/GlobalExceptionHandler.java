package com.lockerroom.dispatchservice.infrastructure.exceptions;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.lockerroom.dispatchservice.common.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException ex) {
        ErrorCode code = ex.getErrorCode();
        log.warn("CustomException: code={}, message={}", code.getCode(), ex.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorItem>>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getStatus())
                .body(new ApiResponse<>(
                        ErrorCode.COMMON_INVALID_INPUT.getCode(),
                        ErrorCode.COMMON_INVALID_INPUT.getMessage(),
                        errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode code = ErrorCode.COMMON_INTERNAL_ERROR;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }

    public record FieldErrorItem(String field, String message) {
    }
}
