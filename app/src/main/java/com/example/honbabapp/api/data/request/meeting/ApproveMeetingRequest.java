package com.example.honbabapp.api.data.request.meeting;

/**
 * 신청 승인/거절 요청
 *
 * POST /api/meeting/approve.php
 *
 * {
 *   "participant_id": 6,
 *   "action": "approve",  // "approve" or "reject"
 *   "host_id": 1
 * }
 */
public class ApproveMeetingRequest {
    private int participant_id;
    private String action;  // "approve" or "reject"
    private int host_id;

    public ApproveMeetingRequest(int participant_id, String action, int host_id) {
        this.participant_id = participant_id;
        this.action = action;
        this.host_id = host_id;
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

    public int getHost_id() {
        return host_id;
    }

    public void setHost_id(int host_id) {
        this.host_id = host_id;
    }
}