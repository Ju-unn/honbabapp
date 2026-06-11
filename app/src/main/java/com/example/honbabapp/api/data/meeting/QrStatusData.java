package com.example.honbabapp.api.data.meeting;

/**
 * QR 상태 조회 응답 DTO
 * GET /api/meeting/qr_status.php 의 data 부분
 *
 * 응답 예시:
 * {
 *   "is_host": true,
 *   "state": "generated",
 *   "code_token": "a3f8...",
 *   "expires_at": "2025-12-26 19:00:00"
 * }
 *
 * state 값:
 *   before_window : 모임 24시간 전이 아직 안 됨
 *   available     : 생성 가능 (호스트, 창 안, 미생성)
 *   generated     : 이미 QR 존재
 *   expired       : 만료됨
 */
public class QrStatusData {

    private boolean is_host;
    private String state;
    private String code_token;   // 호스트 + state=generated 일 때만 서버가 포함
    private String expires_at;

    public QrStatusData() {}

    public boolean isIs_host() {
        return is_host;
    }

    public void setIs_host(boolean is_host) {
        this.is_host = is_host;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

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
}