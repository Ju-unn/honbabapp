package com.example.honbabapp.api.data.request.user;

/**
 * 비밀번호 변경 요청 모델
 *
 * POST /api/user/change_password.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class ChangePasswordRequest {
    private String current_password;
    private String new_password;

    public ChangePasswordRequest(String current_password, String new_password) {
        this.current_password = current_password;
        this.new_password = new_password;
    }
}