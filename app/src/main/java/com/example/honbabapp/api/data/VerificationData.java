package com.example.honbabapp.api.data;

public class VerificationData {
    /**
     * 인증 코드 발송 응답 데이터 클래스
     * send_verification.php의 data 부분
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "인증 코드가 발송되었습니다",
     *   "data": {
     *     "code": "123456",  // 개발 단계에서만 포함
     *     "expires_in": 300
     *   }
     * }
     */

    private String code;        // 인증 코드 (개발용)
    private int expires_in;     // 만료 시간 (초)

    public VerificationData() {
    }

    public VerificationData(String code, int expires_in) {
        this.code = code;
        this.expires_in = expires_in;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(int expires_in) {
        this.expires_in = expires_in;
    }
}
