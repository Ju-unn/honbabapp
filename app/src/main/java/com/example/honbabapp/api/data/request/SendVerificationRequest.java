package com.example.honbabapp.api.data.request;

public class SendVerificationRequest {
    /**
     * 이메일 인증 코드 전송 요청 클래스
     * PHP send_verification.php로 전송
     */
    private String email;

    public SendVerificationRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
