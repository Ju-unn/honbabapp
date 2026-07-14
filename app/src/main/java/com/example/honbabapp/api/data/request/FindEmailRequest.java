package com.example.honbabapp.api.data.request;

public class FindEmailRequest {
    /**
     * 아이디(이메일) 찾기 요청 클래스
     * PHP find_email.php로 전송
     */
    private String phone_number;

    public FindEmailRequest(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }
}
