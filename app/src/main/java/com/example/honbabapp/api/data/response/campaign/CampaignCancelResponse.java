package com.example.honbabapp.api.data.response.campaign;

/**
 * 체험단 참가자 자기 취소 응답 데이터
 *
 * POST /api/campaign/campaign_cancel.php
 * {
 *   "participant_id": 12,
 *   "participation_status": "취소",
 *   "penalty_applied": 5.0,
 *   "new_manner_score": 28.0,
 *   "meeting_current_participants": 3
 * }
 *
 * penalty_applied: 대기 상태에서 취소하면 0.0, 승인 상태에서 취소하면 5.0 고정
 */
public class CampaignCancelResponse {
    private int participant_id;
    private String participation_status;
    private double penalty_applied;
    private double new_manner_score;
    private int meeting_current_participants;

    public int getParticipant_id() { return participant_id; }
    public void setParticipant_id(int participant_id) { this.participant_id = participant_id; }

    public String getParticipation_status() { return participation_status; }
    public void setParticipation_status(String participation_status) { this.participation_status = participation_status; }

    public double getPenalty_applied() { return penalty_applied; }
    public void setPenalty_applied(double penalty_applied) { this.penalty_applied = penalty_applied; }

    public double getNew_manner_score() { return new_manner_score; }
    public void setNew_manner_score(double new_manner_score) { this.new_manner_score = new_manner_score; }

    public int getMeeting_current_participants() { return meeting_current_participants; }
    public void setMeeting_current_participants(int meeting_current_participants) { this.meeting_current_participants = meeting_current_participants; }
}
