package com.example.honbabapp.api.data.request.fcm;

/**
 * 푸시 알림 on/off 설정 변경 요청
 *
 * POST api/user/update_push_setting.php
 * body: { "push_enabled": 1 }   (1: ON, 0: OFF)
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class UpdatePushSettingRequest {
    private int push_enabled;

    public UpdatePushSettingRequest(int push_enabled) {
        this.push_enabled = push_enabled;
    }

    public int getPush_enabled() {
        return push_enabled;
    }
}