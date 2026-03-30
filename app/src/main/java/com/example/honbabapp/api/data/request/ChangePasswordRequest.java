package com.example.honbabapp.api.data.request;

public class ChangePasswordRequest {
    private int user_id;
    private String current_password;
    private String new_password;

    // 3개 모두 필수라 생성자에서 한번에
    public ChangePasswordRequest(int user_id, String current_password, String new_password) {
        this.user_id = user_id;
        this.current_password = current_password;
        this.new_password = new_password;
    }
}
