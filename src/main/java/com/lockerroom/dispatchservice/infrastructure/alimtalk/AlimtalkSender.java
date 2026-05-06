package com.lockerroom.dispatchservice.infrastructure.alimtalk;

public interface AlimtalkSender {

    AlimtalkResult send(AlimtalkMessage message);
}
