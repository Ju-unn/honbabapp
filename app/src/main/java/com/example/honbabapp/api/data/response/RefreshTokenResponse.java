package com.example.honbabapp.api.data.response;

/**
 * Access Token 재발급 응답 DTO
 *
 * PHP refresh.php 응답 데이터:
 * { "access_token": "...", "refresh_token": "..." }
 */
public class RefreshTokenResponse {

    private String access_token;
    private String refresh_token;

    public String getAccess_token() {
        return access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}