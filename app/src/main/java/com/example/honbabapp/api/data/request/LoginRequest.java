package com.example.honbabapp.api.data.request;

public class LoginRequest {
    public String email;
    public String password;

    public LoginRequest (String email , String password) {
        this.email = email;
        this.password = password;
    }
}
