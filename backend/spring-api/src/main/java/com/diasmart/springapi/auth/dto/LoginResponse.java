package com.diasmart.springapi.auth.dto;

import com.diasmart.springapi.users.dto.UserResponse;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresInMs;
    private UserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, Long expiresInMs, UserResponse user) {
        this.accessToken = accessToken;
        this.expiresInMs = expiresInMs;
        this.user = user;
        this.tokenType = "Bearer";
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(Long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}