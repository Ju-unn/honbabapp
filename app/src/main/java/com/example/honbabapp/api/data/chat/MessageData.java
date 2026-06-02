package com.example.honbabapp.api.data.chat;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

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
 *   "image_urls": ["https://..."],
 *   "unread_count": 2,
 *   "created_at": "2026-04-17 10:00:00"
 * }
 *
 * 주의: is_mine, status, client_msg_id, localUris 는 JSON 에 없는 로컬 필드.
 */
public class MessageData {

    // ── 전송 상태 상수 ─────────────────────────
    public static final int STATUS_SENT    = 0;  // 서버 확정 완료
    public static final int STATUS_SENDING = 1;  // 업로드/전송 중 (optimistic)
    public static final int STATUS_FAILED  = 2;  // 전송 실패

    // ── 서버 필드 (Gson 자동 매핑) ──────────────
    private int message_id;
    private int room_id;
    private int sender_id;
    private String sender_name;
    private String sender_profile_image;
    private String msg_type;        // "text" | "image" | "system"
    private String content;
    private String image_url;       // 하위호환용 (항상 image_urls[0] 과 동일)
    private List<String> image_urls; // 다중 이미지 URL 목록 (Gson 자동 매핑)
    private int unread_count;
    private String created_at;
    private String client_msg_id;   // optimistic dedupe 키 (서버가 echo)

    // ── 로컬 전용 필드 (JSON 에 없음, transient) ─
    private transient boolean is_mine;
    private transient int status = STATUS_SENT;
    private transient List<Uri> localUris; // 재전송용 원본 Uri (SENDING 상태일 때 세팅)


    // ── Getter / Setter : 서버 필드 ─────────────

    public int getMessage_id() { return message_id; }
    public void setMessage_id(int message_id) { this.message_id = message_id; }

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getSender_id() { return sender_id; }
    public void setSender_id(int sender_id) { this.sender_id = sender_id; }

    public String getSender_name() { return sender_name; }
    public void setSender_name(String sender_name) { this.sender_name = sender_name; }

    public String getSender_profile_image() { return sender_profile_image; }
    public void setSender_profile_image(String sender_profile_image) {
        this.sender_profile_image = sender_profile_image;
    }

    public String getMsg_type() { return msg_type; }
    public void setMsg_type(String msg_type) { this.msg_type = msg_type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }

    public List<String> getImage_urls() { return image_urls; }
    public void setImage_urls(List<String> image_urls) { this.image_urls = image_urls; }

    public int getUnread_count() { return unread_count; }
    public void setUnread_count(int unread_count) { this.unread_count = unread_count; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getClient_msg_id() { return client_msg_id; }
    public void setClient_msg_id(String client_msg_id) { this.client_msg_id = client_msg_id; }

    // ── Getter / Setter : 로컬 전용 필드 ────────

    public boolean isIs_mine() { return is_mine; }
    public void setIs_mine(boolean is_mine) { this.is_mine = is_mine; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public List<Uri> getLocalUris() { return localUris; }
    public void setLocalUris(List<Uri> localUris) { this.localUris = localUris; }


    // ── 편의 메서드 ──────────────────────────────

    /** 시스템 메시지 여부 */
    public boolean isSystem() { return "system".equals(msg_type); }

    /** 이미지 메시지 여부 */
    public boolean isImage() { return "image".equals(msg_type); }

    /** 전송 중 여부 */
    public boolean isSending() { return status == STATUS_SENDING; }

    /** 전송 실패 여부 */
    public boolean isFailed() { return status == STATUS_FAILED; }

    /**
     * 어댑터에서 이미지 URL 목록을 가져올 때 항상 이 메서드 사용.
     *
     * 우선순위:
     *   1) image_urls 가 있으면 그대로 반환 (서버 신규 포맷)
     *   2) image_url 만 있으면 단일 원소 리스트로 감싸서 반환 (구버전 하위호환)
     *   3) SENDING 상태이고 localUris 가 있으면 Uri 문자열 리스트 반환 (optimistic 미리보기)
     *   4) 아무것도 없으면 빈 리스트
     */
    public List<String> getImages() {
        // 1) 서버에서 내려온 image_urls
        if (image_urls != null && !image_urls.isEmpty()) {
            return image_urls;
        }
        // 2) 구버전 단일 image_url 폴백
        if (image_url != null && !image_url.isEmpty()) {
            List<String> single = new ArrayList<>();
            single.add(image_url);
            return single;
        }
        // 3) optimistic 미리보기 (로컬 Uri → Glide 가 content:// 로드)
        if (localUris != null && !localUris.isEmpty()) {
            List<String> uriStrings = new ArrayList<>();
            for (Uri uri : localUris) {
                uriStrings.add(uri.toString());
            }
            return uriStrings;
        }
        return new ArrayList<>();
    }
}