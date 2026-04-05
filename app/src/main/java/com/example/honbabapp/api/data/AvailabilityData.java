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
     *     "is_restored": true  // 탈퇴 유저인지 확인
     *   }
     * }
     */

    private  boolean available;
    private boolean is_restored;

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

    public boolean isIs_restored() {
        return is_restored;
    }

    public void setIs_restored(boolean is_restored) {
        this.is_restored = is_restored;
    }
}
