package com.example.honbabapp.api.data.request;

public class SendPasswordResetRequest {
    /**
     * 비밀번호 재설정 인증코드 발송 요청 클래스
     * PHP send_password_reset.php로 전송
     */
    private String email;

    public SendPasswordResetRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
