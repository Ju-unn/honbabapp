package com.example.honbabapp.api.data.request.user;

public class CheckCurrentPasswordRequest {
    private int user_id;
    private String current_password;

    public CheckCurrentPasswordRequest(int user_id, String current_password) {
        this.user_id = user_id;
        this.current_password = current_password;
    }
}
