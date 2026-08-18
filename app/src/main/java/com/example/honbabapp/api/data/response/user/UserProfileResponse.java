package com.example.honbabapp.api.data.response.user;

/**
 * 프로필 조회 응답 모델
 *
 * GET /api/user/profile.php
 *
 */
public class UserProfileResponse {

    // 기존 필드
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

    // 체험단 전용 필드
    private String user_instagram_id;
    private String user_blog_id;
    private String user_youtube_id;

    // 소셜 로그인 계정이면 'kakao'/'google', 일반 계정이면 null
    private String social_provider;

    // ── 기존 Getter──────────────────────────────────────

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

    // ── 체험단 전용 Getter ────────────────────────────────────────

    public String getUser_instagram_id() { return user_instagram_id; }
    public String getUser_blog_id() { return user_blog_id; }
    public String getUser_youtube_id() { return user_youtube_id; }

    public String getSocial_provider() { return social_provider; }

    // ── 편의 메서드 ───────────────────────────────────────────────

    /** 인스타그램 ID 등록 여부 */
    public boolean hasInstagramId() {
        return user_instagram_id != null && !user_instagram_id.isEmpty();
    }

    /** 블로그 ID 등록 여부 */
    public boolean hasBlogId() {
        return user_blog_id != null && !user_blog_id.isEmpty();
    }

    /** 유튜브 ID 등록 여부 */
    public boolean hasYoutubeId() {
        return user_youtube_id != null && !user_youtube_id.isEmpty();
    }

    /** SNS 계정 하나라도 등록 여부 — 체험단 신청 가능 조건 (유튜브는 신청 요건에서 제외) */
    public boolean hasSnsId() {
        return hasInstagramId() || hasBlogId();
    }

    /** 소셜 로그인 계정 여부 — 비밀번호 변경 메뉴 노출 조건 */
    public boolean isSocialAccount() {
        return social_provider != null && !social_provider.isEmpty();
    }
}