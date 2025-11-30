package com.example.honbabapp.data.user;

import java.util.Date;

public class User {
    /// 나중에 @SerializedName 필요한 이유공부하기

    public int user_id;     ///유저 고유번호
    public String user_email;    ///유저 아이디(이메일)
    public String user_password;    ///유저 고유번호
    public String user_nickname;    ///유저 닉네임
    public String user_profile_image;   ///유저 프로필 이미지
    public String user_statusMessage;    /// 유저 상태 메세지
    public String user_birthday;    /// 유저 생년월일
    public String user_gender;  /// 유저 성별
    public String user_phone_number; /// 유저 폰 번호
    public Double user_manner_score; /// 유저 매너점수
    public int user_participation_count; /// 모임 참여횟수
    public String user_location; /// 유저 위치(내 주변 모임 우선적으로 보여주기)
    public boolean user_isActive; /// 유저상태 (탈퇴 , 정지)
    public Date user_created_at; /// 유저 가입일시
    public Date user_update_at; /// 유저 마지막 수정일시(변경 및 업데이트)


    /// 생성자
    public User(int user_id , String user_email , String user_password , String user_nickname ,String user_profile_image ,
                String user_statusMessage , String user_birthday , String user_gender , String user_phone_number , Double user_manner_score ,
                int user_participation_count , String user_location , boolean user_isActive , Date user_created_at , Date user_update_at) {

        this.user_id = user_id;
        this.user_email = user_email;
        this.user_password = user_password;
        this.user_nickname = user_nickname;
        this.user_statusMessage = user_statusMessage;
        this.user_birthday = user_birthday;
        this.user_gender = user_gender;
        this.user_phone_number = user_phone_number;
        this.user_manner_score = user_manner_score;
        this.user_participation_count = user_participation_count;
        this.user_location = user_location;
        this.user_isActive = user_isActive;
        this.user_created_at = user_created_at;
        this.user_update_at = user_update_at;
    }

    public User(int user_id , String user_email , String user_password , String user_nickname,
                String user_statusMessage , String user_birthday , String user_gender , String user_phone_number , Double user_manner_score ,
                int user_participation_count , String user_location , boolean user_isActive , Date user_created_at , Date user_update_at) {
        this.user_id = user_id;
        this.user_email = user_email;
        this.user_password = user_password;
        this.user_nickname = user_nickname;
        this.user_statusMessage = user_statusMessage;
        this.user_birthday = user_birthday;
        this.user_gender = user_gender;
        this.user_phone_number = user_phone_number;
        this.user_manner_score = user_manner_score;
        this.user_participation_count = user_participation_count;
        this.user_location = user_location;
        this.user_isActive = user_isActive;
        this.user_created_at = user_created_at;
        this.user_update_at = user_update_at;
    }
}
