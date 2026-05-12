package com.diasmart.springapi.auth.dto;

import com.diasmart.springapi.users.dto.UserResponse;

/**
 * LoginResponse is returned after successful authentication.
 *
 * accessToken is used by frontend for future protected requests.
 */
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresInMs;
    private UserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, Long expiresInMs, UserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresInMs() {
        return expiresInMs;
    }

    public UserResponse getUser() {
        return user;
    }
}