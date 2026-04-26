package com.example.honbabapp.api.data.chat;

/**
 * 채팅 메시지 데이터 모델
 *
 * GET /api/chat/messages.php 응답 &
 * WebSocket "message" 타입 수신 모두 이 모델 사용
 *
 * {
 *   "message_id": 99,
 *   "room_id": 1,
 *   "sender_id": 123,
 *   "sender_name": "김철수",
 *   "sender_profile_image": "https://...",
 *   "msg_type": "text",
 *   "content": "안녕하세요",
 *   "image_url": null,
 *   "unread_count": 2,
 *   "created_at": "2026-04-17 10:00:00"
 * }
 *
 * 주의: is_mine 은 JSON 에 없는 필드.
 * 메시지 로딩 후 sender_id == myUserId 비교해서 setter 로 직접 설정.
 */
public class MessageData {

    private int message_id;
    private int room_id;
    private int sender_id;
    private String sender_name;
    private String sender_profile_image;
    private String msg_type;        // "text" | "image" | "system"
    private String content;
    private String image_url;       // msg_type == "image" 일 때만 사용
    private int unread_count;       // 아직 읽지 않은 인원 수
    private String created_at;
    private boolean is_mine;        // 로컬 계산용 (JSON 에 없음)


    // ── Getter / Setter ──────────────────────────

    public int getMessage_id() { return message_id; }
    public void setMessage_id(int message_id) { this.message_id = message_id; }

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getSender_id() { return sender_id; }
    public void setSender_id(int sender_id) { this.sender_id = sender_id; }

    public String getSender_name() { return sender_name; }
    public void setSender_name(String sender_name) { this.sender_name = sender_name; }

    public String getSender_profile_image() { return sender_profile_image; }
    public void setSender_profile_image(String sender_profile_image) { this.sender_profile_image = sender_profile_image; }

    public String getMsg_type() { return msg_type; }
    public void setMsg_type(String msg_type) { this.msg_type = msg_type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }

    public int getUnread_count() { return unread_count; }
    public void setUnread_count(int unread_count) { this.unread_count = unread_count; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public boolean isIs_mine() { return is_mine; }
    public void setIs_mine(boolean is_mine) { this.is_mine = is_mine; }

    // ── 편의 메서드 ──────────────────────────────

    /** 시스템 메시지 여부 */
    public boolean isSystem() { return "system".equals(msg_type); }

    /** 이미지 메시지 여부 */
    public boolean isImage() { return "image".equals(msg_type); }
}