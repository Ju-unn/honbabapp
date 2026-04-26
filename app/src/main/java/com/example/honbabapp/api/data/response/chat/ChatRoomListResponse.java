package com.example.honbabapp.api.data.response.chat;

import com.example.honbabapp.api.data.chat.ChatRoomData;

import java.util.List;

/**
 * 채팅방 목록 응답 모델
 *
 * GET /api/chat/rooms.php 응답의 data 부분:
 * {
 *   "rooms": [...],
 *   "total_count": 5
 * }
 */
public class ChatRoomListResponse {

    private List<ChatRoomData> rooms;
    private int total_count;

    // 기본 생성자
    public ChatRoomListResponse() {}

    // ── Getter / Setter ──────────────────────────

    public List<ChatRoomData> getRooms() { return rooms; }
    public void setRooms(List<ChatRoomData> rooms) { this.rooms = rooms; }

    public int getTotal_count() { return total_count; }
    public void setTotal_count(int total_count) { this.total_count = total_count; }
}