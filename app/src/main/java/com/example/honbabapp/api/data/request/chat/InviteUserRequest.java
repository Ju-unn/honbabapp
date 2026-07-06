package com.example.honbabapp.api.data.request.chat;

/**
 * 채팅방 초대 요청 모델
 *
 * POST /api/chat/invite.php
 * { "room_id": 1, "target_user_id": 456 }
 *
 * inviter_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class InviteUserRequest {
    private int room_id;
    private int target_user_id;

    // 기본 생성자
    public InviteUserRequest() {}

    public InviteUserRequest(int room_id, int target_user_id) {
        this.room_id = room_id;
        this.target_user_id = target_user_id;
    }

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getTarget_user_id() { return target_user_id; }
    public void setTarget_user_id(int target_user_id) { this.target_user_id = target_user_id; }
}