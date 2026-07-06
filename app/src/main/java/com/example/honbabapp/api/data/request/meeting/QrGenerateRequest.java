package com.example.honbabapp.api.data.request.meeting;

/**
 * QR 생성 요청 바디 DTO
 * POST /api/qr/qr_generate.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class QrGenerateRequest {

    private int meeting_id;

    public QrGenerateRequest(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
    }
}