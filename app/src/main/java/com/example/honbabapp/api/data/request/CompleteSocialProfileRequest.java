package com.example.honbabapp.api.data.request.user;

/**
 * 소셜 로그인 온보딩(추가정보 입력) 요청 모델
 * 모든 필드는 null 가능 - null이면 서버가 해당 필드를 업데이트하지 않음
 */
public class CompleteSocialProfileRequest {
    private String nickname;
    private String phone_number;
    private String location;
    private String gender;
    private String birthday;

    public CompleteSocialProfileRequest(String nickname, String phoneNumber,
                                        String location, String gender, String birthday) {
        this.nickname = nickname;
        this.phone_number = phoneNumber;
        this.location = location;
        this.gender = gender;
        this.birthday = birthday;
    }
}