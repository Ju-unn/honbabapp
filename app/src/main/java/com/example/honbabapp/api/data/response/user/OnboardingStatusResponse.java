package com.example.honbabapp.api.data.response.user;

/**
 * 온보딩 완료 여부 조회 응답 모델
 * api/user/check_onboarding_status.php 응답과 매칭
 */
public class OnboardingStatusResponse {
    private boolean needs_onboarding;
    private MissingFields missing;

    public boolean isNeeds_onboarding() {
        return needs_onboarding;
    }

    public MissingFields getMissing() {
        return missing;
    }

    public static class MissingFields {
        private boolean nickname;
        private boolean phone_number;
        private boolean location;
        private boolean gender;
        private boolean birthday;

        public boolean isNickname() {
            return nickname;
        }

        public boolean isPhone_number() {
            return phone_number;
        }

        public boolean isLocation() {
            return location;
        }

        public boolean isGender() {
            return gender;
        }

        public boolean isBirthday() {
            return birthday;
        }
    }
}