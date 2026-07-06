package com.example.honbabapp.api.data.request;

/**
 * Access Token 재발급 요청 DTO
 *
 * PHP refresh.php 요청 바디:
 * { "refresh_token": "..." }
 */
public class RefreshTokenRequest {

    private String refresh_token;

    public RefreshTokenRequest(String refreshToken) {
        this.refresh_token = refreshToken;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}