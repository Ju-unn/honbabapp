package com.example.honbabapp.api.data.login;

/**
 * 로그인 응답 데이터 모델
 *
 * PHP 응답 형식:
 * {
 *   "success": true,
 *   "message": "로그인 성공",
 *   "data": {
 *     "user_id": 1,
 *     "user_email": "test@example.com",
 *     "user_nickname": "테스트유저",
 *     "user_profile_image": null,
 *     "user_gender": "남성",
 *     "user_birthday": "1990-01-01",
 *     "user_location": "서울특별시 관악구 대학동",
 *     "user_phone_number": "01012345678",
 *     "user_manner_score": 3.0,
 *     "user_email_verified": true
 *   }
 * }
 */

public class LoginData {
    private int user_id;
    private String user_email;
    private String user_nickname;
    private String user_profile_image;
    private String user_gender;
    private String user_birthday;
    private String user_location;
    private String user_phone_number;
    private double user_manner_score;
    private boolean user_email_verified;

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_nickname() {
        return user_nickname;
    }

    public void setUser_nickname(String user_nickname) {
        this.user_nickname = user_nickname;
    }

    public String getUser_profile_image() {
        return user_profile_image;
    }

    public void setUser_profile_image(String user_profile_image) {
        this.user_profile_image = user_profile_image;
    }

    public String getUser_gender() {
        return user_gender;
    }

    public void setUser_gender(String user_gender) {
        this.user_gender = user_gender;
    }

    public String getUser_birthday() {
        return user_birthday;
    }

    public void setUser_birthday(String user_birthday) {
        this.user_birthday = user_birthday;
    }

    public String getUser_location() {
        return user_location;
    }

    public void setUser_location(String user_location) {
        this.user_location = user_location;
    }

    public String getUser_phone_number() {
        return user_phone_number;
    }

    public void setUser_phone_number(String user_phone_number) {
        this.user_phone_number = user_phone_number;
    }

    public double getUser_manner_score() {
        return user_manner_score;
    }

    public void setUser_manner_score(double user_manner_score) {
        this.user_manner_score = user_manner_score;
    }

    public boolean isUser_email_verified() {
        return user_email_verified;
    }

    public void setUser_email_verified(boolean user_email_verified) {
        this.user_email_verified = user_email_verified;
    }
}
