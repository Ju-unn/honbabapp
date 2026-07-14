package com.example.honbabapp.api.data.request;

public class ResetPasswordRequest {
    /**
     * 임시 비밀번호 발급 요청 클래스
     * PHP reset_password.php로 전송
     * (임시 비밀번호는 메일로만 발송되며 응답에 포함되지 않는다)
     */
    private String email;
    private String code;

    public ResetPasswordRequest(String email, String code) {
        this.email = email;
        this.code = code;
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
}
