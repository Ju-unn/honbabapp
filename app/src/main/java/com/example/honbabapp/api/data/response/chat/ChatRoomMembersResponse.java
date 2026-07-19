package com.example.honbabapp.api.data.response.chat;

import com.example.honbabapp.api.data.chat.ChatMemberData;

import java.util.List;

/**
 * 채팅방 멤버 조회 응답 모델
 *
 * GET /api/chat/members.php 응답의 data 부분:
 * {
 *   "room_id": 5,
 *   "owner_id": 12,
 *   "participants": [
 *     { user_id, user_nickname, user_profile_image, last_read_at },
 *     ...
 *   ]
 * }
 *
 * chat_room_members 기준(=실제 현재 채팅방 참가자)의 단일 진실의 원천.
 * ChatRoomDetailActivity 의 "참가자" 타이틀/리스트, ChatRoomActivity 의 인원수 표시가
 * 모두 이 응답을 기준으로 통일된다.
 */
public class ChatRoomMembersResponse {

    private int room_id;
    private int owner_id;
    private List<ChatMemberData> participants;

    public ChatRoomMembersResponse() {}

    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getOwner_id() { return owner_id; }
    public void setOwner_id(int owner_id) { this.owner_id = owner_id; }

    public List<ChatMemberData> getParticipants() { return participants; }
    public void setParticipants(List<ChatMemberData> participants) { this.participants = participants; }
}
