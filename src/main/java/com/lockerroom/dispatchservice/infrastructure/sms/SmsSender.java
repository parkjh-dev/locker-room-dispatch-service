package com.lockerroom.dispatchservice.infrastructure.sms;

public interface SmsSender {

    SmsResult send(SmsMessage message);
}
