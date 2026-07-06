package com.example.honbabapp.api.data.request.meeting;

/**
 * 신청 승인/거절 요청
 *
 * POST /api/meeting/approve.php
 *
 * {
 *   "participant_id": 6,
 *   "action": "approve"  // "approve" or "reject"
 * }
 *
 * host_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class ApproveMeetingRequest {
    private int participant_id;
    private String action;  // "approve" or "reject"

    public ApproveMeetingRequest(int participant_id, String action) {
        this.participant_id = participant_id;
        this.action = action;
    }

    // Getters and Setters
    public int getParticipant_id() {
        return participant_id;
    }

    public void setParticipant_id(int participant_id) {
        this.participant_id = participant_id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}