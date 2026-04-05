package com.example.honbabapp.api.data.request.user;

public class UpdateProfileRequest {
    // Gson이 이 필드명을 그대로 JSON 키로 변환
    // {"user_id": 1, "user_nickname": "홍길동"} 이렇게 변환됨

    private int user_id;
    private String user_nickname;      // 선택적
    private String user_status_message; // 선택적
    private String user_profile_image;  // 선택적 (S3 URL)

    // 생성자: 필수값인 user_id만 받고, 나머지는 setter로
    public UpdateProfileRequest(int user_id) {
        this.user_id = user_id;
    }

    // Setter들 - 바꿀 필드만 세팅하면 됨
    public void setUser_nickname(String user_nickname) {
        this.user_nickname = user_nickname;
    }
    public void setUser_status_message(String user_status_message) {
        this.user_status_message = user_status_message;
    }
    public void setUser_profile_image(String user_profile_image) {
        this.user_profile_image = user_profile_image;
    }
}
