package com.example.honbabapp.api.data.request.meeting;

/**
 * QR 생성 요청 바디 DTO
 * POST /api/meeting/qr_generate.php
 *
 * 요청 예시:
 * {
 *   "meeting_id": 1,
 *   "user_id": 1
 * }
 */
public class QrGenerateRequest {

    private int meeting_id;
    private int user_id;

    public QrGenerateRequest(int meeting_id, int user_id) {
        this.meeting_id = meeting_id;
        this.user_id = user_id;
    }

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