package com.example.honbabapp.api.data.request.fcm;

public class RegisterFcmTokenRequest {
    private int user_id;
    private String fcm_token;

    public RegisterFcmTokenRequest(int user_id, String fcm_token) {
        this.user_id = user_id;
        this.fcm_token = fcm_token;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getFcm_token() {
        return fcm_token;
    }
}
