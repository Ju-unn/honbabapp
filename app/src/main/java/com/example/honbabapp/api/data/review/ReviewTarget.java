package com.example.honbabapp.api.data.review;

/**
 * 후기 대상 1명의 정보
 *
 * GET /api/review/targets.php 응답 data.targets 의 원소
 *
 * 응답 예시:
 * {
 *   "user_id": 5,
 *   "user_nickname": "김철수",
 *   "user_profile_image": "https://...",
 *   "already_reviewed": false
 * }
 */
public class ReviewTarget {

    private int user_id;
    private String user_nickname;
    private String user_profile_image;
    private boolean already_reviewed;

    public ReviewTarget() {}

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

    public boolean isAlready_reviewed() {
        return already_reviewed;
    }

    public void setAlready_reviewed(boolean already_reviewed) {
        this.already_reviewed = already_reviewed;
    }
}