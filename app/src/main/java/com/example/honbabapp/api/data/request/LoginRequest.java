package com.example.honbabapp.api.data.request;

public class LoginRequest {

    /**
     * 로그인 요청 모델
     * POST /api/auth/login.php
     */

    public String email;
    public String password;

    public LoginRequest (String email , String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
