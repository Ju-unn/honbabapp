package com.example.honbabapp.api.data.request.fcm;


/**
 * 푸시 알림 on/off 설정 변경 요청
 *
 * POST api/user/update_push_setting.php
 * body: { "user_id": 7, "push_enabled": 1 }   (1: ON, 0: OFF)
 */

public class UpdatePushSettingRequest {
    private int user_id;
    private int push_enabled;

    public UpdatePushSettingRequest(int user_id, int push_enabled) {
        this.user_id = user_id;
        this.push_enabled = push_enabled;
    }

    public int getUser_id() {
        return user_id;
    }

    public int getPush_enabled() {
        return push_enabled;
    }
}
