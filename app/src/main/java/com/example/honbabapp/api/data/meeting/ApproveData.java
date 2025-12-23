package com.example.honbabapp.api.data.meeting;

/**
 * 신청 승인/거절 응답 데이터
 *
 * POST /api/meeting/approve.php
 *
 * 승인 응답:
 * {
 *   "success": true,
 *   "message": "참여 신청을 승인했습니다",
 *   "data": {
 *     "participant_id": 6,
 *     "participation_status": "승인",
 *     "meeting_current_participants": 3
 *   }
 * }
 *
 * 거절 응답:
 * {
 *   "success": true,
 *   "message": "참여 신청을 거절했습니다",
 *   "data": {
 *     "participant_id": 6,
 *     "participation_status": "거절"
 *   }
 * }
 */
public class ApproveData {
    private int participant_id;
    private String participation_status;
    private Integer meeting_current_participants;  // 승인 시에만 있음

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

    public Integer getMeeting_current_participants() {
        return meeting_current_participants;
    }

    public void setMeeting_current_participants(Integer meeting_current_participants) {
        this.meeting_current_participants = meeting_current_participants;
    }
}