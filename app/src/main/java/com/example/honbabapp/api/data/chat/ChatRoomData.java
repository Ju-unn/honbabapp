package com.example.honbabapp.api.data.chat;

import java.util.List;

/**
 * 채팅방 데이터 모델
 *
 * GET /api/chat/rooms.php 응답:
 * {
 *   "room_id": 1,
 *   "room_type": "group",
 *   "room_name": "오늘 저녁 삼겹살",
 *   "room_image": "https://...",
 *   "meeting_id": 5,
 *   "owner_id": 123,
 *   "last_message": "내일 몇 시에 만나요?",
 *   "last_msg_time": "2026-04-17 10:00:00",
 *   "unread_count": 3,
 *   "participants": [...]
 * }
 */


public class ChatRoomData {

    private int room_id;
    private String room_type;       // "direct" | "group"
    private String room_name;
    private String room_image;
    private Integer meeting_id;     // null 가능 → Integer 사용
    private int owner_id;
    private String last_message;
    private String last_msg_time;
    private int unread_count;
    private List<ChatMemberData> participants;

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public String getRoom_type() { return room_type; }
    public void setRoom_type(String room_type) { this.room_type = room_type; }

    public String getRoom_name() { return room_name; }
    public void setRoom_name(String room_name) { this.room_name = room_name; }

    public String getRoom_image() { return room_image; }
    public void setRoom_image(String room_image) { this.room_image = room_image; }

    public Integer getMeeting_id() { return meeting_id; }
    public void setMeeting_id(Integer meeting_id) { this.meeting_id = meeting_id; }

    public int getOwner_id() { return owner_id; }
    public void setOwner_id(int owner_id) { this.owner_id = owner_id; }

    public String getLast_message() { return last_message; }
    public void setLast_message(String last_message) { this.last_message = last_message; }

    public String getLast_msg_time() { return last_msg_time; }
    public void setLast_msg_time(String last_msg_time) { this.last_msg_time = last_msg_time; }

    public int getUnread_count() { return unread_count; }
    public void setUnread_count(int unread_count) { this.unread_count = unread_count; }

    public List<ChatMemberData> getParticipants() { return participants; }
    public void setParticipants(List<ChatMemberData> participants) { this.participants = participants; }

    // ── 편의 메서드 ──────────────────────────────

    /** 1:1 채팅방 여부 */
    public boolean isDirect() { return "direct".equals(room_type); }

    /** 그룹 채팅방 여부 */
    public boolean isGroup() { return "group".equals(room_type); }
}