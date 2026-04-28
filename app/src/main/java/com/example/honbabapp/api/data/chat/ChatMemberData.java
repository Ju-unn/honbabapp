package com.example.honbabapp.api.data.chat;

/**
 * 채팅방 멤버 데이터 모델
 *
 * ChatRoomData.participants 배열 &
 * POST /api/chat/invite.php 에서 사용
 *
 * {
 *   "user_id": 1,
 *   "user_nickname": "김철수",
 *   "user_profile_image": "https://...",
 *   "last_read_at": "2026-04-17 09:00:00"
 * }
 */
public class ChatMemberData {

    private int user_id;
    private String user_nickname;
    private String user_profile_image;
    private String last_read_at;

    // ── Getter / Setter ──────────────────────────

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public String getUser_nickname() { return user_nickname; }
    public void setUser_nickname(String user_nickname) { this.user_nickname = user_nickname; }

    public String getUser_profile_image() { return user_profile_image; }
    public void setUser_profile_image(String user_profile_image) { this.user_profile_image = user_profile_image; }

    public String getLast_read_at() { return last_read_at; }
    public void setLast_read_at(String last_read_at) { this.last_read_at = last_read_at; }
}