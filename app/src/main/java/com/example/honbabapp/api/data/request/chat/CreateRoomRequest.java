package com.example.honbabapp.api.data.request.chat;

import java.util.Collections;
import java.util.List;

/**
 * 채팅방 생성 요청 모델
 *
 * POST /api/chat/create_room.php
 * {
 *   "room_type": "direct",
 *   "member_ids": [456],
 *   "meeting_id": null,
 *   "room_name": "이영희"
 * }
 *
 * owner_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class CreateRoomRequest {

    private String room_type;
    private List<Integer> member_ids;
    private Integer meeting_id;     // null 가능
    private String room_name;

    // 기본 생성자
    public CreateRoomRequest() {}

    /**
     * 1:1 채팅방 생성
     */
    public CreateRoomRequest(int targetUserId, String room_name) {
        this.room_type = "direct";
        this.member_ids = Collections.singletonList(targetUserId);
        this.meeting_id = null;
        this.room_name = room_name;
    }

    /**
     * 그룹 채팅방 생성
     */
    public CreateRoomRequest(List<Integer> member_ids, int meeting_id, String room_name) {
        this.room_type = "group";
        this.member_ids = member_ids;
        this.meeting_id = meeting_id;
        this.room_name = room_name;
    }

    // ── Getter / Setter ──────────────────────────

    public String getRoom_type() { return room_type; }
    public void setRoom_type(String room_type) { this.room_type = room_type; }

    public List<Integer> getMember_ids() { return member_ids; }
    public void setMember_ids(List<Integer> member_ids) { this.member_ids = member_ids; }

    public Integer getMeeting_id() { return meeting_id; }
    public void setMeeting_id(Integer meeting_id) { this.meeting_id = meeting_id; }

    public String getRoom_name() { return room_name; }
    public void setRoom_name(String room_name) { this.room_name = room_name; }
}