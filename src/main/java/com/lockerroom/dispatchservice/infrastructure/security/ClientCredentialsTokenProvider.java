package com.lockerroom.dispatchservice.infrastructure.security;

public interface ClientCredentialsTokenProvider {

    String getAccessToken();

    void invalidate();
}
