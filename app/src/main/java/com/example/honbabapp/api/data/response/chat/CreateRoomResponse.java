package com.example.honbabapp.api.data.response.chat;

/**
 * 채팅방 생성 응답 모델
 *
 * POST /api/chat/create_room.php 응답의 data 부분:
 * {
 *   "room_id": 10,
 *   "room_type": "direct",
 *   "already_exists": false
 * }
 *
 * already_exists == true 이면 기존 방으로 이동,
 * false 이면 새로 생성된 방으로 이동
 */
public class CreateRoomResponse {

    private int room_id;
    private String room_type;
    private boolean already_exists;

    // 기본 생성자
    public CreateRoomResponse() {}

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public String getRoom_type() { return room_type; }
    public void setRoom_type(String room_type) { this.room_type = room_type; }

    public boolean isAlready_exists() { return already_exists; }
    public void setAlready_exists(boolean already_exists) { this.already_exists = already_exists; }
}