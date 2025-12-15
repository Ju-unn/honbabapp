package com.example.honbabapp.api.data;

public class AvailabilityData {
    /**
     * 중복 확인 응답 데이터 클래스
     * check_nickname, check_email, check_phone의 data 부분
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "사용 가능한 닉네임입니다",
     *   "data": {
     *     "available": true
     *   }
     * }
     */

    private  boolean available;

    public AvailabilityData() {
    }

    public AvailabilityData(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
