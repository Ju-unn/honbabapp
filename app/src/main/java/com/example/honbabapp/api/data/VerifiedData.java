package com.example.honbabapp.api.data;

public class VerifiedData {
    /**
     * 인증 완료 응답 데이터 클래스
     * verify_code.php의 data 부분
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "이메일 인증이 완료되었습니다",
     *   "data": {
     *     "verified": true
     *   }
     * }
     */

    private boolean verified;

    public VerifiedData() {
    }

    public VerifiedData(boolean verified) {
        this.verified = verified;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
