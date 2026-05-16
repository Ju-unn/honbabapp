package com.example.honbabapp.api.data.chat;

/**
 * 채팅방 사진 1개 데이터 모델
 *
 * api/chat/photos.php 응답의 photos[] 한 원소:
 * {
 *   "message_id": 99,
 *   "image_url": "https://s3.../chat/img1.jpg",
 *   "sender_id": 18,
 *   "sender_name": "테스트10",
 *   "created_at": "2026-05-14 10:00:00"
 * }
 *
 * 어댑터에서 Glide 로 image_url 을 로드해 썸네일 표시.
 */
public class ChatPhotoData {

    private int message_id;
    private String image_url;
    private Integer sender_id;     // 시스템 메시지면 null 가능성 있음
    private String sender_name;
    private String created_at;

    public ChatPhotoData() {}


    // ── Getter / Setter ──────────────────────────

    public int getMessage_id() { return message_id; }
    public void setMessage_id(int message_id) { this.message_id = message_id; }

    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }

    public Integer getSender_id() { return sender_id; }
    public void setSender_id(Integer sender_id) { this.sender_id = sender_id; }

    public String getSender_name() { return sender_name; }
    public void setSender_name(String sender_name) { this.sender_name = sender_name; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}