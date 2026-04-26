package com.example.honbabapp.api.data.chat;

/**
 * 채팅방 멤버 데이터 모델
 *
 * ChatRoomData.participants 배열 &
 * POST /api/chat/invite.php 에서 사용
 *
 * {
 *   "user_id": 1,
 *   "nickname": "김철수",
 *   "profile_image": "https://...",
 *   "last_read_at": "2026-04-17 09:00:00"
 * }
 */
public class ChatMemberData {

    private int user_id;
    private String nickname;
    private String profile_image;
    private String last_read_at;

    // ── Getter / Setter ──────────────────────────

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getProfile_image() { return profile_image; }
    public void setProfile_image(String profile_image) { this.profile_image = profile_image; }

    public String getLast_read_at() { return last_read_at; }
    public void setLast_read_at(String last_read_at) { this.last_read_at = last_read_at; }
}