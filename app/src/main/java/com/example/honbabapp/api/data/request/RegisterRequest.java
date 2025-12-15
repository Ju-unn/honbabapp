package com.example.honbabapp.api.data.request;

public class RegisterRequest {

    // 필수 필드
    private String nickname;           // 닉네임
    private String email;              // 이메일
    private String password;           // 비밀번호
    private String phone_number;       // 전화번호 (010-1234-5678)
    private String birthday;           // 생년월일 (YYYY-MM-DD)
    private String gender;             // 성별 (남성/여성)
    private String location;           // 내 동네

    // 선택 필드
    private String profile_image;

    /**
     * 기본 생성자 (필수 필드만)
     */
    public RegisterRequest(String nickname, String email, String password,
                           String phone_number, String birthday, String gender,
                           String location) {
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.phone_number = phone_number;
        this.birthday = birthday;
        this.gender = gender;
        this.location = location;
    }

    /**
     * 전체 생성자 (프로필 이미지 포함)
     */
    public RegisterRequest(String nickname, String email, String password,
                           String phone_number, String birthday, String gender,
                           String location, String profile_image) {
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.phone_number = phone_number;
        this.birthday = birthday;
        this.gender = gender;
        this.location = location;
        this.profile_image = profile_image;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(String profile_image) {
        this.profile_image = profile_image;
    }
}
