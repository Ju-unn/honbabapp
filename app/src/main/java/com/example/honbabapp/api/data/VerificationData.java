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
     *     "expires_in": 300
     *   }
     * }
     * 인증코드 자체는 이메일로만 발송되며 응답에 포함되지 않는다.
     */

    private int expires_in;     // 만료 시간 (초)

    public VerificationData() {
    }

    public VerificationData(int expires_in) {
        this.expires_in = expires_in;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(int expires_in) {
        this.expires_in = expires_in;
    }
}
