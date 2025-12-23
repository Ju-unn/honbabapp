package com.example.honbabapp.api.data.meeting;

import java.util.List;

/**
 * 신청자 목록 조회 응답 데이터
 *
 * GET /api/meeting/applicants.php
 *
 * PHP 응답:
 * {
 *   "success": true,
 *   "message": "신청자 목록 조회 성공",
 *   "data": {
 *     "meeting_id": 1,
 *     "applicants": [
 *       {
 *         "participant_id": 6,
 *         "user_id": 3,
 *         "user_nickname": "카드가든",
 *         "user_profile_image": "...",
 *         "user_manner_score": 4.2,
 *         "user_gender": "남성",
 *         "participation_status": "대기",
 *         "applied_at": "2025-12-22 02:49:47"
 *       }
 *     ],
 *     "total_count": 1
 *   }
 * }
 */
public class ApplicantsData {
    private int meeting_id;
    private List<Applicant> applicants;
    private int total_count;

    // 신청자 정보 클래스
    public static class Applicant {
        private int participant_id;
        private int user_id;
        private String user_nickname;
        private String user_profile_image;
        private double user_manner_score;
        private String user_gender;
        private String participation_status;
        private String applied_at;

        // Getters and Setters
        public int getParticipant_id() {
            return participant_id;
        }

        public void setParticipant_id(int participant_id) {
            this.participant_id = participant_id;
        }

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

        public String getUser_gender() {
            return user_gender;
        }

        public void setUser_gender(String user_gender) {
            this.user_gender = user_gender;
        }

        public String getParticipation_status() {
            return participation_status;
        }

        public void setParticipation_status(String participation_status) {
            this.participation_status = participation_status;
        }

        public String getApplied_at() {
            return applied_at;
        }

        public void setApplied_at(String applied_at) {
            this.applied_at = applied_at;
        }
    }

    // Getters and Setters
    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public List<Applicant> getApplicants() {
        return applicants;
    }

    public void setApplicants(List<Applicant> applicants) {
        this.applicants = applicants;
    }

    public int getTotal_count() {
        return total_count;
    }

    public void setTotal_count(int total_count) {
        this.total_count = total_count;
    }
}