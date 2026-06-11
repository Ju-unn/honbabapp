package com.example.honbabapp.api.data.meeting;

/**
 * QR 생성 응답 DTO
 * POST /api/meeting/qr_generate.php 의 data 부분
 *
 * 응답 예시:
 * {
 *   "code_token": "1d749f4a...(64자)",
 *   "expires_at": "2025-12-26 19:00:00",
 *   "meeting_id": 1,
 *   "is_newly_generated": true
 * }
 */
public class QrCodeData {

    private String code_token;
    private String expires_at;
    private int meeting_id;
    private boolean is_newly_generated;  // true=방금 생성, false=기존 QR 반환

    public QrCodeData() {}

    public String getCode_token() {
        return code_token;
    }

    public void setCode_token(String code_token) {
        this.code_token = code_token;
    }

    public String getExpires_at() {
        return expires_at;
    }

    public void setExpires_at(String expires_at) {
        this.expires_at = expires_at;
    }

    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public boolean isIs_newly_generated() {
        return is_newly_generated;
    }

    public void setIs_newly_generated(boolean is_newly_generated) {
        this.is_newly_generated = is_newly_generated;
    }
}