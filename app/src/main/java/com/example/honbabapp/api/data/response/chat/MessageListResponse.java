package com.example.honbabapp.api.data.response.chat;

import com.example.honbabapp.api.data.chat.MessageData;

import java.util.List;

/**
 * 메시지 목록 응답 모델
 *
 * GET /api/chat/messages.php 응답의 data 부분:
 * {
 *   "messages": [...],
 *   "total_count": 150,
 *   "page": 1,
 *   "total_pages": 5
 * }
 */
public class MessageListResponse {

    private List<MessageData> messages;
    private int total_count;
    private int page;
    private int total_pages;

    // 기본 생성자
    public MessageListResponse() {}

    // ── Getter / Setter ──────────────────────────

    public List<MessageData> getMessages() { return messages; }
    public void setMessages(List<MessageData> messages) { this.messages = messages; }

    public int getTotal_count() { return total_count; }
    public void setTotal_count(int total_count) { this.total_count = total_count; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getTotal_pages() { return total_pages; }
    public void setTotal_pages(int total_pages) { this.total_pages = total_pages; }
}