package com.example.honbabapp.api.data.response.meeting;

/**
 * 모임 참여 신청 응답 데이터
 *
 * POST /api/meeting/join.php
 *
 * PHP 응답:
 * {
 *   "success": true,
 *   "message": "모임 참여 신청이 완료되었습니다. 호스트의 승인을 기다려주세요.",
 *   "data": {
 *     "participant_id": 6,
 *     "participation_status": "대기"
 *   }
 * }
 */
public class ParticipantResponse {
    private int participant_id;
    private String participation_status;

    // Getters and Setters
    public int getParticipant_id() {
        return participant_id;
    }

    public void setParticipant_id(int participant_id) {
        this.participant_id = participant_id;
    }

    public String getParticipation_status() {
        return participation_status;
    }

    public void setParticipation_status(String participation_status) {
        this.participation_status = participation_status;
    }
}