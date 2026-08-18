package com.example.honbabapp.api.data.response.meeting;

import java.util.List;

/**
 * 신청자 목록 조회 응답 데이터
 *
 * GET /api/meeting/applicants.php
 *
 */
public class ApplicantsResponse {

    private int meeting_id;
    private String meeting_type; // "일반" | "체험단"
    private List<Applicant> applicants;
    private int total_count;

    // ── 신청자 정보 내부 클래스 ───────────────────────────────────
    public static class Applicant {

        // 기존 필드 (그대로)
        private int participant_id;
        private int user_id;
        private String user_nickname;
        private String user_profile_image;
        private double user_manner_score;
        private String user_gender;
        private String participation_status;
        private String applied_at;

        // 체험단 전용 필드 (일반 모임은 null)
        private String user_instagram_id;
        private String user_blog_id;
        private String user_youtube_id;
        private String applicant_intro;

        // ── 기존 Getter / Setter─────────────────────────

        public int getParticipant_id() { return participant_id; }
        public void setParticipant_id(int participant_id) { this.participant_id = participant_id; }

        public int getUser_id() { return user_id; }
        public void setUser_id(int user_id) { this.user_id = user_id; }

        public String getUser_nickname() { return user_nickname; }
        public void setUser_nickname(String user_nickname) { this.user_nickname = user_nickname; }

        public String getUser_profile_image() { return user_profile_image; }
        public void setUser_profile_image(String user_profile_image) { this.user_profile_image = user_profile_image; }

        public double getUser_manner_score() { return user_manner_score; }
        public void setUser_manner_score(double user_manner_score) { this.user_manner_score = user_manner_score; }

        public String getUser_gender() { return user_gender; }
        public void setUser_gender(String user_gender) { this.user_gender = user_gender; }

        public String getParticipation_status() { return participation_status; }
        public void setParticipation_status(String participation_status) { this.participation_status = participation_status; }

        public String getApplied_at() { return applied_at; }
        public void setApplied_at(String applied_at) { this.applied_at = applied_at; }

        // ── 체험단 전용 Getter / Setter ────────────────────────────

        public String getUser_instagram_id() { return user_instagram_id; }
        public void setUser_instagram_id(String user_instagram_id) { this.user_instagram_id = user_instagram_id; }

        public String getUser_blog_id() { return user_blog_id; }
        public void setUser_blog_id(String user_blog_id) { this.user_blog_id = user_blog_id; }

        public String getUser_youtube_id() { return user_youtube_id; }
        public void setUser_youtube_id(String user_youtube_id) { this.user_youtube_id = user_youtube_id; }

        public String getApplicant_intro() { return applicant_intro; }
        public void setApplicant_intro(String applicant_intro) { this.applicant_intro = applicant_intro; }

        // ── 편의 메서드 ────────────────────────────────────────────

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

        /** SNS 계정 하나라도 등록 여부 */
        public boolean hasSnsId() {
            return hasInstagramId() || hasBlogId();
        }
    }

    // ── 외부 클래스 Getter / Setter ───────────────────────────────

    public int getMeeting_id() { return meeting_id; }
    public void setMeeting_id(int meeting_id) { this.meeting_id = meeting_id; }

    public String getMeeting_type() { return meeting_type; }
    public void setMeeting_type(String meeting_type) { this.meeting_type = meeting_type; }

    public List<Applicant> getApplicants() { return applicants; }
    public void setApplicants(List<Applicant> applicants) { this.applicants = applicants; }

    public int getTotal_count() { return total_count; }
    public void setTotal_count(int total_count) { this.total_count = total_count; }
}