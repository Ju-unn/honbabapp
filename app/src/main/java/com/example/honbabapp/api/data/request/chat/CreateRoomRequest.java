package com.example.honbabapp.api.data.request.chat;

import java.util.Collections;
import java.util.List;

/**
 * 채팅방 생성 요청 모델
 *
 * POST /api/chat/create_room.php
 * {
 *   "room_type": "direct",
 *   "owner_id": 123,
 *   "member_ids": [456],
 *   "meeting_id": null,
 *   "room_name": "이영희"
 * }
 */
public class CreateRoomRequest {

    private String room_type;
    private int owner_id;
    private List<Integer> member_ids;
    private Integer meeting_id;     // null 가능
    private String room_name;

    // 기본 생성자
    public CreateRoomRequest() {}

    /**
     * 1:1 채팅방 생성
     */
    public CreateRoomRequest(int owner_id, int targetUserId, String room_name) {
        this.room_type = "direct";
        this.owner_id = owner_id;
        this.member_ids = Collections.singletonList(targetUserId);
        this.meeting_id = null;
        this.room_name = room_name;
    }

    /**
     * 그룹 채팅방 생성
     */
    public CreateRoomRequest(int owner_id, List<Integer> member_ids, int meeting_id, String room_name) {
        this.room_type = "group";
        this.owner_id = owner_id;
        this.member_ids = member_ids;
        this.meeting_id = meeting_id;
        this.room_name = room_name;
    }

    // ── Getter / Setter ──────────────────────────

    public String getRoom_type() { return room_type; }
    public void setRoom_type(String room_type) { this.room_type = room_type; }

    public int getOwner_id() { return owner_id; }
    public void setOwner_id(int owner_id) { this.owner_id = owner_id; }

    public List<Integer> getMember_ids() { return member_ids; }
    public void setMember_ids(List<Integer> member_ids) { this.member_ids = member_ids; }

    public Integer getMeeting_id() { return meeting_id; }
    public void setMeeting_id(Integer meeting_id) { this.meeting_id = meeting_id; }

    public String getRoom_name() { return room_name; }
    public void setRoom_name(String room_name) { this.room_name = room_name; }
}