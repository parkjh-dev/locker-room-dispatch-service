package com.lockerroom.dispatchservice.infrastructure.sms;

public class SmsSendException extends RuntimeException {

    private final String resultCode;

    public SmsSendException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public SmsSendException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
