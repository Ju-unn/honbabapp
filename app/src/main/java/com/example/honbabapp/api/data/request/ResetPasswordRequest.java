package com.example.honbabapp.api.data.request;

public class ResetPasswordRequest {
    /**
     * 비밀번호 재설정 요청 클래스
     * PHP set_new_password.php로 전송
     */
    private String email;
    private String code;
    private String new_password;

    public ResetPasswordRequest(String email, String code, String newPassword) {
        this.email = email;
        this.code = code;
        this.new_password = newPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return new_password;
    }

    public void setNewPassword(String newPassword) {
        this.new_password = newPassword;
    }
}
