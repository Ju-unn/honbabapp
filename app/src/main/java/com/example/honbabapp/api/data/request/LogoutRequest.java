package com.example.honbabapp.api.data.request;

public class LogoutRequest {

    private int user_id;

    public LogoutRequest(int userId) {
        this.user_id = userId;
    }

    public int getUser_id() {
        return user_id;
    }
}

