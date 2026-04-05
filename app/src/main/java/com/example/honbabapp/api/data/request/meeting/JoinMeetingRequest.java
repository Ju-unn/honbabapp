package com.example.honbabapp.api.data.request.meeting;

/**
 * 모임 참여 신청 요청
 *
 * POST /api/meeting/join.php
 *
 * {
 *   "meeting_id": 1,
 *   "user_id": 2
 * }
 */
public class JoinMeetingRequest {
    private int meeting_id;
    private int user_id;

    public JoinMeetingRequest(int meeting_id, int user_id) {
        this.meeting_id = meeting_id;
        this.user_id = user_id;
    }

    // Getters and Setters
    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}