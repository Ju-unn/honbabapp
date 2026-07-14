package com.example.honbabapp.api.data;

public class FindEmailData {
    /**
     * 아이디(이메일) 찾기 응답 데이터 클래스
     * find_email.php의 data 부분
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "가입된 이메일을 찾았습니다",
     *   "data": {
     *     "masked_email": "ho***@naver.com"
     *   }
     * }
     */

    private String masked_email;    // 마스킹된 이메일

    public FindEmailData() {
    }

    public FindEmailData(String masked_email) {
        this.masked_email = masked_email;
    }

    public String getMasked_email() {
        return masked_email;
    }

    public void setMasked_email(String masked_email) {
        this.masked_email = masked_email;
    }
}
