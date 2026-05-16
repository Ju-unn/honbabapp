package com.example.honbabapp.api.data.response.chat;

import com.example.honbabapp.api.data.chat.ChatPhotoData;

import java.util.List;

/**
 * 채팅방 사진 조회 응답 모델
 *
 * GET /api/chat/photos.php 응답의 data 부분:
 * {
 *   "photos": [
 *     { message_id, image_url, sender_id, sender_name, created_at },
 *     ...
 *   ],
 *   "page": 1,
 *   "limit": 30,
 *   "total_count": 0,
 *   "has_more": false
 * }
 *
 * 무한 스크롤 활용:
 * - has_more == true 일 때만 다음 페이지 호출
 * - photos.isEmpty() && total_count == 0 이면 빈 상태 placeholder 표시
 */
public class ChatPhotosResponse {

    private List<ChatPhotoData> photos;
    private int page;
    private int limit;
    private int total_count;
    private boolean has_more;

    public ChatPhotosResponse() {}


    // ── Getter / Setter ──────────────────────────

    public List<ChatPhotoData> getPhotos() { return photos; }
    public void setPhotos(List<ChatPhotoData> photos) { this.photos = photos; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getTotal_count() { return total_count; }
    public void setTotal_count(int total_count) { this.total_count = total_count; }

    public boolean isHas_more() { return has_more; }
    public void setHas_more(boolean has_more) { this.has_more = has_more; }
}