package com.example.honbabapp.api.data.login;

/**
 * 소셜 로그인 응답 데이터 모델
 *
 * PHP social_login.php 응답:
 * {
 *   "success": true,
 *   "message": "소셜 로그인 성공",
 *   "data": {
 *     "user": { user_id, user_email, ..., token },
 *     "needs_onboarding": true
 *   }
 * }
 */
public class SocialLoginData {
    private SocialUserData user;
    private boolean needs_onboarding;

    public SocialUserData getUser() {
        return user;
    }

    public boolean isNeeds_onboarding() {
        return needs_onboarding;
    }

    /**
     * user 안의 필드들 - 기존 LoginData와 거의 동일하지만 token 필드가 추가됨
     */
    public static class SocialUserData {
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
        private String token;

        public int getUser_id() {
            return user_id;
        }

        public String getUser_email() {
            return user_email;
        }

        public String getUser_nickname() {
            return user_nickname;
        }

        public String getUser_profile_image() {
            return user_profile_image;
        }

        public String getUser_gender() {
            return user_gender;
        }

        public String getUser_birthday() {
            return user_birthday;
        }

        public String getUser_location() {
            return user_location;
        }

        public String getUser_phone_number() {
            return user_phone_number;
        }

        public double getUser_manner_score() {
            return user_manner_score;
        }

        public boolean isUser_email_verified() {
            return user_email_verified;
        }

        public String getToken() {
            return token;
        }
    }
}