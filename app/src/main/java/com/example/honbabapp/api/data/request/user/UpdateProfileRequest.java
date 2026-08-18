package com.example.honbabapp.api.data.request.user;

/**
 * 프로필 수정 요청 모델
 *
 * POST /api/user/update_profile.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class UpdateProfileRequest {

    private String user_nickname;       // 선택적
    private String user_status_message; // 선택적
    private String user_profile_image;  // 선택적 (S3 URL)

    // 체험단 전용 (선택적 — null이면 서버가 해당 컬럼 업데이트 안 함)
    private String user_instagram_id;
    private String user_blog_id;
    private String user_youtube_id;

    public UpdateProfileRequest() {
    }

    public void setUser_nickname(String user_nickname) {
        this.user_nickname = user_nickname;
    }

    public void setUser_status_message(String user_status_message) {
        this.user_status_message = user_status_message;
    }

    public void setUser_profile_image(String user_profile_image) {
        this.user_profile_image = user_profile_image;
    }

    public void setUser_instagram_id(String user_instagram_id) {
        this.user_instagram_id = user_instagram_id;
    }

    public void setUser_blog_id(String user_blog_id) {
        this.user_blog_id = user_blog_id;
    }

    public void setUser_youtube_id(String user_youtube_id) {
        this.user_youtube_id = user_youtube_id;
    }

    public String getUser_nickname() { return user_nickname; }
    public String getUser_status_message() { return user_status_message; }
    public String getUser_profile_image() { return user_profile_image; }
    public String getUser_instagram_id() { return user_instagram_id; }
    public String getUser_blog_id() { return user_blog_id; }
    public String getUser_youtube_id() { return user_youtube_id; }
}