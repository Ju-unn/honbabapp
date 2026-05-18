package com.example.honbabapp.api.data.request.chat;

/**
 * 강퇴 요청 모델
 *
 * POST /api/chat/kick.php
 * {
 *   "room_id": 1,
 *   "host_id": 5,
 *   "target_user_id": 7
 * }
 *
 * 서버에서 검증:
 *   - room_type='group' (1:1 방 강퇴 불가)
 *   - chat_rooms.owner_id == host_id (방장)
 *   - meetings.meeting_host_id == host_id (모임장 이중 검증)
 *   - host_id != target_user_id (자기 자신 강퇴 불가)
 *   - 대상자가 실제로 chat_room_members 에 있는지
 *   - 대상자의 participation_status='승인' 여부
 */
public class KickMemberRequest {

    private int room_id;
    private int host_id;
    private int target_user_id;

    public KickMemberRequest() {}

    public KickMemberRequest(int room_id, int host_id, int target_user_id) {
        this.room_id = room_id;
        this.host_id = host_id;
        this.target_user_id = target_user_id;
    }

    // ── Getter / Setter ──

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getHost_id() { return host_id; }
    public void setHost_id(int host_id) { this.host_id = host_id; }

    public int getTarget_user_id() { return target_user_id; }
    public void setTarget_user_id(int target_user_id) { this.target_user_id = target_user_id; }
}