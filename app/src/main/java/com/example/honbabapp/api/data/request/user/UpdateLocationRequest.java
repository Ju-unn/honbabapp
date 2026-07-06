package com.example.honbabapp.api.data.request.user;

/**
 * 위치 업데이트 요청 모델
 *
 * POST /api/user/update_location.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class UpdateLocationRequest {
    private String user_location;

    public UpdateLocationRequest(String user_location) {
        this.user_location = user_location;
    }
}