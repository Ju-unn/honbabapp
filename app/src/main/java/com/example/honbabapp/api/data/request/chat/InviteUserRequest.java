package com.example.honbabapp.api.data.request.chat;

/**
 * 채팅방 초대 요청 모델
 *
 * POST /api/chat/invite.php
 * { "room_id": 1, "inviter_id": 123, "target_user_id": 456 }
 */
public class InviteUserRequest {
    private int room_id;
    private int inviter_id;
    private int target_user_id;

    // 기본 생성자
    public InviteUserRequest() {}

    public InviteUserRequest(int room_id, int inviter_id, int target_user_id) {
        this.room_id = room_id;
        this.inviter_id = inviter_id;
        this.target_user_id = target_user_id;
    }

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getInviter_id() { return inviter_id; }
    public void setInviter_id(int inviter_id) { this.inviter_id = inviter_id; }

    public int getTarget_user_id() { return target_user_id; }
    public void setTarget_user_id(int target_user_id) { this.target_user_id = target_user_id; }
}