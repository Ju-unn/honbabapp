package com.example.honbabapp.api.data.request.meeting;

/**
 * 모임 / 체험단 참여 신청 요청 모델
 *
 * POST /api/meeting/join.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class JoinMeetingRequest {

    private int meeting_id;
    private String applicant_intro; // 체험단 전용 (일반 모임은 null)

    // ── 일반 모임 신청 생성자 ─────────────────────────────────────
    public JoinMeetingRequest(int meeting_id) {
        this.meeting_id      = meeting_id;
        this.applicant_intro = null;
    }

    // ── 체험단 신청 생성자 ────────────────────────────────────────
    public JoinMeetingRequest(int meeting_id, String applicant_intro) {
        this.meeting_id      = meeting_id;
        this.applicant_intro = applicant_intro;
    }

    // ── Getter / Setter ───────────────────────────────────────────

    public int getMeeting_id() { return meeting_id; }
    public void setMeeting_id(int meeting_id) { this.meeting_id = meeting_id; }

    public String getApplicant_intro() { return applicant_intro; }
    public void setApplicant_intro(String applicant_intro) { this.applicant_intro = applicant_intro; }
}