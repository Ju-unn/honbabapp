package com.example.honbabapp.api.data;

public class MeetingParticipant {
    private int user_id;
    private String user_nickname;
    private String user_profile_image;
    private double user_manner_score;
    private String joined_at;

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
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

    public double getUser_manner_score() {
        return user_manner_score;
    }

    public void setUser_manner_score(double user_manner_score) {
        this.user_manner_score = user_manner_score;
    }

    public String getJoined_at() {
        return joined_at;
    }

    public void setJoined_at(String joined_at) {
        this.joined_at = joined_at;
    }
}
