package com.example.honbabapp.api.data.request.fcm;

/**
 * FCM 디바이스 토큰 등록 요청 모델
 *
 * POST /api/user/register_fcm_token.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class RegisterFcmTokenRequest {
    private String fcm_token;

    public RegisterFcmTokenRequest(String fcm_token) {
        this.fcm_token = fcm_token;
    }

    public String getFcm_token() {
        return fcm_token;
    }
}