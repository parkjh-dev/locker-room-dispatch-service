package com.lockerroom.dispatchservice.infrastructure.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lockerroom.dispatchservice.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/info")
public class InfoController {

    @Value("${spring.application.name}")
    private String name;

    @Value("${spring.profiles.active:default}")
    private String profile;

    @GetMapping("/name")
    public ApiResponse<String> name() {
        return ApiResponse.success(name);
    }

    @GetMapping("/profile")
    public ApiResponse<String> profile() {
        return ApiResponse.success(profile);
    }
}
