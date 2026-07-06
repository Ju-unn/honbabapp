package com.example.honbabapp.api.data.request.chat;

/**
 * 읽음 처리 요청 모델
 *
 * POST /api/chat/mark_read.php
 * { "room_id": 1 }
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class MarkReadRequest {

    private int room_id;

    // 기본 생성자
    public MarkReadRequest() {}

    public MarkReadRequest(int room_id) {
        this.room_id = room_id;
    }

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }
}