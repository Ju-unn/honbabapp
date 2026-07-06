package com.example.honbabapp.api.data.request.user;

/**
 * 현재 비밀번호 확인 요청 모델
 *
 * POST /api/user/check_current_password.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class CheckCurrentPasswordRequest {
    private String current_password;

    public CheckCurrentPasswordRequest(String current_password) {
        this.current_password = current_password;
    }
}