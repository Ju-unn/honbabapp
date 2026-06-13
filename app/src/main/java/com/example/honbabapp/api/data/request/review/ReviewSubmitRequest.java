package com.example.honbabapp.api.data.request.review;

/**
 * 후기 제출 요청 바디 DTO
 * POST /api/review/submit.php
 *
 * 요청 예시:
 * {
 *   "meeting_id": 1,
 *   "reviewer_id": 3,
 *   "reviewee_id": 5,
 *   "rating": 4,
 *   "content": "좋은 시간이었어요!"
 * }
 *
 * content 는 선택 입력 — 빈 문자열이면 서버가 NULL로 저장
 */
public class ReviewSubmitRequest {

    private int meeting_id;
    private int reviewer_id;
    private int reviewee_id;
    private int rating;
    private String content;

    public ReviewSubmitRequest(int meeting_id, int reviewer_id, int reviewee_id, int rating, String content) {
        this.meeting_id = meeting_id;
        this.reviewer_id = reviewer_id;
        this.reviewee_id = reviewee_id;
        this.rating = rating;
        this.content = content;
    }

    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public int getReviewer_id() {
        return reviewer_id;
    }

    public void setReviewer_id(int reviewer_id) {
        this.reviewer_id = reviewer_id;
    }

    public int getReviewee_id() {
        return reviewee_id;
    }

    public void setReviewee_id(int reviewee_id) {
        this.reviewee_id = reviewee_id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}