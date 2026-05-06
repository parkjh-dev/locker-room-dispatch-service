package com.lockerroom.dispatchservice.infrastructure.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMON_INVALID_INPUT("COMMON_INVALID_INPUT", "잘못된 입력입니다.", HttpStatus.BAD_REQUEST),
    COMMON_FORBIDDEN("COMMON_FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    DISPATCH_DUPLICATE_EVENT("DISPATCH_DUPLICATE_EVENT", "이미 처리된 이벤트입니다.", HttpStatus.CONFLICT),
    DISPATCH_RECIPIENT_NOT_FOUND("DISPATCH_RECIPIENT_NOT_FOUND", "수신자 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DISPATCH_TEMPLATE_NOT_FOUND("DISPATCH_TEMPLATE_NOT_FOUND", "메시지 템플릿이 없습니다.", HttpStatus.NOT_FOUND),

    SMS_SEND_FAILED("SMS_SEND_FAILED", "SMS 발송에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    ALIMTALK_SEND_FAILED("ALIMTALK_SEND_FAILED", "알림톡 발송에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    MAIL_SEND_FAILED("MAIL_SEND_FAILED", "메일 발송에 실패했습니다.", HttpStatus.BAD_GATEWAY),

    RESOURCE_SERVICE_CALL_FAILED("RESOURCE_SERVICE_CALL_FAILED", "리소스 서비스 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    OAUTH2_TOKEN_ISSUE_FAILED("OAUTH2_TOKEN_ISSUE_FAILED", "서비스 토큰 발급에 실패했습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
