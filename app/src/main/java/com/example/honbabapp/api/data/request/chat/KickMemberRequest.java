package com.example.honbabapp.api.data.request.chat;

/**
 * 강퇴 요청 모델
 *
 * POST /api/chat/kick.php
 * {
 *   "room_id": 1,
 *   "target_user_id": 7
 * }
 *
 * host_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 *
 * 서버에서 검증:
 *   - room_type='group' (1:1 방 강퇴 불가)
 *   - chat_rooms.owner_id == 토큰의 user_id (방장)
 *   - meetings.meeting_host_id == 토큰의 user_id (모임장 이중 검증)
 *   - 토큰의 user_id != target_user_id (자기 자신 강퇴 불가)
 *   - 대상자가 실제로 chat_room_members 에 있는지
 *   - 대상자의 participation_status='승인' 여부
 */
public class KickMemberRequest {

    private int room_id;
    private int target_user_id;

    public KickMemberRequest() {}

    public KickMemberRequest(int room_id, int target_user_id) {
        this.room_id = room_id;
        this.target_user_id = target_user_id;
    }

    // ── Getter / Setter ──

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getTarget_user_id() { return target_user_id; }
    public void setTarget_user_id(int target_user_id) { this.target_user_id = target_user_id; }
}