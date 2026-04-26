package com.example.honbabapp.api.data.request.chat;

/**
 * 채팅방 나가기 요청 모델
 *
 * POST /api/chat/leave.php
 * { "room_id": 1, "user_id": 123 }
 */
public class LeaveRoomRequest {

    private int room_id;
    private int user_id;

    // 기본 생성자
    public LeaveRoomRequest() {}

    public LeaveRoomRequest(int room_id, int user_id) {
        this.room_id = room_id;
        this.user_id = user_id;
    }

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }
}