package com.example.honbabapp.api.data.request;

public class VerifyCodeRequest {
    /**
     * 인증 코드 검증 요청 클래스
     * PHP verify_code.php로 전송
     */

    private String email;
    private String code;
    private String purpose;

    public VerifyCodeRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public VerifyCodeRequest(String email, String code, String purpose) {
        this.email = email;
        this.code = code;
        this.purpose = purpose;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
