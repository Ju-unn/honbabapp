package com.example.honbabapp.api.data.response.user;

public class UserProfileResponse {
    private int user_id;
    private String user_nickname;
    private String user_profile_image;
    private String user_status_message;
    private float user_manner_score;
    private int user_participation_count;
    private String user_email;
    private String user_birthday;
    private String user_gender;
    private int push_enabled;

    // Getter들 - Activity에서 데이터 꺼낼 때 사용
    public int getUser_id() { return user_id; }
    public String getUser_nickname() { return user_nickname; }
    public String getUser_profile_image() { return user_profile_image; }
    public String getUser_status_message() { return user_status_message; }
    public float getUser_manner_score() { return user_manner_score; }
    public int getUser_participation_count() { return user_participation_count; }
    public String getUser_email() { return user_email; }
    public String getUser_birthday() { return user_birthday; }
    public String getUser_gender() { return user_gender; }
    public int getPush_enabled() { return push_enabled; }
}