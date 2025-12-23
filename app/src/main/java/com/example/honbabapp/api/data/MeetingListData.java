package com.example.honbabapp.api.data;

import java.util.List;

/**
 * 모임 목록 조회 API 응답 데이터
 *
 * GET /api/meeting/list.php
 *
 * PHP 응답:
 * {
 *   "success": true,
 *   "message": "모임 목록 조회 성공",
 *   "data": {
 *     "meetings": [ ... ],
 *     "total_count": 15,
 *     "page": 1,
 *     "total_pages": 2
 *   }
 * }
 */
public class MeetingListData {
    private List<Meeting> meetings;
    private int total_count;
    private int page;
    private int total_pages;

    // Getters and Setters
    public List<Meeting> getMeetings() {
        return meetings;
    }

    public void setMeetings(List<Meeting> meetings) {
        this.meetings = meetings;
    }

    public int getTotal_count() {
        return total_count;
    }

    public void setTotal_count(int total_count) {
        this.total_count = total_count;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotal_pages() {
        return total_pages;
    }

    public void setTotal_pages(int total_pages) {
        this.total_pages = total_pages;
    }
}